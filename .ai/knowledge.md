# Knowledge base - vendored libs and build

Third-party code under `3rdparty/` is vendored as plain files, no
submodules: jbwapi + mjson kept, javajps/jbweb deleted with
`JBWEBWrapper` and its call sites. Vendor policy: `src/main`
(+`src/java`) + LICENSE/README only.

## Patches we carry (do not lose on vendor refresh)

- `bwapi/Game.java`: `isVisible/isExplored/hasCreepUnsafe` - no-guard
  direct `gameData()` reads for hot paths.
- `bwapi/BWClientConfiguration.java`: public ctor.
- `Main.scala`: config via `Builder...build()`; pass
  `Math.max(framesToBuffer, 1)`.

## Dependencies

- JNA 5.18.1 (matches upstream jbwapi); our `src/` uses no JNA.
- `no.fiken.oss:junixsocket-common:1.0.2`, scope `provided`.
- `org.mockito:mockito-core:4.11.0`, scope `test`.
- IntelliJ artifact path uses `$MAVEN_REPOSITORY$` JNA + junixsocket;
  Maven stays source of truth.

## Checks

- `git submodule status` empty; each `*Unsafe` present in `Game.java`.
- No `commons-lang3|javajps|jbweb` in `pom.xml`; no `jbweb` in `src/`.
- `./build-jar.sh` auto-syncs the slot jar; blank-line gate on
  touched files only (vendored style stays upstream).

## Gotchas

- `woundPercent()` is 0.0-1.0 (Atlantis: 0-100); `Maff.minBy`
  returns Option.
- Keep `cherryvis` (Debugging/CherryVis imports it) and bwem/bwta.
- `/sc-ai/BOTS/__PlayAsEridan/__GENERIC-RUN-ERIDAN.sh` + wrappers
  live outside git.
