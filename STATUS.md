# Project Status

**Last Updated**: 2026-02-16

## Build Status

✅ **Build**: `BUILD SUCCESSFUL`  
✅ **Tests**: All 189 tests passing (latest full suite)
✅ **Module System**: Fully implemented (JPMS)  
✅ **Demo App**: Running successfully  
✅ **CI Automation**: GitHub Actions workflows added for push/PR tests and tag-triggered releases  
✅ **Release Notes Automation**: `git-cliff` generates tag-based GitHub Release notes

## Documentation Scope

- ✅ `STATUS.md` tracks only the current state: build health, current capabilities, and open issues.
- ✅ `ROADMAP.md` tracks planned work only.
- ✅ `DONE.md` tracks completed milestones and delivered capabilities.
- ✅ `CHANGELOG.md` tracks versioned historical changes grouped by tags.
- ✅ `TESTING_POLICY.md` defines stable testing rules only (no temporal test statistics).

## Implementation Progress

### Core Architecture (100% ✅)
- ✅ Tree-based model (DockGraph)
- ✅ DockElement interface hierarchy
- ✅ DockNode, DockSplitPane, DockTabPane
- ✅ Smart flattening algorithm (with correct order: flatten first, then cleanup)
- ✅ Auto-cleanup for empty containers (fixed to prevent orphaned containers)
- ✅ Model-View separation

### View Layer (100% ✅)
- ✅ DockLayoutEngine (Model → SceneGraph)
- ✅ DockNodeView (Header + Content)
- ✅ Bidirectional property bindings
- ✅ View caching
- ✅ CSS styling (Modena theme)
- ✅ Context menus for tab headers, dock-node headers, and split panes (with float-action availability policy support and close/float icon parity)
- ✅ DockNode icons render as independent per-view image nodes (no shared-node icon loss across headers/tabs/floating title bars)

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
- ✅ Main layout drops accept drags originating from floating windows
- ✅ Escape cancels active drag reliably, including while the mouse button remains pressed
- ✅ Drop-zone sizing now guards tiny bounds so drag hover never throws `Math.clamp` min/max-order exceptions

### Persistence (100% ✅)
- ✅ JSON serialization (Gson)
- ✅ DockLayoutSerializer
- ✅ **DockNodeFactory pattern** - Factory for node recreation
- ✅ **Custom Node IDs** - User-defined stable IDs
- ✅ Save/Load functionality across sessions
- ✅ Layout state preservation
- ✅ Locked state persistence

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
- ✅ DockGraphTest (56 tests, +11 regression tests)
- ✅ DockLayoutSerializerTest (9 tests, +1 regression test)
- ✅ DockLayoutEngineTest (29 tests) - Includes tab/header/splitter context-menu coverage, float-availability policy checks, and tiny-bounds drop-zone clamp regression coverage
- ✅ **SnapFXTest (44 tests)** - Hide/Restore + Floating Window API tests plus configurable shortcut behavior
- ✅ DockGraphSplitTargetDockingTest (1 test)
- ✅ DockDragServiceTest (8 tests) - D&D visibility, tab-hover activation, float-detach callback behavior, and ESC drag-cancel handling
- ✅ DockFloatingWindowTest (16 tests) - Floating title bar controls, context menu behavior (attach/pin icons + attach action), pin behavior, icon rendering/sync regression coverage, single-node float-menu policy, and maximize/restore interaction behavior
- ✅ MainDemoTest (5 tests) - Demo app icon resource wiring, menu icon behavior, and demo shortcut wiring
- ✅ EditorCloseDecisionPolicyTest (5 tests) - Deterministic close-decision policy checks
- ✅ SimpleExampleTest (2 tests) - Stylesheet resource resolution behavior
- ✅ MarkdownDocumentationConsistencyTest (12 tests) - Markdown consistency guardrails
- ✅ AboutDialogTest (2 tests) - About dialog branding resources and credit link targets
- ✅ **189/189 tests passing** ✅
- ✅ **Performance tests for large layouts** (50+ nodes with stress move/cleanup operations)
- ✅ **Memory leak cleanup tests** (cache boundedness, undock cleanup, large-layout detach/attach cycles)
- ✅ **Edge case tests** (null inputs, detached nodes, invalid move targets, no-op revision checks)
- ✅ **Regression tests** for all critical bug fixes
- ✅ **Testing Policy** established (TESTING_POLICY.md)
- ✅ ~87% code coverage
- ✅ All structural integrity tests (no empty containers, no nesting)

### Demo Application (100% ✅)
- ✅ MainDemo with IDE-like layout
- ✅ Menu bar (File, Layout, Help)
- ✅ Toolbar with add/remove functions
- ✅ Lock/unlock layout
- ✅ Save/Load layout
- ✅ Hidden nodes menu
- ✅ About dialog extracted into dedicated class with dynamic version info, large logo branding, and icon credits
- ✅ About dialog easter egg animation (triple-click logo)
- ✅ Debug view toggle
- ✅ Settings tab for live layout options (title bar, close buttons, drop visualization, lock, floating pin controls)

### Documentation (100% ✅)
- ✅ README.md updated
- ✅ README now embeds the SnapFX SVG logo from `src/main/resources/images/snapfx.svg`
- ✅ ARCHITECTURE.md complete and corrected
- ✅ SETUP.md
- ✅ DONE.md
- ✅ CHANGELOG.md (tag-grouped release history and unreleased changes)
- ✅ CONTRIBUTING.md (collaboration workflow and PR expectations)
- ✅ RELEASING.md (maintainer release/versioning/tag flow)
- ✅ ROADMAP.md now starts with overall progress, keeps legend directly below, and no longer includes a version-track block.

## Issues

### Open
- ⚠️ Performance: Benchmark trend tracking for large layouts not implemented
- ⚠️ Memory: Automated heap profiling in CI not implemented
- ⚠️ UI: Global interaction animations missing (only About dialog easter egg animation exists; tracked in `ROADMAP.md` Phase 3.3)
- ⚠️ UI: Context-menu extensibility API for custom menu items is not implemented yet (tracked in `ROADMAP.md` Phase 3.2)

## Next Steps

See [ROADMAP.md](ROADMAP.md) for detailed future development plans.

**Priority**: Continue Phase 3 user-experience backlog (customizable context-menu API and interaction polish).

---

**Version**: Git-derived via `gradle-jgitver` (tag-based)  
**Java**: 21 (LTS)  
**JavaFX**: 21  
**Build Tool**: Gradle 9.0

---

## Documentation Policy

- ✅ Core documentation files: `README.md`, `SETUP.md`, `ARCHITECTURE.md`, `STATUS.md`, `ROADMAP.md`, `DONE.md`, `CHANGELOG.md`, `TESTING_POLICY.md`, `CONTRIBUTING.md`, `RELEASING.md`, `AGENTS.md`.
- ✅ Responsibility split: `STATUS.md` = current status and open issues.
- ✅ Responsibility split: `ROADMAP.md` = planned work.
- ✅ Responsibility split: `DONE.md` = completed work.
- ✅ Responsibility split: `CHANGELOG.md` = versioned historical changes.
- ✅ Responsibility split: `TESTING_POLICY.md` = stable testing rules.
- ⚠️ Avoid additional feature-specific markdown logs when information already belongs in one of the core files.

