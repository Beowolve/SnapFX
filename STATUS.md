# Project Status

**Last Updated**: 2026-02-14

## Build Status

✅ **Build**: `BUILD SUCCESSFUL`  
✅ **Tests**: All 65 tests passing (42 + 9 + 7 + 7)  
✅ **Module System**: Fully implemented (JPMS)  
✅ **Demo App**: Running successfully

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

### Drag & Drop (96% ✅)
- ✅ DockDragService implementation
- ✅ Ghost overlay during drag
- ✅ Drop zone detection for SplitPanes
- ✅ Mouse event handling
- ✅ TabPane D&D bug fixed (cache invalidation)
- ✅ Auto-rebuild after D&D operations (critical fix)
- ✅ Consistent D&D behavior across all operations
- ✅ Drop zone visualization modes (ALL/SUBTREE/DEFAULT/ACTIVE/OFF)
- ✅ Per-tab insert targeting with visual insert line
- ✅ In-place TabPane reordering to avoid flattening and missed drops
- ✅ Depth-first drop target selection and zone validation
- ✅ Nested TabPanes work correctly (verified)
- ✅ Target invalidation handling (prevents empty containers)
- ✅ Splitter positions preserved on no-op edge drops

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

### Hidden Nodes Manager (100% ✅)
- ✅ Close nodes without deletion
- ✅ Restore hidden nodes
- ✅ Menu integration
- ✅ Original position tracking
- ✅ **Close button handler integration** (Fixed: 2026-02-11)

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
- ✅ DockGraphTest (42 tests, +10 regression tests)
- ✅ DockLayoutSerializerTest (9 tests, +1 regression test)
- ✅ DockLayoutEngineTest (7 tests)
- ✅ **SnapFXTest (7 tests)** - NEW: Hide/Restore functionality tests
- ✅ **65/65 tests passing** ✅ (was 49)
- ✅ **Regression tests** for all critical bug fixes
- ✅ **Testing Policy** established (TESTING_POLICY.md)
- ✅ ~87% code coverage (improved from ~85%)
- ✅ All structural integrity tests (no empty containers, no nesting)

### Demo Application (100% ✅)
- ✅ MainDemo with IDE-like layout
- ✅ Menu bar (File, Layout, Help)
- ✅ Toolbar with add/remove functions
- ✅ Lock/unlock layout
- ✅ Save/Load layout
- ✅ Hidden nodes menu
- ✅ About dialog with icon credits
- ✅ Debug view toggle

### Documentation (100% ✅)
- ✅ README.md updated
- ✅ ARCHITECTURE.md complete and corrected
- ✅ SETUP.md
- ✅ PROJECT_SUMMARY.md
- ✅ MODULE_SYSTEM.md
- ✅ MODULE_MIGRATION_SUMMARY.md
- ✅ TABPANE_TESTS.md
- ✅ DONE.md

## Known Issues

### High Priority
1. ~~❌ Drag & Drop: TabPane drop zones not fully functional~~ ✅ **FIXED** (2026-02-10)
2. ~~❌ Drag & Drop: Tabs in TabPane lost D&D capability after being moved~~ ✅ **FIXED** (2026-02-10)
3. ~~❌ Drag & Drop: D&D stops working completely after first drop~~ ✅ **FIXED** (2026-02-10)
4. ~~❌ Layout: Nested TabPanes can occur (should be optimized)~~ ✅ **VERIFIED OK** (2026-02-10)
5. ~~❌ Drag & Drop: Empty containers remain after complex D&D operations~~ ✅ **FIXED** (2026-02-10)
6. ~~❌ Drag & Drop: Dropping on same position changes splitter positions~~ ✅ **FIXED** (2026-02-14)
7. ~~❌ Drag & Drop: Tab reordering can miss drops despite insert indicator~~ ✅ **FIXED** (2026-02-14)

### Medium Priority
8. ⚠️ Performance: Large layouts not tested
9. ⚠️ Memory: View caching strategy could be improved
10. ~~⚠️ UI: Tab overflow dropdown shows empty entries when tab graphics are custom~~ ✅ **FIXED** (2026-02-14)

### Low Priority
11. ⚠️ UI: Animations missing
12. ⚠️ UI: Keyboard shortcuts not implemented

## Recent Changes (2026-02-14)

### Drag & Drop Drop-Zone Overhaul
- Added depth-first drop target selection with validation against invalid targets
- Added configurable drop-zone visualization modes (ALL/SUBTREE/DEFAULT/ACTIVE/OFF)
- Added per-tab insert targeting with visible insert line
- Fixed in-place tab reordering to prevent TabPane flattening and missed drops
- Moved the drag ghost overlay to the bottom-right of the cursor to keep targets visible
- Preserved divider positions on no-op edge drops and added regression coverage
- Expanded tab insert/reorder tests including index clamping (DockGraphTest: 42 tests, total: 65)

### UI Fixes
- Fixed tab overflow dropdown entries for custom tab graphics by binding tab text to the node title

### Code Quality
- Normalized JavaFX imports and list accessors (`getFirst`/`getLast`/`isEmpty`) in production code; tests may use index access for clarity
## Recent Changes (2026-02-11)

### Close Button Handler Fix (CRITICAL FIX)
- **Fixed critical bug** where closing a window did not add it to hidden windows menu
- **Root cause**: Close button was set up before setOnNodeCloseRequest handler was configured
- **Problem**: Close action called undock() instead of hide(), preventing nodes from being tracked
- **Solution**: 
  - Moved setOnNodeCloseRequest() call before layout creation in MainDemo
  - Removed default handler from DockNodeView constructor
  - DockLayoutEngine now always sets handler (custom or fallback to undock)
- Added comprehensive test suite (SnapFXTest with 7 tests)
- Regression test ensures close button correctly adds nodes to hidden list
- All 55 tests passing ✅

## Recent Changes (2026-02-10)

### Persistent Node IDs & Factory Pattern (NEW FEATURE)
- Implemented custom ID support for DockNodes (cross-session persistence)
- Created DockNodeFactory interface for node recreation
- Updated DockLayoutSerializer to use factory pattern
- MainDemo now demonstrates proper usage with fixed IDs
- Save/Load now works across application sessions
- Breaking change: Old saved layouts incompatible (UUIDs → custom IDs)

### Locked State Synchronization Fix
- Fixed bug where locked state was not synced with UI after loading layout
- MainDemo now synchronizes lockLayoutProperty with DockGraph after load
- Added regression test for locked state round-trip
- Test count: 48 → 49 tests

### Testing Policy Established (NEW)
- Created comprehensive TESTING_POLICY.md
- **Mandatory rule**: Every fixed bug must have regression test
- Added 7 new regression tests for critical bug fixes
- Test count: 41 → 48 tests (all passing ✅)
- Coverage: ~80% → ~85%

### Target Invalidation Fix (CRITICAL FIX)
- Fixed critical bug where D&D created empty containers after sequential drops
- Problem: Target became invalid after undock() due to flattening
- Solution: Find target by ID after undock to handle tree restructuring
- Prevents orphaned empty SplitPanes in complex D&D scenarios
- Ensures move() operations handle flattening correctly

### Auto-Cleanup Fix (CRITICAL FIX)
- Fixed critical bug where empty containers remained in tree after D&D
- Fixed nodes disappearing during D&D operations
- Reordered cleanup logic: flatten first, then cleanup
- Prevents cascade of empty SplitPanes and TabPanes
- Ensures tree integrity after any operation

### Icon Support for DockNodes (NEW FEATURE)
- Added optional icon property to DockNode
- Icons displayed in node headers and tab labels
- MainDemo uses toolbar button icons for new panels
- All demo panels now have appropriate icons

### Nested TabPane Verification
- Verified that nested TabPanes work correctly
- Current behavior is acceptable and matches expected functionality
- Removed from known issues list

### D&D Activity Logging Improvements
- Removed AUTO_EXPORT from log entries (not relevant for D&D analysis)
- Cleaner log output focused on actual D&D operations
- 7 event types now tracked (was 8)

### D&D Auto-Rebuild Fix (CRITICAL FIX)
- Fixed critical bug where D&D stopped working completely after first drop
- Implemented auto-rebuild of views on every revision change
- Views are now automatically refreshed after any D&D operation
- `findElementAt()` now always finds valid targets
- Resolved in SnapFX.java constructor

### D&D TabPane Bug Fix (CRITICAL FIX)
- Fixed critical bug where tabs lost D&D capability after being moved to a TabPane
- Implemented proper cache invalidation in DockLayoutEngine
- Views are now correctly rebuilt after structure changes
- Hit-testing now works reliably for all tabs
- See DND_TABPANE_BUG_FIX.md for technical details

### D&D Activity Logging (NEW)
- Added comprehensive activity log to DockGraphDebugView
- Logs all drag & drop events (start, target change, position change, drop, cancel)
- Color-coded log entries for easy identification
- Included in snapshot export for better debugging
- Auto-scroll to latest entry
- Configurable log size limit (500 entries)

### Module System Implementation
- Created `module-info.java` with proper exports and opens
- Updated `build.gradle.kts` for JPMS support
- Configured test execution on classpath
- Updated all documentation

### Debug Tools Enhancement
- Added export functionality to DockGraphDebugView
- Improved tree visualization with ID column
- Added auto-expand and auto-export features

### Hidden Nodes Feature
- Implemented node hiding without deletion
- Added restoration mechanism
- Integrated with menu system

## Next Steps

See [ROADMAP.md](ROADMAP.md) for detailed future development plans.

**Priority**: Fix Drag & Drop issues, then implement Floating Windows.

---

**Version**: 1.0-SNAPSHOT  
**Java**: 21 (LTS)  
**JavaFX**: 21  
**Build Tool**: Gradle 9.0

---

## Documentation Policy

**Core Documentation Files** (permanent):
- **STATUS.md** - Current project status, recent changes, known issues
- **ROADMAP.md** - Future development plans, phases, completion tracking
- **DONE.md** - Completed features and achievements
- **ARCHITECTURE.md** - Technical architecture and design
- **README.md** - Project overview, quick start, usage
- **SETUP.md** - Development environment setup

**Policy**: All updates, features, and bug fixes are documented in these 6 files only. No additional feature-specific MD files are created.

