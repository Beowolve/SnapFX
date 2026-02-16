# SnapFX Framework - Completed Features

**Last Updated**: 2026-02-16

SnapFX has been fully implemented with core functionality and is production-ready for large applications.

## ✅ What Has Been Completed

### Core Framework (23 classes)

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

#### Persistence (1 class)
- ✅ `DockLayoutSerializer` - JSON-based serialization with Gson

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

#### Demo (4 classes)
- ✅ `MainDemo` - Full IDE-like layout with all features
- ✅ `SimpleExample` - Minimal usage example
- ✅ `DockNodeFactory` - Helper for creating demo nodes
- ✅ `AboutDialog` - Dedicated About dialog with branding, credits, and easter egg animation
- ✅ `Settings tab` - Live layout options in the debug panel

### Module System
- ✅ `module-info.java` - Java Platform Module System descriptor
- ✅ Full JPMS support with proper exports and opens
- ✅ Compatible with Java 21 module system

### Testing (12 test classes, 189 tests)
- ✅ `DockGraphTest` (56 tests) - Tree manipulation and algorithms
  - **+11 regression tests** for critical bug fixes
  - Tests for empty container prevention
  - Tests for target invalidation during move
  - Tests for complex D&D sequences
  - Tests for flattening logic
  - Performance stress tests for large layouts (50+ nodes)
  - Edge case tests for null/no-op/detached-target handling
- ✅ `DockLayoutSerializerTest` (9 tests) - Persistence functionality
  - **+1 regression test** for locked state synchronization (2026-02-10)
- ✅ `DockLayoutEngineTest` (29 tests) - View creation with TestFX, context-menu interaction coverage, float-availability policy checks, and tiny-bounds drop-zone clamp regression coverage
  - Memory cleanup tests for cache boundedness and undock/rebuild cycles
  - Layout optimization tests for empty/single-child roots
- ✅ `SnapFXTest` (44 tests) - Hide/Restore + Floating Window API behavior plus configurable shortcut behavior
- ✅ `DockGraphSplitTargetDockingTest` (1 test) - Split-target docking regression coverage
- ✅ `DockDragServiceTest` (8 tests) - D&D visibility, tab-hover activation, float-detach callback behavior, and ESC drag-cancel handling
- ✅ `DockFloatingWindowTest` (16 tests) - Floating title-bar controls, context-menu behavior (attach/pin icons + attach action), pin behavior, icon rendering/sync regression coverage, single-node float-menu policy, and maximize/restore interaction behavior
- ✅ `MainDemoTest` (5 tests) - Application icon resources, menu icon behavior, and demo shortcut wiring
- ✅ `AboutDialogTest` (2 tests) - About dialog resources and credit link targets
- ✅ `EditorCloseDecisionPolicyTest` (5 tests) - Deterministic close-decision behavior for dirty editor nodes
- ✅ `SimpleExampleTest` (2 tests) - Stylesheet resource resolution behavior
- ✅ `MarkdownDocumentationConsistencyTest` (12 tests) - Documentation consistency guardrails
- ✅ All tests passing ✅
- ✅ **Testing Policy** established (TESTING_POLICY.md)
- ✅ Mandatory regression tests for all bug fixes

### Documentation (11 files)
- ✅ `README.md` - Project overview and quick start
- ✅ `SETUP.md` - Development environment setup
- ✅ `ARCHITECTURE.md` - Complete architecture documentation
- ✅ `STATUS.md` - Current project status
- ✅ `ROADMAP.md` - Future development plans
- ✅ `DONE.md` - Completed features (this file)
- ✅ `CHANGELOG.md` - Versioned release history grouped by tags
- ✅ `TESTING_POLICY.md` - Stable testing rules and quality gates (policy-only)
- ✅ `CONTRIBUTING.md` - Collaboration workflow, branch strategy, and PR quality gates
- ✅ `RELEASING.md` - Maintainer release/versioning/tag and CI release flow
- ✅ `AGENTS.md` - Persistent collaboration rules and workflow constraints
- ✅ README clarifies TitleBarMode.AUTO behavior and tab-only drag handling
- ✅ README includes a MainDemo screenshot preview near the top
- ✅ README embeds the SnapFX SVG logo for repository and future GitHub Pages branding
- ✅ README now includes a documentation map that defines ownership and purpose of each core markdown file
- ✅ README and SETUP license sections now state MIT licensing with explicit personal/commercial use support
- ✅ README quick start is now framework-focused and excludes generic Gradle/module setup templates
- ✅ README no longer includes repository-maintainer workflow sections (Versioning/Branch Strategy/CI/CD)
- ✅ Removed README maintainer workflow sections are now documented in dedicated `CONTRIBUTING.md` and `RELEASING.md` guides
- ✅ Historical project changes are now consolidated into `CHANGELOG.md` with release-tag grouping
- ✅ `STATUS.md` now keeps open issues only; completed/fixed history is maintained in `CHANGELOG.md`
- ✅ `ROADMAP.md` version-track section removed; overall progress is now first and legend follows directly for faster status scanning
- ✅ `STATUS.md` wording now avoids historical delta suffixes in current-state bullets (for example `was ...` / `improved from ...`)
- ✅ `ROADMAP.md` update instructions now include recalculating phase percentages and total completion
- ✅ Issue tracking consolidated into STATUS.md; ROADMAP lists planned work only
- ✅ Fixed markdown encoding artifacts (Unicode icon Mojibake) in roadmap/docs content
- ✅ Documentation scope is now explicit: `STATUS` = current, `ROADMAP` = planned, `DONE` = completed, `TESTING_POLICY` = rules-only
- ✅ AGENTS collaboration rules now require per-fix commit message body lines and method extraction for multi-statement UI callbacks
- ✅ AGENTS collaboration rules now require minimal-diff edits across all file changes, plus inline rendered-icon comments for `\u...` icon constants
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
- ✅ **Smart Flattening**: Automatic optimization to prevent nested containers with same orientation
- ✅ **SplitPane optimization verified**: No nested same-orientation SplitPanes (horizontal + vertical coverage)
- ✅ **Auto-Cleanup**: Empty containers automatically remove themselves
- ✅ **Hidden Nodes**: Close without deletion, restore later

### Visual Features
- ✅ **DockNodeView**: Header with title plus float/close controls
- ✅ **Property Bindings**: Reactive UI updates via JavaFX properties
- ✅ **CSS Styling**: Native Modena theme integration
- ✅ **Tab Overflow Dropdown**: Menu shows titles when using custom tab graphics
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
- ✅ **Floating single-node float policy parity**: Float context action is hidden for single-node floating layouts, matching button visibility behavior
- ✅ **Floating title-bar icon correctness**: DockNode icons are image-based and rendered per view, so floating title-bar icons stay visible and follow active tabs
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
- ✅ **Float-from-floating extraction**: Floating layout float actions now detach the selected node into a separate floating window
- ✅ **Floating close-to-hidden behavior**: Closing floating windows via `X` moves nodes to hidden windows list
- ✅ **Configurable close behavior**: Close requests now support framework-level default behavior selection (`HIDE` or `REMOVE`)
- ✅ **Source-aware close callbacks**: Close interception and outcome callbacks now cover tab, title-bar, and floating-window close requests consistently
- ✅ **Floating window save/load persistence**: Open floating windows and bounds are restored via layout snapshots
- ✅ **Floating drag/drop feedback parity**: Floating windows render drop zones and active drop indicator during drag
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

### Build & Deployment
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

**Current Priority**: Focus on Phase 3 user-experience backlog (customizable context-menu API and interaction polish).

---

**Version**: Git-derived via `gradle-jgitver` (tag-based)  
**Status**: Production-ready for large applications  
**License**: MIT (personal and commercial use)  
**Developed**: 2026-02  
**Last Update**: 2026-02-16

