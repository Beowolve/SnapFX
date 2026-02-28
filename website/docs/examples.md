# Examples

This page collects practical SnapFX usage patterns.

## IDE Layout (Editor + Console + Project)

```java
DockNode project = new DockNode("project", new TreeView<>(), "Project");
DockNode editor = new DockNode("editor", new TextArea(), "Editor");
DockNode console = new DockNode("console", new TextArea(), "Console");

snapFX.getDockGraph().setRoot(editor);
snapFX.getDockGraph().dock(project, editor, DockPosition.LEFT);
snapFX.getDockGraph().dock(console, editor, DockPosition.BOTTOM);
```

## Tab Grouping

```java
DockNode tasks = new DockNode("tasks", new ListView<>(), "Tasks");
snapFX.getDockGraph().dock(tasks, console, DockPosition.CENTER);
```

## Float and Reattach

```java
DockFloatingWindow floating = snapFX.floatNode(console);
// ... user works in floating window ...
snapFX.attachFloatingWindow(floating);
```

## Layout Locking

```java
snapFX.setLocked(true);
// user cannot rearrange or close layout elements while locked
```

## Shortcut Override

```java
snapFX.setShortcut(
    DockShortcutAction.CLOSE_ACTIVE_NODE,
    new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN)
);
```

## Theme Switch

```java
String dark = SnapFX.getAvailableThemeStylesheets().get("Dark");
snapFX.setThemeStylesheet(dark);
```

## Save and Load

```java
String state = snapFX.saveLayout();
snapFX.loadLayout(state);
```

## API Deep Dive

Use JavaDoc for full API details and class-level examples:

- [SnapFX API JavaDoc](https://snapfx.org/api/)
