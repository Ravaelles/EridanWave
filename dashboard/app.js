/* Results dashboard: table + per-row win-rate bars, zero dependencies.
 *
 * Sections (keep them separate as this file grows into the research base):
 *   LOAD    - fetch data/history.jsonl, parse one JSON object per line.
 *   MODEL   - group rows per map, win rates, averages.
 *   TABLE   - render the per-map summary and the full run list.
 *
 * Row schema (written by scripts/ums-batch.sh; melee rows later reuse it):
 *   {date, kind, map, enemy, result, frames, killed, lost, times_hit,
 *    duration_s, errors}
 * result is VICTORY | DEFEAT | TIMEOUT | NOATTACH | UNKNOWN.
 */
"use strict";

const DATA_URL = "data/history.jsonl";
const WIN = new Set(["VICTORY"]);
const LOSS = new Set(["DEFEAT"]);

/* ---------- LOAD ---------- */

function snapshotRows() {
  return typeof window.HISTORY_DATA !== "undefined" ? window.HISTORY_DATA : [];
}

async function loadRows() {
  // file:// blocks fetch (CORS) and Chrome logs the blocked request as an
  // error, so never attempt it there: the history.js snapshot that
  // scripts/dashboard-record.sh writes next to this file is loadable from
  // disk, and the file picker covers a dashboard opened without one.
  if (window.location.protocol === "file:") {
    if (snapshotRows().length > 0) return snapshotRows();
    throw new Error("file:// cannot read data/history.jsonl");
  }
  try {
    const res = await fetch(DATA_URL);
    if (!res.ok) throw new Error("HTTP " + res.status);
    const text = await res.text();
    return text.split("\n")
      .map((l) => l.trim())
      .filter((l) => l.length > 0)
      .map((l) => JSON.parse(l));
  } catch (e) {
    if (snapshotRows().length > 0) return snapshotRows();
    throw e;
  }
}

/* ---------- MODEL ---------- */

function summarize(rows) {
  const byMap = new Map();
  for (const r of rows) {
    if (!byMap.has(r.map)) byMap.set(r.map, []);
    byMap.get(r.map).push(r);
  }
  return [...byMap.entries()].map(([map, rs]) => {
    rs.sort((a, b) => (a.date < b.date ? -1 : 1));
    const wins = rs.filter((r) => WIN.has(r.result)).length;
    const losses = rs.filter((r) => LOSS.has(r.result)).length;
    const decided = wins + losses;
    const frames = rs.map((r) => r.frames).filter((f) => typeof f === "number");
    // Form of the last 5 runs, oldest first: V = win, X = loss,
    // · = undecided (TIMEOUT / NOATTACH / UNKNOWN).
    const last5 = rs.slice(-5).map((r) =>
      WIN.has(r.result) ? "V" : (LOSS.has(r.result) ? "X" : "·"));
    return {
      map,
      kind: rs[0].kind || "?",
      runs: rs.length,
      wins,
      losses,
      winPct: decided > 0 ? (100 * wins / decided) : null,
      last5,
      avgFrames: frames.length > 0
        ? Math.round(frames.reduce((a, b) => a + b, 0) / frames.length)
        : null,
      last: rs[rs.length - 1].result,
      lastDate: rs[rs.length - 1].date,
    };
  }).sort((a, b) => (b.wins + b.losses) - (a.wins + a.losses) || a.map.localeCompare(b.map));
}

function fmt(v) {
  return v === null || v === undefined ? "–" : String(v);
}

/* ---------- TABLE ---------- */

function renderSummary(rows) {
  const tb = document.querySelector("#summary tbody");
  tb.innerHTML = "";
  for (const s of summarize(rows)) {
    const tr = document.createElement("tr");
    const cls = WIN.has(s.last) ? "win" : (LOSS.has(s.last) ? "loss" : "other");
    tr.className = cls;
    tr.innerHTML =
      `<td>${s.map}</td><td>${s.kind}</td>` +
      `<td class="num">${s.runs}</td><td class="num">${s.wins}</td>` +
      `<td class="num">${s.losses}</td>` +
      `<td class="winrate">${winBar(s.winPct)}</td>` +
      `<td class="form">${formHtml(s.last5)}</td>` +
      `<td class="num">${fmt(s.avgFrames)}</td>` +
      `<td class="result">${s.last}</td><td>${s.lastDate}</td>`;
    tb.appendChild(tr);
  }
}

function winBar(winPct) {
  if (winPct === null) return "–";
  const pct = Math.round(winPct);
  const cls = winPct >= 50 ? "fill hi" : "fill lo";
  return `<span class="bar"><span class="${cls}" style="width:${pct}%"></span></span> ` +
    `<span class="num">${pct}%</span>`;
}

function formHtml(last5) {
  return last5.map((m) =>
    m === "V" ? `<span class="fwin">V</span>`
      : m === "X" ? `<span class="floss">X</span>`
        : `<span class="fother">·</span>`).join(" ");
}

function renderRuns(rows) {
  const tb = document.querySelector("#runs tbody");
  tb.innerHTML = "";
  const sorted = [...rows].sort((a, b) => (a.date < b.date ? 1 : -1));
  for (const r of sorted) {
    const tr = document.createElement("tr");
    tr.className = WIN.has(r.result) ? "win" : (LOSS.has(r.result) ? "loss" : "other");
    tr.innerHTML =
      `<td>${r.date}</td><td>${r.map}</td><td class="result">${r.result}</td>` +
      `<td class="num">${fmt(r.frames)}</td><td class="num">${fmt(r.killed)}</td>` +
      `<td class="num">${fmt(r.lost)}</td><td class="num">${fmt(r.times_hit)}</td>` +
      `<td class="num">${fmt(r.duration_s)}</td>` +
      `<td class="actions"><button class="trash" title="Remove this run from the dashboard" ` +
      `data-key="${encodeURIComponent(runKey(r))}">\u{1F5D1}</button></td>`;
    tb.appendChild(tr);
  }
  renderCounts(rows);
}

function renderCounts(rows) {
  const hidden = hiddenRunKeys().size;
  const counts = document.querySelector("#counts");
  counts.textContent = `${rows.length} runs shown` + (hidden > 0 ? ` (${hidden} removed)` : "");
  const restore = document.querySelector("#restore");
  restore.style.display = hidden > 0 ? "inline" : "none";
  restore.textContent = `Restore ${hidden} removed`;
}

/* ---------- main ---------- */

let ALL_ROWS = [];

/* A run the owner removed from the dashboard. The history file itself is
 * never touched (the browser cannot rewrite it); the key lives in
 * localStorage, so the removal survives reloads in this browser. */
const HIDDEN_KEY = "eridanwave.dashboard.removedRuns";

function runKey(r) {
  return [r.date, r.map, r.result, r.frames].join("|");
}

function hiddenRunKeys() {
  try {
    return new Set(JSON.parse(localStorage.getItem(HIDDEN_KEY) || "[]"));
  } catch (e) {
    return new Set();
  }
}

function saveHiddenRunKeys(keys) {
  localStorage.setItem(HIDDEN_KEY, JSON.stringify([...keys]));
}

function removeRun(key) {
  const keys = hiddenRunKeys();
  keys.add(key);
  saveHiddenRunKeys(keys);
  applyFilter();
}

function restoreRemovedRuns() {
  saveHiddenRunKeys(new Set());
  applyFilter();
}

function applyFilter() {
  const q = document.querySelector("#filter").value.toLowerCase();
  const kind = document.querySelector("#kind").value;
  const hidden = hiddenRunKeys();
  const rows = ALL_ROWS.filter((r) =>
    !hidden.has(runKey(r)) &&
    (kind === "all" || r.kind === kind) &&
    (q === "" || r.map.toLowerCase().includes(q)));
  renderSummary(rows);
  renderRuns(rows);
}

async function refresh() {
  try {
    ALL_ROWS = await loadRows();
  } catch (e) {
    document.querySelector("#counts").textContent =
      "No data yet (" + e.message + "). Play a game through scripts/run.sh, " +
      "serve over HTTP, or pick the file below.";
    document.querySelector("#localfile").style.display = "block";
    return;
  }
  document.querySelector("#localfile").style.display = "none";
  applyFilter();
}

function loadFile(file) {
  const reader = new FileReader();
  reader.onload = () => {
    try {
      ALL_ROWS = String(reader.result).split("\n")
        .map((l) => l.trim())
        .filter((l) => l.length > 0)
        .map((l) => JSON.parse(l));
      applyFilter();
    } catch (e) {
      document.querySelector("#counts").textContent = "Could not parse file: " + e.message;
    }
  };
  reader.readAsText(file);
}

document.querySelector("#filepicker").addEventListener("change", (ev) => {
  if (ev.target.files.length > 0) loadFile(ev.target.files[0]);
});

// Trash button per run row: event delegation, so re-rendering never stacks
// listeners on fresh rows.
document.querySelector("#runs").addEventListener("click", (ev) => {
  const button = ev.target.closest("button.trash");
  if (button) removeRun(decodeURIComponent(button.dataset.key));
});

document.querySelector("#restore").addEventListener("click", restoreRemovedRuns);

document.querySelector("#reload").addEventListener("click", refresh);
document.querySelector("#filter").addEventListener("input", applyFilter);
document.querySelector("#kind").addEventListener("change", applyFilter);
refresh();
