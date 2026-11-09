#!/usr/bin/env python3
"""Blank-line gate around `return`, ported from Atlantis tools/checkstyle.xml.

Reads rules (name, format regex, message) from tools/blank-lines.xml and
reports violations as file:line: message. Covers both .scala and .java
(Checkstyle itself is Java-only, so this script is our equivalent tool
for the mixed tree).

Usage:
  python3 scripts/check-blank-lines.py [paths...]            # default: src/
  python3 scripts/check-blank-lines.py --apply [paths...]    # insert the
    missing blank lines, then re-check (only ever ADDS blank lines)
Exit 0 when clean, 1 on any violation, 2 on tooling problems.

Like its Atlantis model, --apply fixes exactly what the rules report and
re-validates each site (a real `return` keyword outside strings/comments)
before touching the file. Never run it on the whole tree: legacy files
carry pre-existing violations on purpose (convention 8) - apply only to
files you touched.
"""
import glob
import re
import sys
import xml.etree.ElementTree as ET

REPO = __import__("pathlib").Path(__file__).resolve().parent.parent
CONFIG = REPO / "tools" / "blank-lines.xml"
EXTENSIONS = (".scala", ".java")


def load_rules(path):
    root = ET.parse(str(path)).getroot()
    rules = []
    for rule in root.findall("rule"):
        props = {p.get("name"): p.get("value") for p in rule.findall("property")}
        rules.append((rule.get("name"), re.compile(props["format"], re.M), props["message"]))
    return rules


def check_file(path, rules):
    text = open(path, encoding="utf-8", errors="replace").read()
    hits = []
    for name, pattern, message in rules:
        for match in pattern.finditer(text):
            line = text.count("\n", 0, match.start()) + 1
            hits.append((line, name, message))
    return sorted(hits)


def collect_files(targets):
    import os
    files = []
    for target in targets:
        if os.path.isfile(target):
            files.append(target)
        else:
            files.extend(
                f for f in glob.glob(target + "/**", recursive=True)
                if f.endswith(EXTENSIONS))
    return files


def code_spans(text):
    """Map each 1-based line number to code ranges, skipping // comments,
    /* */ blocks, "..." strings (Scala/Java), '...' char literals and
    Scala triple-quoted blocks. Mirrors the Atlantis --apply validator."""
    spans = {}
    i, n = 0, len(text)
    line, seg_start = 1, 0
    in_block, in_string, in_char, in_triple = False, False, False, False

    def close_seg(end):
        spans.setdefault(line, []).append((seg_start, end))

    while i < n:
        c = text[i]
        nxt = text[i + 1] if i + 1 < n else ""
        nxt2 = text[i + 2] if i + 2 < n else ""
        if c == "\n":
            if not (in_block or in_string or in_char or in_triple):
                close_seg(i)
            line += 1
            seg_start = i + 1
            i += 1
            continue
        if in_triple:
            if c == '"' and nxt == '"' and nxt2 == '"':
                in_triple = False
                i += 3
                seg_start = i
                continue
            i += 1
            continue
        if in_string:
            if c == "\\":
                i += 2
                continue
            if c == '"':
                in_string = False
                seg_start = i + 1
            i += 1
            continue
        if in_char:
            if c == "\\":
                i += 2
                continue
            if c == "'":
                in_char = False
                seg_start = i + 1
            i += 1
            continue
        if in_block:
            if c == "*" and nxt == "/":
                in_block = False
                i += 2
                seg_start = i
                continue
            i += 1
            continue
        if c == "/" and nxt == "/":
            close_seg(i)
            while i < n and text[i] != "\n":
                i += 1
            seg_start = i
            continue
        if c == "/" and nxt == "*":
            close_seg(i)
            in_block = True
            i += 2
            continue
        if c == '"' and nxt == '"' and nxt2 == '"':
            close_seg(i)
            in_triple = True
            i += 3
            continue
        if c == '"':
            close_seg(i)
            in_string = True
            i += 1
            continue
        if c == "'":
            close_seg(i)
            in_char = True
            i += 1
            continue
        i += 1
    if not (in_block or in_string or in_char or in_triple):
        close_seg(n)
    return spans


def has_real_return(text, spans, lineno):
    for start, end in spans.get(lineno, []):
        if re.search(r"\breturn\b", text[start:end]):
            return True
    return False


def apply_fixes(files, rules):
    fixed_files, fixed_sites, skipped = 0, 0, 0
    for path in sorted(files):
        try:
            with open(path, "rb") as handle:
                raw = handle.read()
        except OSError as error:
            print("  [apply] SKIP %s: cannot read (%s)" % (path, error))
            continue
        try:
            text = raw.decode("utf-8")
        except UnicodeDecodeError:
            print("  [apply] SKIP %s: not UTF-8" % path)
            continue
        crlf = "\r\n" in text
        body = text.replace("\r\n", "\n")
        try:
            spans = code_spans(body)
        except Exception:
            print("  [apply] SKIP %s: cannot scan" % path)
            continue
        rows = body.split("\n")
        hits = []
        for _name, pattern, _message in rules:
            for match in pattern.finditer(body):
                hits.append((body.count("\n", 0, match.start()) + 1, _name))
        inserts = set()
        for lineno, name in hits:
            if name == "ReturnFollowedByBlank":
                if not has_real_return(body, spans, lineno):
                    print("  [apply] SKIP %s:%d: no real return outside strings/comments" % (path, lineno))
                    skipped += 1
                    continue
                if lineno < len(rows) and rows[lineno].strip() == "":
                    continue  # already blank below (idempotent)
                inserts.add(lineno)
            elif name == "FinalReturnPrecededByBlank":
                # Reported on the setup line; the return sits on the next
                # line, the closing brace on the one after.
                setup, ret = lineno, lineno + 1
                if ret > len(rows) or rows[setup - 1].strip() == "":
                    print("  [apply] SKIP %s:%d: setup line not code" % (path, lineno))
                    skipped += 1
                    continue
                if not has_real_return(body, spans, ret):
                    print("  [apply] SKIP %s:%d: no real return on next line" % (path, ret))
                    skipped += 1
                    continue
                if ret + 1 > len(rows) or not rows[ret].lstrip().startswith("}"):
                    print("  [apply] SKIP %s:%d: next line is not a closing brace" % (path, ret))
                    skipped += 1
                    continue
                inserts.add(lineno)
        changed = 0
        for idx in sorted(inserts, reverse=True):
            rows.insert(idx, "")
            changed += 1
        if changed:
            out = ("\r\n" if crlf else "\n").join(rows)
            with open(path, "wb") as handle:
                handle.write(out.encode("utf-8"))
            print("  [apply] %s: %d blank line(s) inserted" % (path, changed))
            fixed_files += 1
            fixed_sites += changed
    # Re-check: the fixer is only done when the gate agrees.
    violations = 0
    for path in sorted(files):
        for line, name, message in check_file(path, load_rules(CONFIG)):
            print("%s:%d: [%s] %s" % (path, line, message))
            violations += 1
    print("  [apply] done: %d insertion(s) in %d file(s), %d skipped; %d violation(s) remain"
          % (fixed_sites, fixed_files, skipped, violations))
    return 1 if violations else 0


def main(argv):
    args = argv[1:]
    apply = "--apply" in args
    targets = [a for a in args if a != "--apply"] or ["src"]
    try:
        rules = load_rules(CONFIG)
    except Exception as error:
        print("cannot load rules from %s: %s" % (CONFIG, error))
        return 2
    files = collect_files(targets)
    if apply:
        return apply_fixes(files, rules)
    violations = 0
    for path in sorted(files):
        for line, name, message in check_file(path, rules):
            print("%s:%d: [%s] %s" % (path, line, name, message))
            violations += 1
    if violations:
        print("%d blank-line violation(s) in %d file(s)" % (violations, len(files)))
        return 1
    print("blank lines OK in %d file(s)" % len(files))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
