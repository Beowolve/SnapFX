# SnapFX Development Roadmap

**Last Updated**: 2026-02-28

This document tracks planned and proposed work for SnapFX.
This roadmap keeps a high-level progress view plus planned/proposed work; detailed completed/fixed release history is tracked in [CHANGELOG.md](CHANGELOG.md).

---

## Overall Progress

| Phase | Status | Completion |
|-------|--------|------------|
| Phase 1: Core Stability | ✅ Completed | 100% |
| Phase 2: Floating Windows | ✅ Completed | 100% |
| Phase 3: User Experience | 🚧 In Progress | 64% |
| Phase 4: Advanced Features | 🚧 In Progress | 35% |
| Phase 5: Themes & Customization | 🚧 In Progress | 60% |
| Phase 6: Performance & Polish | 📋 Planned | 0% |
| Phase 7: Developer Experience | 📋 Planned | 50% |
| Phase 8: Production Readiness | 📋 Planned | 35% |

**Total Project Completion**: ~85%

## Legend

- ✅ **Completed** - Feature fully implemented and tested
- 🚧 **In Progress** - Currently being worked on
- 📋 **Planned** - Scheduled for implementation
- 💡 **Proposed** - Idea under consideration
- ❌ **Blocked** - Waiting on dependencies or decisions

---

## Phase 1: Core Stability

### 1.1 Drag & Drop Improvements
**Priority**: 🔴 Critical

- ✅ **Tiny-bounds drop-zone stability**: Drop-zone edge sizing now avoids `Math.clamp` min/max-order exceptions during drag hover on very small bounds
- 📋 No planned items; see [CHANGELOG.md](CHANGELOG.md) for completed fixes.

---

### 1.2 Debug & Monitoring
**Priority**: 🟡 High

- ✅ **DockDebugOverlay HUD fixes**: Fixed missing panel background rendering (layout sizing), top-left clipping, and stale `none` target/zone diagnostics text during active drags; MainDemo HUD remains a local debug-toggle option
- ✅ **MainDemo debug/settings split divider stability**: The outer demo split divider between the dock area and Debug/Settings tabs now remains stable during dock-layout rebuilds by updating a persistent left-side host container instead of replacing the split item

### 1.3 Testing & Quality Assurance  
**Priority**: 🟡 High

- ✅ **CI module-split follow-up**: Stability-check test filters were updated to `org.snapfx...` and now run on `:snapfx-core:test`
- ✅ **Add performance tests**: Large layouts with 50+ nodes
- ✅ **Add memory leak tests**: Ensure proper cleanup
- ✅ **Add edge case tests**: More boundary conditions
- ✅ **Floating drag-state regression hardening**: Added scene-level drag release/reset and non-primary activation guard coverage for floating title-bar movement
- ✅ **Persistence edge-case hardening**: Added complex floating snapshot roundtrip coverage and invalid floating snapshot validation/no-partial-apply tests
- ✅ **CI flake guard for critical interactions**: CI now reruns `SnapFXTest`, `DockFloatingWindowTest`, and `DockDragServiceTest` three times per run
- ✅ **Sidebar overlay width test CI race hardening**: `SnapFXTest` overlay-width/resize-handle tests avoid prebuilding `SnapFX` before reflective sidebar icon clicks, preventing an async sidebar `requestRebuild()` race before the final scene build on CI
- ✅ **Floating reattach structure hardening**: Added three-window floating-layout detach/attach roundtrip regression coverage (top-left/top-right/bottom nodes), plus detach-close-remaining-attach host-restore fallback cases

**Estimated Time**: Completed


### 1.4 Layout Optimization
**Priority**: 🟡 High

- ✅ **Smart divider positioning**: Better initial divider positions
- ✅ **Empty layout handling**: Proper behavior when graph is empty
- ✅ **Single node optimization**: Skip unnecessary containers
- ✅ **Demo baseline split ratio preset**: MainDemo primary three-pane split initializes as `25% | 50% | 25%`

**Estimated Time**: Completed

### 1.5 UI Bug Fixes
**Priority**: 🟡 High

- ✅ **Container tab title/icon UX**: Tabs containing nested split/tab layouts now derive title/icon from a representative DockNode (`Title +N`) instead of showing container class names
- ✅ **Layout-load diagnostics UX**: Invalid layout files now return typed `DockLayoutLoadException` failures with JSON path context, and MainDemo surfaces the details in an error dialog
- ✅ **Unknown-type layout recovery UX**: Corrupted/unsupported serialized node types now recover via factory custom fallback or framework placeholder diagnostics without aborting full layout load
- 📋 No planned items; see [CHANGELOG.md](CHANGELOG.md) for completed fixes.

---

## Phase 2: Floating Windows

### 2.1 Floating Window Core
**Priority**: 🟢 Medium

- ✅ **DockFloatingWindow class**: Manage external stages
- ✅ **Detach from main window**: Drag node outside main window to create floating window
- ✅ **Attach to main window**: Drag-attach and title-bar attach are both supported
- ✅ **Multi-monitor support**: Position by explicit screen coordinates (`floatNode(node, x, y)`)
- ✅ **Cross-window D&D**: Dock between main and floating windows, including floating-to-floating
- ✅ **Window state persistence**: Save/load floating window positions

**Estimated Time**: Completed

---

### 2.2 Floating Window Features
**Priority**: 🟢 Medium

- ✅ **MainDemo close-hook sample**: Demo now shows editor-specific save prompts on close requests
- ✅ **MainDemo editor file actions**: Demo now includes open/save/save-as workflow for `SerializableEditor` nodes
- ✅ **MainDemo FileChooser consolidation**: Layout and editor open/save dialogs now share reusable chooser builders, filter constants, and save-default resolution helpers
- ✅ **Theme runtime switching API**: `initialize(...)` now auto-applies the default SnapFX stylesheet, `setThemeStylesheet(...)` updates primary/floating scenes at runtime, and built-in themes are exposed as an ordered named catalog (`Light`, `Dark`) via list/map helpers
- ✅ **MainDemo theme selector**: Settings tab now reads themes from the SnapFX named catalog and switches styles via API
- ✅ **Theme modularization**: Theme catalog metadata and stylesheet apply/resolve logic are extracted from `SnapFX` into dedicated classes under `org.snapfx.theme`
- ✅ **MainDemo layout menu icon parity**: Hidden/Floating menu entries display dock-node icons when available
- ✅ **Maximize/restore**: Custom floating title-bar toggle implemented
- ✅ **Maximized drag restore**: Dragging the title bar from maximized restores and moves the window
- ✅ **Title-bar drag continuity**: Floating window drag continues even when the cursor leaves the title bar during a pressed drag
- ✅ **Floating D&D visual feedback**: Floating windows show drop zones and active drop indicator during drag
- ✅ **Topmost overlap targeting**: In overlapping floating/main windows, preview and drop target only the frontmost surface under cursor
- ✅ **Locked-mode floating controls**: Floating title-bar controls now hide in locked mode and close is blocked while locked
- ✅ **Single-node inner control cleanup**: Inner dock-node close/float controls are hidden for single-node floating layouts
- ✅ **Float-from-floating detach parity**: Float actions and unresolved D&D releases inside floating sub-layouts create a new floating window for the selected node
- ✅ **Configurable close behavior**: Close behavior is centrally configurable (`HIDE`/`REMOVE`) with `HIDE` as default
- ✅ **Close callback hooks**: Source-aware close callbacks now support interception/outcome handling for tab, title-bar, and floating-window close requests
- ✅ **Always on top / pinning**: Floating windows now support configurable pin-button visibility (`ALWAYS`/`AUTO`/`NEVER`), default pinned state, lock-mode behavior, and persisted always-on-top snapshots
- ✅ **Window decorations**: Custom title bar styling and controls
- ✅ **Resizable undecorated windows**: Edge/corner resize behavior for floating stages
- ✅ **Tab-level float action**: Float button available in tab headers
- ✅ **Floating title-bar icon reliability**: Title-bar icons now stay visible across dock/header/tab views and track active tab selection
- ✅ **Floating title-bar context-menu dismiss behavior**: Context menu now hides immediately when clicking the title bar outside the menu
- ✅ **Maximized restore drag threshold**: Restore-on-drag from maximized requires deliberate pointer movement
- ✅ **Adaptive resize minimum constraints**: Floating resize honors effective stage/content minimum sizes
- ✅ **Interactive-target resize cursor reliability**: Edge resize cursors now apply consistently over content controls (for example console text areas)
- ✅ **Owner-aware MainDemo error alerts**: Error dialogs attach to the primary stage for better multi-monitor usability
- ✅ **Host-aware floating reattach restore**: Attach now restores detached floating-sub-layout nodes back to remembered host context (preferred/neighbor anchors) and falls back silently to active host root or main layout when anchors are unavailable, including when source floating-layout neighbors changed after detach

**Estimated Time**: Completed

---

## Phase 3: User Experience

### 3.1 Keyboard Shortcuts
**Priority**: 🟢 Medium

- ✅ **Framework shortcut API**: `SnapFX` now supports configurable key mappings (`setShortcut`, `clearShortcut`, `resetShortcutsToDefaults`, `getShortcuts`)
- ✅ **Ctrl+W**: Close active dock node (default mapping, fully configurable)
- ✅ **Ctrl+Tab**: Switch to next tab (default mapping, fully configurable)
- ✅ **Ctrl+Shift+Tab**: Switch to previous tab (default mapping, fully configurable)
- ✅ **Escape**: Cancel active drag operation (default mapping, fully configurable)
- ✅ **Escape reliability**: Active drag cancellation now works even while the mouse button is still held
- ✅ **Ctrl+Shift+P**: Toggle active floating window always-on-top (default mapping, fully configurable)
- ✅ **F11 (MainDemo)**: Fullscreen toggle implemented at application level (kept out of framework defaults)

**Estimated Time**: Completed (framework baseline)

### 3.2 Context Menus
**Priority**: 🟢 Medium

- ✅ **Right-click on tab**: Close, Close Others, Close All, Float (with control-glyph icons for close/float)
- ✅ **Right-click on splitter**: Reset splitter ratios to balanced layout
- ✅ **Right-click on header**: Float and Close actions for dock-node headers (with control-glyph icons)
- ✅ **Header context-menu dismiss behavior**: Dock-node header context menu now hides when pressing the header (including the same toolbar area)
- ✅ **Right-click on floating title bar**: `Attach to Layout` and toggle "Always on Top"
- 📋 **Customizable menu items**: API for adding custom actions

**Estimated Time**: Remaining ~0.5 day

---

### 3.3 Animations
**Priority**: 🟡 Low

- ✅ Completed: Demo About dialog easter egg animation (triple-click logo trigger on SnapFX branding).
- 💡 **Smooth docking**: Fade in/out when adding/removing nodes
- 💡 **Tab switching**: Slide animation
- 💡 **Splitter adjustment**: Smooth resize
- 💡 **Ghost overlay**: Fade in/out
- 💡 **Configurable**: Enable/disable animations globally

**Estimated Time**: 2-3 days

---

## Phase 4: Advanced Features

### 4.1 Perspectives
**Priority**: 🟢 Medium

- 📋 **Perspective API**: Save/load named layouts
- 📋 **Perspective switcher**: Quick switch between predefined layouts
- 📋 **Default perspectives**: "Editor", "Debug", "Review" modes
- 📋 **Custom perspectives**: User-defined layouts

**Estimated Time**: 2 days

---

### 4.2 Side Bars (Pinned + Interaction Parity)
**Priority**: 🟢 Medium

- ✅ **Phase 1 API + MainDemo manual controls**: `SnapFX` facade APIs and MainDemo Settings/Layout-menu controls now exercise pinned side-bar model/persistence foundations (pinned entries, pinned-open state, deterministic restore anchors)
- ✅ **MainDemo visual sidebar strips (validation UI)**: MainDemo now renders visible left/right pinned-sidebar strips for manual Phase-C testing without waiting for framework-side view integration
- ✅ **Phase 1 visual side-panel integration**: SnapFX now renders framework-level left/right side bars (icon strips + pinned side panels) connected to the existing model/persistence state
- ✅ **Phase 1 restore placement reliability**: Sidebar restore in `SnapFX` now reuses the floating-style placement-memory fallback pipeline (preferred + neighbor anchors + fallback) to restore nodes to their remembered layout context more reliably
- ✅ **Phase 1 naming cleanup (pre-release)**: Sidebar API/model semantics were renamed to `pinOpen...` / `collapse...` / `is...PinnedOpen` and serializer state key `visible` -> `pinnedOpen` (no compatibility aliases while project is private)
- ✅ **Phase 1 overlay interaction baseline**: Click-to-open/click-to-toggle overlay panels, outside-click close, pin/unpin transitions, configurable pinned active-icon collapse policy (with pin-mode-preserving temporary collapse), default-collapsed pinning, and hit-testing/side placement fixes are implemented
- ✅ **Phase 1 persistence UX coverage**: Manual SnapFX/MainDemo verification for mixed main/floating/sidebar save-load and lock-mode scenarios is completed on top of the model/serializer/test coverage
- ✅ **Phase 2 scope refinement**: Hover auto-hide/reveal behavior was dropped from the near-term plan in favor of higher-value sidebar interaction parity (DnD, framework context menus, and resize)
- ✅ **Phase 2 model foundation (sidebar ordering)**: `DockGraph` now supports index-based sidebar insertion/reordering and cross-side moves with clamped bounds, providing the base semantics for sidebar DnD drop-positioning
- ✅ **Phase 2 sidebar DnD baseline (strip target)**: `SnapFX` now accepts unresolved drops onto visible sidebar icon strips with exact insertion index resolution, renders a sidebar insert-position line during drag hover, supports strip-icon drag as a sidebar DnD source, and reuses the sidebar-ordering model semantics for reorder/cross-source moves; pinned sidebar nodes are also handled correctly as DnD sources for main/floating drops
- ✅ **Framework sidebar move context menus**: Built-in `Move to Left Sidebar` / `Move to Right Sidebar` actions are now available in framework dock-node header and tab context menus (including floating layouts), wired to SnapFX sidebar pinning with lock-aware disabling
- ✅ **Framework sidebar node context menus**: Sidebar strip icons and expanded sidebar panel headers now provide built-in restore/move/pin actions (`Restore from Sidebar`, `Move to Left/Right Sidebar`, `Pin/Unpin Sidebar Panel`) with lock-aware disabling
- ✅ **Resizable sidebar panel widths**: Sidebar panels are resizable per side via framework resize handles; pinned and overlay modes share the same per-side width with runtime clamping and MainDemo Settings controls
- ✅ **Sidebar width persistence**: Per-side sidebar panel widths now roundtrip via `DockLayoutSerializer` side-bar state (`panelWidth`) with backward-compatible default fallback
- ✅ **Sidebar visibility mode API (`DockSideBarMode`)**: SnapFX now exposes `ALWAYS` / `AUTO` / `NEVER` sidebar UI modes; `ALWAYS` renders empty strips for direct DnD targets, `AUTO` keeps the current behavior, and `NEVER` suppresses sidebar UI plus framework sidebar move context-menu actions
- ✅ **Right overlay sidebar resize handle reliability**: Unpinned right sidebar overlays now resize correctly after raising the resize handle above overlay panel chrome/shadow and making the full handle bounds explicitly pickable
- ✅ **Phase 2 completion (current scope)**: Sidebar interaction parity scope is complete for the current release target (DnD strip baseline, framework/sidebar context menus, resize + persistence, and sidebar visibility mode); broader panel-surface DnD targeting/polish is deferred to optional backlog
- 📋 **Sidebar view extraction/refactor**: Optional extraction of the current framework sidebar rendering from `SnapFX` into a dedicated view/controller component if complexity grows further
- 💡 **Optional sidebar DnD panel-surface targeting**: Extend sidebar DnD beyond the current strip-target baseline (for example broader sidebar-surface targeting around pinned/overlay panels) while keeping deterministic target resolution and lock-aware behavior
- 💡 **Optional sidebar DnD preview polish**: Extend/align sidebar insertion preview behavior beyond the current strip-target line baseline (for example additional target surfaces or richer preview states)

**Status**: ✅ Completed for current scope (optional follow-ups remain in backlog)

---

### 4.3 Size Constraints
**Priority**: 🟡 High

- 📋 **Min/max width**: Prevent panels from becoming too small/large
- 📋 **Preferred size**: Respect component size hints
- 📋 **Resize limits**: Constrain splitter movement
- 📋 **Proportional resize**: Maintain ratios when resizing

**Estimated Time**: 2 days

---

### 4.4 Grid Layout
**Priority**: 🔵 Low

- 💡 **DockGridPane class**: 2D grid container
- 💡 **Row/column spanning**: Multi-cell panels
- 💡 **Grid resize**: Adjust row heights and column widths
- 💡 **Nested layouts**: Combine with split/tab containers

**Estimated Time**: 5-7 days

---

### 4.5 Floating Window Snapping
**Priority**: 🟢 Medium

- ✅ **Window-to-window snapping**: Floating windows now snap to other floating window edges while moving
- ✅ **Snap to main docking area**: Floating windows now snap to the main window edges
- ✅ **Configurable snap distance**: `SnapFX.setFloatingWindowSnapDistance(...)` controls tolerance in pixels
- ✅ **Configurable snap targets**: `SnapFX.setFloatingWindowSnapTargets(...)` controls screen/main/floating snap surfaces
- ✅ **MainDemo snapping settings controls**: Debug settings tab now exposes snapping enable, snap distance, and snap targets for direct manual API verification
- ✅ **Snapping engine consolidation**: Candidate generation and overlap-aware snap logic are centralized in `DockFloatingSnapEngine` for maintainability and focused testing
- 📋 **Visual snap guides**: Alignment indicator lines while dragging

**Estimated Time**: 2-3 days

---

## Phase 5: Themes & Customization

### 5.1 Theme Support
**Priority**: 🟢 Medium

- ✅ **Light theme**: Bright, clean appearance (`snapfx.css`) is built in and remains the default
- ✅ **Dark theme**: Low-light alternative (`snapfx-dark.css`) is built in
- ✅ **Custom theme API**: Runtime stylesheet switching supports classpath resources and absolute stylesheet URLs
- ✅ **Theme switcher**: Runtime theme changes are wired through API and exposed in MainDemo Settings
- ✅ **Named theme discovery**: Built-in themes are exposed as ordered name/path metadata (`Light`, `Dark`) for UI pickers

**Estimated Time**: Completed

---

### 5.2 Visual Customization
**Priority**: 🔵 Low

- ✅ **Control glyph styling via CSS**: Dock/floating control icons are stylesheet-defined (no hardcoded vector icon factory)
- ✅ **MainDemo application icon branding**: Multi-size SnapFX app icons are wired to the demo stage
- 💡 **Custom icons**: Replace default icons
- 💡 **Tab styles**: Different tab appearances
- 💡 **Header styles**: Customizable node headers
- 💡 **Splitter styles**: Custom divider appearance

**Estimated Time**: 2 days

---

## Phase 6: Performance & Polish

### 6.1 Performance Optimization
**Priority**: 🟡 High

- 📋 **Lazy view creation**: Create views only when visible
- 📋 **View recycling**: Reuse views when possible
- 📋 **Virtual rendering**: For large tab sets
- 📋 **Async layout**: Background layout calculations
- 📋 **Benchmark suite**: Measure and track performance

**Estimated Time**: 3-4 days

---

### 6.2 Memory Optimization
**Priority**: 🟡 High

- 📋 **Weak references**: For cached views
- 📋 **Proper cleanup**: Ensure no memory leaks
- 📋 **Resource disposal**: Cleanup on close
- 📋 **Memory profiling**: Identify and fix leaks

**Estimated Time**: 2 days

---

### 6.3 Accessibility
**Priority**: 🟢 Medium

- 📋 **Screen reader support**: Proper ARIA labels
- 📋 **Keyboard navigation**: Full keyboard access
- 📋 **Focus management**: Logical tab order
- 📋 **High contrast**: Support high contrast themes
- 📋 **Font scaling**: Respect system font sizes

**Estimated Time**: 2-3 days

---

## Phase 7: Developer Experience

### 7.1 API Improvements
**Priority**: 🟢 Medium

- ✅ **Split ratio API**: Configure split pane ratios directly via `SnapFX.setRootSplitRatios(...)` / `setSplitRatios(...)`
- 📋 **Builder pattern**: Fluent API for layout construction
- 📋 **Event API**: Listen to layout changes
- 📋 **Validation API**: Check layout validity
- 📋 **Query API**: Find nodes by criteria

**Estimated Time**: 2 days

---

### 7.2 Documentation
**Priority**: 🟡 High

- ✅ **Documentation ownership baseline**: Scope split clarified (`STATUS` current, `ROADMAP` planned, `DONE` completed, `TESTING_POLICY` rules-only)
- ✅ **Minimal-diff editing baseline**: collaboration rules now require targeted edits over full-file rewrites whenever smaller diffs are sufficient
- ✅ **Unicode icon readability baseline**: `\u...` icon constants in doc consistency tests now include inline rendered-icon comments
- ✅ **ADR baseline**: Significant design decisions are documented under `docs/adr/` and mirrored in API/architecture documentation
- ✅ **License wording baseline**: README/SETUP now state MIT licensing and explicit personal/commercial use positioning
- ✅ **README consumer-focus baseline**: Quick Start now focuses on SnapFX usage, without generic Gradle/module templates or repository workflow sections
- ✅ **Maintainer docs baseline**: Collaboration/release workflow is now documented in [CONTRIBUTING.md](CONTRIBUTING.md) and [RELEASING.md](RELEASING.md)
- ✅ **Versioned changelog baseline**: Historical changes are now consolidated in [CHANGELOG.md](CHANGELOG.md) and grouped by release tags
- ✅ **Status scope baseline**: [STATUS.md](STATUS.md) now keeps only current state and open issues; completed/fixed history is maintained in [CHANGELOG.md](CHANGELOG.md)
- ✅ **Roadmap signal baseline**: Overall progress is now the first section, legend follows directly below, and version-track metadata was removed
- ✅ **README public-preview messaging baseline**: Top-level messaging now clearly states `0.x` release-readiness/public-preview status, pre-Maven-Central publication state, and links users to live status/roadmap tracking.
- 📋 **Roadmap structure cleanup**: Keep each subsection as one block (`Priority` + open/planned items) and move detailed completed history to [CHANGELOG.md](CHANGELOG.md) / [DONE.md](DONE.md)
- ✅ **API documentation**: `snapfx-core` JavaDoc completion baseline is now warning-free via `./gradlew :snapfx-core:javadoc --rerun-tasks`, collaboration rules require immediate full JavaDoc updates for new/changed public API, and API docs are now published at `https://snapfx.org/api/`.
- ✅ **JavaDoc discoverability baseline**: Exported API packages now include `package-info.java` overviews, and key API classes now include usage snippets to improve first-time navigation on hosted docs.
- 📋 **User guide**: Comprehensive usage guide
- 📋 **Tutorial series**: Step-by-step tutorials
- 📋 **Example projects**: Real-world examples
- ✅ **GitHub Pages site**: Docusaurus-based documentation portal is now live at `https://snapfx.org/` (guides, architecture, release status) with JavaDoc integrated under `/api`.
- ✅ **Changelog release hygiene baseline**: `CHANGELOG.md` now keeps explicit sections per release tag (including backfilled `v0.5.0` and `v0.6.0`), and `Unreleased` is reserved for post-tag work.
- 📋 **Docs versioning (post-`1.0.0`)**: Start Docusaurus multi-version docs and versioned API JavaDoc paths only after the first stable release to keep pre-`1.0.0` maintenance overhead low.
- 📋 **Video tutorials**: Screen recordings
- 📋 **Doc consistency guardrails**: Keep markdown consistency tests aligned with documentation scope rules while avoiding brittle content-specific assertions (focus on encoding/format guardrails)

**Estimated Time**: 5-7 days

---

### 7.3 Tooling
**Priority**: 🔵 Low

- ✅ Completed: `runSimpleExample` task for launching `SimpleExample` with JavaFX module runtime wiring.
- 💡 **Layout designer**: Visual layout editor
- 💡 **FXML support**: Alternative to programmatic API
- 💡 **CSS inspector**: Debug styling issues
- 💡 **Layout validator**: Check for common issues

**Estimated Time**: 7-10 days

---

## Phase 8: Production Readiness

### 8.1 Packaging & Distribution
**Priority**: 🟡 High

- ✅ **GitHub tag release workflow**: Pushing `v*` tags now runs build/test and publishes a GitHub Release with distribution artifacts
- ✅ **Release notes automation**: `git-cliff` now generates tag-based release notes used as GitHub Release body
- ✅ **Wrapper execution hardening**: `gradlew` is tracked as executable (`100755`) for Linux runner compatibility
- ✅ **Headless JavaFX CI stability**: CI/release workflows run Gradle tests via `xvfb-run -a` to support JavaFX toolkit initialization on Linux runners
- ✅ **Test JVM arg cleanup**: Removed obsolete JavaFX `--add-opens/--add-exports` flags to avoid classpath-run warning noise
- ✅ **JavaFX module-path test launch**: Gradle test runtime now moves required JavaFX jars to `--module-path` and applies TestFX access flags to avoid unnamed-module startup warnings and reflective-access stack traces
- ✅ **Version catalog migration**: Build script now sources plugin/dependency versions from `gradle/libs.versions.toml` to reduce duplicated version literals
- ✅ **Core/demo module split baseline**: Gradle project now separates framework code/resources/tests (`snapfx-core`) from demo app code/resources/tests (`snapfx-demo`); root now acts as the aggregate Gradle entry point
- ✅ **Release workflow split-path alignment**: Release build now runs `:snapfx-demo:distZip` / `:distTar` explicitly and uploads artifacts from split module output paths
- ✅ **Domain-backed Maven namespace baseline**: Gradle publish namespace now uses `org.snapfx` (aligned with `snapfx.org`) and README documents planned first-release coordinates `org.snapfx:snapfx-core`
- ✅ **Package/module namespace baseline**: Java packages and JPMS module names now use `org.snapfx...` across core/demo code and tests
- ✅ **Repository metadata hygiene baseline**: `.idea/` is fully ignored and tracked IntelliJ workspace/project metadata has been removed from Git
- ✅ **Maven publish dry-run baseline (`snapfx-core`)**: `maven-publish` + optional signing wiring, `sourcesJar`/`javadocJar`, and POM metadata are in place; local `:snapfx-core:publishToMavenLocal` succeeds
- 📋 **Maven Central**: Publish to Maven Central
- ✅ **jlink support baseline**: `snapfx-demo` now uses `org.beryx.jlink` and provides `:snapfx-demo:jlink` runtime-image packaging
- 🚧 **jpackage support baseline**: `snapfx-demo` now provides `:snapfx-demo:jpackageImage` and `:snapfx-demo:packageJPackageImageZip` with OS-specific app icon selection (`.ico`/`.icns`/`.png`) plus macOS-compatible app-version normalization (major version floor `1`), and release automation now publishes per-OS demo ZIP assets (Windows/macOS/Linux); installer generation/signing remains pending
- ✅ **Smoke validation policy baseline**: `RELEASING.md` now defines required local-OS smoke validation per RC, cross-OS checks as nice-to-have, per-OS start commands/checklist, and optional CI startup-smoke scope (validated against successful `v0.4.1-rc.2` release workflow run).
- 📋 **Remove temporary macOS appVersion workaround at v1.x**: After the first real `v1.x` project release, drop the major-floor mapping so demo appVersion equals the core/project version again.
- 📋 **Version management**: Semantic versioning

**Estimated Time**: 2-3 days

---

### 8.1.1 Release-Ready Version Lane (`0.5.x` to `1.0.0`)
**Priority**: 🟡 High

- 📋 **Policy baseline (not release-now)**: Keep SnapFX continuously release-ready while feature development continues; cut `1.0.0` only after an explicit go-public decision.
- ✅ **`0.5.x` Documentation baseline**: Public API JavaDoc is now warning-free for `snapfx-core` (`./gradlew :snapfx-core:javadoc --rerun-tasks`), and AGENTS workflow rules now enforce immediate complete JavaDoc updates for API changes.
- ✅ **`0.6.x` Public docs + domain baseline**: GitHub Pages now publishes a Docusaurus documentation portal at `https://snapfx.org/` and generated JavaDoc at `https://snapfx.org/api/`, with `CNAME`-based domain routing and release docs covering setup/validation flow.
- 📋 **`0.7.x` Packaging hardening**: Finalize cross-platform demo packaging validation, smoke-check checklists, and release-asset checksum flow.
- 📋 **`0.8.x` Publishing readiness**: Finalize Maven Central CI signing/staging/release flow and maintainer release checklist.
- 📋 **`0.9.x` Release rehearsal + freeze**: Run end-to-end RC drills (`v0.9.x-rc.y`), close final blockers, and freeze public API for `1.0.0`.
- 📋 **`1.0.0` stable cut**: Tag and publish the first stable public release once all release-readiness gates are green.
- 📋 **Scope guard**: Keep optional backlog items (for example video tutorials, layout designer, major UX expansions) out of the `1.0.0` critical path unless explicitly promoted.

**Estimated Time**: Iterative across `0.5.x` to `0.9.x`

---

### 8.2 Quality Assurance
**Priority**: 🔴 Critical

- 📋 **Code coverage**: Target 85%+ coverage
- 📋 **Mutation testing**: Verify test quality
- 📋 **Static analysis**: SonarQube, SpotBugs
- 📋 **Security audit**: Check for vulnerabilities
- 📋 **Compliance check**: License compliance

**Estimated Time**: 3-4 days

---

### 8.3 Production Testing
**Priority**: 🔴 Critical

- 📋 **User acceptance testing**: Real user feedback
- 📋 **Load testing**: Handle complex layouts
- 📋 **Stress testing**: Resource exhaustion scenarios
- 📋 **Compatibility testing**: Multiple Java versions
- 📋 **Platform testing**: Windows, macOS, Linux

**Estimated Time**: 5-7 days

---

## Future Ideas (Backlog)

### Potential Features
- 💡 **Layout history**: Undo/redo for layout changes
- 💡 **Layout diff**: Compare two layouts
- 💡 **Layout merge**: Combine layouts
- 💡 **Remote layouts**: Share layouts across network
- 💡 **Layout versioning**: Track layout changes over time
- 💡 **Collaborative editing**: Multiple users editing layout
- 💡 **Layout templates**: Pre-built layout patterns
- 💡 **Layout marketplace**: Share layouts with community
- 💡 **AI-assisted layout**: Suggest optimal layouts
- 💡 **Gesture support**: Touch gestures for tablets

### Technical Debt
- 💡 **Refactor DockDragService**: Simplify complex logic
- 💡 **Improve error handling**: Better error messages
- 💡 **Reduce coupling**: Further decouple components
- 💡 **Optimize imports**: Clean up dependencies

---

## How to Update This Roadmap

1. **Feature Complete**: Change status from 📋 to ✅
   - Also capture release-relevant details in [CHANGELOG.md](CHANGELOG.md) under the correct tag section.
2. **Started Work**: Change status from 📋 to 🚧
3. **New Feature**: Add to appropriate phase or Future Ideas
4. **Priority Change**: Update priority emoji (🔴🟡🟢🔵)
5. **Blocked**: Change status to ❌ and document reason
6. **Update Date**: Update "Last Updated" at the top
   - Recalculate `Overall Progress` phase percentages and `Total Project Completion` when status mix changes.

---

## Contributing

If you'd like to contribute to SnapFX:

1. Pick an item marked 📋 (Planned)
2. Create an issue discussing the implementation
3. Fork the repository
4. Implement the feature with tests
5. Submit a pull request
6. Update this roadmap when merged
7. Follow repository collaboration standards in [AGENTS.md](AGENTS.md) (commit message and callback-structure conventions).

---

**Maintained by**: SnapFX Development Team  
**Questions?**: Open an issue on GitHub

