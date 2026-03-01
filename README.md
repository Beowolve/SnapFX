# ![SnapFX Logo](snapfx-demo/src/main/resources/images/32/snapfx.png) SnapFX - Lightweight JavaFX Docking Framework

![License](https://img.shields.io/github/license/Beowolve/SnapFX)
![Build](https://img.shields.io/github/actions/workflow/status/Beowolve/SnapFX/ci.yml?branch=main)
![Release](https://img.shields.io/github/v/release/Beowolve/SnapFX)
![Java](https://img.shields.io/badge/Java-21+-blue)
![JavaFX](https://img.shields.io/badge/JavaFX-21+-orange)

![MainDemo App Screenshot](docs/images/main-demo.png)
*MainDemo application showing docking and tabbing.*

A high-performance, lightweight JavaFX docking framework that behaves like native professional software (IntelliJ, Visual Studio).

* [x] JPMS compatible
* [x] No reflection hacks
* [x] Production-ready architecture
* [x] Cross-window drag & dock

## Getting Started

Maven Central publishing is currently in progress and not yet publicly available.
Until then, use the project source directly and/or the demo assets from GitHub Releases.

## Public Preview Status

- **Status:** Public Preview (0.6.1)
- API considered stable but packaging is still being finalized.
- Maven Central publishing is not live yet; current work focuses on packaging hardening and publishing readiness.
- Planned first Maven coordinates remain: `org.snapfx:snapfx-core`.
- Current project state and open items are tracked in [STATUS.md](STATUS.md) and [ROADMAP.md](ROADMAP.md).

### Installation

**Gradle**
```
implementation("org.snapfx:snapfx-core:<version>")
```

Temporarily build from source:

```bash
git clone https://github.com/Beowolve/SnapFX.git
./gradlew publishToMavenLocal
```

### Simple Example

```java
import org.snapfx.SnapFX;
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

        stage.setScene(scene);
        snapFX.initialize(stage);
        // Optional: switch theme at runtime via named catalog entry
        // snapFX.setThemeStylesheet(SnapFX.getAvailableThemeStylesheets().get("Dark"));
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

// Override framework shortcut defaults (or clear with snapFX.clearShortcut(...))
snapFX.setShortcut(
    DockShortcutAction.CLOSE_ACTIVE_NODE,
    new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN)
);

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

// Optional: implement DockNodeFactory#createUnknownNode(...) when you want
// a custom fallback for unsupported/corrupted serialized element types.
// Returning null keeps the built-in SnapFX placeholder behavior.

// Save/load layout (works across application restarts)
String json = snapFX.saveLayout();
Files.writeString(Path.of("layout.json"), json);

// Later session:
String json = Files.readString(Path.of("layout.json"));
try {
    snapFX.loadLayout(json); // Factory recreates nodes from IDs
} catch (DockLayoutLoadException e) {
    // Handle invalid/corrupt layout JSON.
}
```

## Documentation Map

| File                               | Purpose                                                                      |
|------------------------------------|------------------------------------------------------------------------------|
| [README.md](README.md)             | Entry point, feature overview, and quick start                               |
| [SETUP.md](SETUP.md)               | Local development environment setup                                          |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Technical architecture and design                                            |
| [STATUS.md](STATUS.md)             | Current state and open issues                                                |
| [ROADMAP.md](ROADMAP.md)           | Planned work and future priorities                                           |
| [DONE.md](DONE.md)                 | Completed milestones and delivered capabilities                              |
| [CHANGELOG.md](CHANGELOG.md)       | Versioned release history grouped by tags                                    |
| [TESTING_POLICY.md](TESTING_POLICY.md)           | Stable testing rules and quality gates                                       |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Contribution workflow, branch strategy, and PR checklist                     |
| [RELEASING.md](RELEASING.md)                | Maintainer release process, versioning, tags, and CI release flow            |
| [AGENTS.md](AGENTS.md)             | Collaboration and workflow rules for AI agents                               |
| `docs/adr/*.md`                    | Architecture Decision Records (context, decisions, and trade-offs)           |

## Hosted Documentation

- Documentation portal: [https://snapfx.org/](https://snapfx.org/)
- Public API JavaDoc: [https://snapfx.org/api/](https://snapfx.org/api/)

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
- **Snapping**: Floating windows snap to edges and corners (configurable)
- **Cross-Window D&D**: Dock nodes between main layout and floating windows, including split/tab targets
- **Quick Float Actions**: Float buttons in title bars and tab headers
- **Context Menus**: Right-click actions for tabs, splitters, dock headers, and floating title bars (`Attach to Layout`, always-on-top toggle)
- **Resizable Floating Windows**: Resize from edges and corners (undecorated behavior)
- **Locked Mode**: Lock the layout; no D&D; no close buttons
- **Configurable Keyboard Shortcuts**: Default actions (`Ctrl+W`, `Ctrl+Tab`, `Ctrl+Shift+Tab`, `Escape`, `Ctrl+Shift+P`) can be remapped or disabled via API
- **Title Bar Modes**: ALWAYS/NEVER/AUTO; AUTO hides title bars for tabbed nodes to save space, so those nodes are moved via tabs only (pairs well with compact/locked layouts)

### Persistence
- **Layout Save/Load**: JSON-based serialization
- **Full Structure**: Positions and split percentages
- **Runtime Floating Memory**: Float/attach toggles preserve last floating bounds per node in-session

### Look & Feel
- **Native Look**: Seamless integration with the JavaFX Modena theme
- **Themeable**: Dark/Light mode included
- **CSS-based**: Fully customizable


## Demo Application

A full demo app is included in the project.
You can run it with gradle or download the release assets from GitHub Releases.
Everything is packaged with the jre and dependencies, so no installation required.
Just unzip the archive and run it.

**Windows**, **macOS**, **Linux** packages are provided for each release.

```bash
./gradlew run
```

### The demo shows all the features, including:
- Typical IDE layout (sidebar, editor, console)
- Lock/unlock functionality
- Save/load layout
- D&D between main layout and floating windows
- Floating/attach workflows from menu, title bars, and tab headers
- Theme switching

## Technology Stack

- **Java 21+**
- **JavaFX 21+**
- **Gson** (JSON serialization)
- **JUnit 5 / TestFX** (testing)
- **Gradle** (build)


## Contributing

* For contribution workflow, branch strategy, commit/PR expectations, and quality gates, see [CONTRIBUTING.md](CONTRIBUTING.md).
* For maintainer release/versioning/tag flow, see [RELEASING.md](RELEASING.md).
* Contact me via Discord if you have questions or suggestions. (Discord invite: https://discord.gg/WwDGWkVsnB)


## License

This project is licensed under the MIT License.

SnapFX is intended for personal and commercial use, including large applications.


## Support

* If you have questions or issues, open an issue in the repository.
* Discord server to get in contact with the devs and community: https://discord.gg/WwDGWkVsnB

---

**SnapFX** - Making JavaFX Docking Simple and Powerful
