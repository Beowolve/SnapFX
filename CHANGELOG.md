# Changelog

All notable changes to this project are documented in this file.

The changelog is grouped by release tags (`vX.Y.Z`) and includes an `Unreleased` section for changes after the latest tag.

## Unreleased

### Build and Test
- ✅ Fixed demo jpackage app-version normalization for macOS runners by enforcing a major-version floor of `1` (for example `0.4.1-*` becomes `1.4.1` for packaging), resolving `jpackageImage` failures in the release matrix.
- ✅ Expanded the release workflow into a multi-job pipeline with a Windows/macOS/Linux matrix for demo `jpackage` ZIP assets, and centralized final release publishing from aggregated artifacts.
- ✅ Added `org.beryx.jlink` demo packaging baseline in `snapfx-demo` with `jlink`, `jpackageImage`, and `packageJPackageImageZip`, including jpackage-safe version normalization for git-derived dev versions and OS-specific app icon selection (`snapfx.ico`/`snapfx.icns`/`snapfx.png`).
- ✅ Aligned release workflow build tasks and artifact upload paths with split modules (`:snapfx-demo:distZip`/`:distTar`, `snapfx-demo/build/distributions/*`, `snapfx-core/build/libs/*`).
- ✅ Fixed CI stability-check filters after the core/demo split by targeting `:snapfx-core:test` with `org.snapfx...` test classes.
- ✅ Added `DockDebugOverlayTest` coverage for HUD layout defaults (managed pref-sized panel + background presence) and live target/zone diagnostics refresh when drag data mutates in place.
- ✅ Hardened `SnapFXTest` sidebar overlay width/resize-handle tests against a CI-only async sidebar rebuild race by removing an unnecessary prebuild step before reflective sidebar icon clicks.
- ✅ Simplified `MarkdownDocumentationConsistencyTest` to keep non-brittle guardrails only (Mojibake detection and markdown status-bullet icon-prefix checks), removing content-specific wording assertions.
- ✅ Split the Gradle project into `snapfx-core` (framework code/resources/tests) and `snapfx-demo` (demo app/resources/tests) modules, and updated the markdown consistency test to resolve markdown files from the repository root after the test move.
- ✅ Switched the Gradle `groupId` baseline to `org.snapfx` after registering the project domain (`snapfx.org`), preparing Maven Central publishing coordinates for `snapfx-core`.
- ✅ Renamed Java package and JPMS module namespaces from `com.github.beowolve.snapfx...` to `org.snapfx...` across core/demo sources and tests (including demo launch configuration and documentation references).
- ✅ Added a `snapfx-core` Maven publish dry-run baseline (`maven-publish`, `sourcesJar`, `javadocJar`, POM metadata for `snapfx.org`/MIT/SCM/developer) and verified local publication with `:snapfx-core:publishToMavenLocal`.

### Framework and UI
- ✅ Fixed `DockDebugOverlay` HUD rendering and diagnostics behavior used by MainDemo debugging: the HUD panel now lays out correctly with visible background (no top-left clipping), and target/zone text updates live during active drags instead of staying at `none`.
- ✅ Improved `DockDebugOverlay` target diagnostics text to show the dragged hover target dock-node title for `DockNode` targets (instead of the generic `DockNode` type label).
- ✅ Kept the MainDemo `DockDebugOverlay` HUD as a local debug-toggle option after the overlay fixes (no default-on requirement).

### Documentation
- ✅ Added an explicit release-readiness version lane plan (`0.5.x` to `0.9.x`) and documented the policy split between continuous release readiness (`0.x`) and controlled public launch (`1.0.0`).
- ✅ Refined demo smoke validation guidance in `RELEASING.md` to a pragmatic policy: required local-OS smoke per RC, cross-OS checks as nice-to-have, per-OS start commands/checklist, and optional CI startup-smoke scope.
- ✅ Updated roadmap/status/done docs to mark the `DockDebugOverlay` HUD fixes follow-up as completed (while noting the MainDemo HUD remains opt-in) and return current priority focus to the Phase 3 UX backlog.
- ✅ Started the `0.5.x` JavaDoc completion track with a broad first-pass remediation across `snapfx-core` public API classes (including model/view/dnd/floating/debug surfaces), plus follow-up roadmap/status tracking for the remaining warning backlog.
- ✅ Closed the `0.5.x` JavaDoc baseline by making `:snapfx-core:javadoc --rerun-tasks` warning-free and tightening AGENTS workflow rules to require complete JavaDoc updates for every new/changed public/protected API element in the same change.

## v0.4.0 - 2026-02-25

### Build and Test
- ✅ Added `DockGraphTest` coverage for sidebar panel width defaults/validation/lock behavior.
- ✅ Added `DockLayoutSerializerTest` coverage for sidebar panel width roundtrip persistence and legacy missing-width fallback behavior.
- ✅ Added `DockGraphTest` coverage for index-based sidebar insertion, same-side reorder, cross-side moves, clamped insertion bounds, and lock-mode no-op behavior (foundation for exact-position sidebar DnD).
- ✅ Added `SnapFXTest` coverage for sidebar strip-target DnD insertion/reordering, sidebar insert-preview visibility, sidebar strip icon drag wiring, and pinned-sidebar-node source handling in resolved/unresolved drop paths (main-layout drop + float fallback).
- ✅ Added `SnapFXTest` coverage for sidebar strip/panel context menus (restore, move-to-other-side, pin-panel toggle, and lock-aware disabled state).
- ✅ Added `SnapFXTest` coverage for sidebar panel width API behavior, save/load roundtrips, rendered panel widths in pinned/overlay modes, resize-handle presence, and runtime width clamping.
- ✅ Added `DockLayoutEngineTest` and `DockFloatingWindowTest` coverage for framework sidebar move context-menu actions (header/tab menu callbacks, lock-aware disabling, and floating-layout callback forwarding).
- ✅ Added `MainDemoTest` coverage for the temporary MainDemo DockDebugOverlay HUD disable switch used while sidebar interaction work is in progress.
- ✅ Added `MainDemoTest` coverage for sidebar Settings width controls (API-to-settings parity wiring).
- ✅ Added `MainDemoTest` regression coverage to keep the MainDemo outer Debug/Settings split divider stable while dock layouts rebuild.
- ✅ Added `SnapFXTest` coverage for `DockSideBarMode` defaults and sidebar rendering behavior (`AUTO`, `ALWAYS`, `NEVER`), including hidden-sidebar state preservation under `NEVER`.
- ✅ Added `DockLayoutEngineTest` coverage to hide framework `Move to Left/Right Sidebar` context-menu items when sidebar move callbacks are unavailable (used by `DockSideBarMode.NEVER`).
- ✅ Added `MainDemoTest` coverage for the new sidebar mode Settings control (`DockSideBarMode`) and SnapFX API parity wiring.
- ✅ Added `SnapFXTest` regression coverage for right sidebar overlay resize-handle pick/z-order behavior and width updates in unpinned mode.

### Framework and UI
- ✅ Added `DockGraph.pinToSideBar(DockNode, Side, int)` for deterministic sidebar insertion/reordering with clamped index handling, enabling upcoming sidebar DnD drop-position support.
- ✅ Added a Phase-D sidebar DnD baseline in `SnapFX`: unresolved drag releases can now drop into visible sidebar icon strips with exact insertion index resolution, sidebar strip icons now act as drag sources (including drag-outside fallback), the strip renders a visible insert-position line during drag hover, and pinned sidebar nodes are handled correctly when dragged back to the main layout, into floating windows, or into float fallback.
- ✅ Added framework `Move to Left Sidebar` / `Move to Right Sidebar` actions to dock-node header and tab context menus (including floating layouts), wired to the existing SnapFX pin-to-sidebar flow with lock-aware disabling.
- ✅ Added framework sidebar-node context menus on sidebar strip icons and expanded sidebar panel headers with built-in `Restore from Sidebar`, `Move to Left/Right Sidebar`, and `Pin/Unpin Sidebar Panel` actions (lock-aware).
- ✅ Added per-side resizable sidebar panel widths in `SnapFX` (shared by pinned and overlay modes) with runtime clamping, resize handles, `SnapFX` width APIs, `DockGraph` width state, and `DockLayoutSerializer` persistence (`panelWidth`).
- ✅ Added `DockSideBarMode` (`ALWAYS` / `AUTO` / `NEVER`) to SnapFX and MainDemo Settings, with empty-strip rendering in `ALWAYS` and framework sidebar move context-menu suppression in `NEVER`.
- ✅ Temporarily disabled the MainDemo `DockDebugOverlay` D&D HUD while sidebar interaction work continues; a post-sidebar fix/re-enable follow-up is tracked for background rendering, top-left clipping, and incorrect `none` diagnostics text.
- ✅ Fixed MainDemo debug/settings split divider jumps during dock-layout rebuilds by updating dock content inside a persistent split-host container instead of replacing the split item.
- ✅ Fixed unpinned right-sidebar overlay resizing by making resize handles explicitly bounds-pickable and rendering them above overlay panel chrome/shadow (prevents right-side hit-target occlusion).

### Documentation
- ✅ Re-scoped the next sidebar Phase 2 work from hover auto-hide to higher-value interaction parity (sidebar DnD, framework context-menu actions, and resizable sidebar widths) in planning/status docs.
- ✅ Updated planning/status docs to mark the sidebar DnD strip-target baseline (including strip-icon drag source + insert-position line feedback) as completed and narrow the remaining Phase 2 DnD parity work.
- ✅ Updated planning/status docs to mark the framework sidebar move context-menu baseline as completed and narrow the remaining context-menu scope to sidebar restore/context parity plus resize work.
- ✅ Updated planning/status docs to mark sidebar strip/panel context-menu parity as completed and re-focus remaining Phase 2 work on DnD parity/polish plus resizable sidebar widths.
- ✅ Updated planning/status docs to mark sidebar width resize/persistence as completed and re-focus remaining Phase 2 work on DnD parity/polish only; added architecture/ADR documentation for sidebar width state and runtime clamping.
- ✅ Added a post-sidebar `DockDebugOverlay` fix/re-enable follow-up to roadmap/status docs and documented the temporary MainDemo HUD disable.
- ✅ Refreshed `docs/images/main-demo.png` after temporarily disabling the MainDemo `DockDebugOverlay` HUD during sidebar work.
- ✅ Updated planning/status docs to note the MainDemo debug/settings split divider stability fix while sidebar work continues.
- ✅ Updated planning/status docs to track the right-overlay sidebar resize-handle reliability fix during Phase 2 sidebar interaction work.
- ✅ Marked sidebar interaction parity (Phase 2 current scope) as complete after adding `DockSideBarMode`, and deferred broader panel-surface sidebar DnD targeting/preview extensions to optional backlog items.
- ✅ Documented sidebar visibility mode and framework sidebar-menu gating behavior in [ARCHITECTURE.md](ARCHITECTURE.md) and ADR [docs/adr/0005-sidebar-visibility-mode-and-framework-menu-gating.md](docs/adr/0005-sidebar-visibility-mode-and-framework-menu-gating.md).

## v0.3.0 - 2026-02-24

### Build and Test
- ✅ Added `SnapFXTest` build-layout structure coverage for framework sidebar rendering (collapsed strip vs. pinned-open panel states), regression coverage for configurable active-icon collapse behavior in pinned sidebars, and a sidebar-restore placement regression test covering tab-parent collapse fallback; added `DockGraphTest` coverage for `unpinFromSideBar(...)`; updated sidebar menu-title helper assertions for the new pinned/collapsed wording; full suite now runs with 278 tests.
- ✅ Added `DockGraphTest` coverage for pinned side-bar model behavior (pin/restore, side switching, lock-mode no-op behavior, and dock-node counting with pinned entries).
- ✅ Added `DockLayoutSerializerTest` coverage for side-bar persistence roundtrips (sidebar pinned-open state, pinned entries, restore-anchor roundtrip behavior, and sidebar-only layouts); full suite now runs with 267 tests.
- ✅ Added `SnapFXTest` coverage for the new side-bar facade APIs (pin/restore, pinned-open behavior under lock, and save/load roundtrip preservation via `SnapFX.saveLayout(...)` / `loadLayout(...)`).
- ✅ Added `MainDemoTest` coverage for sidebar menu/list label helpers used by the new Phase-C manual test controls; full suite now runs with 272 tests.
- ✅ Added `MainDemoTest` coverage for the `MainDemo` FileChooser helper refactor (shared layout/editor chooser builders and editor save-default resolution), plus theme-resource coverage for runtime theme switching in settings.
- ✅ Added `SnapFXTest` coverage for automatic default stylesheet application during `initialize(...)` and runtime theme switching via the new theme stylesheet API (including floating-scene propagation and invalid-resource validation).
- ✅ Added named-theme catalog coverage (`Light`/`Dark`) in `SnapFXTest` and `MainDemoTest` (named map/list exposure plus fallback name resolution), and validated the refactored theme helpers after extraction from `SnapFX`.
- ✅ Removed obsolete `SimpleExampleTest` stylesheet checks after framework-managed default stylesheet wiring, and moved `DockFloatingWindowTest` into the `floating` package for test-structure alignment; full suite now runs with 259 tests.

### Framework and UI
- ✅ Pre-release sidebar naming cleanup (without compatibility aliases): renamed API/model methods from ambiguous `show/hide/is...Visible` semantics to `pinOpen...`, `collapse...`, and `is...PinnedOpen`, and renamed serialized sidebar state key from `visible` to `pinnedOpen`.
- ✅ `SnapFX.restoreFromSideBar(...)` now reuses the same placement-memory restore pipeline as floating attach (preferred + neighbor anchors + fallback), fixing sidebar restore misplacement cases when pinning collapses the original parent container.
- ✅ Added `DockGraph.unpinFromSideBar(...)` to support higher-level restore strategies (for example `SnapFX` placement-memory restore) without forcing immediate model-level fallback docking.
- ✅ Added `SnapFX` option `setCollapsePinnedSideBarOnActiveIconClick(...)` (default `true`) so pinned-side-panel active-icon click collapse behavior is configurable; MainDemo exposes it in the Settings tab.
- ✅ Fixed pinned active-icon collapse so it now preserves pin mode (temporary collapse only) instead of switching the sidebar entry back to overlay/unpinned behavior.
- ✅ Fixed Phase-C sidebar interaction polish regressions: newly pinned nodes now stay collapsed by default, overlay hit-testing no longer blocks sidebar icon/panel buttons, right-side overlay panels keep the correct side on unpin, and outside-click overlay close works consistently across the main scene.
- ✅ Renamed MainDemo sidebar move actions from `Pin to ... Sidebar` to `Move to ... Sidebar` (and corresponding settings labels) to better match the actual operation.
- ✅ SnapFX now renders framework-level side bars (left/right icon strips) with IDE-like panel behavior: immediate tooltips, click-to-open overlay panels, same-icon click to close, side-local icon switching, outside-click overlay close, and pin/unpin toggle between overlay and layout-consuming side panel modes.
- ✅ MainDemo now uses the framework-rendered sidebar UI for Phase-C testing and updates sidebar wording to `Pinned Open` / `Collapse` (`pinned` / `collapsed`) to match the new behavior semantics.
- ✅ Added Phase-C side-bar foundations in `DockGraph`: pinned sidebar state per side, pinned-open sidebar flags, deterministic `pinToSideBar(...)` / `restoreFromSideBar(...)` workflows, and layout-ID assignment for detached pinned nodes.
- ✅ Extended `DockLayoutSerializer` to persist and restore sidebar entries (including pinned-open state and remembered main-layout restore anchors), and to support sidebar-only layout payloads without a main root.
- ✅ Added `SnapFX` facade APIs for side bars (`pinToSideBar`, `restoreFromSideBar`, `pinOpenSideBar`, `collapsePinnedSideBar`, pinned-open/query accessors) so Phase-C workflows are testable without direct `DockGraph` calls.
- ✅ MainDemo now includes Phase-C pinned side-bar manual test controls in the Settings tab (dock-node pin actions, left/right pinned lists, restore actions, pin-open/collapse toggles) and Layout-menu entries for pin/restore/pin-open workflows.
- ✅ MainDemo now renders visible left/right sidebar strips for pinned nodes (driven by the new sidebar model state + pinned-open flags) with restore and side-switch actions for manual Phase-C validation.
- ✅ Refactored `MainDemo` FileChooser setup into reusable helper functions shared by `saveLayout`/`loadLayout` and `openTextFileInEditor`/`chooseEditorSaveTargetFile`, with centralized extension-filter constants.
- ✅ SnapFX now applies the default stylesheet automatically during `initialize(...)` and exposes runtime theme switching via `setThemeStylesheet(...)`.
- ✅ Added `snapfx-dark.css` and wired a theme selector into MainDemo Settings so light/dark switching uses the SnapFX API live.
- ✅ Replaced theme-ID handling with a simpler named theme catalog (`Light`, `Dark`) exposed via `SnapFX.getAvailableThemeStylesheets()` / `getAvailableThemeNames()`, while keeping path-based `setThemeStylesheet(...)`.
- ✅ Extracted stylesheet resolution/application logic from `SnapFX` into dedicated classes under `org.snapfx.theme` (`DockThemeCatalog`, `DockThemeStylesheetManager`) to reduce `SnapFX` complexity.

### Maintainability and Collaboration
- ✅ Introduced `DockThemeStyleClasses`, replacing hardcoded style class constants with members of this class for better maintainability and consistency across the codebase.

### Documentation
- ✅ Documented sidebar overlay/pin rendering state split in [ARCHITECTURE.md](ARCHITECTURE.md) and ADR [docs/adr/0003-sidebar-overlay-and-pin-rendering-state-split.md](docs/adr/0003-sidebar-overlay-and-pin-rendering-state-split.md).
- ✅ Updated sidebar interaction docs to include the configurable pinned active-icon collapse policy (default collapse).
- ✅ Documented that sidebar restore now reuses the floating-style placement-memory restore strategy in architecture/ADR docs.
- ✅ Updated [STATUS.md](STATUS.md), [DONE.md](DONE.md), and [ROADMAP.md](ROADMAP.md) to reflect framework-level sidebar rendering progress and current test totals.
- ✅ Refreshed `docs/images/main-demo.png` after switching MainDemo back to framework-rendered sidebars.
- ✅ Updated [STATUS.md](STATUS.md), [DONE.md](DONE.md), and [ROADMAP.md](ROADMAP.md) to reflect the Phase-C sidebar foundation slice and current test totals.
- ✅ Updated `docs/images/main-demo.png` preview after MainDemo Settings/Layout menu changes for Phase-C sidebar testing controls.
- ✅ Refreshed `docs/images/main-demo.png` again after adding visible pinned-sidebar strips to the MainDemo layout.
- ✅ Updated README/ARCHITECTURE docs for automatic stylesheet handling and runtime theme switching.
- ✅ Added ADR [docs/adr/0002-runtime-theme-stylesheet-management.md](docs/adr/0002-runtime-theme-stylesheet-management.md) for theme lifecycle ownership and API behavior.

## v0.2.6 - 2026-02-17

### Build and Test
- ✅ Added `MainDemoTest` coverage for floating snap-target settings resolution used by the debug settings panel and added `SnapFXTest` regression coverage for host-aware floating reattach restore/fallback behavior (float-button + unresolved-drag detach paths), including three-window floating-layout detach/attach roundtrip cases (top-left/top-right/bottom) and detach-close-remaining-attach fallback cases; full suite now runs with 245 tests.

### Framework and UI
- ✅ MainDemo Settings tab now includes floating-window snapping controls for API verification: enable toggle, snap distance, and snap-target selection (`SCREEN`, `MAIN_WINDOW`, `FLOATING_WINDOWS`).
- ✅ `attachFloatingWindow(...)` now restores detached floating-sub-layout nodes to remembered host context when possible (preferred/neighbor anchors), with deterministic silent fallback to active host-root or main layout when anchors are unavailable, including cases where source-layout neighbors changed after detach.

### Documentation
- ✅ Added a persistent collaboration rule in [AGENTS.md](AGENTS.md): every SnapFX API function must be represented in the MainDemo Settings tab for manual verification.
- ✅ Added a persistent collaboration rule in [AGENTS.md](AGENTS.md): significant design decisions must be documented in JavaDoc, [ARCHITECTURE.md](ARCHITECTURE.md), and ADRs under `docs/adr/`.
- ✅ Added ADR [docs/adr/0001-floating-reattach-placement-strategy.md](docs/adr/0001-floating-reattach-placement-strategy.md) and updated architecture/readme/status docs to reflect the new reattach strategy and documentation baseline.
- ✅ Updated [STATUS.md](STATUS.md), [DONE.md](DONE.md), and [ROADMAP.md](ROADMAP.md) to reflect the new reattach behavior and current test totals.

## v0.2.5 - 2026-02-16

### Build and Test
- ✅ Added serializer/API regression tests for layout-load failures (`blank`, malformed JSON, missing required fields, invalid tab index), expanded floating-title-bar drag continuity coverage (release/reset + non-primary guards), resize-min/cursor reliability coverage, persistence edge-case coverage for complex floating snapshots plus unknown-node placeholder diagnostics, unsupported-type recovery/custom-fallback coverage (including SnapFX + DemoNodeFactory integration), dock-node-header context-menu dismiss-on-press regression coverage, unresolved floating-sub-layout D&D detach regression coverage, and floating-window snapping coverage (engine scoring, overlap-aware candidate generation, drag edge snapping, adjacent-edge alignment, main-window parity checks including shadow-inset compensation, and API propagation/validation); full suite now runs with 234 tests.

### Framework and UI
- ✅ Added `DockLayoutLoadException` with JSON-location context for deserialization failures.
- ✅ `DockLayoutSerializer.deserialize(...)` now throws precise typed load errors instead of printing to `System.out`/`System.err`.
- ✅ `SnapFX.loadLayout(...)` now throws `DockLayoutLoadException` and validates payloads before applying them so invalid layouts do not partially apply.
- ✅ MainDemo now shows a user-facing error dialog for invalid layout files, including detailed load failure context.
- ✅ Floating window title-bar dragging now continues while the pointer leaves the title-bar node (scene-level drag tracking after title-bar press).
- ✅ Floating title-bar clicks now hide visible floating context menus immediately, and maximized restore-on-drag requires a deliberate threshold.
- ✅ Dock-node header presses now hide visible dock-node header context menus, including clicks directly on the same header toolbar area.
- ✅ Floating resize now respects effective stage/content minimum constraints and applies resize cursors reliably over interactive targets.
- ✅ MainDemo error dialogs are now owner-aware and attach to the primary stage when available (better multi-monitor behavior).
- ✅ Unknown/unsupported serialized node types now recover without aborting layout load: `DockLayoutSerializer` routes them through `DockNodeFactory` fallback hooks and otherwise inserts framework diagnostic placeholders (including node-id and JSON-path context).
- ✅ Unresolved D&D releases from multi-node floating sub-layouts now detach the dragged node into a new floating window (matching float-button/context-menu behavior).
- ✅ Added floating-window snapping MVP: title-bar drag can snap against screen work-area edges, main window edges, and peer floating-window edges with configurable enable/targets/distance API in `SnapFX`; snapping to main and peer windows now requires perpendicular overlap and supports adjacent-edge snapping, and main-window snapping compensates decorated-stage shadow insets.
- ✅ Refactored snapping internals to centralize candidate generation and overlap-aware logic in `DockFloatingSnapEngine`, removing unused helper paths and reducing `DockFloatingWindow` complexity.

### Documentation
- ✅ Updated README/ARCHITECTURE persistence examples to show `loadLayout(...)` / `deserialize(...)` error handling with `DockLayoutLoadException`.

## v0.2.4 - 2026-02-16

### Build and Test
- ✅ Removed obsolete JavaFX test JVM module flags that caused classpath/module warning noise (`24ac0bc`).
- ✅ Stabilized JavaFX module-path test runtime without FXML assumptions (`e8197a9`).
- ✅ Migrated plugin/dependency versions to `gradle/libs.versions.toml` and reduced duplicated build configuration (`517b5db`).
- ✅ Expanded shortcut, context-menu, and demo-accelerator coverage in unit tests; full suite now runs with 191 tests.

### Framework and UI
- ✅ Added configurable framework keyboard shortcut API in `SnapFX` (`setShortcut`, `clearShortcut`, `resetShortcutsToDefaults`, `getShortcuts`).
- ✅ Added default shortcut actions for `Ctrl+W`, `Ctrl+Tab`, `Ctrl+Shift+Tab`, `Escape`, and `Ctrl+Shift+P` with active tab/node/floating-window resolution.
- ✅ Added app-level `F11` fullscreen shortcut wiring in `MainDemo` (kept outside framework defaults).
- ✅ Hardened ESC drag cancellation in `DockDragService` so active drags cancel even while mouse is still pressed.
- ✅ Bound shortcut key filters to floating-window scenes so framework shortcuts also trigger while floating windows are focused.
- ✅ Added context menus for tabs, dock-node headers, split panes, and floating title bars (including `Attach to Layout` plus always-on-top toggle with control-icon parity).
- ✅ Added floating float-action availability policy so single-node floating layouts hide the `Float` context action (matching existing button behavior).
- ✅ Added close/float control-glyph icons to dock-node and tab context-menu actions for visual parity with header/tab buttons.
- ✅ Reworked DockNode icon model to image-based rendering so each view creates its own icon node, preventing parent-sharing losses in floating title bars.
- ✅ Floating title-bar icon now tracks active tab selection in floating tab layouts.
- ✅ Hardened drop-zone edge-size clamping for tiny bounds so drag hover no longer throws `Math.clamp` min/max-order exceptions.
- ✅ Tab headers for nested container layouts now use representative DockNode title/icon summaries (`Title +N`) instead of container class names.

### Documentation
- ✅ Split repository workflow content out of [README.md](README.md) into dedicated [CONTRIBUTING.md](CONTRIBUTING.md) and [RELEASING.md](RELEASING.md) (`6f93d0a`).
- ✅ Tightened markdown guardrails and clarified MIT personal/commercial usage wording (`34a5ac8`).
- ✅ Refocused [STATUS.md](STATUS.md) to open-issues-only tracking and moved completed-history logging fully to [CHANGELOG.md](CHANGELOG.md).
- ✅ Removed roadmap version-track metadata, moved overall progress to the top, and aligned roadmap fixed-item references to [CHANGELOG.md](CHANGELOG.md).
- ✅ Removed historical delta suffixes from [STATUS.md](STATUS.md) current-state bullets and added roadmap update guidance for progress-percentage recalculation.
- ✅ Updated README/STATUS/DONE/ROADMAP to reflect shortcut API baseline, app-level shortcut separation, and current test totals.
- ✅ Updated README/STATUS/DONE/ROADMAP to reflect delivered context menus and remaining context-menu extensibility API work.

## v0.2.3 - 2026-02-16

### Fixes
- ✅ CI/release workflows now run JavaFX tests via `xvfb-run -a` to avoid Linux headless toolkit initialization failures (`3f14cfc`).

## v0.2.2 - 2026-02-15

### Fixes
- ✅ `gradlew` is tracked as executable (`100755`) so Linux CI/release runners can execute the wrapper reliably (`c306575`).

## v0.2.1 - 2026-02-15

### CI and Release
- ✅ Added tag-triggered GitHub Release automation for `v*` tags with `git-cliff` release notes (`a3f230c`).

## v0.2.0 - 2026-02-15

### Floating Window and Close Workflow
- ✅ Expanded floating windows from single-node hosts to full dock-subtree hosts with split/tab layouts (`ea0807f`).
- ✅ Added floating window persistence in save/load snapshots including bounds and subtree restoration (`6cf6bf4`).
- ✅ Added source-aware close callback hooks and configurable default close behavior (`HIDE` / `REMOVE`) (`8fe50d0`).
- ✅ Improved floating UX for lock mode, overlapping-window targeting, float extraction from floating sub-layouts, and control reliability (`c77a6f1`, `7262464`, `89cfaf2`, `dd2ef66`).
- ✅ Added configurable floating pin behavior and always-on-top event/persistence support (`205540e`).

### Demo and UI
- ✅ Added editor close-hook flow with Save / Don't Save / Cancel behavior for dirty editors (`2893dea`).
- ✅ Added demo text file workflow (`Open`, `Save`, `Save As`) and improved menu icon parity for hidden/float/attach actions (`2893dea`, `610224c`).
- ✅ Extracted About dialog into a dedicated component with branding, links, and test coverage (`9149865`).
- ✅ Applied multi-size SnapFX app icon assets in demo and documentation (`cbfffd3`).
- ✅ Fixed floating maximize/restore behavior (double-click and drag-from-maximized parity) (`0462d3b`).
- ✅ Replaced hardcoded vector control icons with CSS-driven glyph styling for dock/floating controls (`833e046`).

### Build, Versioning, and Workflow
- ✅ Switched to tag-driven versioning with `gradle-jgitver` (`fba9c60`).
- ✅ Added automated MainDemo preview screenshot script and workflow rule for visual changes (`4640331`, `63f7340`).
- ✅ Added split ratio API (`setRootSplitRatios(...)`, `setSplitRatios(...)`) and used it in MainDemo default layout (`0db6954`).
- ✅ Cleaned repository workflow state (moved [AGENTS.md](AGENTS.md) to project root and removed legacy `.ai` folder) (`6852bf1`).

### Documentation
- ✅ Synced roadmap/status/done docs with phase transitions and current state (`5f434da`, `2b9c5e0`, `8603d3b`).
- ✅ Fixed markdown encoding/mojibake artifacts in roadmap/docs (`28d0683`).
- ✅ Enforced markdown status-icon consistency and updated collaboration guardrails (`0d36555`).

### Detailed History Migrated from [STATUS.md](STATUS.md)
- ✅ MainDemo close callbacks now prompt only for dirty editor nodes (Save / Don't Save / Cancel) before closing.
- ✅ MainDemo added editor file actions (`Open Text File`, `Save Active Editor`, `Save Active Editor As`) with dirty-title markers.
- ✅ Floating windows support close-to-hidden behavior and keep hidden-node workflows consistent.
- ✅ Floating lock mode propagates correctly and floating title-bar controls hide/disable while locked.
- ✅ Overlapping floating/main surfaces now resolve preview/drop to the frontmost window only.
- ✅ Floating windows now render drag/drop zones and indicators like the main layout.
- ✅ Floating title-bar interactions were stabilized (attach, maximize/restore, close, drag-from-maximized behavior).
- ✅ Tab-header float button click handling was fixed to avoid drag-handler interception.
- ✅ MainDemo floating-window list updates reliably for all float/attach paths.
- ✅ MainDemo reset-to-default now closes floating windows and clears hidden state.
- ✅ Added source-aware close/pin event callbacks and always-on-top persistence in floating snapshots.
- ✅ Added CSS-based dock/floating control glyph styling and aligned close-button visual consistency.
- ✅ Added automated MainDemo screenshot update workflow and refreshed preview assets after visual changes.
- ✅ Added split ratio API usage in MainDemo (`25% | 50% | 25%`) and regression coverage.
- ✅ Added deterministic editor close-decision policy tests and About dialog coverage.
- ✅ CI/release docs and workflows aligned with tag-driven release notes and artifact publishing.

## v0.1.0 - 2026-02-15

### Core Framework Baseline
- ✅ Initialized SnapFX project structure with model/view/dnd/persistence/demo components (`992320b`).
- ✅ Delivered core docking model and layout behavior with SplitPane flattening, auto-cleanup, and DockGraph integrity checks (phase-1 baseline).
- ✅ Added hidden-node workflows, debug tooling, and module-system setup (`module-info.java`) with Java 21/JavaFX 21 alignment.
- ✅ Introduced persistent node IDs and `DockNodeFactory` for cross-session layout restoration.

### Drag and Drop Stability
- ✅ Fixed critical DnD regressions around empty containers, target invalidation, and post-drop rebuild reliability.
- ✅ Fixed tab-related DnD behavior (tab target activation, reordering stability, insert targeting, split edge docking, and divider preservation).
- ✅ Improved ghost overlay behavior and drop-zone handling.

### Testing and Quality
- ✅ Established testing policy baseline requiring regression coverage for bug fixes.
- ✅ Added broad test coverage expansions for edge cases, large-layout stress scenarios, and memory cleanup behavior.
- ✅ Added/refined regression tests for critical DnD and persistence fixes.

### Refactoring and Documentation
- ✅ Performed a broad readability/maintainability refactor pass across model, layout engine, drag service, and demo components.
- ✅ Updated README/STATUS/DONE/ROADMAP for phase-1 closure and project orientation.

### Detailed History Migrated from [STATUS.md](STATUS.md)
- ✅ Fixed critical DnD bug where operations stopped after first drop by rebuilding views on revision changes.
- ✅ Fixed tab DnD regression where moved tabs lost drag capability due to stale layout-engine caches.
- ✅ Fixed empty-container/orphaned-container regressions by reordering cleanup (flatten first, then cleanup).
- ✅ Fixed target invalidation after `undock()` by re-resolving targets in restructured trees.
- ✅ Introduced persistent custom node IDs and factory-based node recreation for save/load across sessions.
- ✅ Synced locked-state behavior after layout load and added regression coverage.
- ✅ Implemented hidden-nodes close/restore integration across the UI lifecycle.
- ✅ Added and refined DockGraph debug tooling (tree export, activity log tracking, color-coded log entries).
- ✅ Verified nested TabPane behavior and documented accepted structure constraints.
- ✅ Expanded tests for performance (50+ nodes), memory cleanup, no-op/boundary operations, and layout optimization paths.

## Pre-Release Bootstrap (Before v0.1.0)

### Repository Start
- ✅ Initial repository bootstrap commit (`9421801`, 2021-08-04).
