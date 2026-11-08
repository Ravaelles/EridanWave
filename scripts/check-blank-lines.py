#!/usr/bin/env python3
"""Blank-line gate around `return`, ported from Atlantis tools/checkstyle.xml.

Reads rules (name, format regex, message) from tools/blank-lines.xml and
reports violations as file:line: message. Check-only, like its Atlantis
model: fixes stay manual, one blank line at a time.

Usage:
  python3 scripts/check-blank-lines.py [paths...]   # default: src/
Exit 0 when clean, 1 on any violation.
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


def main(argv):
    targets = argv[1:] or ["src"]
    rules = load_rules(CONFIG)
    files = collect_files(targets)
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
