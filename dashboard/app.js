/* Results dashboard: table + SVG charts, zero dependencies.
 *
 * Sections (keep them separate as this file grows into the research base):
 *   LOAD    - fetch data/history.jsonl, parse one JSON object per line.
 *   MODEL   - group rows per map, win rates, averages.
 *   TABLE   - render the per-map summary and the full run list.
 *   CHARTS  - hand-rolled SVG bars (no chart library; offline box).
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

async function loadRows() {
  try {
    const res = await fetch(DATA_URL);
    if (!res.ok) throw new Error("HTTP " + res.status);
    const text = await res.text();
    return text.split("\n")
      .map((l) => l.trim())
      .filter((l) => l.length > 0)
      .map((l) => JSON.parse(l));
  } catch (e) {
    // file:// blocks fetch (CORS): fall back to the snapshot baked by
    // scripts/ums-batch.sh into data/history.js, if present.
    if (typeof window.HISTORY_DATA !== "undefined" && window.HISTORY_DATA.length > 0) {
      return window.HISTORY_DATA;
    }
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
    return {
      map,
      kind: rs[0].kind || "?",
      runs: rs.length,
      wins,
      losses,
      winPct: decided > 0 ? (100 * wins / decided) : null,
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
      `<td class="num">${s.winPct === null ? "–" : s.winPct.toFixed(0) + "%"}</td>` +
      `<td class="num">${fmt(s.avgFrames)}</td>` +
      `<td class="result">${s.last}</td><td>${s.lastDate}</td>`;
    tb.appendChild(tr);
  }
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
      `<td class="num">${fmt(r.duration_s)}</td>`;
    tb.appendChild(tr);
  }
  document.querySelector("#counts").textContent = `${rows.length} runs shown`;
}

/* ---------- CHARTS ---------- */

function renderChart(rows) {
  const div = document.querySelector("#chart");
  div.innerHTML = "";
  const data = summarize(rows).filter((s) => s.wins + s.losses > 0);
  if (data.length === 0) {
    div.textContent = "No decided games yet.";
    return;
  }
  const W = 720, BH = 22, GAP = 6, LABEL = 320, MAXV = 320;
  const H = data.length * (BH + GAP) + 10;
  const NS = "http://www.w3.org/2000/svg";
  const svg = document.createElementNS(NS, "svg");
  svg.setAttribute("width", String(W));
  svg.setAttribute("height", String(H));
  data.forEach((s, i) => {
    const y = 10 + i * (BH + GAP);
    const pct = 100 * s.wins / (s.wins + s.losses);
    const w = Math.round(MAXV * pct / 100);
    const label = document.createElementNS(NS, "text");
    label.setAttribute("x", "4");
    label.setAttribute("y", String(y + BH - 6));
    label.setAttribute("font-size", "12");
    label.textContent = `${s.map} (${s.wins}W/${s.losses}L)`;
    const bg = document.createElementNS(NS, "rect");
    bg.setAttribute("x", String(LABEL));
    bg.setAttribute("y", String(y));
    bg.setAttribute("width", String(MAXV));
    bg.setAttribute("height", String(BH));
    bg.setAttribute("fill", "#eee");
    const bar = document.createElementNS(NS, "rect");
    bar.setAttribute("x", String(LABEL));
    bar.setAttribute("y", String(y));
    bar.setAttribute("width", String(w));
    bar.setAttribute("height", String(BH));
    bar.setAttribute("fill", pct >= 50 ? "#4a4" : "#a44");
    const val = document.createElementNS(NS, "text");
    val.setAttribute("x", String(LABEL + MAXV + 6));
    val.setAttribute("y", String(y + BH - 6));
    val.setAttribute("font-size", "12");
    val.textContent = pct.toFixed(0) + "%";
    svg.append(label, bg, bar, val);
  });
  div.appendChild(svg);
}

/* ---------- main ---------- */

let ALL_ROWS = [];

function applyFilter() {
  const q = document.querySelector("#filter").value.toLowerCase();
  const kind = document.querySelector("#kind").value;
  const rows = ALL_ROWS.filter((r) =>
    (kind === "all" || r.kind === kind) &&
    (q === "" || r.map.toLowerCase().includes(q)));
  renderSummary(rows);
  renderRuns(rows);
  renderChart(rows);
}

async function refresh() {
  try {
    ALL_ROWS = await loadRows();
  } catch (e) {
    document.querySelector("#counts").textContent =
      "No data yet (" + e.message + "). Run scripts/ums-batch.sh first, " +
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

document.querySelector("#reload").addEventListener("click", refresh);
document.querySelector("#filter").addEventListener("input", applyFilter);
document.querySelector("#kind").addEventListener("change", applyFilter);
refresh();
