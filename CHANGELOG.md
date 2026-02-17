# Changelog

All notable changes to this project are documented in this file.

The changelog is grouped by release tags (`vX.Y.Z`) and includes an `Unreleased` section for changes after the latest tag.

## Unreleased

### Build and Test
- ✅ Added `MainDemoTest` coverage for floating snap-target settings resolution used by the debug settings panel; full suite now runs with 236 tests.

### Framework and UI
- ✅ MainDemo Settings tab now includes floating-window snapping controls for API verification: enable toggle, snap distance, and snap-target selection (`SCREEN`, `MAIN_WINDOW`, `FLOATING_WINDOWS`).

### Documentation
- ✅ Added a persistent collaboration rule in `AGENTS.md`: every SnapFX API function must be represented in the MainDemo Settings tab for manual verification.
- ✅ Updated `STATUS.md`, `DONE.md`, and `ROADMAP.md` to reflect the new MainDemo snapping settings integration and current test totals.

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
- ✅ Split repository workflow content out of `README.md` into dedicated `CONTRIBUTING.md` and `RELEASING.md` (`6f93d0a`).
- ✅ Tightened markdown guardrails and clarified MIT personal/commercial usage wording (`34a5ac8`).
- ✅ Refocused `STATUS.md` to open-issues-only tracking and moved completed-history logging fully to `CHANGELOG.md`.
- ✅ Removed roadmap version-track metadata, moved overall progress to the top, and aligned roadmap fixed-item references to `CHANGELOG.md`.
- ✅ Removed historical delta suffixes from `STATUS.md` current-state bullets and added roadmap update guidance for progress-percentage recalculation.
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
- ✅ Cleaned repository workflow state (moved `AGENTS.md` to project root and removed legacy `.ai` folder) (`6852bf1`).

### Documentation
- ✅ Synced roadmap/status/done docs with phase transitions and current state (`5f434da`, `2b9c5e0`, `8603d3b`).
- ✅ Fixed markdown encoding/mojibake artifacts in roadmap/docs (`28d0683`).
- ✅ Enforced markdown status-icon consistency and updated collaboration guardrails (`0d36555`).

### Detailed History Migrated from `STATUS.md`
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

### Detailed History Migrated from `STATUS.md`
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
