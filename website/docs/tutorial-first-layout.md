# Tutorial: First Layout

This tutorial builds a simple IDE-style SnapFX layout in small steps.

## Step 1: Create SnapFX and Root Node

```java
SnapFX snapFX = new SnapFX();

DockNode editor = new DockNode(
    "editor.main",
    new TextArea("Start typing..."),
    "Editor"
);
snapFX.getDockGraph().setRoot(editor);
```

## Step 2: Add Console and Project Tree

```java
DockNode console = new DockNode(
    "console.main",
    new TextArea("Build output"),
    "Console"
);

DockNode project = new DockNode(
    "project.tree",
    new TreeView<>(),
    "Project"
);

snapFX.getDockGraph().dock(console, editor, DockPosition.BOTTOM);
snapFX.getDockGraph().dock(project, editor, DockPosition.LEFT);
```

## Step 3: Build Scene and Initialize

```java
Scene scene = new Scene(snapFX.buildLayout(), 1200, 760);
stage.setScene(scene);
snapFX.initialize(stage);
stage.setTitle("SnapFX Tutorial");
stage.show();
```

## Step 4: Add Persistence Factory

```java
snapFX.setNodeFactory(nodeId -> switch (nodeId) {
    case "editor.main" -> new DockNode(nodeId, new TextArea(), "Editor");
    case "console.main" -> new DockNode(nodeId, new TextArea(), "Console");
    case "project.tree" -> new DockNode(nodeId, new TreeView<>(), "Project");
    default -> null;
});
```

## Step 5: Save and Restore Layout

```java
String json = snapFX.saveLayout();
Files.writeString(Path.of("layout.json"), json);

String loaded = Files.readString(Path.of("layout.json"));
snapFX.loadLayout(loaded);
```

## What To Try Next

- Float and reattach a dock node, then review related options in the [User Guide](/user-guide).
- Test lock mode behavior and persistence with real layout changes.
- Configure shortcuts and sidebars, then compare with [Examples](/examples).
- Switch theme at runtime and check API-level details in [API JavaDoc](https://snapfx.org/api/).
