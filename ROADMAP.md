# SnapFX Development Roadmap

**Last Updated**: 2026-02-15

This document tracks all planned features, improvements, and bug fixes for SnapFX. Items are marked as they are completed and new features are added as they are identified.
This roadmap lists planned work only; fixed issues are tracked in `STATUS.md`.

## Version Track

- Current development version: `0.2.1-SNAPSHOT`
- Current roadmap milestone: `0.2` (Floating Windows)
- Latest milestone tag: `v0.2.0`
- Next milestone target: `v0.3.0` (Phase 3 baseline)

## Legend

- ✅ **Completed** - Feature fully implemented and tested
- 🚧 **In Progress** - Currently being worked on
- 📋 **Planned** - Scheduled for implementation
- 💡 **Proposed** - Idea under consideration
- ❌ **Blocked** - Waiting on dependencies or decisions

---

## Overall Progress

| Phase | Status | Completion |
|-------|--------|------------|
| Phase 1: Core Stability | ✅ Completed | 100% |
| Phase 2: Floating Windows | 🚧 In Progress | 85% |
| Phase 3: User Experience | 📋 Planned | 0% |
| Phase 4: Advanced Features | 📋 Planned | 0% |
| Phase 5: Themes & Customization | 📋 Planned | 0% |
| Phase 6: Performance & Polish | 📋 Planned | 0% |
| Phase 7: Developer Experience | 📋 Planned | 32% |
| Phase 8: Production Readiness | 📋 Planned | 20% |

**Total Project Completion**: ~59%

---

## Phase 1: Core Stability

### 1.1 Drag & Drop Improvements
**Priority**: 🔴 Critical

- No planned items; see `STATUS.md` for fixed issues.

---

### 1.2 Debug & Monitoring
**Priority**: 🟡 High

- No planned items; see `STATUS.md` for fixed issues.

### 1.3 Testing & Quality Assurance  
**Priority**: 🟡 High

- ✅ **Add performance tests**: Large layouts with 50+ nodes
- ✅ **Add memory leak tests**: Ensure proper cleanup
- ✅ **Add edge case tests**: More boundary conditions

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

- No planned items; see `STATUS.md` for fixed issues.

---

## Phase 2: Floating Windows

### 2.1 Floating Window Core
**Priority**: 🟢 Medium

- ✅ **DockFloatingWindow class**: Manage external stages
- ✅ **Detach from main window**: Drag node outside main window to create floating window
- ✅ **Attach to main window**: Drag-attach and title-bar attach are both supported
- ✅ **Multi-monitor support**: Position by explicit screen coordinates (`floatNode(node, x, y)`)
- ✅ **Cross-window D&D**: Dock between main and floating windows, including floating-to-floating
- 📋 **Window state persistence**: Save/load floating window positions

**Estimated Time**: In progress

---

### 2.2 Floating Window Features
**Priority**: 🟢 Medium

- ✅ **Maximize/restore**: Custom floating title-bar toggle implemented
- 📋 **Always on top**: Pin window above main window
- ✅ **Window decorations**: Custom title bar styling and controls
- ✅ **Resizable undecorated windows**: Edge/corner resize behavior for floating stages
- ✅ **Tab-level float action**: Float button available in tab headers
- 📋 **Close behavior**: Return to hidden nodes or destroy

**Estimated Time**: 2 days

---

## Phase 3: User Experience

### 3.1 Keyboard Shortcuts
**Priority**: 🟢 Medium

- 📋 **Ctrl+W**: Close current tab/node
- 📋 **Ctrl+Tab**: Switch between tabs
- 📋 **Ctrl+Shift+Tab**: Switch tabs backwards
- 📋 **Alt+1..9**: Focus specific panel
- 📋 **F11**: Toggle fullscreen
- 📋 **Escape**: Cancel drag operation

**Estimated Time**: 1 day

### 3.2 Context Menus
**Priority**: 🟢 Medium

- 📋 **Right-click on tab**: Close, Close Others, Close All, Float
- 📋 **Right-click on splitter**: Reset to 50/50
- 📋 **Right-click on header**: Minimize, Maximize, Float
- 📋 **Customizable menu items**: API for adding custom actions

**Estimated Time**: 2 days

---

### 3.3 Animations
**Priority**: 🟡 Low

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

- 📋 **Window-to-window snapping**: Snap floating windows to each other while moving
- 📋 **Snap to main docking area**: Magnetic alignment at main layout borders
- 📋 **Configurable snap distance**: API/property to tune sensitivity
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

- 📋 **API documentation**: Complete JavaDoc
- 📋 **User guide**: Comprehensive usage guide
- 📋 **Tutorial series**: Step-by-step tutorials
- 📋 **Example projects**: Real-world examples
- 📋 **Video tutorials**: Screen recordings

**Estimated Time**: 5-7 days

---

### 7.3 Tooling
**Priority**: 🔵 Low

- 💡 **Layout designer**: Visual layout editor
- 💡 **FXML support**: Alternative to programmatic API
- 💡 **CSS inspector**: Debug styling issues
- 💡 **Layout validator**: Check for common issues

**Estimated Time**: 7-10 days

---

## Phase 8: Production Readiness

### 8.1 Packaging & Distribution
**Priority**: 🟡 High

- 📋 **Maven Central**: Publish to Maven Central
- 📋 **jlink support**: Create custom runtime images
- 📋 **jpackage support**: Native installers
- 📋 **Version management**: Semantic versioning
- 📋 **Release notes**: Automated changelog

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
2. **Started Work**: Change status from 📋 to 🚧
3. **New Feature**: Add to appropriate phase or Future Ideas
4. **Priority Change**: Update priority emoji (🔴🟡🟢🔵)
5. **Blocked**: Change status to ❌ and document reason
6. **Update Date**: Update "Last Updated" at the top

---

## Contributing

If you'd like to contribute to SnapFX:

1. Pick an item marked 📋 (Planned)
2. Create an issue discussing the implementation
3. Fork the repository
4. Implement the feature with tests
5. Submit a pull request
6. Update this roadmap when merged

---

**Maintained by**: SnapFX Development Team  
**Questions?**: Open an issue on GitHub
