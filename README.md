# SnapFX - Lightweight JavaFX Docking Framework

<p align="center">
  <img src="src/main/resources/images/snapfx.svg" alt="SnapFX Logo" width="180">
</p>

A high-performance, lightweight JavaFX docking framework that behaves like native professional software (IntelliJ, Visual Studio).

**Note**: SnapFX is a Java Module (JPMS). Make sure your project is configured accordingly.

## MainDemo Preview

![MainDemo App Screenshot](docs/images/main-demo.png)

## Documentation Map

| File | Purpose |
|------|---------|
| `README.md` | Entry point, feature overview, and quick start |
| `SETUP.md` | Local development environment setup |
| `ARCHITECTURE.md` | Technical architecture and design |
| `STATUS.md` | Current state, open issues, and latest changes |
| `ROADMAP.md` | Planned work and future priorities |
| `DONE.md` | Completed milestones and delivered capabilities |
| `TESTING_POLICY.md` | Stable testing rules and quality gates |
| `CONTRIBUTING.md` | Contribution workflow, branch strategy, and PR checklist |
| `RELEASING.md` | Maintainer release process, versioning, tags, and CI release flow |
| `AGENTS.md` | Collaboration and workflow rules for AI agents |

## Features

### Core Architecture
- **Tree-Based Model**: Logical structure (DockGraph) decoupled from the visual representation
- **Minimal Wrapper**: Simple API `SnapFX.dock(myNode, "Title")`
- **Smart Splitting**: Automatic flattening when orientation matches
- **Auto-Cleanup**: Empty containers remove themselves automatically
- **Java Module**: Full support for Java Platform Module System (JPMS)

### Visual Features
- **Drag & Drop**: Global drag service with visual feedback
- **Dock Zones**: 5 zones (Top, Bottom, Left, Right, Center)
- **Floating Windows**: Custom undecorated floating windows with attach/maximize/restore/close controls
- **Cross-Window D&D**: Dock nodes between main layout and floating windows, including split/tab targets
- **Quick Float Actions**: Float buttons in title bars and tab headers
- **Resizable Floating Windows**: Resize from edges and corners (undecorated behavior)
- **Locked Mode**: Lock the layout; no D&D; no close buttons
- **Title Bar Modes**: ALWAYS/NEVER/AUTO; AUTO hides title bars for tabbed nodes to save space, so those nodes are moved via tabs only (pairs well with compact/locked layouts)

### Persistence
- **Layout Save/Load**: JSON-based serialization
- **Full Structure**: Positions and split percentages
- **Runtime Floating Memory**: Float/attach toggles preserve last floating bounds per node in-session

### Look & Feel
- **Native Look**: Seamless integration with the JavaFX Modena theme
- **CSS-based**: Fully customizable

## Quick Start

Maven Central dependency coordinates will be documented here once the first Maven Central release is published.

### Simple Example

```java
import com.github.beowolve.snapfx.SnapFX;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class SimpleDemo extends Application {
    @Override
    public void start(Stage stage) {
        SnapFX snapFX = new SnapFX();

        // Dock nodes
        snapFX.dock(new TextArea("Editor 1"), "Editor");
        snapFX.dock(new TextArea("Console"), "Console");

        // Build layout
        Scene scene = new Scene(snapFX.buildLayout(), 800, 600);
        scene.getStylesheets().add(getClass().getResource("/snapfx.css").toExternalForm());

        stage.setScene(scene);
        snapFX.initialize(stage);
        stage.show();
    }
}
```

### Advanced Usage

```java
// Create nodes with custom IDs (required for cross-session persistence)
DockNode editor = new DockNode("mainEditor", new TextArea(), "Editor");
DockNode console = new DockNode("console", new TextArea(), "Console");
DockNode sidebar = new DockNode("projectExplorer", new TreeView<>(), "Files");

// Dock nodes at specific positions
snapFX.getDockGraph().setRoot(editor);
snapFX.getDockGraph().dock(console, editor, DockPosition.BOTTOM);
snapFX.getDockGraph().dock(sidebar, editor, DockPosition.LEFT);

// Add as tab
DockNode tasks = new DockNode("tasks", new ListView<>(), "Tasks");
snapFX.getDockGraph().dock(tasks, console, DockPosition.CENTER);

// Lock layout
snapFX.setLocked(true);

// Move a node to a floating window and attach it back later
DockFloatingWindow floating = snapFX.floatNode(console);
snapFX.attachFloatingWindow(floating);

// Setup factory for save/load across sessions
snapFX.setNodeFactory(nodeId -> switch(nodeId) {
    case "mainEditor" -> new DockNode(nodeId, new TextArea(), "Editor");
    case "console" -> new DockNode(nodeId, new TextArea(), "Console");
    case "projectExplorer" -> new DockNode(nodeId, new TreeView<>(), "Files");
    case "tasks" -> new DockNode(nodeId, new ListView<>(), "Tasks");
    default -> null;
});

// Save/load layout (works across application restarts)
String json = snapFX.saveLayout();
Files.writeString(Path.of("layout.json"), json);

// Later session:
String json = Files.readString(Path.of("layout.json"));
snapFX.loadLayout(json); // Factory recreates nodes from IDs
```

## Architecture

### Model Layer
- `DockElement`: Base interface for all dock elements
- `DockNode`: Wrapper for dockable nodes
- `DockContainer`: Interface for containers (SplitPane, TabPane)
- `DockSplitPane`: Split container with smart flattening
- `DockTabPane`: Tab container with auto-hide
- `DockGraph`: Central data structure

### View Layer
- `DockLayoutEngine`: Converts model -> scene graph
- `DockNodeView`: Visual representation of a DockNode

### Drag & Drop
- `DockDragService`: Central D&D service
- `DockDragData`: Transfer object for D&D operations

### Persistence
- `DockLayoutSerializer`: JSON serialization/deserialization
- `DockNodeFactory`: Factory interface for node recreation (cross-session)

## Testing

The framework includes comprehensive tests:

```bash
# Run tests
./gradlew test

# With more output
./gradlew test --info
```

Test coverage:
- Tree manipulation (add/move/remove)
- Smart flattening
- Auto-cleanup
- Serialization/deserialization
- Layout engine
- Floating window lifecycle and restore behavior
- Cross-window drag/drop routing
- Current test status: **161/161 passing**

## Demo Application

A full demo app is included:

```bash
./gradlew run
```

The demo shows:
- Typical IDE layout (sidebar, editor, console)
- Lock/unlock functionality
- Save/load layout
- Multiple tabs
- Floating/attach workflows from menu, title bars, and tab headers

### Update Preview Screenshot

```bash
# Regenerate README preview image from the current MainDemo UI
./scripts/update-main-demo-preview.ps1
```

Optional output path:

```bash
./scripts/update-main-demo-preview.ps1 -OutputPath "docs/images/main-demo.png"
```

## Example Layouts

### IDE Layout
```
┌─────────────┬──────────────────────┬─────────────┐
│   Project   │       Editor         │ Properties  │
│   Explorer  │                      │             │
│             ├──────────────────────┤             │
│             │   Console | Tasks    │             │
└─────────────┴──────────────────────┴─────────────┘
```

### Code with SnapFX
```java
DockNode project = snapFX.dock(projectTree, "Project");
DockNode editor = snapFX.dock(editorArea, "Editor", project, DockPosition.RIGHT);
DockNode props = snapFX.dock(propsPanel, "Properties", editor, DockPosition.RIGHT);
DockNode console = snapFX.dock(consoleArea, "Console", editor, DockPosition.BOTTOM);
DockNode tasks = snapFX.dock(tasksList, "Tasks", console, DockPosition.CENTER);
```

## Roadmap

- [x] **Core drag & drop**: Hit-testing, zone detection, and tab insert targeting
- [x] **Floating windows (core)**: External stage lifecycle + custom controls
- [x] **Floating windows (D&D)**: Main <-> floating docking and multi-node floating layouts
- [x] **Drag preview**: Snapshot ghost overlay
- [ ] **Animations**: Smooth transitions
- [ ] **Keyboard shortcuts**: Layout navigation
- [ ] **Context menus**: Right-click options
- [ ] **Perspectives**: Predefined layouts

## License

This project is licensed under the MIT License.

SnapFX is intended for personal and commercial use, including large applications.

## Contributing

For contribution workflow, branch strategy, commit/PR expectations, and quality gates, see `CONTRIBUTING.md`.

For maintainer release/versioning/tag flow, see `RELEASING.md`.

## Documentation

Each class is documented. See JavaDoc in the source files.

### Key Concepts

**Smart flattening**: Prevents unnecessary nesting
```java
// Automatically optimized:
SplitPane(H) { SplitPane(H) { A, B }, C }
// Becomes:
SplitPane(H) { A, B, C }
```

**Auto-cleanup**: Empty containers remove themselves automatically
```java
// After removing the last tab:
TabPane { Tab1 } -> Tab1 (TabPane removed)
```

**Locked mode**: Prevents layout changes
```java
snapFX.setLocked(true);
// No D&D, no close buttons, tabs only visible when >1
```

## Technology Stack

- **Java 21** (LTS)
- **JavaFX 21**
- **Gson 2.10.1** (JSON)
- **JUnit 5** (testing)
- **TestFX 4.0.18** (UI testing)
- **Gradle** (build)

## Support

If you have questions or issues, open an issue in the repository.

---

**SnapFX** - Making JavaFX Docking Simple and Powerful
