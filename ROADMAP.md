# SnapFX Development Roadmap

**Last Updated**: 2026-02-17

This document tracks planned and proposed work for SnapFX.
This roadmap lists planned work only; completed/fixed history is tracked in `CHANGELOG.md`.

---

## Overall Progress

| Phase | Status | Completion |
|-------|--------|------------|
| Phase 1: Core Stability | ✅ Completed | 100% |
| Phase 2: Floating Windows | ✅ Completed | 100% |
| Phase 3: User Experience | 🚧 In Progress | 64% |
| Phase 4: Advanced Features | 🚧 In Progress | 20% |
| Phase 5: Themes & Customization | 📋 Planned | 0% |
| Phase 6: Performance & Polish | 📋 Planned | 0% |
| Phase 7: Developer Experience | 📋 Planned | 45% |
| Phase 8: Production Readiness | 📋 Planned | 25% |

**Total Project Completion**: ~75%

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
- 📋 No planned items; see `CHANGELOG.md` for completed fixes.

---

### 1.2 Debug & Monitoring
**Priority**: 🟡 High

- 📋 No planned items; see `CHANGELOG.md` for completed fixes.

### 1.3 Testing & Quality Assurance  
**Priority**: 🟡 High

- ✅ **Add performance tests**: Large layouts with 50+ nodes
- ✅ **Add memory leak tests**: Ensure proper cleanup
- ✅ **Add edge case tests**: More boundary conditions
- ✅ **Floating drag-state regression hardening**: Added scene-level drag release/reset and non-primary activation guard coverage for floating title-bar movement
- ✅ **Persistence edge-case hardening**: Added complex floating snapshot roundtrip coverage and invalid floating snapshot validation/no-partial-apply tests
- ✅ **CI flake guard for critical interactions**: CI now reruns `SnapFXTest`, `DockFloatingWindowTest`, and `DockDragServiceTest` three times per run

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
- 📋 No planned items; see `CHANGELOG.md` for completed fixes.

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

### 4.2 Side Bars (Auto-Hide)
**Priority**: 🟢 Medium

- 📋 **DockSideBar class**: Collapsible side panel
- 📋 **Auto-hide behavior**: Like IntelliJ IDEA sidebars
- 📋 **Pin/unpin**: Toggle between docked and auto-hide
- 📋 **Hover to expand**: Show on mouse hover
- 📋 **Click to pin**: Make permanently visible

**Estimated Time**: 3 days

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

- 📋 **Light theme**: Bright, clean appearance
- 📋 **Dark theme**: Low-light environment
- 📋 **Custom theme API**: User-defined color schemes
- 📋 **Theme switcher**: Runtime theme changes
- 📋 **CSS variables**: Parameterized styling

**Estimated Time**: 2-3 days

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
- ✅ **License wording baseline**: README/SETUP now state MIT licensing and explicit personal/commercial use positioning
- ✅ **README consumer-focus baseline**: Quick Start now focuses on SnapFX usage, without generic Gradle/module templates or repository workflow sections
- ✅ **Maintainer docs baseline**: Collaboration/release workflow is now documented in `CONTRIBUTING.md` and `RELEASING.md`
- ✅ **Versioned changelog baseline**: Historical changes are now consolidated in `CHANGELOG.md` and grouped by release tags
- ✅ **Status scope baseline**: `STATUS.md` now keeps only current state and open issues; completed/fixed history is maintained in `CHANGELOG.md`
- ✅ **Roadmap signal baseline**: Overall progress is now the first section, legend follows directly below, and version-track metadata was removed
- 📋 **Roadmap structure cleanup**: Keep each subsection as one block (`Priority` + open/planned items) and move detailed completed history to `CHANGELOG.md` / `DONE.md`
- 📋 **API documentation**: Complete JavaDoc
- 📋 **User guide**: Comprehensive usage guide
- 📋 **Tutorial series**: Step-by-step tutorials
- 📋 **Example projects**: Real-world examples
- 📋 **GitHub Pages site**: Full project documentation portal (guides, architecture, API overview)
- 📋 **Video tutorials**: Screen recordings
- 📋 **Doc consistency guardrails**: Keep markdown consistency tests aligned with documentation scope rules

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
- 📋 **Maven Central**: Publish to Maven Central
- 📋 **jlink support**: Create custom runtime images
- 📋 **jpackage support**: Native installers
- 📋 **Version management**: Semantic versioning

**Estimated Time**: 2-3 days

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
   - Also capture release-relevant details in `CHANGELOG.md` under the correct tag section.
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
7. Follow repository collaboration standards in `AGENTS.md` (commit message and callback-structure conventions).

---

**Maintained by**: SnapFX Development Team  
**Questions?**: Open an issue on GitHub

