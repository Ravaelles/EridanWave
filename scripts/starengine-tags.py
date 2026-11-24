#!/usr/bin/env python3
"""StarEngine tag catalog: what topics exist and which scenarios prove them.

Parses StarEngineTags.scala for the registered vocabulary and every
*Suite.scala for StarEngineTags.X uses (per-test tags and suite-level
`+ StarEngineTags.X.name` overrides). Prints the catalog tag -> suites,
then fails on: a suite file with no topical tag, a registered tag with
no suite. Usage: python3 scripts/starengine-tags.py
"""
import os
import re
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..",
                    "tests-starengine", "scala", "starengine")

TAG_DEF = re.compile(r"object (\w+) extends Tag\(\"starengine\.([\w]+)\"\)")
TAG_USE = re.compile(r"StarEngineTags\.(\w+)")


def main():
    with open(os.path.join(ROOT, "StarEngineTags.scala"), encoding="utf-8") as f:
        defined = dict(TAG_DEF.findall(f.read()))
    by_tag = {obj: [] for obj in defined}
    untagged = []
    for name in sorted(os.listdir(ROOT)):
        if not name.endswith("Suite.scala"):
            continue
        if name in ("StarEngineSuite.scala",):
            continue  # infra trait, not a scenario
        with open(os.path.join(ROOT, name), encoding="utf-8") as f:
            src = f.read()
        used = sorted(set(TAG_USE.findall(src)) - {"StarEngineTags"})
        # Drop the self-import line of the registry file itself.
        if name == "StarEngineTags.scala":
            continue
        if not used:
            untagged.append(name)
        for obj in used:
            by_tag.setdefault(obj, []).append(name)
    print("== tag catalog ==")
    for obj in sorted(by_tag, key=str.lower):
        suites = sorted(set(by_tag[obj]))
        mark = "" if suites else "  <-- UNUSED"
        print("  %-12s %2d suite(s)%s" % (obj, len(suites), mark))
        for s in suites:
            print("                   %s" % s)
    errors = []
    if untagged:
        errors.append("untagged suites: %s" % ", ".join(untagged))
    unused = [o for o, s in by_tag.items() if not s]
    if unused:
        errors.append("unused tags: %s" % ", ".join(unused))
    unknown = [o for o in by_tag if o not in defined]
    if unknown:
        errors.append("unknown tags used: %s" % ", ".join(unknown))
    if errors:
        print("FAIL:")
        for e in errors:
            print("  " + e)
        return 1
    print("OK: %d tags, %d suites, everything tagged and used."
          % (len(defined), sum(1 for _ in os.listdir(ROOT)
                               if _.endswith("Suite.scala"))))
    return 0


if __name__ == "__main__":
    sys.exit(main())
