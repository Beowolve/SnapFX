# SnapFX Framework - Completed Milestones

**Last Updated**: 2026-04-29

## Product Baseline

- ✅ **Core Docking Delivered**: Stable docking model/view architecture with drag-and-drop, hide/restore, and lock mode
- ✅ **Floating Workflows Delivered**: Detach/attach, title-bar actions, always-on-top, drag+resize snapping, and persistence
- ✅ **Sidebar Interaction Scope Delivered**: Strip DnD baseline, framework/sidebar move/restore/pin actions, resize + persistence, and visibility modes
- ✅ **Persistence and Recovery Delivered**: Save/load pipeline with typed load errors and unknown-node fallback recovery
- ✅ **Theme Runtime Delivered**: Default stylesheet auto-apply, runtime theme switching (`Light` / `Dark`), plus user-agent integration controls (`AUTO` / `MODENA` / `ATLANTAFX_COMPAT`) with optional AtlantaFX compatibility and no hard core dependency
- ✅ **Localization Runtime Delivered**: Built-in `EN`/`DE` locale support, runtime locale/provider API, provider fallback chain, ResourceBundle adapter extensibility, and a fully localized MainDemo showcase with demo-specific `EN`/`DE`/`FR` content

## Engineering Baseline

- ✅ **Platform Baseline Delivered**: Java 21, JavaFX 21, and JPMS module boundaries
- ✅ **Project Structure Delivered**: `snapfx-core` / `snapfx-demo` split with aligned package/module namespace
- ✅ **Quality Baseline Delivered**: Automated tests with regression-test policy and CI stability guards
- ✅ **Release Pipeline Delivered**: Tag-driven releases, cross-platform demo packaging, and release checksums
- ✅ **Maven Central Readiness Delivered**: Sonatype wiring and publish-policy gating for stable releases

## Documentation Baseline

- ✅ **Documentation Split Delivered**: `STATUS` (current), `ROADMAP` (planned), `DONE` (milestones), `CHANGELOG` (history), `TESTING_POLICY` (rules)
- ✅ **Public Documentation Delivered**: Docusaurus docs and bundled API JavaDoc at `https://snapfx.org/` and `/api`
- ✅ **ADR Practice Delivered**: Significant architecture decisions captured under `docs/adr/`
- ✅ **Manual Verification Support Delivered**: MainDemo API-to-settings parity baseline, including simplified theme-source switching (`Internal` / `AtlantaFX`) with source-aware theme selection

## Scope Rule

- ⚠️ This file stays milestone-oriented only; detailed history belongs in [CHANGELOG.md](CHANGELOG.md)
