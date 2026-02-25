# SnapFX Architecture Documentation

## Overview

SnapFX is a lightweight JavaFX docking framework with a strict separation between model and view. The framework follows a clean layered architecture that ensures maintainability, testability, and extensibility.

### Key Features
- **Tree-Based Model**: Logical structure completely decoupled from visual representation
- **Smart Layout Optimization**: Automatic flattening of nested containers with same orientation
- **Auto-Cleanup**: Empty containers automatically remove themselves from the tree
- **Drag & Drop**: Visual feedback with ghost overlay and drop zone indicators
- **Floating Windows**: Detachable tabs that create new stages
- **Locked Mode**: Prevents layout modifications in production mode
- **Persistence**: JSON-based serialization for saving/loading layouts
- **Flat Scene Graph**: Minimal wrapper layers for optimal performance

### Architecture Overview

```
┌───────────────────────────────────────────────────────────────────┐
│                          SnapFX API                               │
│                   (Simple, fluent public API)                     │
│  - dock(content, title)                                           │
│  - dock(content, title, target, position)                         │
│  - undock(node)                                                   │
│  - buildLayout()                                                  │
│  - saveLayout() / loadLayout(json) throws DockLayoutLoadException │
│  - setLocked(boolean)                                             │
│  - ...                                                            │
└────────────────┬──────────────────────────────────────────────────┘
                 │
                 │ coordinates
                 │
    ┌────────────┼────────────┬──────────────┬───────────────┐
    │            │            │              │               │
    ▼            ▼            ▼              ▼               ▼
┌───────┐  ┌───────────┐  ┌──────────┐  ┌─────────┐  ┌──────────┐
│ Dock  │  │   Dock    │  │  Dock    │  │  Dock   │  │  Hidden  │
│ Graph │  │  Layout   │  │   Drag   │  │ Layout  │  │  Nodes   │
│       │  │  Engine   │  │ Service  │  │ Serial- │  │ Manager  │
│(Model)│  │  (View)   │  │  (D&D)   │  │  izer   │  │          │
└───┬───┘  └─────┬─────┘  └────┬─────┘  └────┬────┘  └────┬─────┘
    │            │             │             │            │
    │            │             │             │            │
    │  uses      │  reads      │  modifies   │  persists  │
    │            │             │             │            │
    ▼            │             ▼             │            │
┌─────────────────────────────────────────────────────────────────┐
│                        Model Layer                              │
│  ┌────────────────────────────────────────────────────┐         │
│  │ DockElement (Interface)                            │         │
│  │  - getId(), getParent(), removeFromParent()        │         │
│  └──┬────────────────────────────────────────────┬────┘         │
│     │                                            │              │
│     ▼                                            ▼              │
│  ┌──────────────┐                    ┌────────────────────┐     │
│  │  DockNode    │                    │  DockContainer     │     │
│  │  (Leaf)      │                    │  (Interface)       │     │
│  │              │                    │  - getChildren()   │     │
│  │ - title      │                    │  - addChild()      │     │
│  │ - content    │                    │  - removeChild()   │     │
│  │ - closeable  │                    └──┬────────────┬────┘     │
│  └──────────────┘                       │            │          │
│                                         ▼            ▼          │
│                              ┌───────────────┐  ┌─────────────┐ │
│                              │ DockSplitPane │  │ DockTabPane │ │
│                              │ (H/V splits)  │  │   (Tabs)    │ │
│                              │               │  │             │ │
│                              │ - orientation │  │ - selected  │ │
│                              │ - dividers    │  │   Index     │ │
│                              └───────────────┘  └─────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                                     │
                                     │ converts to
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                         View Layer                              │
│  ┌──────────────────────────────────────────────────┐           │
│  │         DockLayoutEngine.buildSceneGraph()       │           │
│  │  Traverses DockGraph and creates JavaFX nodes    │           │
│  └────────────────────┬─────────────────────────────┘           │
│                       │                                         │
│                       ├──► DockNode → DockNodeView              │
│                       │     (VBox: Header + Content)            │
│                       │                                         │
│                       ├──► DockSplitPane → javafx.SplitPane     │
│                       │     (with bound divider positions)      │
│                       │                                         │
│                       └──► DockTabPane → javafx.TabPane         │
│                            (with synchronized selection)        │
└─────────────────────────────────────────────────────────────────┘
                                     │
                                     │ rendered as
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                    JavaFX SceneGraph                            │
│  - Standard JavaFX components (SplitPane, TabPane, VBox, etc.)  │
│  - Minimal wrapper layers                                       │
│  - Bidirectional property bindings to model                     │
│  - Event handlers for drag & drop, close buttons, etc.          │
└─────────────────────────────────────────────────────────────────┘
```

### Component Relationships

```
                   ┌───────────────────┐
                   │   Application     │
                   │   (MainDemo)      │
                   └─────────┬─────────┘
                             │ uses
                             │
                   ┌─────────▼─────────┐
                   │     SnapFX        │◄────────────┐
                   │   (Facade)        │             │
                   └───┬───┬───┬───┬───┘             │
                       │   │   │   │                 │
        ┌──────────────┘   │   │   └─────────┐       │
        │                  │   │             │       │
        ▼                  ▼   ▼             ▼       │
  ┌──────────┐      ┌──────────────┐   ┌─────────┐   │
  │DockGraph │      │DockLayoutEng │   │  Dock   │   │
  │          │─────►│              │   │Layout   │   │
  │          │      │              │   │Serial-  │   │
  │          │      │              │   │ izer    │   │
  └────▲─────┘      └──────────────┘   └─────────┘   │
       │                                             │
       │ modifies                                    │
       │                                             │
  ┌────┴─────────┐                                   │
  │ DockDrag     │                                   │
  │ Service      │───────────────────────────────────┘
  │              │    fires layout change events
  └──────────────┘
```

## 1. Model Layer

### DockGraph
**Purpose**: Central data structure of the docking system.

```java
DockGraph {
    - root: DockElement
    - locked: BooleanProperty

    + dock(node, target, position)
    + undock(node)
    + move(node, target, position)
}
```

**Core responsibilities**:
- Holds the root of the logical tree
- Orchestrates docking operations
- Manages locked state

### DockElement (Interface)
**Purpose**: Base interface for all elements in the tree.

```java
interface DockElement {
    + getId(): String
    + getParent(): DockContainer
    + setParent(parent)
    + removeFromParent()
}
```

**Implementations**:
- `DockNode`: Leaf node (contains UI content)
- `DockSplitPane`: Split container
- `DockTabPane`: Tab container

### DockContainer (Interface)
**Purpose**: Containers can hold children.

```java
interface DockContainer extends DockElement {
    + getChildren(): ObservableList<DockElement>
    + addChild(element)
    + removeChild(element)
    + isEmpty(): boolean
    + cleanupIfEmpty()
}
```

### DockNode
**Purpose**: Wrapper for dockable nodes.

```java
DockNode {
    - id: String
    - title: StringProperty
    - content: ObjectProperty<Node>
    - closeable: BooleanProperty
    - parent: DockContainer
}
```

**Properties**:
- Wraps a JavaFX Node
- Stores metadata
- Reacts to property changes

### DockSplitPane
**Purpose**: Split container (horizontal/vertical).

```java
DockSplitPane {
    - orientation: Orientation
    - children: ObservableList<DockElement>
    - dividerPositions: List<DoubleProperty>

    + addChild(element)  // Smart flattening!
    + removeChild(element)
}
```

**Smart flattening**:
```java
// Before adding:
SplitPane(H) { A, B }
child = SplitPane(H) { C, D }

// After addChild:
SplitPane(H) { A, B, C, D }  // Flattened!
```

**Auto-cleanup**:
```java
// If only 1 child remains:
SplitPane { A } → A  // SplitPane removes itself
```

### DockTabPane
**Purpose**: Tab container.

```java
DockTabPane {
    - children: ObservableList<DockElement>
    - selectedIndex: IntegerProperty

    + addChild(node)  // DockNodes only!
    + removeChild(node)
}
```

**Auto-cleanup & flattening**:
```java
// If only 1 tab remains:
TabPane { Tab1 } → Tab1  // TabPane removes itself
```

## 2. View Layer

### DockLayoutEngine
**Purpose**: Converts DockGraph → JavaFX SceneGraph.

```java
DockLayoutEngine {
    - dockGraph: DockGraph
    - viewCache: Map<String, Node>
    - dockNodeViews: Map<String, DockNodeView>

    + buildSceneGraph(): Node
    - createView(element): Node
    - createSplitPaneView(model): SplitPane
    - createTabPaneView(model): TabPane
    - createDockNodeView(model): DockNodeView
}
```

**How it works**:
1. Traverses DockGraph recursively
2. Creates matching JavaFX components
3. Binds properties (bidirectional)
4. Caches views for performance

**Example**:
```java
// Model:
SplitPane(V) {
    TabPane { Node1, Node2 },
    Node3
}

// Becomes:
javafx.scene.control.SplitPane (vertical) {
    javafx.scene.control.TabPane {
        Tab { DockNodeView(Node1) },
        Tab { DockNodeView(Node2) }
    },
    DockNodeView(Node3)
}
```

### DockNodeView
**Purpose**: Visual representation of a DockNode.

```java
DockNodeView extends VBox {
    - header: HBox
        - titleLabel: Label
        - closeButton: Button
    - contentPane: StackPane
        - content: Node
}
```

**Behavior**:
- Header with title and close button
- Close button reacts to `closeable` and `locked`
- Content is bound from DockNode

## 3. Drag & Drop Layer

### DockDragService
**Purpose**: Central service for D&D operations.

```java
DockDragService {
    - dockGraph: DockGraph
    - currentDrag: DockDragData
    - ghostOverlay: DockGhostOverlay
    - dropIndicator: DockDropIndicator

    + startDrag(node, event)
    + updateDrag(event)
    + endDrag(event)
    + cancelDrag()
}
```

**Workflow**:
```
1. startDrag()
   → create DockDragData
   → show GhostOverlay

2. updateDrag()
   → update ghost position
   → compute drop target & zone
   → show DropIndicator

3. endDrag()
   → hide overlays
   → perform dockGraph.move()
```

### DockDragData
**Purpose**: Transfer object for D&D.

```java
DockDragData {
    - draggedNode: DockNode
    - dropTarget: DockElement
    - dropPosition: DockPosition
    - mouseX, mouseY: double
}
```

### DockPosition (Enum)
**Purpose**: Defines drop zones.

```java
enum DockPosition {
    TOP,      // Split top
    BOTTOM,   // Split bottom
    LEFT,     // Split left
    RIGHT,    // Split right
    CENTER    // Add as tab
}
```

## 4. Persistence Layer

### DockLayoutSerializer
**Purpose**: JSON serialization/deserialization.

```java
DockLayoutSerializer {
    - dockGraph: DockGraph
    - gson: Gson
    - nodeRegistry: Map<String, DockNode>

    + serialize(): String
    + deserialize(json)
    + registerNode(node)
}
```

**JSON structure**:
```json
{
  "locked": false,
  "root": {
    "id": "uuid-123",
    "type": "DockSplitPane",
    "orientation": "HORIZONTAL",
    "dividerPositions": [0.3, 0.7],
    "children": [
      {
        "id": "uuid-456",
        "type": "DockNode",
        "title": "Editor",
        "closeable": true
      },
      {
        "id": "uuid-789",
        "type": "DockTabPane",
        "selectedIndex": 0,
        "children": [...]
      }
    ]
  }
}
```

**Note**: JavaFX nodes cannot be serialized. The application must register DockNodes with `registerNode()` beforehand.

## 5. API Layer

### SnapFX (Main class)
**Purpose**: Simple, fluent API for end users.

```java
SnapFX {
    - dockGraph: DockGraph
    - layoutEngine: DockLayoutEngine
    - dragService: DockDragService
    - serializer: DockLayoutSerializer

    + initialize(stage)
    + getDefaultThemeName(): String
    + getAvailableThemeStylesheets(): Map<String, String>
    + getAvailableThemeNames(): List<String>
    + setThemeStylesheet(stylesheetResourcePath)
    + getThemeStylesheetResourcePath(): String
    + dock(content, title): DockNode
    + dock(content, title, target, position): DockNode
    + undock(node)
    + buildLayout(): Parent
    + setLocked(locked)
    + saveLayout(): String
    + loadLayout(json) throws DockLayoutLoadException
}
```

**Usage**:
```java
SnapFX snapFX = new SnapFX();
snapFX.initialize(stage); // applies default /snapfx.css automatically
snapFX.setThemeStylesheet(
    SnapFX.getAvailableThemeStylesheets().get("Dark")
); // optional runtime theme switch

// Simple docking
DockNode editor = snapFX.dock(new TextArea(), "Editor");

// With position
DockNode console = snapFX.dock(
    new TextArea(),
    "Console",
    editor,
    DockPosition.BOTTOM
);

// Build layout
Parent layout = snapFX.buildLayout();
scene.setRoot(layout);

// Lock layout
snapFX.setLocked(true);

// Save/load
String json = snapFX.saveLayout();
try {
    snapFX.loadLayout(json);
} catch (DockLayoutLoadException e) {
    // Handle invalid/corrupt layout data.
}
```

Theme stylesheet internals are encapsulated in `com.github.beowolve.snapfx.theme`:

- `DockThemeCatalog`: built-in named theme map/list (`Light`, `Dark`)
- `DockThemeStylesheetManager`: path/url resolution and scene stylesheet application

## 6. Data Flow

### Dock operation
```
User: snapFX.dock(node, target, position)
  │
  ▼
SnapFX: dockGraph.dock(node, target, position)
  │
  ▼
DockGraph: Analyze position
  ├─ CENTER? → dockAsTab()
  │    └─ Create/extend TabPane
  │
  └─ Other? → dockAsSplit()
       └─ Create SplitPane with Orientation
  │
  ▼
Model updated
  │
  ▼
User: snapFX.buildLayout()
  │
  ▼
DockLayoutEngine: buildSceneGraph()
  │
  ▼
Traverse DockGraph
  └─ For each element:
       createView(element)
         ├─ DockNode → DockNodeView
         ├─ SplitPane → javafx.SplitPane
         └─ TabPane → javafx.TabPane
  │
  ▼
SceneGraph ready
```

### Property binding
```
DockNode.title (Model)
    ↕ (bidirectional)
DockNodeView.titleLabel.text (View)
    ↕
Tab.text (if used in a TabPane)
```

### Floating Reattach Strategy

SnapFX uses host-aware placement memory to restore nodes after floating-window attach operations.

- Placement capture happens before undocking from either the main layout or a floating layout.
- Captured anchors include preferred target/position/tab-index, previous and next sibling anchors, and source host context (main layout or specific floating window).
- During `attachFloatingWindow(...)`, restore is attempted in this order:
  - preferred anchor in original host
  - neighbor anchors in original host
  - remembered anchors in main layout (when applicable)
  - fallback docking
- Fallback behavior is non-blocking:
  - if original floating host is still active, fallback can dock into that host root
  - otherwise fallback docks into the main layout
- Hidden/closed floating windows are never treated as valid restore hosts.
- No user dialogs are shown when restore anchors are invalid; attach always resolves via fallback.

### Sidebar Overlay and Pinned Panel Rendering

Phase-C side bars split state across model and view layers on purpose:

- `DockGraph` persists pinned entries per side and a side-bar pinned-open flag.
- `DockGraph` also persists a preferred side-bar panel width per side (LEFT/RIGHT) for layout roundtrips.
- SnapFX uses the pinned-open flag as the persistent "pinned open" state for left/right side panels.
- `SnapFX` keeps transient UI-only state for collapsed overlay panels (selected node per side + whether an overlay is currently open).

Rendering architecture:

- The main dock layout still comes from `DockLayoutEngine.buildSceneGraph()`.
- `SnapFX.buildLayout()` wraps that layout in a composed root (`BorderPane` inside `StackPane`).
- Left/right side hosts render icon strips plus optional pinned side panels that consume layout space.
- Both pinned and overlay side-bar panels read the same per-side preferred width and apply runtime clamping based on current layout width.
- A dedicated resize handle is rendered on the inner panel edge (left panel = right edge, right panel = left edge).
- Overlay panels are rendered in a top `StackPane` layer so they can overlap the main layout without changing the model.

Interaction rules (Phase-C visual baseline):

- Side-bar strips show icon buttons for pinned `DockNode`s (title tooltip with zero show delay).
- Pinning a node into a side bar keeps the strip collapsed by default; callers/UI explicitly open pinned panels.
- In collapsed mode, clicking an icon opens an overlay panel for that side.
- Clicking the same icon again closes the overlay; clicking a different icon switches the overlay content.
- Outside clicks close collapsed overlays.
- In pinned-open mode, clicking the active icon collapses the panel by default; this policy is configurable via SnapFX API for alternative UX preferences.
- This icon-collapse for pinned panels is transient UI state in `SnapFX` and does not change the underlying pinned-mode flag in `DockGraph`.
- Pin toggle moves the panel between overlay mode and pinned-open mode without moving/removing the pinned `DockNode`.
- Only one side-bar panel is open per side at a time.
- Sidebar restore from `SnapFX` reuses the same remembered placement strategy as floating-window attach (preferred anchor, neighbor anchors, fallback), which avoids common restore misplacements when parent containers collapse during pinning.
- Sidebar strip icons and expanded panel headers expose built-in framework context menus for restore/move/pin actions, so host apps do not need demo-specific menus for common sidebar workflows.
- Sidebar panel width changes (resize handle or API) are treated as persisted view preferences; they remain available while the layout is locked because they do not mutate docking structure topology.

## 7. Design Patterns

### 1. Model-View-Controller (MVC)
- **Model**: DockGraph, DockNode, etc.
- **View**: DockLayoutEngine, DockNodeView
- **Controller**: SnapFX API, DockDragService

### 2. Composite Pattern
```
DockElement
    ├─ DockNode (Leaf)
    └─ DockContainer (Composite)
         ├─ DockSplitPane
         └─ DockTabPane
```

### 3. Observer Pattern
- ObservableList for children
- Properties for bindings
- ListChangeListener for updates

### 4. Facade Pattern
- SnapFX exposes a simplified API
- Hides complexity of graph, engine, etc.

### 5. Builder Pattern (implizit)
```java
DockNode node = snapFX.dock(content, "Title")
    .dock(other, "Other", node, DockPosition.RIGHT)
    .dock(third, "Third", other, DockPosition.BOTTOM);
```

## 8. Performance Optimizations

### 1. View caching
```java
Map<String, Node> viewCache;
// Prevents unnecessary recreation
```

### 2. Lazy loading
```java
// Views are created only during buildSceneGraph()
```

### 3. Smart flattening
```java
// Prevents deep nesting
SplitPane(H) { SplitPane(H) { A, B }, C }
// → SplitPane(H) { A, B, C }
```

### 4. Flat scene graph
```java
// Minimal wrapper layers
// Directly: SplitPane → TabPane → Content
```

## 9. Extension Points

### New container types
```java
class DockGridPane implements DockContainer {
    // 2D-Grid Layout
}
```

### Custom DockNodeView
```java
class MinimalDockNodeView extends StackPane {
    // No header, content only
}
```

### Advanced D&D zones
```java
enum DockPosition {
    // ... existing
    TOP_LEFT,
    TOP_RIGHT,
    // etc.
}
```

### Floating Windows
```java
class DockFloatingWindow {
    - stage: Stage
    - dockElement: DockElement
}
```

## 10. Best Practices

### 1. Register nodes before deserialization
```java
serializer.registerNode(myNode);
String json = serializer.serialize();
try {
    serializer.deserialize(json);
} catch (DockLayoutLoadException e) {
    // Handle invalid/corrupt layout data.
}
```

### 2. Rebuild after layout changes
```java
try {
    snapFX.loadLayout(json);
} catch (DockLayoutLoadException e) {
    // Handle invalid/corrupt layout data.
}
Parent newLayout = snapFX.buildLayout();
scene.setRoot(newLayout);
```

### 3. Use DockPosition.CENTER for tabs
```java
// Multiple editors in tabs
snapFX.dock(editor1, "File1.java");
snapFX.dock(editor2, "File2.java", editor1, CENTER);
```

### 4. Lock the layout in production
```java
snapFX.setLocked(true);
// Prevents accidental rearranging
```

## Summary

SnapFX provides a clean architecture with:

✅ Strict separation: model ↔ view  
✅ SOLID-friendly design: interface-based, extensible  
✅ Performance: flat scene graph, smart flattening  
✅ Simple API: `dock()`, `undock()`, `buildLayout()`  
✅ Persistence: JSON-based  
✅ Tests: comprehensive JUnit 5 + TestFX coverage

The architecture is production-ready and can serve as a base for complex docking systems.
