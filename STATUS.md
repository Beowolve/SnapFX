# Project Status

**Last Updated**: 2026-02-28

## Build Status

✅ **Build**: `BUILD SUCCESSFUL`  
✅ **Tests**: All 310 tests passing (latest full suite)
✅ **Module System**: Fully implemented (JPMS)  
✅ **Demo App**: Running successfully  
✅ **CI Automation**: GitHub Actions workflows added for push/PR tests and tag-triggered releases  
✅ **Release Notes Automation**: `git-cliff` generates tag-based GitHub Release notes
✅ **CI Stability Guard**: Critical interaction test suites run 3x per CI execution to catch flakes early

✅ **CI Module-Split Alignment**: Critical stability suites now run `:snapfx-core:test` with `org.snapfx...` test filters
✅ **Release Module-Split Alignment**: Tag release workflow now builds `:snapfx-demo` distributions explicitly and publishes artifacts from split module output paths
✅ **Demo Runtime Packaging Baseline**: `org.beryx.jlink` now configures `:snapfx-demo:jlink`, `:snapfx-demo:jpackageImage`, and `:snapfx-demo:packageJPackageImageZip`, including OS-specific app icons (`.ico`/`.icns`/`.png`) and a temporary macOS-compatible jpackage app-version normalization (major version floor `1`, planned removal at first `v1.x` release)
✅ **Cross-Platform Demo Release Assets**: Tag releases now build `jpackage` demo ZIPs on Windows/macOS/Linux and publish them with deterministic OS-specific filenames (`snapfx-demo-jpackage-image-<os>-<tag>.zip`)
✅ **Release Smoke Validation Baseline**: `RELEASING.md` now defines a practical smoke policy (required on at least one local OS, cross-OS manual checks as nice-to-have) plus per-OS start commands and a minimal checklist; an optional CI startup-smoke scope is documented.
✅ **Release Asset Checksum Baseline (`0.7.x`)**: Release workflow now generates SHA256 files for all shipped `.zip`/`.tar`/`.jar` assets and publishes matching `*.sha256` artifacts in GitHub Releases.

✅ **Release-Ready Version Strategy**: Roadmap/releasing docs now define an explicit `0.5.x` to `0.9.x` readiness lane with RC drill tags and a controlled `1.0.0` public-launch cut.
✅ **JavaDoc Completion Track (`0.5.x`)**: `snapfx-core` JavaDoc is now warning-free (`./gradlew :snapfx-core:javadoc --rerun-tasks`), with AGENTS workflow rules tightened to require immediate complete JavaDoc updates for new/changed API elements.
✅ **Docs Portal Baseline (`0.6.x`)**: GitHub Pages workflow now builds and publishes the Docusaurus documentation portal at `https://snapfx.org/`, bundles generated `:snapfx-core:javadoc` under `/api`, and writes a `CNAME` for `snapfx.org` (`.github/workflows/docs-pages.yml`).
✅ **JavaDoc Usability Pass**: Exported API packages now include `package-info.java` overviews, and key entry classes (`SnapFX`, `DockGraph`, `DockLayoutSerializer`, `DockFloatingWindow`) now include concise usage snippets in JavaDoc.
✅ **Changelog Tag Backfill**: `CHANGELOG.md` now contains explicit `v0.5.0` and `v0.6.0` sections, and `Unreleased` is reset to post-tag changes only.
✅ **Docs Content Baseline**: Docusaurus now includes first structured content slices beyond overview pages (User Guide, First Layout tutorial, and Examples catalog) with sidebar navigation wiring.
✅ **Docs Link/Status Refresh**: Documentation pages now expose references as clickable links (including API references), and release-status content reflects current `v0.6.x` progress.
🚧 **Maven Central Publish Baseline (`0.8.x`)**: `snapfx-core` now has Sonatype Central repository wiring plus release-workflow stable-tag publish job gating (`>= v1.0.0`); first successful live Central publish verification is still pending.

## Documentation Scope

- ✅ [STATUS.md](STATUS.md) tracks only the current state: build health, current capabilities, and open issues.
- ✅ [ROADMAP.md](ROADMAP.md) tracks planned work only.
- ✅ [DONE.md](DONE.md) tracks completed milestones and delivered capabilities.
- ✅ [CHANGELOG.md](CHANGELOG.md) tracks versioned historical changes grouped by tags.
- ✅ [TESTING_POLICY.md](TESTING_POLICY.md) defines stable testing rules only (no temporal test statistics).

## Implementation Progress

### Core Architecture (100% ✅)
- ✅ Tree-based model (DockGraph)
- ✅ DockElement interface hierarchy
- ✅ DockNode, DockSplitPane, DockTabPane
- ✅ Smart flattening algorithm (with correct order: flatten first, then cleanup)
- ✅ Auto-cleanup for empty containers (fixed to prevent orphaned containers)
- ✅ Model-View separation
- ✅ Theme management is modularized into dedicated classes (`DockThemeCatalog`, `DockThemeStylesheetManager`) to keep `SnapFX` focused on API orchestration
- ✅ Side-bar Phase 1 model foundation is implemented in `DockGraph` (pinned entries per side, pinned-open state, deterministic pin/restore workflow)
- ✅ Side-bar Phase 1 API + MainDemo manual-test integration is implemented (SnapFX facade + Settings/Layout-menu controls)
- ✅ Side-bar Phase 1 framework rendering baseline is implemented in SnapFX (icon strips + pinned panels + overlay click behavior); newly pinned entries stay collapsed by default, pinned active-icon click collapse behavior is configurable (default collapse) and preserves pin mode during temporary collapse, overlay hit-testing/right-side unpin placement issues were corrected, and sidebar restore now reuses floating-style placement-memory fallback logic

### View Layer (100% ✅)
- ✅ DockLayoutEngine (Model → SceneGraph)
- ✅ DockNodeView (Header + Content)
- ✅ Bidirectional property bindings
- ✅ View caching
- ✅ CSS styling (Modena theme)
- ✅ Context menus for tab headers, dock-node headers, and split panes (with float-action availability policy support and close/float icon parity)
- ✅ DockNode header context menus now close on header press (including presses directly on the same toolbar)
- ✅ DockNode icons render as independent per-view image nodes (no shared-node icon loss across headers/tabs/floating title bars)
- ✅ Container tabs now use representative DockNode title/icon summaries (`Title +N`) instead of internal container class names

### Drag & Drop (100% ✅)
- ✅ DockDragService implementation
- ✅ Ghost overlay during drag
- ✅ Drop zone detection for SplitPanes
- ✅ Mouse event handling
- ✅ TabPane D&D bug fixed (cache invalidation)
- ✅ Auto-rebuild after D&D operations
- ✅ Consistent D&D behavior across all operations
- ✅ Drop zone visualization modes (ALL/SUBTREE/DEFAULT/ACTIVE/OFF)
- ✅ Per-tab insert targeting with visual insert line
- ✅ In-place TabPane reordering to avoid flattening and missed drops
- ✅ Depth-first drop target selection and zone validation
- ✅ Nested TabPanes work correctly (verified)
- ✅ Target invalidation handling (prevents empty containers)
- ✅ Splitter positions preserved on no-op edge drops
- ✅ Ghost overlay stays visible outside the main window (transparent utility stage)
- ✅ Unresolved drops always trigger floating fallback (not only outside main scene)
- ✅ Unresolved drops from multi-node floating layouts now detach the dragged node into a separate floating window (matching float-button/context-menu behavior)
- ✅ Main layout drops accept drags originating from floating windows
- ✅ Empty-main-layout center drops are now accepted: when all nodes are floating and the main layout is empty, dragging back into the empty main area re-docks correctly
- ✅ Escape cancels active drag reliably, including while the mouse button remains pressed
- ✅ Drop-zone sizing now guards tiny bounds so drag hover never throws `Math.clamp` min/max-order exceptions

### Persistence (100% ✅)
- ✅ JSON serialization (Gson)
- ✅ DockLayoutSerializer
- ✅ Typed load-failure diagnostics via `DockLayoutLoadException` (with JSON location context)
- ✅ Unknown/unsupported serialized node types now recover without load failure: factory fallback can provide a custom node, otherwise SnapFX inserts a diagnostic placeholder
- ✅ **DockNodeFactory pattern** - Factory for node recreation
- ✅ **Custom Node IDs** - User-defined stable IDs
- ✅ Save/Load functionality across sessions
- ✅ Layout state preservation
- ✅ Locked state persistence
- ✅ Sidebar state persistence foundation is implemented in `DockLayoutSerializer` (pinned entries, sidebar pinned-open state, and restore-anchor roundtrip)
- ✅ Sidebar state now roundtrips through `SnapFX.saveLayout(...)` / `loadLayout(...)`, and framework side panels are rendered from that state; mixed main/floating/sidebar persistence + lock UX verification in MainDemo is completed

### Locked Mode (100% ✅)
- ✅ Layout locking
- ✅ Disable drag & drop when locked
- ✅ Hide close buttons
- ✅ Tab auto-hide (single tab)

### Keyboard Shortcuts (Framework Baseline ✅)
- ✅ Configurable shortcut API in `SnapFX` (`setShortcut`, `clearShortcut`, `resetShortcutsToDefaults`, `getShortcuts`)
- ✅ Default framework shortcuts: `Ctrl+W` (close active node), `Ctrl+Tab` (next tab), `Ctrl+Shift+Tab` (previous tab), `Escape` (cancel drag), `Ctrl+Shift+P` (toggle active floating always-on-top)
- ✅ `Ctrl+Shift+P` now resolves the active floating window and works from both main and floating scenes
- ✅ MainDemo provides app-level `F11` fullscreen shortcut example (outside framework defaults)

### Hidden Nodes Manager (100% ✅)
- ✅ Close nodes without deletion
- ✅ Restore hidden nodes
- ✅ Menu integration
- ✅ Original position tracking
- ✅ **Close button handler integration**

### Floating Windows (Phase 2 100% ✅)
- ✅ `DockFloatingWindow` as external dock host with its own `DockGraph`/`DockLayoutEngine`
- ✅ Programmatic floating API: `SnapFX.floatNode(...)`
- ✅ Drag-out detach: unresolved drop positions open/update floating windows
- ✅ Programmatic attach API: `SnapFX.attachFloatingWindow(...)`
- ✅ Attach action directly from floating window title bar
- ✅ Demo menu integration for floating/attach workflows
- ✅ Screen-coordinate positioning for multi-monitor usage
- ✅ Floating windows are valid D&D drop targets (split/tab), including floating-to-floating
- ✅ Tab headers include a Float button for tabbed nodes
- ✅ Runtime floating bounds memory per node (float/attach toggle keeps position/size)
- ✅ Undecorated resize handling via edges/corners
- ✅ Re-attach after floating from tabs restores as tab (not forced split)
- ✅ Save/load persistence for floating windows, including floating snapshot restore on layout load
- ✅ Configurable floating pin controls (`ALWAYS`/`AUTO`/`NEVER`) with default always-on-top and lock-mode behavior
- ✅ Source-aware floating pin change callbacks plus always-on-top snapshot persistence
- ✅ Floating title-bar context menu with `Attach to Layout` and always-on-top toggle (icon parity with title-bar controls)
- ✅ Floating title-bar icon sync follows active tab changes in floating tab layouts
- ✅ Floating window title-bar drag now continues while cursor leaves the title bar (scene-level drag tracking)
- ✅ Floating window snapping during title-bar drag supports screen edges, main-window edges, and floating-window edges with configurable API controls, overlap guards, and adjacent-edge alignment
- ✅ Floating title-bar press now hides visible title-bar context menus for consistent outside-click close behavior
- ✅ Maximized floating windows now require a deliberate drag threshold before restore-on-drag triggers
- ✅ Floating resize now respects effective minimum constraints from stage/content minimum sizes
- ✅ Resize cursors now apply reliably near edges over interactive content targets (for example console text areas)
- ✅ Attach-to-layout now restores detached floating-sub-layout nodes back to their previous host context when possible (preferred/neighbor anchors), with silent fallback to active host-root or main layout when anchors are unavailable, including detach-close-remaining-attach host-restore cases
- ✅ Framework stylesheet handling is now automatic on `initialize(...)`; built-in named themes are exposed via map/list (`Light`, `Dark`), and runtime theme switching updates primary and floating scenes via `setThemeStylesheet(...)`

### Debug Tools (100% ✅)
- ✅ DockGraphDebugView
- ✅ Tree visualization
- ✅ Export snapshot functionality
- ✅ Drag state tracking
- ✅ Auto-expand tree view
- ✅ **D&D Activity Log**: Complete logging of all drag & drop actions (7 event types)
- ✅ **Log Export**: Activity log included in clipboard export
- ✅ **Color-coded log entries**: Different colors for different event types

### Module System (100% ✅)
- ✅ module-info.java created
- ✅ JPMS fully configured
- ✅ All dependencies declared
- ✅ Public API exported
- ✅ Reflection access configured
- ✅ Build configuration updated
- ✅ Documentation updated

### Testing (100% ✅)
- ✅ DockGraphTest (69 tests, +11 regression tests plus sidebar model coverage) - Includes sidebar width preference defaults/validation/lock-policy coverage
- ✅ DockLayoutSerializerTest (21 tests) - Includes strict load-failure diagnostics for blank content, malformed JSON, missing required fields, invalid tab selection metadata, unknown-node placeholder diagnostics, unsupported-type recovery with optional factory custom fallback, and sidebar persistence/restore-anchor/width roundtrip coverage (including legacy width fallback)
- ✅ DockLayoutEngineTest (37 tests) - Includes tab/header/splitter context-menu coverage, representative container-tab title/icon behavior, float-availability policy checks, sidebar move-to-sidebar context-menu callback/lock-state coverage, hidden sidebar move-menu state when framework sidebar callbacks are unavailable, header-context-menu dismiss-on-press regression coverage, tiny-bounds drop-zone clamp regression coverage, and empty-layout center-drop-zone collection coverage
- ✅ **SnapFXTest (95 tests)** - Hide/Restore + Floating Window API tests plus configurable shortcut behavior, floating-window snap API propagation/validation, side-bar facade API behavior (pin/restore, lock-aware pinned-open state, save/load roundtrip preservation), side-bar panel width API/roundtrip coverage, framework sidebar build-layout rendering structure coverage (collapsed strip vs. pinned/overlay panels, resize handle presence, width runtime clamping, right-overlay resize-handle pick/z-order regression coverage, sidebar overlay width/resize-handle CI race hardening in tests, and sidebar visibility mode rendering behavior for `AUTO`/`ALWAYS`/`NEVER`), configurable pinned-sidebar active-icon collapse policy coverage, sidebar-restore placement regression coverage for collapsed tab-parent fallback, sidebar strip/panel context-menu action and lock-state coverage, invalid-load failure handling, persistence edge-case coverage for complex floating snapshots, unknown-type layout recovery, unresolved floating-sub-layout D&D detach behavior, floating reattach placement restore/fallback behavior for both float-button and unresolved-drag detach paths, three-window floating-layout detach/attach roundtrip regression coverage (top-left/top-right/bottom cases), detach-close-remaining-attach host-restore fallback coverage, theme stylesheet API behavior (initialize auto-apply + runtime switching + named theme catalog exposure), and empty-main-layout resolved-drop behavior for floating-node re-dock
- ✅ DockGraphSplitTargetDockingTest (1 test)
- ✅ DockDragServiceTest (10 tests) - D&D visibility, tab-hover activation, float-detach callback behavior, ESC drag-cancel handling, and empty-root center-drop-zone validation behavior
- ✅ Gradle multi-module split baseline is in place: framework code/resources now live in `snapfx-core`, demo code/resources in `snapfx-demo`, and tests are now split across both modules (root test task remains a no-source aggregator entry)
- ✅ Gradle publish namespace baseline now uses `org.snapfx` (domain-backed) to prepare Maven Central coordinates for `snapfx-core`
- ✅ Java package and JPMS module namespaces now use `org.snapfx...` across core/demo code and tests (pre-release rename completed before Maven Central publishing)
- ✅ Repository hygiene baseline now ignores `.idea/` entirely and removes tracked IntelliJ workspace/project metadata from version control for cleaner public OSS commits
- ✅ `snapfx-core` now has a local Maven publish dry-run baseline (`maven-publish` + `sourcesJar`/`javadocJar` + POM metadata), and `:snapfx-core:publishToMavenLocal` succeeds
- ✅ DockFloatingWindowTest (31 tests) - Floating title bar controls, context menu behavior (attach/pin icons + attach action), floating-header sidebar move-menu callback forwarding, pin behavior, icon rendering/sync regression coverage, single-node float-menu policy, maximize/restore interaction behavior, scene-level drag continuity (including release/reset and non-primary guard behavior), resize-min constraints, interactive-target cursor reliability, and floating/main edge snapping behavior (including overlap-guard, adjacent-edge cases, and main-window shadow-inset compensation)
- ✅ DockFloatingSnapEngineTest (6 tests) - Snap candidate scoring, overlap-aware candidate generation, and shadow-inset compensation behavior
- ✅ DockDebugOverlayTest (2 tests) - HUD layout-state defaults (managed + pref-sized panel background) and live diagnostics refresh coverage for mutated drag-state updates (`Target`/`Zone` no longer stuck at `none`)
- ✅ MainDemoTest (23 tests) - Demo app icon resource wiring, menu icon behavior, sidebar menu/list helper coverage for Phase-C manual controls, sidebar settings width-control API-parity wiring (including `DockSideBarMode` settings control), debug/settings outer-split divider-stability regression coverage, demo shortcut wiring, floating snap-target settings resolution coverage, load-error message formatting, owner-aware error-alert behavior, FileChooser helper coverage for shared layout/editor chooser configuration, and named theme-catalog/resource coverage
- ✅ DemoNodeFactoryTest (3 tests) - Unknown-node fallback strategy coverage (framework placeholder vs. custom demo fallback node) plus SnapFX integration coverage for unsupported-type recovery with the default demo factory
- ✅ EditorCloseDecisionPolicyTest (5 tests) - Deterministic close-decision policy checks
- ✅ MarkdownDocumentationConsistencyTest (4 tests) - Markdown consistency guardrails focused on Mojibake detection and icon-prefix validation (no brittle content-specific assertions)
- ✅ AboutDialogTest (2 tests) - About dialog branding resources and credit link targets
- ✅ **310/310 tests passing** ✅
- ✅ **Performance tests for large layouts** (50+ nodes with stress move/cleanup operations)
- ✅ **Memory leak cleanup tests** (cache boundedness, undock cleanup, large-layout detach/attach cycles)
- ✅ **Edge case tests** (null inputs, detached nodes, invalid move targets, no-op revision checks)
- ✅ **Regression tests** for all critical bug fixes
- ✅ **Testing Policy** established ([TESTING_POLICY.md](TESTING_POLICY.md))
- ✅ ~87% code coverage
- ✅ All structural integrity tests (no empty containers, no nesting)

### Demo Application (100% ✅)
- ✅ MainDemo with IDE-like layout
- ✅ Menu bar (File, Layout, Help)
- ✅ Toolbar with add/remove functions
- ✅ Lock/unlock layout
- ✅ Save/Load layout
- ✅ Invalid layout loads show user-facing error details in MainDemo (including JSON path context), while unknown serialized node types recover in-place via placeholders without error popups
- ✅ MainDemo error dialogs are owner-aware and attach to the primary stage when available
- ✅ Hidden nodes menu
- ✅ About dialog extracted into dedicated class with dynamic version info, large logo branding, and icon credits
- ✅ About dialog easter egg animation (triple-click logo)
- ✅ Debug view toggle
- ✅ DockDebugOverlay HUD issues are fixed after sidebar Phase 2 (panel layout/background rendering, top-left clipping, and live target/zone diagnostics updates during drag); MainDemo keeps the HUD behind a local debug toggle
- ✅ Settings tab for live layout options (title bar, close buttons, drop visualization, lock, floating pin controls, floating-window snapping controls, and Phase-C pinned side-bar manual test controls for pin/restore/pin-open)
- ✅ File workflows now use shared `FileChooser` helpers for layout open/save and editor open/save-as to keep extension filters and defaults consistent
- ✅ Settings tab now includes a theme selector driven by the SnapFX named theme catalog (`Light`, `Dark`) and applies styles via runtime API
- ✅ Layout menu now includes Phase-C side-bar pin/restore/pin-open test menus for left/right side bars
- ✅ SnapFX now renders framework-level left/right sidebars (icon strips + overlay/pinned panels) in the main layout, and MainDemo uses that rendering for Phase-C manual validation
- ✅ Phase-D sidebar DnD baseline is started in `SnapFX`: unresolved drag releases can now drop into visible sidebar icon strips with exact insertion positioning (including reorder/cross-source handling via the new `DockGraph` sidebar index semantics), sidebar strip icons are now drag sources, and the sidebar strip shows a visible insert-position line during drag hover
- ✅ DnD source handling now supports pinned sidebar nodes in `SnapFX` drop paths (drop back into main layout, drop into floating windows, and unresolved float fallback)
- ✅ Framework node/tab context menus now include built-in `Move to Left Sidebar` / `Move to Right Sidebar` actions (main and floating layouts), wired to the existing SnapFX pin-to-sidebar flow with lock-aware disabling
- ✅ Sidebar nodes now expose framework context menus on both sidebar strip icons and expanded sidebar panel headers (restore, move left/right, pin/unpin panel), with lock-aware disable states
- ✅ Sidebar panels are now resizable per side (LEFT/RIGHT) via framework resize handles, with shared pinned/overlay widths, runtime clamping, layout serializer persistence, and MainDemo Settings parity controls
- ✅ Right sidebar overlay resize now works in unpinned mode as well; resize handles are explicitly bounds-pickable and rendered above overlay panel chrome/shadow to prevent right-side hit-target occlusion
- ✅ SnapFX now exposes `DockSideBarMode` (`ALWAYS`, `AUTO`, `NEVER`) with MainDemo Settings parity; `ALWAYS` renders empty strips for direct DnD targets and `NEVER` suppresses framework sidebar UI plus built-in sidebar move context-menu actions
- ✅ Sidebar Phase 2 interaction parity is complete for the current scope; broader panel-surface sidebar DnD target expansion/polish is deferred to optional backlog items
- ✅ MainDemo debug/settings outer `SplitPane` divider no longer jumps during dock-layout rebuilds; the demo now updates dock content inside a stable host container instead of replacing the split item

### Documentation (100% ✅)
- ✅ [README.md](README.md) updated
- ✅ README now embeds the SnapFX SVG logo from `snapfx-demo/src/main/resources/images/snapfx.svg`
- ✅ README quick start now states planned Maven Central coordinates `org.snapfx:snapfx-core`
- ✅ [ARCHITECTURE.md](ARCHITECTURE.md) complete and corrected
- ✅ [SETUP.md](SETUP.md)
- ✅ [DONE.md](DONE.md)
- ✅ [CHANGELOG.md](CHANGELOG.md) (tag-grouped release history and unreleased changes)
- ✅ [CONTRIBUTING.md](CONTRIBUTING.md) (collaboration workflow and PR expectations)
- ✅ [RELEASING.md](RELEASING.md) (maintainer release/versioning/tag flow)
- ✅ [ROADMAP.md](ROADMAP.md) now starts with overall progress, keeps legend directly below, and no longer includes a version-track block.
- ✅ Architecture decision records are now tracked under `docs/adr/` and linked from README documentation map.
- ✅ Public documentation portal is now hosted at `https://snapfx.org/`, with generated API JavaDoc published at `https://snapfx.org/api/`.
- ✅ README now exposes a clear public-preview status (`0.x` release-readiness), explicitly notes pre-Maven-Central state, and points users to status/roadmap docs for ongoing work.
- ✅ Public API JavaDoc now includes package-level overview pages (`package-info.java`) across exported modules and practical entry-point snippets for core API classes.
- ✅ Runtime theme-stylesheet behavior is documented in ADR [docs/adr/0002-runtime-theme-stylesheet-management.md](docs/adr/0002-runtime-theme-stylesheet-management.md)
- ✅ Sidebar overlay/pin rendering state split is documented in ADR [docs/adr/0003-sidebar-overlay-and-pin-rendering-state-split.md](docs/adr/0003-sidebar-overlay-and-pin-rendering-state-split.md)
- ✅ Sidebar panel width state/runtime-clamping behavior is documented in ADR [docs/adr/0004-sidebar-panel-width-state-and-runtime-clamping.md](docs/adr/0004-sidebar-panel-width-state-and-runtime-clamping.md)
- ✅ Sidebar visibility mode and framework sidebar-menu gating behavior are documented in ADR [docs/adr/0005-sidebar-visibility-mode-and-framework-menu-gating.md](docs/adr/0005-sidebar-visibility-mode-and-framework-menu-gating.md)
- ✅ Docusaurus content baseline pages are now available at `https://snapfx.org/`: User Guide, First Layout tutorial, and Examples.

## Issues

### Open
- ⚠️ Performance: Benchmark trend tracking for large layouts not implemented
- ⚠️ Memory: Automated heap profiling in CI not implemented
- ⚠️ UI: Global interaction animations missing (only About dialog easter egg animation exists; tracked in [ROADMAP.md](ROADMAP.md) Phase 3.3)
- ⚠️ UI: Context-menu extensibility API for custom menu items is not implemented yet (tracked in [ROADMAP.md](ROADMAP.md) Phase 3.2)
- ⚠️ Docs: Docusaurus/JavaDoc multi-version publication is intentionally deferred until after `1.0.0` (current pre-`1.0.0` policy is latest-only docs/API on `snapfx.org`).

## Next Steps

See [ROADMAP.md](ROADMAP.md) for detailed future development plans.

**Priority**: `0.7.x` packaging hardening is now covered (cross-platform packaging, smoke policy, release checksums); next release-readiness focus is `0.8.x` Maven Central dry-run verification and maintainer hardening for the first live publish at `v1.0.0`.

---

**Version**: Git-derived via `gradle-jgitver` (tag-based)  
**Java**: 21 (LTS)  
**JavaFX**: 21  
**Build Tool**: Gradle 9.0

---

## Documentation Policy

- ✅ Core documentation files: [README.md](README.md), [SETUP.md](SETUP.md), [ARCHITECTURE.md](ARCHITECTURE.md), [STATUS.md](STATUS.md), [ROADMAP.md](ROADMAP.md), [DONE.md](DONE.md), [CHANGELOG.md](CHANGELOG.md), [TESTING_POLICY.md](TESTING_POLICY.md), [CONTRIBUTING.md](CONTRIBUTING.md), [RELEASING.md](RELEASING.md), [AGENTS.md](AGENTS.md).
- ✅ Responsibility split: [STATUS.md](STATUS.md) = current status and open issues.
- ✅ Responsibility split: [ROADMAP.md](ROADMAP.md) = planned work.
- ✅ Responsibility split: [DONE.md](DONE.md) = completed work.
- ✅ Responsibility split: [CHANGELOG.md](CHANGELOG.md) = versioned historical changes.
- ✅ Responsibility split: [TESTING_POLICY.md](TESTING_POLICY.md) = stable testing rules.
- ⚠️ Avoid additional feature-specific markdown logs when information already belongs in one of the core files.
