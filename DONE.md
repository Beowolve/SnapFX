# SnapFX Framework - Completed Features

**Last Updated**: 2026-02-26

SnapFX has been fully implemented with core functionality and is production-ready for large applications.

## ✅ What Has Been Completed

### Core Framework (26 classes)

#### Model Layer (7 classes)
- ✅ `DockGraph` - Central data structure for the docking system
- ✅ `DockElement` - Base interface for all dockable elements
- ✅ `DockContainer` - Interface for container elements
- ✅ `DockNode` - Wrapper for dockable JavaFX nodes
- ✅ `DockSplitPane` - Split container with smart flattening
- ✅ `DockTabPane` - Tab container with auto-hide
- ✅ `DockPosition` - Enum for dock zones (5 positions)

#### View Layer (4 classes)

- ✅ `DockLayoutEngine` - Converts model to JavaFX SceneGraph
- ✅ `DockNodeView` - Visual representation with header and float/close controls
- ✅ `DockDropZone` - Drop-zone definition (bounds, target, depth)
- ✅ `DockDropZoneType` - Drop-zone category enum
- ✅ CSS-driven control glyphs for dock/floating controls (stylesheet-configurable)

#### Drag & Drop (3 classes)

- ✅ `DockDragService` - Central D&D management with overlays
- ✅ `DockDragData` - Transfer object for drag operations
- ✅ `DockDropVisualizationMode` - Drop-zone visualization modes

#### Persistence (2 classes)
- ✅ `DockLayoutSerializer` - JSON-based serialization with Gson
- ✅ `DockLayoutLoadException` - Typed layout-load failure model with JSON-path diagnostics
#### Debug Tools (1 class)
- ✅ `DockGraphDebugView` - Tree visualization and export
- ✅ `D&D Activity Log` - Comprehensive logging of all drag & drop operations
- ✅ `Color-coded entries` - Visual differentiation of event types
- ✅ `Export integration` - Log included in snapshot export

#### API (3 classes)
- ✅ `SnapFX` - Main facade providing simple, fluent API
- ✅ `DockShortcutAction` - Built-in framework shortcut action enum (`CLOSE_ACTIVE_NODE`, `NEXT_TAB`, `PREVIOUS_TAB`, `CANCEL_DRAG`, `TOGGLE_ACTIVE_FLOATING_ALWAYS_ON_TOP`)
- ✅ `DockFloatingWindow` - Floating dock host with custom title bar, resize handling, and subtree support
- ✅ Split ratio API in `SnapFX` (`setRootSplitRatios(...)`, `setSplitRatios(...)`)
- ✅ Configurable shortcut API in `SnapFX` (`setShortcut(...)`, `clearShortcut(...)`, `resetShortcutsToDefaults(...)`, `getShortcuts()`)

#### Theme Management (2 classes)
- ✅ `DockThemeCatalog` - Built-in named theme metadata (`Light`/`Dark`) with deterministic map/list access
- ✅ `DockThemeStylesheetManager` - Stylesheet normalization, resolution, and managed scene application lifecycle

#### Demo (4 classes)
- ✅ `MainDemo` - Full IDE-like layout with all features
- ✅ `SimpleExample` - Minimal usage example
- ✅ `DockNodeFactory` - Helper for creating demo nodes
- ✅ `AboutDialog` - Dedicated About dialog with branding, credits, and easter egg animation
- ✅ `Settings tab` - Live layout options in the debug panel, including floating-window snapping API controls (enable, distance, targets) and runtime theme switching via named SnapFX themes (`Light`/`Dark`)

### Module System
- ✅ `module-info.java` - Java Platform Module System descriptor
- ✅ Full JPMS support with proper exports and opens
- ✅ Compatible with Java 21 module system

### Testing (13 test classes, 278 tests)
- ✅ `DockGraphTest` (62 tests) - Tree manipulation and algorithms plus sidebar model behavior coverage (pin/restore, side switching, lock no-op semantics, pinned-node counting, and unpin-without-restore helper behavior)
  - **+11 regression tests** for critical bug fixes
  - Tests for empty container prevention
  - Tests for target invalidation during move
  - Tests for complex D&D sequences
  - Tests for flattening logic
  - Performance stress tests for large layouts (50+ nodes)
  - Edge case tests for null/no-op/detached-target handling
- ✅ `DockLayoutSerializerTest` (21 tests) - Persistence functionality and strict load-failure diagnostics for blank content, malformed JSON, missing required fields, invalid tab selection metadata, unknown-node placeholder diagnostics, unsupported-type recovery with optional factory custom fallback, plus sidebar persistence/restore-anchor/width roundtrip coverage (including legacy width fallback)
  - **+1 regression test** for locked state synchronization (2026-02-10)
- ✅ `DockLayoutEngineTest` (36 tests) - View creation with TestFX, context-menu interaction coverage, representative container-tab title/icon behavior, float-availability policy checks, sidebar move-to-sidebar context-menu callback/lock-state coverage plus hidden-menu behavior when sidebar callbacks are unavailable, header-context-menu dismiss-on-press regression coverage, and tiny-bounds drop-zone clamp regression coverage
  - Memory cleanup tests for cache boundedness and undock/rebuild cycles
  - Layout optimization tests for empty/single-child roots
- ✅ `SnapFXTest` (94 tests) - Hide/Restore + Floating Window API behavior, configurable shortcut behavior, floating-window snap API propagation/validation, side-bar facade API coverage (pin/restore, lock-aware pinned-open state, save/load roundtrip preservation, panel-width API/roundtrip behavior), framework sidebar build-layout rendering structure coverage (collapsed strip vs. pinned/overlay panels, resize-handle presence, runtime width clamping, right-overlay resize-handle pick/z-order regression coverage, and `DockSideBarMode` rendering behavior for `AUTO`/`ALWAYS`/`NEVER`), configurable pinned-sidebar active-icon collapse policy coverage, sidebar-restore placement regression coverage for collapsed tab-parent fallback, invalid-load failure handling, persistence edge-case coverage for complex floating snapshots, unknown-type layout recovery, unresolved floating-sub-layout D&D detach coverage, host-aware floating reattach placement restore/fallback coverage, three-window floating-layout detach/attach roundtrip coverage for top-left/top-right/bottom nodes, detach-close-remaining-attach host-restore fallback coverage, and theme stylesheet API behavior (initialize auto-apply + runtime switching + named theme catalog exposure)
- ✅ `DockGraphSplitTargetDockingTest` (1 test) - Split-target docking regression coverage
- ✅ `DockDragServiceTest` (8 tests) - D&D visibility, tab-hover activation, float-detach callback behavior, and ESC drag-cancel handling
- ✅ `DockFloatingWindowTest` (31 tests) - Floating title-bar controls, context-menu behavior (attach/pin icons + attach action), floating-header sidebar move-menu callback forwarding, pin behavior, icon rendering/sync regression coverage, single-node float-menu policy, maximize/restore interaction behavior, scene-level drag continuity (including release/reset and non-primary guard behavior), resize-min constraints, interactive-target cursor reliability, and floating/main edge snapping behavior (including overlap-guard, adjacent-edge cases, and main-window shadow-inset compensation)
- ✅ `DockFloatingSnapEngineTest` (6 tests) - Snap candidate scoring, overlap-aware candidate generation, and shadow-inset compensation coverage
- ✅ `MainDemoTest` (23 tests) - Application icon resources, menu icon behavior, sidebar menu/list helper coverage for Phase-C manual controls, sidebar settings width-control API-parity wiring (including `DockSideBarMode` settings control), debug/settings outer-split divider-stability regression coverage, demo shortcut wiring, floating snap-target settings resolution coverage, load-error message formatting, owner-aware error-alert behavior, FileChooser helper coverage for shared layout/editor chooser configuration, and named theme-catalog/resource coverage
- ✅ `DemoNodeFactoryTest` (3 tests) - Unknown-node fallback strategy coverage (framework placeholder vs. demo custom fallback), plus SnapFX integration coverage for unsupported-type recovery with the default demo factory
- ✅ `AboutDialogTest` (2 tests) - About dialog resources and credit link targets
- ✅ `EditorCloseDecisionPolicyTest` (5 tests) - Deterministic close-decision behavior for dirty editor nodes
- ✅ `MarkdownDocumentationConsistencyTest` (4 tests) - Documentation consistency guardrails focused on Mojibake detection and icon-prefix validation (content-specific wording assertions removed)
- ✅ CI flake guard for critical interaction suites (`SnapFXTest`, `DockFloatingWindowTest`, `DockDragServiceTest`) runs 3x per CI execution
- ✅ `SnapFXTest` sidebar overlay width/resize-handle tests no longer prebuild the layout before reflective sidebar icon clicks, avoiding a CI-only async `requestRebuild()` race before the final scene build
- ✅ All tests passing (306/306) ✅
- ✅ **Testing Policy** established ([TESTING_POLICY.md](TESTING_POLICY.md))
- ✅ Mandatory regression tests for all bug fixes

### Documentation (11 files)
- ✅ [README.md](README.md) - Project overview and quick start
- ✅ [SETUP.md](SETUP.md) - Development environment setup
- ✅ [ARCHITECTURE.md](ARCHITECTURE.md) - Complete architecture documentation
- ✅ [STATUS.md](STATUS.md) - Current project status
- ✅ [ROADMAP.md](ROADMAP.md) - Future development plans
- ✅ [DONE.md](DONE.md) - Completed features (this file)
- ✅ [CHANGELOG.md](CHANGELOG.md) - Versioned release history grouped by tags
- ✅ [TESTING_POLICY.md](TESTING_POLICY.md) - Stable testing rules and quality gates (policy-only)
- ✅ [CONTRIBUTING.md](CONTRIBUTING.md) - Collaboration workflow, branch strategy, and PR quality gates
- ✅ [RELEASING.md](RELEASING.md) - Maintainer release/versioning/tag and CI release flow
- ✅ [AGENTS.md](AGENTS.md) - Persistent collaboration rules and workflow constraints
- ✅ README clarifies TitleBarMode.AUTO behavior and tab-only drag handling
- ✅ README includes a MainDemo screenshot preview near the top
- ✅ MainDemo preview screenshot (`docs/images/main-demo.png`) is regenerated after visual changes, including Phase-C sidebar testing controls
- ✅ MainDemo preview screenshot (`docs/images/main-demo.png`) refreshed again after adding visible pinned-sidebar strips to the demo layout
- ✅ MainDemo preview screenshot (`docs/images/main-demo.png`) refreshed again after switching sidebar visuals to the framework-rendered icon-strip/panel implementation
- ✅ README embeds the SnapFX SVG logo for repository and future GitHub Pages branding
- ✅ README now includes a documentation map that defines ownership and purpose of each core markdown file
- ✅ README and SETUP license sections now state MIT licensing with explicit personal/commercial use support
- ✅ README quick start is now framework-focused and excludes generic Gradle/module setup templates
- ✅ README no longer includes repository-maintainer workflow sections (Versioning/Branch Strategy/CI/CD)
- ✅ Removed README maintainer workflow sections are now documented in dedicated [CONTRIBUTING.md](CONTRIBUTING.md) and [RELEASING.md](RELEASING.md) guides
- ✅ Historical project changes are now consolidated into [CHANGELOG.md](CHANGELOG.md) with release-tag grouping
- ✅ [STATUS.md](STATUS.md) now keeps open issues only; completed/fixed history is maintained in [CHANGELOG.md](CHANGELOG.md)
- ✅ [ROADMAP.md](ROADMAP.md) version-track section removed; overall progress is now first and legend follows directly for faster status scanning
- ✅ [STATUS.md](STATUS.md) wording now avoids historical delta suffixes in current-state bullets (for example `was ...` / `improved from ...`)
- ✅ [ROADMAP.md](ROADMAP.md) update instructions now include recalculating phase percentages and total completion
- ✅ Issue tracking consolidated into [STATUS.md](STATUS.md); ROADMAP lists planned work only
- ✅ Fixed markdown encoding artifacts (Unicode icon Mojibake) in roadmap/docs content
- ✅ Documentation scope is now explicit: `STATUS` = current, `ROADMAP` = planned, `DONE` = completed, `TESTING_POLICY` = rules-only
- ✅ AGENTS collaboration rules now require per-fix commit message body lines and method extraction for multi-statement UI callbacks
- ✅ AGENTS collaboration rules now require minimal-diff edits across all file changes, plus inline rendered-icon comments for `\u...` icon constants
- ✅ ADR documentation baseline added: significant design decisions are now captured under `docs/adr/` and reflected in architecture/API docs
- ✅ Markdown documentation consistency tests now keep Unicode icon constants readable via inline icon comments

### Resources
- ✅ `snapfx.css` - Native Modena theme styling
- ✅ Icon set from Yusuke Kamiyamane (64 icons in 16px size)
- ✅ SnapFX logo asset set (`svg`, `ico`, `xcf`, and PNG sizes 16/24/32/48/64/128)

## 🎯 Core Features Implemented

### Architecture
- ✅ **Model-View Separation**: Complete decoupling of logical structure from visual representation
- ✅ **Tree-Based Model**: Hierarchical structure for docking elements
- ✅ **Interface-Based Design**: Clean contracts for elements and containers
- ✅ **SOLID Principles**: Single responsibility, open/closed, etc.
- ✅ **Java Module System**: Full JPMS support

### Docking Operations
- ✅ **Dock**: Add nodes to the layout with position (CENTER, TOP, BOTTOM, LEFT, RIGHT)
- ✅ **Undock**: Remove nodes from the layout
- ✅ **Move**: Reposition nodes within the layout
- ✅ **Pinned side-bar model foundation**: `DockGraph` now supports pinned sidebar entries per side, pinned-open sidebar state flags, and deterministic pin/restore workflows for Phase C groundwork
- ✅ **Sidebar ordered-insert/reorder model foundation**: `DockGraph` now supports index-based sidebar insertion/reordering and cross-side moves with clamped bounds (foundation for exact-position sidebar DnD)
- ✅ **Side-bar facade API foundation**: `SnapFX` now exposes pinned side-bar pin/restore/pin-open/collapse/query operations so Phase C behavior is testable without direct model access
- ✅ **Smart Flattening**: Automatic optimization to prevent nested containers with same orientation
- ✅ **SplitPane optimization verified**: No nested same-orientation SplitPanes (horizontal + vertical coverage)
- ✅ **Auto-Cleanup**: Empty containers automatically remove themselves
- ✅ **Hidden Nodes**: Close without deletion, restore later

### Visual Features
- ✅ **DockNodeView**: Header with title plus float/close controls
- ✅ **Property Bindings**: Reactive UI updates via JavaFX properties
- ✅ **CSS Styling**: Native Modena theme integration
- ✅ **Tab Overflow Dropdown**: Menu shows titles when using custom tab graphics
- ✅ **Container tab summary headers**: Tabs hosting nested layouts now show representative DockNode title/icon with `+N` suffix instead of container class names
- ✅ **Tab Auto-Hide**: In locked mode, tabs only visible when >1
- ✅ **Close Button Options**: Toggle tab/title close buttons, keep tab close always visible, align styling, and hide the title bar when desired
- ✅ **CSS-based Control Glyphs**: Dock/floating control icons are stylesheet-defined; title close glyph is aligned with tab close styling
- ✅ **Control Button Interaction Fixes**: Tab float and floating-window title-bar buttons no longer lose clicks to drag interception
- ✅ **Maximized title-bar interaction parity**: Double-click restore and drag-to-restore behavior for floating windows
- ✅ **MainDemo application icon**: Multi-size SnapFX branding icons are applied to the primary stage
- ✅ **MainDemo layout menu icons**: Hidden/Floating menu actions now show dock-node icons when available
- ✅ **Floating pin configuration**: Pin button mode (`ALWAYS`/`AUTO`/`NEVER`), default pinned state, lock behavior, and toggle enablement are configurable via API
- ✅ **Floating pin persistence + events**: Always-on-top state is persisted in floating layout snapshots and exposed through source-aware pin change callbacks
- ✅ **Context menu baseline**: Right-click actions for tabs, splitters, dock-node headers, and floating title bars are implemented (including `Attach to Layout`, always-on-top toggle, and control-icon parity for attach/pin/close/float actions)
- ✅ **Dock-node header context-menu dismiss behavior**: Header context menus now hide on header press, including direct clicks on the same toolbar area
- ✅ **Floating single-node float policy parity**: Float context action is hidden for single-node floating layouts, matching button visibility behavior
- ✅ **Floating title-bar icon correctness**: DockNode icons are image-based and rendered per view, so floating title-bar icons stay visible and follow active tabs
- ✅ **Floating window snapping (MVP)**: Title-bar drag snapping now supports screen edges, main-window edges, and peer floating-window edges with configurable enable/targets/distance API, perpendicular-overlap guards, adjacent-edge alignment for both main and floating snap targets, and main-window shadow-inset compensation for decorated stages
- ✅ **Snapping architecture cleanup**: Candidate generation and overlap-aware snapping logic is centralized in `DockFloatingSnapEngine`, reducing `DockFloatingWindow` complexity and improving testability
- ✅ **MainDemo snapping API controls**: Debug settings now expose snapping enable toggle, snap distance, and snap-target selection to support manual verification workflows
- ✅ **MainDemo side-bar Phase 1 test controls**: Settings tab and Layout menu now expose pinned side-bar pin/restore/pin-open workflows for manual Phase-C validation before visual side-panel rendering lands
- ✅ **MainDemo visual pinned-sidebar strips**: MainDemo now renders left/right sidebar strips from the pinned side-bar model state (pinned-open-state-aware) with restore and side-switch actions for direct manual testing
- ✅ **Framework sidebar visual baseline**: SnapFX now renders left/right sidebar icon strips with immediate title tooltips, click-to-open overlay panels, same-icon toggle close, outside-click overlay close, and pin/unpin switching to layout-consuming side panels
- ✅ **MainDemo framework-sidebar validation path**: MainDemo now uses the framework-rendered sidebar UI again (instead of the temporary demo-only strip wrapper) for Phase-C manual testing
- ✅ **MainDemo D&D debug HUD temporary disable**: `DockDebugOverlay` HUD overlay is temporarily disabled in MainDemo until sidebar interaction work is completed; a post-sidebar fix/re-enable follow-up is tracked for background/clipping/data-text issues
- ✅ **DockDebugOverlay HUD fixes**: `DockDebugOverlay` now renders as a managed pref-sized HUD panel again (background and top-left clipping fixed), refreshes live target/zone diagnostics during drag even when `DockDragData` mutates in place, and shows dock-node target titles in the HUD; MainDemo keeps the HUD behind a local debug toggle
- ✅ **Phase-C sidebar interaction polish fixes**: Pinning now starts collapsed by default, overlay hit-testing no longer blocks sidebar interactions, and right-side unpin keeps overlay panels on the correct side
- ✅ **Pinned active-icon collapse policy option**: SnapFX now exposes a configurable policy for clicking the active icon of a pinned-open sidebar panel (default collapses), and MainDemo exposes the toggle in Settings for manual UX validation
- ✅ **Pinned active-icon collapse pin-preservation fix**: Collapsing a pinned-open sidebar panel via active icon now keeps the sidebar in pinned mode and reopens pinned on the next icon click
- ✅ **Sidebar restore placement-memory fix**: Restoring from sidebars via `SnapFX` now reuses floating-style preferred/neighbor anchor fallback logic, avoiding restore misplacements when pinning collapses the original parent container
- ✅ **Model helper for sidebar-restore orchestration**: `DockGraph.unpinFromSideBar(...)` removes pinned entries without forcing immediate fallback docking, enabling higher-level restore pipelines
- ✅ **Phase-C sidebar manual verification completion**: Mixed main/floating/sidebar save-load, lock-mode behavior, and configurable pinned active-icon collapse policy were re-tested in MainDemo without new defects
- ✅ **Host-aware floating reattach placement**: Nodes detached from floating sub-layouts now reattach to remembered host context (preferred/neighbor anchors) with deterministic fallback to active host root or main layout when anchors are unavailable
- ✅ **Runtime theme API + auto stylesheet wiring**: `initialize(...)` now applies the default SnapFX stylesheet automatically; `setThemeStylesheet(...)` switches theme at runtime for primary and floating scenes
- ✅ **Dark theme resource**: Added `snapfx-dark.css` and integrated theme switching in MainDemo settings
- ✅ **Named theme catalog API**: SnapFX now exposes built-in themes as ordered name/path metadata (`Light`/`Dark`) via list/map helpers
- ✅ **Theme logic modularization**: Stylesheet catalog + apply/resolve logic moved out of `SnapFX` into `org.snapfx.theme`
- ✅ **View Caching**: Performance optimization through view reuse

### Drag & Drop (Baseline + Critical Bug Fixes)

- ✅ **Ghost Overlay**: Visual feedback during drag
- ✅ **Global Ghost Overlay**: Visible across window boundaries via transparent utility stage
- ✅ **Ghost Overlay Offset**: Positioned away from cursor to keep drop targets visible
- ✅ **Unresolved Drop Fallback**: Non-drop-zone releases trigger floating behavior
- ✅ **Escape Drag Cancel Reliability**: Active drag now cancels reliably even while mouse is still held
- ✅ **Cross-Window D&D**: Dock between main layout and floating windows
- ✅ **Topmost-surface D&D targeting**: For overlapping floating/main windows, preview and drop resolve only on the frontmost surface under the cursor
- ✅ **Floating lock-state control parity**: Floating title-bar controls hide in lock mode and floating window close is blocked while locked
- ✅ **Single-node floating control cleanup**: Inner dock-node close/float controls are hidden for single-node floating layouts while keeping header drag usability
- ✅ **Float-from-floating extraction parity**: Floating layout float actions and unresolved D&D releases now both detach the selected node into a separate floating window
- ✅ **Floating close-to-hidden behavior**: Closing floating windows via `X` moves nodes to hidden windows list
- ✅ **Configurable close behavior**: Close requests now support framework-level default behavior selection (`HIDE` or `REMOVE`)
- ✅ **Source-aware close callbacks**: Close interception and outcome callbacks now cover tab, title-bar, and floating-window close requests consistently
- ✅ **Floating window save/load persistence**: Open floating windows and bounds are restored via layout snapshots
- ✅ **Sidebar persistence foundation**: `DockLayoutSerializer` now serializes/deserializes pinned sidebar entries, pinned-open sidebar state, and remembered restore anchors (including sidebar-only payloads without a main root)
- ✅ **Floating drag/drop feedback parity**: Floating windows render drop zones and active drop indicator during drag
- ✅ **Sidebar DnD strip-target baseline**: Unresolved drag releases can drop onto visible sidebar icon strips with exact insertion positioning, sidebar strip icons now act as drag sources (including drag-outside float fallback), the sidebar strip renders an insert-position line during drag hover, and pinned sidebar nodes now participate correctly as DnD sources for main-layout, floating-window, and float-fallback drops
- ✅ **Framework sidebar move context-menu baseline**: Dock-node header and tab context menus now expose built-in `Move to Left Sidebar` / `Move to Right Sidebar` actions (including floating layouts) wired to the SnapFX sidebar pinning flow with lock-aware disabling
- ✅ **Sidebar node context-menu parity**: Sidebar strip icons and expanded sidebar panel headers now expose built-in restore/move/pin actions (`Restore from Sidebar`, `Move to Left/Right Sidebar`, `Pin/Unpin Sidebar Panel`) with lock-aware disabling
- ✅ **Resizable sidebar panel widths**: Sidebar panels can now be resized per side via framework resize handles, use shared width state for pinned/overlay modes, clamp at runtime to available layout width, and persist via `DockLayoutSerializer`
- ✅ **Sidebar visibility mode API (`DockSideBarMode`)**: SnapFX now supports `ALWAYS` / `AUTO` / `NEVER` sidebar UI modes (with MainDemo Settings parity), including empty-strip rendering for direct DnD targets and framework sidebar move-context-menu suppression in `NEVER`
- ✅ **Right overlay sidebar resize-handle hit-target fix**: Unpinned right sidebar overlays now resize reliably because the resize handle is rendered above overlay panel chrome/shadow and remains explicitly pickable on its full bounds
- ✅ **MainDemo debug/settings split divider stability**: The outer demo `SplitPane` divider between the dock area and Debug/Settings tabs now stays stable across dock-layout rebuilds by updating a persistent left-side host container instead of replacing the split item
- ✅ **Drop Zones**: Detection for SplitPane areas
- ✅ **Drop zone visualization modes (ALL/SUBTREE/DEFAULT/ACTIVE/OFF)**
- ✅ **Per-tab insert targeting with visible insert line**
- ✅ **Depth-first drop target selection and zone validation**
- ✅ **In-place TabPane reordering**: Prevents flattening and missed drops (2026-02-14)
- ✅ **Mouse Tracking**: Cursor position tracking
- ✅ **Drag Initiation**: From tab headers and node headers
- ✅ **TabPane D&D Fixed**: Tabs maintain D&D capability after being moved (Critical bug fix 2026-02-10)
  - Proper cache invalidation in DockLayoutEngine
  - Views correctly rebuilt after structure changes
  - Hit-testing works reliably
- ✅ **Auto-Rebuild Fixed**: D&D works consistently after every operation (Critical bug fix 2026-02-10)
  - Auto-rebuild on every revision change
  - Views automatically refreshed in Scene-Graph
  - findElementAt() always finds valid targets
- ✅ **Empty Container Prevention**: Tree integrity maintained (Critical bug fix 2026-02-10)
  - Cleanup logic reordered: flatten first, then cleanup
  - Target invalidation fixed: find target by ID after undock
  - Handles tree restructuring during move operations
- ✅ **Nested TabPanes**: Verified to work correctly (2026-02-10)
  - TabPanes can be nested when needed
  - Current behavior is acceptable
- ✅ **Splitter Preservation**: No-op edge drops preserve dividers (2026-02-14)
- ✅ **Divider Insert Preservation**: Middle inserts keep existing divider positions stable (2026-02-14)

### Persistence
- ✅ **JSON Serialization**: Save complete layout structure
- ✅ **JSON Deserialization**: Restore layout from JSON
- ✅ **Unsupported-type layout recovery**: Unknown serialized node types no longer abort loading; they recover via factory custom fallback or framework placeholder diagnostics
- ✅ **Custom Node IDs**: Stable, user-defined IDs (2026-02-10)
- ✅ **DockNodeFactory pattern**: Factory for node recreation (2026-02-10)
- ✅ **Cross-session support**: Works across application restarts (2026-02-10)
- ✅ **State Preservation**: Divider positions, selected tabs, locked state
- ✅ **File I/O**: Save to and load from files

### Locked Mode
- ✅ **Layout Locking**: Prevent accidental modifications
- ✅ **Disable D&D**: No drag and drop when locked
- ✅ **Hide Controls**: Close buttons hidden
- ✅ **Tab Behavior**: Auto-hide single tabs
- ✅ **Property Binding**: Synchronized across UI

### User Interface
- ✅ **Editor file workflow demo**: MainDemo supports opening text files into editor nodes plus save/save-as actions for active editors
- ✅ **Editor close-hook demo**: MainDemo intercepts close requests and prompts only for dirty editor nodes (Save / Don't Save / Cancel)
- ✅ **Menu Bar**: File, Layout, Help menus
- ✅ **Toolbar**: Add/remove panel buttons
- ✅ **Context Actions**: Save, Load, Reset layout
- ✅ **Hidden Nodes Menu**: Restore closed nodes
- ✅ **About Dialog**: Dedicated class with dynamic version text, large SnapFX branding, credit links, and easter egg animation
- ✅ **Debug View**: Tree visualization with export
- ✅ **D&D Activity Log**: Real-time logging of all drag & drop actions with color-coding
- ✅ **Pinned sidebar visual demo strip**: Visible left/right sidebars in MainDemo reflect pinned entries and pin-open/collapse toggles without requiring framework-side view integration yet

### Build & Deployment
- ✅ Split the Gradle project into `snapfx-core` (framework code/resources) and `snapfx-demo` (demo app/resources), and moved framework/demo tests into the corresponding modules (root remains a no-source aggregator)
- ✅ Switched the Gradle publish namespace baseline to `org.snapfx` (backed by `snapfx.org`) to prepare Maven Central coordinates for `snapfx-core`
- ✅ Renamed Java package and JPMS module namespaces from `com.github.beowolve.snapfx...` to `org.snapfx...` across `snapfx-core` and `snapfx-demo`
- ✅ Ignored `.idea/` entirely and removed tracked IntelliJ project/workspace metadata from Git to keep the public repository clean and editor-agnostic
- ✅ Added a `snapfx-core` Maven-publish dry-run baseline with `sourcesJar`/`javadocJar`, Maven POM metadata (`snapfx.org`, MIT, SCM, developer), and verified local publication via `:snapfx-core:publishToMavenLocal`
- ✅ Completed `runSimpleExample` Gradle task for launching `SimpleExample` with JavaFX module runtime support.
- ✅ `SimpleExample` now logs a warning through `System.Logger` when `snapfx.css` is missing, without requiring a logging framework dependency.
- ✅ Added GitHub Actions CI workflow for push/PR validation with `./gradlew test` on JDK 21.
- ✅ Added GitHub Actions release workflow for `v*` tags that runs build/tests and publishes GitHub Releases with distribution artifacts.
- ✅ Added `git-cliff` release-note generation (`cliff.toml`) and wired release workflow to publish generated notes.
- ✅ Tracked `gradlew` as executable (`100755`) so Linux CI/release runners can execute the Gradle wrapper reliably.
- ✅ Added `xvfb-run -a` to CI/release Gradle test execution so JavaFX tests run reliably on headless Linux runners.
- ✅ Removed obsolete JavaFX test `--add-opens/--add-exports` JVM args to eliminate classpath-mode "Unknown module: javafx.graphics" warnings.
- ✅ Updated Gradle test JVM wiring so required JavaFX runtime jars are loaded via module path with TestFX access flags (`--add-exports`/`--add-opens`), eliminating unnamed-module warnings and reflective-access stack traces.
- ✅ Centralized plugin/dependency versions with a Gradle version catalog (`gradle/libs.versions.toml`) and removed duplicated JavaFX module literals from build configuration.
- ✅ **Gradle Build**: Modern Kotlin DSL build script
- ✅ **Module Configuration**: Java 21 module support
- ✅ **JavaFX Integration**: JavaFX Gradle plugin
- ✅ **Test Configuration**: JUnit 5 + TestFX setup
- ✅ **Distribution**: Tar and Zip archives

## 🎓 Key Achievements

### Technical Excellence
- ✅ Clean separation of concerns
- ✅ Smart algorithms (flattening, auto-cleanup)
- ✅ Comprehensive test suite
- ✅ Consistent JavaFX imports and sequenced collection accessors (production code)
- ✅ Full module system support
- ✅ Zero warnings in build

### User Experience
- ✅ Intuitive drag & drop
- ✅ Visual feedback
- ✅ Keyboard-friendly baseline: framework shortcut API with defaults (`Ctrl+W`, `Ctrl+Tab`, `Ctrl+Shift+Tab`, `Escape`, `Ctrl+Shift+P`) plus app-level `F11` fullscreen example in MainDemo
- ✅ Native look and feel
- ✅ Debug tools for troubleshooting

### Developer Experience
- ✅ Simple, fluent API
- ✅ Well-documented code
- ✅ Example applications
- ✅ Comprehensive documentation
- ✅ Easy to extend

## 📋 Next Steps

See [ROADMAP.md](ROADMAP.md) for planned features and improvements.

**Current Priority**: Sidebar interaction parity (Phase 2, current scope) and the `DockDebugOverlay` HUD follow-up are complete; next focus returns to the Phase 3 user-experience backlog.

---

**Version**: Git-derived via `gradle-jgitver` (tag-based)  
**Status**: Production-ready for large applications  
**License**: MIT (personal and commercial use)  
**Developed**: 2026-02  
**Last Update**: 2026-02-25
