# Knowledge base — vendored libs and build

Third-party code under `3rdparty/` is vendored as plain files, no submodules:
jbwapi + mjson kept, javajps/jbweb deleted with `JBWEBWrapper` and its call
sites. Vendor policy: `src/main` (+`src/java`) + LICENSE/README only
(`src/test`, `it/`, `bwapistructs/`, `.mvn`, `mvnw*` trimmed).

## Patches we carry (do not lose on vendor refresh)

- `bwapi/Game.java`: `isVisible/isExplored/hasCreepUnsafe` — no-guard direct
  `gameData()` reads for hot paths (bytecode-verified against the old fat jar).
- `bwapi/BWClientConfiguration.java`: public ctor (we configure our own
  instance instead of mutating shared `DEFAULT`; upstream keeps it private).
- `Main.scala`: config via `Builder...build()`; pass
  `Math.max(framesToBuffer, 1)` (`withAsyncFrameBufferCapacity` rejects <1,
  but unbuffered means 0).

## Dependencies

- JNA 5.18.1 (matches upstream jbwapi's own pom; needed for
  `ClientConnectionPosix`'s `Fcntl`/`LibRT`/`Mman`). Our `src/` uses no JNA.
- `no.fiken.oss:junixsocket-common:1.0.2`, scope `provided` (compile-only for
  Posix; old coords = what the offline repo has; never loads at runtime —
  Windows/Wine picks W32). Shade filters drop dead natives.
 - `org.mockito:mockito-core:4.11.0`, scope `test` (stub harness; offline repo has it with byte-buddy + objenesis).
- IntelliJ artifact path uses `$MAVEN_REPOSITORY$` JNA 5.18.1 plus
  junixsocket (matches `pom.xml`); Maven stays source of truth.

## Checks

- `git submodule status` empty; each `*Unsafe` present in `Game.java`.
- `grep -c "commons-lang3\|javajps\|jbweb" pom.xml` = 0; no `jbweb` refs in `src/`.
- `./build-jar.sh` auto-syncs the slot jar; `mvn -o -Ptests test` (MAVEN_OPTS
  in `build-jar.sh:30`); blank-line gate on touched files only (pre-existing
  violations inside vendored jbwapi files are upstream style — leave them).

## Gotchas

- `woundPercent()` is 0.0–1.0 (Atlantis: 0–100); `Maff.minBy` returns Option.
- Keep `cherryvis` (our Debugging/CherryVis imports it) and bwem/bwta.
- `/sc-ai/BOTS/__PlayAsEridan/__GENERIC-RUN-ERIDAN.sh` + wrappers live outside git.
