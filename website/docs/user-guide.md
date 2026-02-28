# User Guide

This guide covers the main runtime features of SnapFX and the recommended usage flow.

## Core Concepts

- `SnapFX`: main facade API for setup, behavior toggles, and persistence.
- `DockGraph`: model tree that represents layout structure.
- `DockNode`: wrapper around each dockable JavaFX `Node` with ID/title/icon metadata.

## Typical Setup Flow

1. Create `SnapFX`.
2. Create dock nodes (or use facade helpers).
3. Build and show layout.
4. Initialize SnapFX with the primary stage.

```java
SnapFX snapFX = new SnapFX();
snapFX.dock(new TextArea("Editor"), "Editor");
snapFX.dock(new TextArea("Console"), "Console");

Scene scene = new Scene(snapFX.buildLayout(), 1100, 700);
stage.setScene(scene);
snapFX.initialize(stage);
stage.show();
```

## Docking and Layout Composition

- Use `dock(...)` to place new nodes relative to existing nodes.
- Use `DockPosition.LEFT/RIGHT/TOP/BOTTOM/CENTER` for split/tab targets.
- Use stable IDs for nodes when you need cross-session restore.

```java
DockNode editor = new DockNode("editor.main", new TextArea(), "Editor");
DockNode console = new DockNode("console.main", new TextArea(), "Console");
snapFX.getDockGraph().setRoot(editor);
snapFX.getDockGraph().dock(console, editor, DockPosition.BOTTOM);
```

## Floating Windows

- Float any node with `floatNode(...)`.
- Attach floating windows back with `attachFloatingWindow(...)`.
- Configure always-on-top behavior and snap settings through API.

```java
DockFloatingWindow floating = snapFX.floatNode(editor);
snapFX.attachFloatingWindow(floating);
```

## Sidebars

SnapFX supports left/right sidebar workflows for pinned tool windows:

- move nodes to sidebars
- restore from sidebars
- pinned-open vs collapsed panel behavior
- optional sidebar visibility modes (`ALWAYS`, `AUTO`, `NEVER`)

## Persistence

For robust save/load across restarts:

- assign stable node IDs
- register a node factory that can recreate nodes from IDs

```java
snapFX.setNodeFactory(nodeId -> switch (nodeId) {
    case "editor.main" -> new DockNode(nodeId, new TextArea(), "Editor");
    case "console.main" -> new DockNode(nodeId, new TextArea(), "Console");
    default -> null;
});

String json = snapFX.saveLayout();
snapFX.loadLayout(json);
```

## Themes

- default theme is auto-applied on `initialize(...)`
- runtime switching is supported via `setThemeStylesheet(...)`
- built-in named themes are exposed by the API

## Recommended Workflow

1. Model your primary use cases in MainDemo-like layout patterns.
2. Add persistence and verify load behavior with factory-backed IDs.
3. Enable lock mode for final user-facing safety where needed.
4. Add regression tests for model-level behavior changes.

## Related References

- [Examples](/examples)
- [Tutorial: First Layout](/tutorial-first-layout)
- [API JavaDoc](https://snapfx.org/api/)
