# Project Status

**Last Updated**: 2026-03-01

## Current Snapshot

- ✅ **Build Health**: `BUILD SUCCESSFUL`
- ✅ **Test Health**: Full `./gradlew test` suite is green (detailed counts stay in CI output, not in this file)
- ✅ **Runtime Baseline**: Java 21 + JavaFX 21 + JPMS are in place
- ✅ **Delivery Baseline**: Demo app, CI workflows, tag-based releases, and release checksums are operational
- ✅ **Release Readiness Focus**: `0.8.x` readiness is complete; active focus is `0.9.x` rehearsal/freeze before `v1.0.0`
- 🚧 **Maintainability Refactor Track**: `WS-1` started; shortcut binding management was extracted from `SnapFX` into `org.snapfx.shortcuts.DockShortcutController` (with `DockShortcutAction` in the same package) as the first decomposition slice

## Current Capability Baseline

- ✅ **Core Docking**: Stable model/view separation with drag-and-drop, hide/restore, and lock mode
- ✅ **Floating Windows**: Detach/attach, snapping, persistence, and always-on-top behavior
- ✅ **Side Bars**: Current scope completed (DnD strip baseline, built-in move/restore/pin actions, resize + persistence, visibility modes)
- ✅ **Theming**: Default stylesheet auto-apply plus runtime theme switching (`Light` / `Dark`)
- ✅ **Manual Verification**: MainDemo settings keep API-to-settings parity for framework features

## Open Items

- ⚠️ **Performance**: Benchmark trend tracking for large layouts is not implemented
- ⚠️ **Memory**: Automated heap profiling in CI is not implemented
- ⚠️ **UI**: Global interaction animations are still pending (outside About dialog easter egg)
- ⚠️ **UI Extensibility**: Custom context-menu item API is not implemented yet
- ⚠️ **Docs Strategy**: Multi-version docs/API publication remains intentionally deferred until after `v1.0.0`

## Document Boundaries

- ✅ [STATUS.md](STATUS.md): Current snapshot only (health, focus, open items)
- ✅ [DONE.md](DONE.md): Completed milestones only (no granular history)
- ✅ [ROADMAP.md](ROADMAP.md): Planned/proposed work only
- ✅ [CHANGELOG.md](CHANGELOG.md): Versioned history only
- ✅ [TESTING_POLICY.md](TESTING_POLICY.md): Stable testing rules only

## Maintenance Rule

- ⚠️ Keep this file concise: no commit-style bullets, no changelog history, and no per-test-class/test-count breakdowns
