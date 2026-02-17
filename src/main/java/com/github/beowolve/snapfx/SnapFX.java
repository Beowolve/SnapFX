package com.github.beowolve.snapfx;

import com.github.beowolve.snapfx.close.DockCloseBehavior;
import com.github.beowolve.snapfx.close.DockCloseDecision;
import com.github.beowolve.snapfx.close.DockCloseRequest;
import com.github.beowolve.snapfx.close.DockCloseResult;
import com.github.beowolve.snapfx.close.DockCloseSource;
import com.github.beowolve.snapfx.dnd.DockDragService;
import com.github.beowolve.snapfx.dnd.DockDropVisualizationMode;
import com.github.beowolve.snapfx.floating.DockFloatingPinButtonMode;
import com.github.beowolve.snapfx.floating.DockFloatingPinChangeEvent;
import com.github.beowolve.snapfx.floating.DockFloatingPinLockedBehavior;
import com.github.beowolve.snapfx.floating.DockFloatingPinSource;
import com.github.beowolve.snapfx.floating.DockFloatingSnapTarget;
import com.github.beowolve.snapfx.floating.DockFloatingWindow;
import com.github.beowolve.snapfx.model.*;
import com.github.beowolve.snapfx.persistence.DockLayoutSerializer;
import com.github.beowolve.snapfx.persistence.DockLayoutLoadException;
import com.github.beowolve.snapfx.persistence.DockNodeFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.github.beowolve.snapfx.view.DockCloseButtonMode;
import com.github.beowolve.snapfx.view.DockLayoutEngine;
import com.github.beowolve.snapfx.view.DockNodeView;
import com.github.beowolve.snapfx.view.DockTitleBarMode;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Main API class for the SnapFX docking framework.
 * Provides a simple, fluent API for docking JavaFX nodes.
 */
public class SnapFX {
    private static final Gson SNAPSHOT_GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String SNAPSHOT_MAIN_LAYOUT_KEY = "mainLayout";
    private static final String SNAPSHOT_FLOATING_WINDOWS_KEY = "floatingWindows";
    private static final String SNAPSHOT_FLOATING_LAYOUT_KEY = "layout";
    private static final String SNAPSHOT_FLOATING_X_KEY = "x";
    private static final String SNAPSHOT_FLOATING_Y_KEY = "y";
    private static final String SNAPSHOT_FLOATING_WIDTH_KEY = "width";
    private static final String SNAPSHOT_FLOATING_HEIGHT_KEY = "height";
    private static final String SNAPSHOT_FLOATING_ALWAYS_ON_TOP_KEY = "alwaysOnTop";
    private static final KeyCombination DEFAULT_SHORTCUT_CLOSE_ACTIVE_NODE = new KeyCodeCombination(
        KeyCode.W,
        KeyCombination.SHORTCUT_DOWN
    );
    private static final KeyCombination DEFAULT_SHORTCUT_NEXT_TAB = new KeyCodeCombination(
        KeyCode.TAB,
        KeyCombination.SHORTCUT_DOWN
    );
    private static final KeyCombination DEFAULT_SHORTCUT_PREVIOUS_TAB = new KeyCodeCombination(
        KeyCode.TAB,
        KeyCombination.SHORTCUT_DOWN,
        KeyCombination.SHIFT_DOWN
    );
    private static final KeyCombination DEFAULT_SHORTCUT_CANCEL_DRAG = new KeyCodeCombination(KeyCode.ESCAPE);
    private static final KeyCombination DEFAULT_SHORTCUT_TOGGLE_ACTIVE_FLOATING_PIN = new KeyCodeCombination(
        KeyCode.P,
        KeyCombination.SHORTCUT_DOWN,
        KeyCombination.SHIFT_DOWN
    );

    private final DockGraph dockGraph;
    private final DockLayoutEngine layoutEngine;
    private final DockDragService dragService;
    private final DockLayoutSerializer serializer;
    private final EnumMap<DockShortcutAction, KeyCombination> shortcuts;
    private final EventHandler<KeyEvent> shortcutKeyEventFilter;
    private final ChangeListener<Scene> rootContainerSceneListener;
    private final Map<DockFloatingWindow, Scene> floatingShortcutScenes;
    private final Map<DockNode, DockPlacementMemory> dockPlacementMemory;
    private Stage primaryStage;
    private DockNodeFactory nodeFactory;
    private Scene shortcutScene;
    private DockFloatingWindow activeFloatingWindow;

    // Hidden nodes (removed from layout but not destroyed)
    private final ObservableList<DockNode> hiddenNodes;
    private final ObservableList<DockFloatingWindow> floatingWindows;
    private final ObservableList<DockFloatingWindow> readOnlyFloatingWindows;
    private DockCloseBehavior defaultCloseBehavior = DockCloseBehavior.HIDE;
    private Function<DockCloseRequest, DockCloseDecision> onCloseRequest;
    private Consumer<DockCloseResult> onCloseHandled;
    private Consumer<DockNode> legacyNodeCloseRequest;
    private DockFloatingPinButtonMode floatingPinButtonMode = DockFloatingPinButtonMode.AUTO;
    private boolean defaultFloatingAlwaysOnTop = true;
    private boolean allowFloatingPinToggle = true;
    private DockFloatingPinLockedBehavior floatingPinLockedBehavior = DockFloatingPinLockedBehavior.ALLOW;
    private boolean floatingWindowSnappingEnabled = true;
    private double floatingWindowSnapDistance = 12.0;
    private EnumSet<DockFloatingSnapTarget> floatingWindowSnapTargets = EnumSet.of(
        DockFloatingSnapTarget.SCREEN,
        DockFloatingSnapTarget.MAIN_WINDOW,
        DockFloatingSnapTarget.FLOATING_WINDOWS
    );
    private Consumer<DockFloatingPinChangeEvent> onFloatingPinChanged;

    private Pane rootContainer; // Container that holds the buildLayout() result

    public SnapFX() {
        this.dockGraph = new DockGraph();
        this.dragService = new DockDragService(dockGraph);
        this.layoutEngine = new DockLayoutEngine(dockGraph, dragService);
        this.serializer = new DockLayoutSerializer(dockGraph);
        this.shortcuts = new EnumMap<>(DockShortcutAction.class);
        this.shortcutKeyEventFilter = this::handleShortcutKeyPressed;
        this.rootContainerSceneListener = (obs, oldScene, newScene) -> rebindShortcutScene(newScene);
        this.floatingShortcutScenes = new HashMap<>();
        this.dockPlacementMemory = new HashMap<>();
        this.hiddenNodes = FXCollections.observableArrayList();
        this.floatingWindows = FXCollections.observableArrayList();
        this.readOnlyFloatingWindows = FXCollections.unmodifiableObservableList(floatingWindows);
        resetShortcutsToDefaults();
        this.layoutEngine.setOnNodeCloseRequest(this::handleDockNodeCloseRequest);
        this.layoutEngine.setOnNodeFloatRequest(node -> floatNode(node));
        this.dragService.setOnDropRequest(this::handleResolvedDropRequest);
        this.dragService.setOnFloatDetachRequest(this::handleUnresolvedDropRequest);
        this.dragService.setOnDragHover(this::handleDragHover);
        this.dragService.setOnDragFinished(this::clearFloatingDropPreviews);
        this.dragService.setSuppressMainDropAtScreenPoint(this::isMainDropSuppressedByFloatingWindow);

        // Auto-rebuild view when revision changes (after D&D, dock/undock operations)
        this.dockGraph.revisionProperty().addListener((obs, o, n) -> {
            if (rootContainer != null) {
                // Rebuild on next frame to ensure all model changes are complete
                Platform.runLater(this::rebuildRootView);
            }
        });

        // Auto-rebuild view when root element changes
        this.dockGraph.rootProperty().addListener((obs, oldRoot, newRoot) -> {
            if (oldRoot != newRoot && rootContainer != null) {
                // Root element changed, rebuild the view
                Platform.runLater(this::rebuildRootView);
            }
        });
    }

    /**
     * Initializes SnapFX with the primary stage.
     */
    public void initialize(Stage stage) {
        this.primaryStage = stage;
        dragService.initialize(stage);
        dragService.setLayoutEngine(layoutEngine);
        for (DockFloatingWindow floatingWindow : floatingWindows) {
            floatingWindow.show(primaryStage);
            bindFloatingShortcutScene(floatingWindow);
        }
    }

    /**
     * Simple API to dock a node with a title.
     * Creates a DockNode and adds it to the root.
     */
    public DockNode dock(Node content, String title) {
        DockNode dockNode = new DockNode(content, title);

        if (dockGraph.getRoot() == null) {
            dockGraph.setRoot(dockNode);
        } else {
            // Default: split to the right
            dockGraph.dock(dockNode, dockGraph.getRoot(), DockPosition.RIGHT);
        }

        return dockNode;
    }

    /**
     * Docks a node at a specific position relative to a target.
     */
    public DockNode dock(Node content, String title, DockElement target, DockPosition position) {
        DockNode dockNode = new DockNode(content, title);
        dockGraph.dock(dockNode, target, position);
        return dockNode;
    }

    /**
     * Docks an existing DockNode at a specific position.
     */
    public void dock(DockNode node, DockElement target, DockPosition position) {
        dockGraph.dock(node, target, position);
    }

    /**
     * Removes a DockNode from the graph.
     */
    @SuppressWarnings("unused")
    public void undock(DockNode node) {
        dockGraph.undock(node);
    }

    /**
     * Builds the visual representation of the current dock layout.
     * The returned Parent will automatically update when the model changes.
     */
    public Parent buildLayout() {
        Node layout = layoutEngine.buildSceneGraph();

        // Wrap in a container that we can update
        if (rootContainer == null) {
            rootContainer = new StackPane();
            rootContainer.sceneProperty().addListener(rootContainerSceneListener);
        }

        rootContainer.getChildren().clear();
        if (layout != null) {
            rootContainer.getChildren().add(layout);
        }
        rebindShortcutScene(rootContainer.getScene());

        return rootContainer;
    }

    private void rebuildRootView() {
        if (rootContainer == null) {
            return;
        }

        Node layout = layoutEngine.buildSceneGraph();
        rootContainer.getChildren().clear();
        if (layout != null) {
            rootContainer.getChildren().add(layout);
        }
    }

    private void requestRebuild() {
        if (rootContainer != null) {
            Platform.runLater(this::rebuildRootView);
        }
    }

    /**
     * Restores the default framework shortcut mapping.
     *
     * <p>Default bindings:</p>
     * <ul>
     *   <li>{@code Ctrl+W}: {@link DockShortcutAction#CLOSE_ACTIVE_NODE}</li>
     *   <li>{@code Ctrl+Tab}: {@link DockShortcutAction#NEXT_TAB}</li>
     *   <li>{@code Ctrl+Shift+Tab}: {@link DockShortcutAction#PREVIOUS_TAB}</li>
     *   <li>{@code Escape}: {@link DockShortcutAction#CANCEL_DRAG}</li>
     *   <li>{@code Ctrl+Shift+P}: {@link DockShortcutAction#TOGGLE_ACTIVE_FLOATING_ALWAYS_ON_TOP}</li>
     * </ul>
     */
    public void resetShortcutsToDefaults() {
        shortcuts.clear();
        shortcuts.put(DockShortcutAction.CLOSE_ACTIVE_NODE, DEFAULT_SHORTCUT_CLOSE_ACTIVE_NODE);
        shortcuts.put(DockShortcutAction.NEXT_TAB, DEFAULT_SHORTCUT_NEXT_TAB);
        shortcuts.put(DockShortcutAction.PREVIOUS_TAB, DEFAULT_SHORTCUT_PREVIOUS_TAB);
        shortcuts.put(DockShortcutAction.CANCEL_DRAG, DEFAULT_SHORTCUT_CANCEL_DRAG);
        shortcuts.put(DockShortcutAction.TOGGLE_ACTIVE_FLOATING_ALWAYS_ON_TOP, DEFAULT_SHORTCUT_TOGGLE_ACTIVE_FLOATING_PIN);
    }

    /**
     * Assigns or removes a key binding for a built-in shortcut action.
     *
     * @param action Shortcut action to configure
     * @param keyCombination Key combination to assign, or {@code null} to remove the binding
     */
    public void setShortcut(DockShortcutAction action, KeyCombination keyCombination) {
        if (action == null) {
            return;
        }
        if (keyCombination == null) {
            shortcuts.remove(action);
            return;
        }

        shortcuts.entrySet().removeIf(entry ->
            entry.getKey() != action && entry.getValue().equals(keyCombination)
        );
        shortcuts.put(action, keyCombination);
    }

    /**
     * Removes the key binding for a shortcut action.
     */
    public void clearShortcut(DockShortcutAction action) {
        setShortcut(action, null);
    }

    /**
     * Returns the configured key binding for a shortcut action.
     */
    public KeyCombination getShortcut(DockShortcutAction action) {
        if (action == null) {
            return null;
        }
        return shortcuts.get(action);
    }

    /**
     * Returns a snapshot of all currently configured shortcut bindings.
     */
    public Map<DockShortcutAction, KeyCombination> getShortcuts() {
        return Collections.unmodifiableMap(new EnumMap<>(shortcuts));
    }

    private void rebindShortcutScene(Scene scene) {
        if (shortcutScene == scene) {
            return;
        }
        if (shortcutScene != null) {
            shortcutScene.removeEventFilter(KeyEvent.KEY_PRESSED, shortcutKeyEventFilter);
        }
        shortcutScene = scene;
        if (shortcutScene != null) {
            shortcutScene.addEventFilter(KeyEvent.KEY_PRESSED, shortcutKeyEventFilter);
        }
    }

    private void bindFloatingShortcutScene(DockFloatingWindow floatingWindow) {
        if (floatingWindow == null) {
            return;
        }
        Scene scene = floatingWindow.getScene();
        Scene previousScene = floatingShortcutScenes.get(floatingWindow);
        if (previousScene == scene) {
            return;
        }
        if (previousScene != null) {
            previousScene.removeEventFilter(KeyEvent.KEY_PRESSED, shortcutKeyEventFilter);
        }
        if (scene == null) {
            floatingShortcutScenes.remove(floatingWindow);
            return;
        }
        scene.addEventFilter(KeyEvent.KEY_PRESSED, shortcutKeyEventFilter);
        floatingShortcutScenes.put(floatingWindow, scene);
    }

    private void unbindFloatingShortcutScene(DockFloatingWindow floatingWindow) {
        if (floatingWindow == null) {
            return;
        }
        Scene scene = floatingShortcutScenes.remove(floatingWindow);
        if (scene == null) {
            return;
        }
        scene.removeEventFilter(KeyEvent.KEY_PRESSED, shortcutKeyEventFilter);
    }

    private void handleShortcutKeyPressed(KeyEvent event) {
        if (event == null || event.getEventType() != KeyEvent.KEY_PRESSED) {
            return;
        }
        DockShortcutAction action = resolveShortcutAction(event);
        if (action == null) {
            return;
        }
        if (executeShortcutAction(action, event.getTarget())) {
            event.consume();
        }
    }

    private DockShortcutAction resolveShortcutAction(KeyEvent event) {
        for (Map.Entry<DockShortcutAction, KeyCombination> entry : shortcuts.entrySet()) {
            KeyCombination combination = entry.getValue();
            if (combination != null && combination.match(event)) {
                return entry.getKey();
            }
        }
        return null;
    }

    boolean executeShortcutAction(DockShortcutAction action, Object eventTarget) {
        if (action == null) {
            return false;
        }
        return switch (action) {
            case CLOSE_ACTIVE_NODE -> closeActiveDockNode(eventTarget);
            case NEXT_TAB -> selectTabRelative(1, eventTarget);
            case PREVIOUS_TAB -> selectTabRelative(-1, eventTarget);
            case CANCEL_DRAG -> cancelActiveDrag();
            case TOGGLE_ACTIVE_FLOATING_ALWAYS_ON_TOP -> toggleActiveFloatingAlwaysOnTop(eventTarget);
        };
    }

    private boolean closeActiveDockNode(Object eventTarget) {
        if (dockGraph.isLocked()) {
            return false;
        }
        DockNode activeNode = resolveActiveDockNode(eventTarget);
        if (activeNode == null || !activeNode.isCloseable()) {
            return false;
        }
        handleDockNodeCloseRequest(activeNode, DockCloseSource.TITLE_BAR);
        return true;
    }

    private boolean selectTabRelative(int direction, Object eventTarget) {
        TabPane activeTabPane = resolveActiveTabPane(eventTarget);
        if (activeTabPane == null || activeTabPane.getTabs().size() < 2) {
            return false;
        }

        int tabCount = activeTabPane.getTabs().size();
        int selectedIndex = activeTabPane.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0) {
            selectedIndex = 0;
        }
        int nextIndex = Math.floorMod(selectedIndex + direction, tabCount);
        activeTabPane.getSelectionModel().select(nextIndex);

        Tab selectedTab = activeTabPane.getSelectionModel().getSelectedItem();
        if (selectedTab != null && selectedTab.getContent() != null) {
            selectedTab.getContent().requestFocus();
        }
        return true;
    }

    private boolean cancelActiveDrag() {
        if (!dragService.isDragging()) {
            return false;
        }
        dragService.cancelDrag();
        return true;
    }

    private boolean toggleActiveFloatingAlwaysOnTop(Object eventTarget) {
        DockFloatingWindow activeWindow = resolveActiveFloatingWindow(eventTarget);
        if (activeWindow == null) {
            return false;
        }
        activeWindow.setAlwaysOnTop(!activeWindow.isAlwaysOnTop(), DockFloatingPinSource.API);
        return true;
    }

    private DockNode resolveActiveDockNode(Object eventTarget) {
        Node targetNode = resolveNodeFromEventTarget(eventTarget);
        DockNode nodeFromTarget = resolveDockNodeFromHierarchy(targetNode);
        if (nodeFromTarget != null) {
            return nodeFromTarget;
        }

        Node focusedNode = resolveFocusedNode(eventTarget);
        DockNode nodeFromFocus = resolveDockNodeFromHierarchy(focusedNode);
        if (nodeFromFocus != null) {
            return nodeFromFocus;
        }

        return resolveSelectedDockNode(dockGraph.getRoot());
    }

    private DockNode resolveDockNodeFromHierarchy(Node node) {
        Node current = node;
        while (current != null) {
            if (current instanceof DockNodeView dockNodeView) {
                return dockNodeView.getDockNode();
            }
            if (current instanceof TabPane tabPane) {
                DockNode selectedNode = resolveDockNodeFromSelectedTab(tabPane);
                if (selectedNode != null) {
                    return selectedNode;
                }
            }
            current = current.getParent();
        }
        return null;
    }

    private DockNode resolveDockNodeFromSelectedTab(TabPane tabPane) {
        if (tabPane == null) {
            return null;
        }
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == null) {
            return null;
        }
        Object tabNode = selectedTab.getProperties().get(DockLayoutEngine.TAB_DOCK_NODE_KEY);
        if (tabNode instanceof DockNode dockNode) {
            return dockNode;
        }
        if (selectedTab.getContent() != null) {
            return resolveDockNodeFromHierarchy(selectedTab.getContent());
        }
        return null;
    }

    private DockNode resolveSelectedDockNode(DockElement element) {
        if (element == null) {
            return null;
        }
        if (element instanceof DockNode dockNode) {
            return dockNode;
        }
        if (element instanceof DockTabPane tabPane) {
            if (tabPane.getChildren().isEmpty()) {
                return null;
            }
            int selectedIndex = Math.clamp(tabPane.getSelectedIndex(), 0, tabPane.getChildren().size() - 1);
            return resolveSelectedDockNode(tabPane.getChildren().get(selectedIndex));
        }
        if (element instanceof DockContainer container && !container.getChildren().isEmpty()) {
            return resolveSelectedDockNode(container.getChildren().getFirst());
        }
        return null;
    }

    private TabPane resolveActiveTabPane(Object eventTarget) {
        Node targetNode = resolveNodeFromEventTarget(eventTarget);
        TabPane tabPaneFromTarget = findTabPaneInHierarchy(targetNode);
        if (tabPaneFromTarget != null) {
            return tabPaneFromTarget;
        }

        Node focusedNode = resolveFocusedNode(eventTarget);
        TabPane tabPaneFromFocus = findTabPaneInHierarchy(focusedNode);
        if (tabPaneFromFocus != null) {
            return tabPaneFromFocus;
        }

        return findFirstTabPane(rootContainer);
    }

    private Node resolveNodeFromEventTarget(Object eventTarget) {
        if (eventTarget instanceof Node node) {
            return node;
        }
        if (eventTarget instanceof Scene scene) {
            return scene.getFocusOwner();
        }
        return null;
    }

    private Node resolveFocusedNode(Object eventTarget) {
        if (eventTarget instanceof Node node && node.getScene() != null) {
            return node.getScene().getFocusOwner();
        }
        if (eventTarget instanceof Scene scene) {
            return scene.getFocusOwner();
        }
        if (shortcutScene != null) {
            return shortcutScene.getFocusOwner();
        }
        return null;
    }

    private DockFloatingWindow resolveActiveFloatingWindow(Object eventTarget) {
        DockFloatingWindow fromEventScene = resolveFloatingWindowByScene(resolveSceneFromEventTarget(eventTarget));
        if (fromEventScene != null) {
            return fromEventScene;
        }

        DockFloatingWindow fromFocusedScene = resolveFloatingWindowByScene(resolveSceneFromNode(resolveFocusedNode(eventTarget)));
        if (fromFocusedScene != null) {
            return fromFocusedScene;
        }

        if (activeFloatingWindow != null && floatingWindows.contains(activeFloatingWindow)) {
            return activeFloatingWindow;
        }

        if (floatingWindows.isEmpty()) {
            return null;
        }
        return floatingWindows.getLast();
    }

    private DockFloatingWindow resolveFloatingWindowByScene(Scene scene) {
        if (scene == null || floatingWindows.isEmpty()) {
            return null;
        }
        for (DockFloatingWindow floatingWindow : floatingWindows) {
            if (floatingWindow != null && floatingWindow.ownsScene(scene)) {
                return floatingWindow;
            }
        }
        return null;
    }

    private Scene resolveSceneFromEventTarget(Object eventTarget) {
        if (eventTarget instanceof Scene scene) {
            return scene;
        }
        if (eventTarget instanceof Node node) {
            return node.getScene();
        }
        return null;
    }

    private Scene resolveSceneFromNode(Node node) {
        if (node == null) {
            return null;
        }
        return node.getScene();
    }

    private TabPane findTabPaneInHierarchy(Node node) {
        Node current = node;
        while (current != null) {
            if (current instanceof TabPane tabPane) {
                return tabPane;
            }
            current = current.getParent();
        }
        return null;
    }

    private TabPane findFirstTabPane(Node root) {
        if (root == null) {
            return null;
        }
        if (root instanceof TabPane tabPane) {
            return tabPane;
        }
        if (!(root instanceof Parent parent)) {
            return null;
        }
        for (Node child : parent.getChildrenUnmodifiable()) {
            TabPane childTabPane = findFirstTabPane(child);
            if (childTabPane != null) {
                return childTabPane;
            }
        }
        return null;
    }

    /**
     * Locks or unlocks the layout (disables drag & drop when locked).
     */
    public void setLocked(boolean locked) {
        dockGraph.setLocked(locked);
        for (DockFloatingWindow floatingWindow : floatingWindows) {
            floatingWindow.getDockGraph().setLocked(locked);
        }
    }

    @SuppressWarnings("unused")
    public boolean isLocked() {
        return dockGraph.isLocked();
    }

    /**
     * Sets the factory used to create DockNodes when loading layouts.
     * This is required for proper persistence across application sessions.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * snapFX.setNodeFactory(nodeId -> switch(nodeId) {
     *     case "projectExplorer" -> createProjectExplorer();
     *     case "mainEditor" -> createMainEditor();
     *     default -> null;
     * });
     * }</pre>
     *
     * @param factory Factory that creates nodes from their IDs
     */
    public void setNodeFactory(DockNodeFactory factory) {
        this.nodeFactory = factory;
        serializer.setNodeFactory(factory);
    }

    /**
     * Saves the current layout as JSON.
     */
    public String saveLayout() {
        String mainLayoutJson = serializer.serialize();
        if (floatingWindows.isEmpty()) {
            return mainLayoutJson;
        }

        JsonObject snapshot = new JsonObject();
        snapshot.add(SNAPSHOT_MAIN_LAYOUT_KEY, parseJsonObjectOrEmpty(mainLayoutJson));
        snapshot.add(SNAPSHOT_FLOATING_WINDOWS_KEY, serializeFloatingWindows());
        return SNAPSHOT_GSON.toJson(snapshot);
    }

    /**
     * Loads a layout from JSON.
     *
     * @throws DockLayoutLoadException if layout JSON is invalid or cannot be deserialized
     */
    public void loadLayout(String json) throws DockLayoutLoadException {
        LayoutSnapshot snapshot = tryParseLayoutSnapshot(json);
        if (snapshot != null) {
            validateSnapshotLayout(snapshot);
        } else {
            validateLayoutJson(json, "$");
        }

        clearFloatingDropPreviews();
        closeAllFloatingWindows(false);
        if (snapshot != null) {
            serializer.deserialize(SNAPSHOT_GSON.toJson(snapshot.mainLayout()));
            restoreFloatingWindows(snapshot.floatingWindows());
        } else {
            serializer.deserialize(json);
        }
        // Rebuild view
        layoutEngine.clearCache();
    }

    /**
     * Hides a DockNode (removes from layout but keeps in memory for restore).
     */
    public void hide(DockNode node) {
        if (node == null || hiddenNodes.contains(node)) {
            return;
        }
        DockFloatingWindow floatingWindow = findFloatingWindow(node);
        if (floatingWindow != null) {
            rememberLastKnownPlacement(node, floatingWindow);
            rememberFloatingBoundsForNodes(floatingWindow);
            floatingWindow.undockNode(node);
            node.setHiddenRestoreTarget(DockNode.HiddenRestoreTarget.FLOATING);
            if (floatingWindow.isEmpty()) {
                removeFloatingWindowSilently(floatingWindow);
            }
        } else if (isInGraph(node)) {
            rememberLastKnownPlacement(node);
            dockGraph.undock(node);
            node.setHiddenRestoreTarget(DockNode.HiddenRestoreTarget.DOCKED);
        } else {
            node.setHiddenRestoreTarget(DockNode.HiddenRestoreTarget.DOCKED);
        }

        hiddenNodes.add(node);
    }

    /**
     * Removes a DockNode from layout/floating windows without adding it to the hidden list.
     */
    public void remove(DockNode node) {
        if (node == null) {
            return;
        }

        hiddenNodes.remove(node);
        DockFloatingWindow floatingWindow = findFloatingWindow(node);
        if (floatingWindow != null) {
            rememberLastKnownPlacement(node, floatingWindow);
            rememberFloatingBoundsForNodes(floatingWindow);
            floatingWindow.undockNode(node);
            if (floatingWindow.isEmpty()) {
                removeFloatingWindowSilently(floatingWindow);
            }
            return;
        }

        if (isInGraph(node)) {
            dockGraph.undock(node);
        }
    }

    /**
     * Programmatically requests a close action for a DockNode.
     * The request is processed using the configured close behavior and callbacks.
     */
    public void close(DockNode node) {
        handleDockNodeCloseRequest(node, DockCloseSource.TITLE_BAR);
    }

    /**
     * Restores a hidden DockNode back to the layout.
     */
    public void restore(DockNode node) {
        if (node == null || !hiddenNodes.contains(node)) {
            return;
        }

        hiddenNodes.remove(node);
        if (node.getHiddenRestoreTarget() == DockNode.HiddenRestoreTarget.FLOATING) {
            floatNode(node);
            return;
        }
        dockAtRememberedOrFallback(node);
    }

    /**
     * Moves an existing DockNode into an external floating window.
     */
    public DockFloatingWindow floatNode(DockNode node) {
        return floatNode(node, null, null);
    }

    /**
     * Moves an existing DockNode into an external floating window at a screen position.
     * The screen coordinates can be on any monitor.
     *
     * <p>Before detaching a node from its current host (main layout or another floating window),
     * SnapFX captures placement anchors so {@link #attachFloatingWindow(DockFloatingWindow)} can
     * restore the node as close as possible to its previous location later.</p>
     */
    public DockFloatingWindow floatNode(DockNode node, Double screenX, Double screenY) {
        if (node == null) {
            return null;
        }

        DockFloatingWindow existingWindow = findFloatingWindow(node);
        if (existingWindow != null) {
            existingWindow.setPreferredPosition(screenX, screenY);
            existingWindow.show(primaryStage);
            bindFloatingShortcutScene(existingWindow);
            activeFloatingWindow = existingWindow;
            return existingWindow;
        }

        hiddenNodes.remove(node);

        DockFloatingWindow sourceFloatingWindow = null;
        if (isInGraph(node)) {
            rememberLastKnownPlacement(node);
            dockGraph.undock(node);
        } else {
            sourceFloatingWindow = findFloatingWindow(node);
            if (sourceFloatingWindow != null) {
                rememberLastKnownPlacement(node, sourceFloatingWindow);
                sourceFloatingWindow.undockNode(node);
                if (sourceFloatingWindow.isEmpty()) {
                    removeFloatingWindowSilently(sourceFloatingWindow);
                }
            }
        }

        DockFloatingWindow floatingWindow = new DockFloatingWindow(node, dragService);
        floatingWindow.getDockGraph().setLocked(dockGraph.isLocked());
        applyRememberedFloatingBounds(node, floatingWindow);
        if (screenX != null || screenY != null) {
            floatingWindow.setPreferredPosition(screenX, screenY);
        }
        configureFloatingWindowCallbacks(floatingWindow);
        Boolean initialAlwaysOnTop = node.getLastFloatingAlwaysOnTop();
        applyFloatingWindowInitialAlwaysOnTop(
            floatingWindow,
            initialAlwaysOnTop,
            initialAlwaysOnTop != null ? DockFloatingPinSource.API : DockFloatingPinSource.WINDOW_CREATE_DEFAULT
        );
        floatingWindows.add(floatingWindow);
        if (primaryStage != null) {
            floatingWindow.show(primaryStage);
            bindFloatingShortcutScene(floatingWindow);
        }
        activeFloatingWindow = floatingWindow;
        return floatingWindow;
    }

    /**
     * Attaches a floating window back into the main layout.
     *
     * <p>Attach uses a best-effort placement restore strategy per node:</p>
     * <ul>
     *   <li>Try remembered exact target/position/tab-index in the original host.</li>
     *   <li>Try remembered previous/next-neighbor anchors from the original host.</li>
     *   <li>If anchors are missing but the original floating host is still active, dock into that host.</li>
     *   <li>If the original floating host no longer exists, skip it and continue with main-layout fallback.</li>
     *   <li>If nothing is restorable, dock into the main layout without interruption.</li>
     * </ul>
     *
     * <p>No dialogs are shown for restore failures; attach is always resolved via fallback.</p>
     */
    public void attachFloatingWindow(DockFloatingWindow floatingWindow) {
        if (floatingWindow == null || !floatingWindows.remove(floatingWindow)) {
            return;
        }

        unbindFloatingShortcutScene(floatingWindow);
        if (activeFloatingWindow == floatingWindow) {
            activeFloatingWindow = null;
        }
        floatingWindow.closeWithoutNotification();
        rememberFloatingBoundsForNodes(floatingWindow);

        List<DockNode> nodesToAttach = new ArrayList<>(floatingWindow.getDockNodes());
        for (DockNode node : nodesToAttach) {
            floatingWindow.undockNode(node);
        }

        List<DockNode> pendingNodes = new ArrayList<>(nodesToAttach);
        for (int pass = 0; pass < nodesToAttach.size() && !pendingNodes.isEmpty(); pass++) {
            int before = pendingNodes.size();
            pendingNodes.removeIf(this::tryDockAtRememberedPlacement);
            if (pendingNodes.size() == before) {
                break;
            }
        }
        for (DockNode node : pendingNodes) {
            dockAtHostFallbackOrMain(node);
        }
    }

    /**
     * Returns all currently open floating windows.
     */
    public ObservableList<DockFloatingWindow> getFloatingWindows() {
        return readOnlyFloatingWindows;
    }

    /**
     * Closes all currently open floating windows.
     *
     * @param attachBack whether floating nodes should be attached back to the main layout
     */
    public void closeFloatingWindows(boolean attachBack) {
        closeAllFloatingWindows(attachBack);
    }

    private void handleResolvedDropRequest(DockDragService.DropRequest request) {
        if (request == null
            || request.draggedNode() == null
            || request.target() == null
            || request.position() == null) {
            return;
        }

        DockNode draggedNode = request.draggedNode();
        DockFloatingWindow sourceWindow = findFloatingWindow(draggedNode);

        if (sourceWindow == null) {
            dockGraph.move(draggedNode, request.target(), request.position(), request.tabIndex());
            return;
        }

        rememberLastKnownPlacement(draggedNode, sourceWindow);
        sourceWindow.undockNode(draggedNode);
        if (sourceWindow.isEmpty()) {
            rememberFloatingBoundsForNodes(sourceWindow);
            removeFloatingWindowSilently(sourceWindow);
        }

        dockGraph.dock(draggedNode, request.target(), request.position(), request.tabIndex());
    }

    private void handleUnresolvedDropRequest(DockDragService.FloatDetachRequest request) {
        if (request == null || request.draggedNode() == null) {
            return;
        }
        if (tryDropIntoFloatingWindow(request.draggedNode(), request.screenX(), request.screenY())) {
            return;
        }
        DockFloatingWindow sourceWindow = findFloatingWindow(request.draggedNode());
        if (sourceWindow != null && sourceWindow.getDockNodes().size() > 1) {
            floatNodeFromFloatingLayout(request.draggedNode(), request.screenX(), request.screenY());
            return;
        }
        floatNode(request.draggedNode(), request.screenX(), request.screenY());
    }

    private void handleDragHover(DockDragService.DragHoverEvent hoverEvent) {
        if (hoverEvent == null || hoverEvent.draggedNode() == null || floatingWindows.isEmpty()) {
            clearFloatingDropPreviews();
            return;
        }

        DockFloatingWindow topWindow = findTopFloatingWindowAt(hoverEvent.screenX(), hoverEvent.screenY());
        if (topWindow == null) {
            clearFloatingDropPreviews();
            return;
        }

        DockDropVisualizationMode visualizationMode = dragService.getDropVisualizationMode();
        for (DockFloatingWindow floatingWindow : new ArrayList<>(floatingWindows)) {
            if (floatingWindow == topWindow) {
                floatingWindow.updateDropPreview(
                    hoverEvent.draggedNode(),
                    hoverEvent.screenX(),
                    hoverEvent.screenY(),
                    visualizationMode
                );
                continue;
            }
            floatingWindow.clearDropPreview();
        }
    }

    private void clearFloatingDropPreviews() {
        if (floatingWindows.isEmpty()) {
            return;
        }
        for (DockFloatingWindow floatingWindow : new ArrayList<>(floatingWindows)) {
            floatingWindow.clearDropPreview();
        }
    }

    private boolean tryDropIntoFloatingWindow(DockNode node, double screenX, double screenY) {
        DockFloatingWindow topWindow = findTopFloatingWindowAt(screenX, screenY);
        if (topWindow == null) {
            return false;
        }
        DockFloatingWindow.DropTarget bestTarget = topWindow.resolveDropTarget(screenX, screenY, node);
        if (bestTarget == null) {
            return false;
        }

        DockFloatingWindow sourceWindow = findFloatingWindow(node);
        if (sourceWindow != null && sourceWindow == topWindow) {
            topWindow.moveNode(node, bestTarget.target(), bestTarget.position(), bestTarget.tabIndex());
            topWindow.toFront();
            return true;
        }

        if (isInGraph(node)) {
            rememberLastKnownPlacement(node);
            dockGraph.undock(node);
        } else if (sourceWindow != null) {
            rememberLastKnownPlacement(node, sourceWindow);
            rememberFloatingBoundsForNodes(sourceWindow);
            sourceWindow.undockNode(node);
            if (sourceWindow.isEmpty()) {
                removeFloatingWindowSilently(sourceWindow);
            }
        }

        hiddenNodes.remove(node);
        topWindow.dockNode(node, bestTarget.target(), bestTarget.position(), bestTarget.tabIndex());
        topWindow.toFront();
        return true;
    }

    private void configureFloatingWindowCallbacks(DockFloatingWindow floatingWindow) {
        if (floatingWindow == null) {
            return;
        }
        floatingWindow.setOnAttachRequested(() -> attachFloatingWindow(floatingWindow));
        floatingWindow.setOnCloseRequested(() -> handleFloatingWindowCloseRequested(floatingWindow));
        floatingWindow.setOnWindowClosed(window -> {
            unbindFloatingShortcutScene(window);
            if (activeFloatingWindow == window) {
                activeFloatingWindow = null;
            }
            handleFloatingWindowClosed(window);
        });
        floatingWindow.setOnWindowActivated(() -> {
            bindFloatingShortcutScene(floatingWindow);
            activeFloatingWindow = floatingWindow;
            promoteFloatingWindowToFront(floatingWindow);
        });
        floatingWindow.setOnNodeCloseRequest(this::handleDockNodeCloseRequest);
        floatingWindow.setOnNodeFloatRequest(this::floatNodeFromFloatingLayout);
        floatingWindow.setOnAlwaysOnTopChanged((alwaysOnTop, source) ->
            handleFloatingPinChanged(floatingWindow, alwaysOnTop, source)
        );
        applyFloatingPinSettings(floatingWindow);
        applyFloatingSnapSettings(floatingWindow);
    }

    private void applyFloatingPinSettings(DockFloatingWindow floatingWindow) {
        if (floatingWindow == null) {
            return;
        }
        floatingWindow.setPinButtonMode(floatingPinButtonMode);
        floatingWindow.setPinToggleEnabled(allowFloatingPinToggle);
        floatingWindow.setPinLockedBehavior(floatingPinLockedBehavior);
    }

    private void applyFloatingSnapSettings(DockFloatingWindow floatingWindow) {
        if (floatingWindow == null) {
            return;
        }
        floatingWindow.setSnappingEnabled(floatingWindowSnappingEnabled);
        floatingWindow.setSnapDistance(floatingWindowSnapDistance);
        floatingWindow.setSnapTargets(floatingWindowSnapTargets);
        floatingWindow.setSnapPeerWindowsSupplier(() -> new ArrayList<>(floatingWindows));
    }

    private void applyFloatingWindowInitialAlwaysOnTop(
        DockFloatingWindow floatingWindow,
        Boolean initialValue,
        DockFloatingPinSource source
    ) {
        if (floatingWindow == null) {
            return;
        }
        boolean resolved = initialValue != null ? initialValue : defaultFloatingAlwaysOnTop;
        floatingWindow.setAlwaysOnTop(resolved, source);
    }

    private void handleFloatingPinChanged(
        DockFloatingWindow floatingWindow,
        boolean alwaysOnTop,
        DockFloatingPinSource source
    ) {
        if (floatingWindow == null) {
            return;
        }
        rememberFloatingAlwaysOnTopForNodes(floatingWindow);
        if (onFloatingPinChanged == null) {
            return;
        }
        DockFloatingPinSource effectiveSource = source == null ? DockFloatingPinSource.API : source;
        onFloatingPinChanged.accept(new DockFloatingPinChangeEvent(floatingWindow, alwaysOnTop, effectiveSource));
    }

    private DockFloatingWindow floatNodeFromFloatingLayout(DockNode node) {
        return floatNodeFromFloatingLayout(node, null, null);
    }

    private DockFloatingWindow floatNodeFromFloatingLayout(DockNode node, Double screenX, Double screenY) {
        if (node == null) {
            return null;
        }

        DockFloatingWindow sourceWindow = findFloatingWindow(node);
        if (sourceWindow == null) {
            return floatNode(node, screenX, screenY);
        }
        if (sourceWindow.getDockNodes().size() <= 1) {
            sourceWindow.toFront();
            return sourceWindow;
        }

        rememberLastKnownPlacement(node, sourceWindow);
        rememberFloatingBoundsForNodes(sourceWindow);
        sourceWindow.undockNode(node);
        if (sourceWindow.isEmpty()) {
            removeFloatingWindowSilently(sourceWindow);
        }

        DockFloatingWindow floatingWindow = new DockFloatingWindow(node, dragService);
        floatingWindow.getDockGraph().setLocked(dockGraph.isLocked());
        applyRememberedFloatingBounds(node, floatingWindow);
        if (screenX != null || screenY != null) {
            floatingWindow.setPreferredPosition(screenX, screenY);
        } else if (sourceWindow.getPreferredX() != null || sourceWindow.getPreferredY() != null) {
            floatingWindow.setPreferredPosition(
                sourceWindow.getPreferredX() != null ? sourceWindow.getPreferredX() + 24.0 : null,
                sourceWindow.getPreferredY() != null ? sourceWindow.getPreferredY() + 24.0 : null
            );
        }
        configureFloatingWindowCallbacks(floatingWindow);
        applyFloatingWindowInitialAlwaysOnTop(
            floatingWindow,
            sourceWindow.isAlwaysOnTop(),
            DockFloatingPinSource.API
        );
        floatingWindows.add(floatingWindow);
        if (primaryStage != null) {
            floatingWindow.show(primaryStage);
            bindFloatingShortcutScene(floatingWindow);
        }
        activeFloatingWindow = floatingWindow;
        return floatingWindow;
    }

    private void handleDockNodeCloseRequest(DockNode node, DockCloseSource source) {
        if (node == null) {
            return;
        }

        DockCloseSource closeSource = source == null ? DockCloseSource.TITLE_BAR : source;
        DockCloseRequest request = new DockCloseRequest(
            closeSource,
            List.of(node),
            findFloatingWindow(node),
            defaultCloseBehavior
        );
        processCloseRequest(request);
    }

    private boolean handleFloatingWindowCloseRequested(DockFloatingWindow floatingWindow) {
        if (floatingWindow == null) {
            return false;
        }

        List<DockNode> nodes = new ArrayList<>(floatingWindow.getDockNodes());
        if (nodes.isEmpty()) {
            return true;
        }

        DockCloseRequest request = new DockCloseRequest(
            DockCloseSource.FLOATING_WINDOW,
            nodes,
            floatingWindow,
            defaultCloseBehavior
        );
        processCloseRequest(request);
        return false;
    }

    private void processCloseRequest(DockCloseRequest request) {
        if (request == null || request.nodes().isEmpty()) {
            return;
        }

        if (legacyNodeCloseRequest != null && request.nodes().size() == 1 && request.source() != DockCloseSource.FLOATING_WINDOW) {
            legacyNodeCloseRequest.accept(request.nodes().getFirst());
            return;
        }

        DockCloseDecision decision = DockCloseDecision.DEFAULT;
        if (onCloseRequest != null) {
            DockCloseDecision resolved = onCloseRequest.apply(request);
            if (resolved != null) {
                decision = resolved;
            }
        }

        if (decision == DockCloseDecision.CANCEL) {
            fireCloseHandled(request, null, true);
            return;
        }

        DockCloseBehavior behavior = switch (decision) {
            case HIDE -> DockCloseBehavior.HIDE;
            case REMOVE -> DockCloseBehavior.REMOVE;
            case DEFAULT -> request.defaultBehavior();
            case CANCEL -> null;
        };

        if (behavior == null) {
            fireCloseHandled(request, null, true);
            return;
        }

        if (behavior == DockCloseBehavior.HIDE) {
            for (DockNode node : request.nodes()) {
                hide(node);
            }
        } else {
            for (DockNode node : request.nodes()) {
                remove(node);
            }
        }
        fireCloseHandled(request, behavior, false);
    }

    private void fireCloseHandled(DockCloseRequest request, DockCloseBehavior appliedBehavior, boolean canceled) {
        if (onCloseHandled == null || request == null) {
            return;
        }
        onCloseHandled.accept(new DockCloseResult(request, appliedBehavior, canceled));
    }

    private boolean isMainDropSuppressedByFloatingWindow(Double screenX, Double screenY) {
        if (screenX == null || screenY == null) {
            return false;
        }
        return findTopFloatingWindowAt(screenX, screenY) != null;
    }

    private DockFloatingWindow findTopFloatingWindowAt(double screenX, double screenY) {
        for (int i = floatingWindows.size() - 1; i >= 0; i--) {
            DockFloatingWindow floatingWindow = floatingWindows.get(i);
            if (floatingWindow != null && floatingWindow.containsScreenPoint(screenX, screenY)) {
                return floatingWindow;
            }
        }
        return null;
    }

    private void promoteFloatingWindowToFront(DockFloatingWindow floatingWindow) {
        if (floatingWindow == null) {
            return;
        }
        int index = floatingWindows.indexOf(floatingWindow);
        if (index < 0 || index == floatingWindows.size() - 1) {
            return;
        }
        floatingWindows.remove(index);
        floatingWindows.add(floatingWindow);
    }

    private void applyRememberedFloatingBounds(DockNode node, DockFloatingWindow floatingWindow) {
        if (node == null || floatingWindow == null) {
            return;
        }
        if (node.getLastFloatingWidth() != null && node.getLastFloatingHeight() != null) {
            floatingWindow.setPreferredSize(node.getLastFloatingWidth(), node.getLastFloatingHeight());
        }
        if (node.getLastFloatingX() != null || node.getLastFloatingY() != null) {
            floatingWindow.setPreferredPosition(node.getLastFloatingX(), node.getLastFloatingY());
        }
        if (node.getLastFloatingAlwaysOnTop() != null) {
            floatingWindow.setAlwaysOnTop(node.getLastFloatingAlwaysOnTop(), DockFloatingPinSource.API);
        }
    }

    private void rememberFloatingBoundsForNodes(DockFloatingWindow floatingWindow) {
        if (floatingWindow == null) {
            return;
        }
        floatingWindow.captureCurrentBounds();
        rememberFloatingAlwaysOnTopForNodes(floatingWindow);
        for (DockNode node : floatingWindow.getDockNodes()) {
            node.setLastFloatingX(floatingWindow.getPreferredX());
            node.setLastFloatingY(floatingWindow.getPreferredY());
            node.setLastFloatingWidth(floatingWindow.getPreferredWidth());
            node.setLastFloatingHeight(floatingWindow.getPreferredHeight());
        }
    }

    private void rememberFloatingAlwaysOnTopForNodes(DockFloatingWindow floatingWindow) {
        if (floatingWindow == null) {
            return;
        }
        for (DockNode node : floatingWindow.getDockNodes()) {
            node.setLastFloatingAlwaysOnTop(floatingWindow.isAlwaysOnTop());
        }
    }

    private boolean isInGraph(DockNode node) {
        return node != null && findInGraph(dockGraph.getRoot(), node);
    }

    private boolean isInGraph(DockElement element) {
        if (element == dockGraph.getRoot()) {
            return true;
        }
        return findInGraph(dockGraph.getRoot(), element);
    }

    private boolean findInGraph(DockElement current, DockElement target) {
        if (current == null) {
            return false;
        }
        if (current == target) {
            return true;
        }
        if (current instanceof DockContainer container) {
            for (DockElement child : container.getChildren()) {
                if (findInGraph(child, target)) {
                    return true;
                }
            }
        }
        return false;
    }

    public ObservableList<DockNode> getHiddenNodes() {
        return hiddenNodes;
    }

    // Getters
    public DockGraph getDockGraph() {
        return dockGraph;
    }

    @SuppressWarnings("unused")
    public DockLayoutEngine getLayoutEngine() {
        return layoutEngine;
    }

    public DockDragService getDragService() {
        return dragService;
    }

    public void setDropVisualizationMode(DockDropVisualizationMode mode) {
        dragService.setDropVisualizationMode(mode);
    }

    public DockDropVisualizationMode getDropVisualizationMode() {
        return dragService.getDropVisualizationMode();
    }

    public void setCloseButtonMode(DockCloseButtonMode mode) {
        layoutEngine.setCloseButtonMode(mode);
        requestRebuild();
    }

    public DockCloseButtonMode getCloseButtonMode() {
        return layoutEngine.getCloseButtonMode();
    }

    public void setTitleBarMode(DockTitleBarMode mode) {
        layoutEngine.setTitleBarMode(mode);
        requestRebuild();
    }

    public DockTitleBarMode getTitleBarMode() {
        return layoutEngine.getTitleBarMode();
    }

    @SuppressWarnings("unused")
    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void setDefaultCloseBehavior(DockCloseBehavior defaultCloseBehavior) {
        if (defaultCloseBehavior != null) {
            this.defaultCloseBehavior = defaultCloseBehavior;
        }
    }

    public DockCloseBehavior getDefaultCloseBehavior() {
        return defaultCloseBehavior;
    }

    public void setOnCloseRequest(Function<DockCloseRequest, DockCloseDecision> handler) {
        this.onCloseRequest = handler;
    }

    public void setOnCloseHandled(Consumer<DockCloseResult> handler) {
        this.onCloseHandled = handler;
    }

    /**
     * Controls pin-button visibility behavior for all floating windows.
     */
    public void setFloatingPinButtonMode(DockFloatingPinButtonMode mode) {
        floatingPinButtonMode = mode == null ? DockFloatingPinButtonMode.AUTO : mode;
        for (DockFloatingWindow floatingWindow : floatingWindows) {
            floatingWindow.setPinButtonMode(floatingPinButtonMode);
        }
    }

    /**
     * Returns the current global pin-button mode.
     */
    public DockFloatingPinButtonMode getFloatingPinButtonMode() {
        return floatingPinButtonMode;
    }

    /**
     * Sets the default always-on-top state for newly created floating windows.
     */
    public void setDefaultFloatingAlwaysOnTop(boolean defaultAlwaysOnTop) {
        this.defaultFloatingAlwaysOnTop = defaultAlwaysOnTop;
    }

    /**
     * Returns the default always-on-top state for newly created floating windows.
     */
    public boolean isDefaultFloatingAlwaysOnTop() {
        return defaultFloatingAlwaysOnTop;
    }

    /**
     * Enables or disables user pin toggling in floating title bars.
     */
    public void setAllowFloatingPinToggle(boolean allowPinToggle) {
        allowFloatingPinToggle = allowPinToggle;
        for (DockFloatingWindow floatingWindow : floatingWindows) {
            floatingWindow.setPinToggleEnabled(allowFloatingPinToggle);
        }
    }

    /**
     * Returns whether user pin toggling is enabled in floating title bars.
     */
    public boolean isAllowFloatingPinToggle() {
        return allowFloatingPinToggle;
    }

    /**
     * Sets lock-mode behavior for pin controls in floating windows.
     */
    public void setFloatingPinLockedBehavior(DockFloatingPinLockedBehavior behavior) {
        floatingPinLockedBehavior = behavior == null ? DockFloatingPinLockedBehavior.ALLOW : behavior;
        for (DockFloatingWindow floatingWindow : floatingWindows) {
            floatingWindow.setPinLockedBehavior(floatingPinLockedBehavior);
        }
    }

    /**
     * Returns lock-mode behavior for floating pin controls.
     */
    public DockFloatingPinLockedBehavior getFloatingPinLockedBehavior() {
        return floatingPinLockedBehavior;
    }

    /**
     * Enables or disables floating-window snapping while title bars are dragged.
     */
    public void setFloatingWindowSnappingEnabled(boolean enabled) {
        floatingWindowSnappingEnabled = enabled;
        for (DockFloatingWindow floatingWindow : floatingWindows) {
            floatingWindow.setSnappingEnabled(floatingWindowSnappingEnabled);
        }
    }

    /**
     * Returns whether floating-window snapping is enabled.
     */
    public boolean isFloatingWindowSnappingEnabled() {
        return floatingWindowSnappingEnabled;
    }

    /**
     * Sets the snap distance in pixels used for floating-window drag snapping.
     */
    public void setFloatingWindowSnapDistance(double pixels) {
        if (!isFiniteNumber(pixels) || pixels < 0.0) {
            return;
        }
        floatingWindowSnapDistance = pixels;
        for (DockFloatingWindow floatingWindow : floatingWindows) {
            floatingWindow.setSnapDistance(floatingWindowSnapDistance);
        }
    }

    /**
     * Returns the configured floating-window snap distance in pixels.
     */
    public double getFloatingWindowSnapDistance() {
        return floatingWindowSnapDistance;
    }

    /**
     * Configures which targets are used for floating-window snapping.
     */
    public void setFloatingWindowSnapTargets(Set<DockFloatingSnapTarget> targets) {
        EnumSet<DockFloatingSnapTarget> resolvedTargets = EnumSet.noneOf(DockFloatingSnapTarget.class);
        if (targets != null) {
            for (DockFloatingSnapTarget target : targets) {
                if (target != null) {
                    resolvedTargets.add(target);
                }
            }
        }
        floatingWindowSnapTargets = resolvedTargets;
        for (DockFloatingWindow floatingWindow : floatingWindows) {
            floatingWindow.setSnapTargets(floatingWindowSnapTargets);
        }
    }

    /**
     * Returns the configured floating-window snap targets.
     */
    public Set<DockFloatingSnapTarget> getFloatingWindowSnapTargets() {
        if (floatingWindowSnapTargets.isEmpty()) {
            return Set.of();
        }
        return Collections.unmodifiableSet(EnumSet.copyOf(floatingWindowSnapTargets));
    }

    /**
     * Sets callback for floating pin-state changes.
     */
    public void setOnFloatingPinChanged(Consumer<DockFloatingPinChangeEvent> handler) {
        onFloatingPinChanged = handler;
    }

    /**
     * Sets always-on-top for an open floating window.
     */
    public void setFloatingWindowAlwaysOnTop(DockFloatingWindow floatingWindow, boolean alwaysOnTop) {
        if (floatingWindow == null || !floatingWindows.contains(floatingWindow)) {
            return;
        }
        floatingWindow.setAlwaysOnTop(alwaysOnTop, DockFloatingPinSource.API);
    }

    /**
     * Legacy close hook retained for compatibility.
     * Prefer {@link #setOnCloseRequest(Function)} for source-aware close handling.
     */
    @Deprecated(forRemoval = false)
    public void setOnNodeCloseRequest(Consumer<DockNode> handler) {
        legacyNodeCloseRequest = handler;
    }

    public int getDockNodeCount(String id) {
        return dockGraph.getDockNodeCount(id);
    }

    /**
     * Sets the pane ratios of the root split pane.
     * <p>
     * Ratios are normalized automatically, so both {@code (0.25, 0.5, 0.25)}
     * and {@code (25, 50, 25)} are valid inputs.
     * </p>
     *
     * @param paneRatios Ratios for each pane of the root split pane
     * @return {@code true} if ratios were applied; {@code false} if the root is not a split pane
     * or if input validation failed
     */
    public boolean setRootSplitRatios(double... paneRatios) {
        DockElement root = dockGraph.getRoot();
        if (!(root instanceof DockSplitPane rootSplit)) {
            return false;
        }
        return setSplitRatios(rootSplit, paneRatios);
    }

    /**
     * Sets the pane ratios of a specific split pane.
     * <p>
     * The number of ratios must match the number of split children.
     * </p>
     *
     * @param splitPane Split pane to configure
     * @param paneRatios Ratios for each pane in the split pane
     * @return {@code true} if ratios were applied; {@code false} if validation failed
     */
    public boolean setSplitRatios(DockSplitPane splitPane, double... paneRatios) {
        if (splitPane == null || paneRatios == null) {
            return false;
        }

        int paneCount = splitPane.getChildren().size();
        if (paneCount < 2 || paneRatios.length != paneCount) {
            return false;
        }

        double sum = 0.0;
        for (double ratio : paneRatios) {
            if (!Double.isFinite(ratio) || ratio <= 0.0) {
                return false;
            }
            sum += ratio;
        }

        if (sum <= 0.0) {
            return false;
        }

        double cumulative = 0.0;
        for (int i = 0; i < paneRatios.length - 1; i++) {
            cumulative += paneRatios[i] / sum;
            splitPane.setDividerPosition(i, cumulative);
        }

        requestRebuild();
        return true;
    }

    private DockFloatingWindow findFloatingWindow(DockNode node) {
        if (node == null) {
            return null;
        }
        for (DockFloatingWindow floatingWindow : floatingWindows) {
            if (floatingWindow.containsNode(node)) {
                return floatingWindow;
            }
        }
        return null;
    }

    private void closeAllFloatingWindows(boolean attachBack) {
        clearFloatingDropPreviews();
        if (floatingWindows.isEmpty()) {
            return;
        }
        for (DockFloatingWindow floatingWindow : new ArrayList<>(floatingWindows)) {
            if (attachBack) {
                attachFloatingWindow(floatingWindow);
            } else {
                removeFloatingWindowSilently(floatingWindow);
            }
        }
    }

    private void handleFloatingWindowClosed(DockFloatingWindow floatingWindow) {
        if (floatingWindow == null || !floatingWindows.remove(floatingWindow)) {
            return;
        }
        if (activeFloatingWindow == floatingWindow) {
            activeFloatingWindow = null;
        }
        rememberFloatingBoundsForNodes(floatingWindow);
        List<DockNode> nodes = new ArrayList<>(floatingWindow.getDockNodes());
        for (DockNode node : nodes) {
            floatingWindow.undockNode(node);
            node.setHiddenRestoreTarget(DockNode.HiddenRestoreTarget.FLOATING);
            if (!hiddenNodes.contains(node)) {
                hiddenNodes.add(node);
            }
        }
    }

    private void removeFloatingWindowSilently(DockFloatingWindow floatingWindow) {
        if (floatingWindow == null || !floatingWindows.remove(floatingWindow)) {
            return;
        }
        unbindFloatingShortcutScene(floatingWindow);
        if (activeFloatingWindow == floatingWindow) {
            activeFloatingWindow = null;
        }
        floatingWindow.closeWithoutNotification();
    }

    /**
     * Captures placement anchors for a node in the main layout.
     */
    private void rememberLastKnownPlacement(DockNode node) {
        rememberLastKnownPlacement(node, null);
    }

    /**
     * Captures host-aware placement anchors for a node before it is undocked.
     * The host is {@code null} for the main layout or the source floating window otherwise.
     */
    private void rememberLastKnownPlacement(DockNode node, DockFloatingWindow hostWindow) {
        if (node == null || node.getParent() == null) {
            return;
        }

        DockContainer parent = node.getParent();
        int index = parent.getChildren().indexOf(node);
        DockElement previousNeighbor = index > 0 ? parent.getChildren().get(index - 1) : null;
        DockElement nextNeighbor = index >= 0 && index < parent.getChildren().size() - 1
            ? parent.getChildren().get(index + 1)
            : null;

        DockElement preferredTarget;
        DockPosition preferredPosition;
        Integer preferredTabIndex = null;
        DockPosition previousNeighborPosition = null;
        Integer previousNeighborTabIndex = null;
        DockPosition nextNeighborPosition = null;
        Integer nextNeighborTabIndex = null;

        if (parent instanceof DockTabPane tabPane) {
            int resolvedTabIndex = index >= 0 ? index : tabPane.getChildren().size();
            preferredTarget = tabPane;
            preferredPosition = DockPosition.CENTER;
            preferredTabIndex = resolvedTabIndex;
            if (previousNeighbor != null) {
                previousNeighborPosition = DockPosition.CENTER;
                previousNeighborTabIndex = resolvedTabIndex;
            }
            if (nextNeighbor != null) {
                nextNeighborPosition = DockPosition.CENTER;
                nextNeighborTabIndex = resolvedTabIndex;
            }
        } else if (parent instanceof DockSplitPane splitPane) {
            Orientation orientation = splitPane.getOrientation();
            if (previousNeighbor != null) {
                previousNeighborPosition = resolveSplitPositionAfterPrevious(orientation);
            }
            if (nextNeighbor != null) {
                nextNeighborPosition = resolveSplitPositionBeforeNext(orientation);
            }

            if (previousNeighbor != null && previousNeighborPosition != null) {
                preferredTarget = previousNeighbor;
                preferredPosition = previousNeighborPosition;
            } else if (nextNeighbor != null && nextNeighborPosition != null) {
                preferredTarget = nextNeighbor;
                preferredPosition = nextNeighborPosition;
            } else {
                preferredTarget = parent;
                preferredPosition = DockPosition.CENTER;
                preferredTabIndex = 0;
            }
        } else {
            if (previousNeighbor != null) {
                preferredTarget = previousNeighbor;
                preferredPosition = DockPosition.RIGHT;
            } else if (nextNeighbor != null) {
                preferredTarget = nextNeighbor;
                preferredPosition = DockPosition.LEFT;
            } else {
                preferredTarget = parent;
                preferredPosition = DockPosition.CENTER;
                preferredTabIndex = 0;
            }
        }

        Integer normalizedPreferredTabIndex = preferredPosition == DockPosition.CENTER ? preferredTabIndex : null;
        node.setLastKnownTarget(preferredTarget);
        node.setLastKnownPosition(preferredPosition);
        node.setLastKnownTabIndex(normalizedPreferredTabIndex);
        dockPlacementMemory.put(
            node,
            new DockPlacementMemory(
                hostWindow,
                preferredTarget,
                preferredPosition,
                normalizedPreferredTabIndex,
                previousNeighbor,
                previousNeighborPosition,
                previousNeighborTabIndex,
                nextNeighbor,
                nextNeighborPosition,
                nextNeighborTabIndex
            )
        );
    }

    private DockPosition resolveSplitPositionAfterPrevious(Orientation orientation) {
        return orientation == Orientation.HORIZONTAL ? DockPosition.RIGHT : DockPosition.BOTTOM;
    }

    private DockPosition resolveSplitPositionBeforeNext(Orientation orientation) {
        return orientation == Orientation.HORIZONTAL ? DockPosition.LEFT : DockPosition.TOP;
    }

    private DockFloatingWindow resolveActivePlacementHost(DockFloatingWindow hostWindow) {
        if (hostWindow != null && floatingWindows.contains(hostWindow)) {
            return hostWindow;
        }
        return null;
    }

    /**
     * Attempts to dock a node using remembered placement anchors only.
     * Returns {@code false} when anchors are not restorable in currently active hosts.
     */
    private boolean tryDockAtRememberedPlacement(DockNode node) {
        if (node == null) {
            return false;
        }

        DockPlacementMemory placementMemory = dockPlacementMemory.get(node);
        if (placementMemory != null) {
            DockFloatingWindow preferredHost = resolveActivePlacementHost(placementMemory.hostWindow());
            if (tryDockUsingPlacementMemory(node, placementMemory, preferredHost)) {
                return true;
            }
            if (preferredHost != null && tryDockUsingPlacementMemory(node, placementMemory, null)) {
                return true;
            }
        }

        DockElement target = node.getLastKnownTarget();
        DockPosition position = node.getLastKnownPosition();
        Integer tabIndex = node.getLastKnownTabIndex();
        if (target != null && position != null && isInGraph(target)) {
            dockGraph.dock(node, target, position, position == DockPosition.CENTER ? tabIndex : null);
            return true;
        }

        return false;
    }

    private boolean tryDockUsingPlacementMemory(
        DockNode node,
        DockPlacementMemory placementMemory,
        DockFloatingWindow hostWindow
    ) {
        if (placementMemory == null) {
            return false;
        }

        if (tryDockInHost(
            node,
            hostWindow,
            placementMemory.preferredTarget(),
            placementMemory.preferredPosition(),
            placementMemory.preferredTabIndex()
        )) {
            return true;
        }
        if (tryDockInHost(
            node,
            hostWindow,
            placementMemory.previousNeighbor(),
            placementMemory.positionRelativeToPreviousNeighbor(),
            placementMemory.tabIndexRelativeToPreviousNeighbor()
        )) {
            return true;
        }
        return tryDockInHost(
            node,
            hostWindow,
            placementMemory.nextNeighbor(),
            placementMemory.positionRelativeToNextNeighbor(),
            placementMemory.tabIndexRelativeToNextNeighbor()
        );
    }

    private boolean tryDockInHost(
        DockNode node,
        DockFloatingWindow hostWindow,
        DockElement target,
        DockPosition position,
        Integer tabIndex
    ) {
        if (node == null || target == null || position == null || target == node || !isElementInHost(target, hostWindow)) {
            return false;
        }

        Integer resolvedTabIndex = position == DockPosition.CENTER ? tabIndex : null;
        if (hostWindow == null) {
            dockGraph.dock(node, target, position, resolvedTabIndex);
        } else {
            hostWindow.dockNode(node, target, position, resolvedTabIndex);
        }
        return true;
    }

    private boolean isElementInHost(DockElement element, DockFloatingWindow hostWindow) {
        if (element == null) {
            return false;
        }
        DockElement hostRoot = getHostRoot(hostWindow);
        if (hostRoot == null) {
            return false;
        }
        return hostRoot == element || findInGraph(hostRoot, element);
    }

    private DockElement getHostRoot(DockFloatingWindow hostWindow) {
        if (hostWindow == null) {
            return dockGraph.getRoot();
        }
        if (!floatingWindows.contains(hostWindow)) {
            return null;
        }
        return hostWindow.getDockGraph().getRoot();
    }

    private void dockAtRememberedOrFallback(DockNode node) {
        if (tryDockAtRememberedPlacement(node)) {
            return;
        }
        dockAtHostFallbackOrMain(node);
    }

    /**
     * Fallback used after anchor restore attempts failed.
     * If the remembered host floating window is still active, dock into that host root;
     * otherwise dock into the main layout.
     */
    private void dockAtHostFallbackOrMain(DockNode node) {
        DockPlacementMemory placementMemory = dockPlacementMemory.get(node);
        DockFloatingWindow preferredHost = placementMemory == null
            ? null
            : resolveActivePlacementHost(placementMemory.hostWindow());
        if (preferredHost != null) {
            DockElement hostRoot = preferredHost.getDockGraph().getRoot();
            if (hostRoot == null) {
                preferredHost.getDockGraph().setRoot(node);
                return;
            }
            preferredHost.dockNode(node, hostRoot, DockPosition.RIGHT, null);
            return;
        }
        dockAtMainFallback(node);
    }

    private void dockAtMainFallback(DockNode node) {
        if (dockGraph.getRoot() == null) {
            dockGraph.setRoot(node);
        } else {
            dockGraph.dock(node, dockGraph.getRoot(), DockPosition.RIGHT);
        }
    }

    private JsonArray serializeFloatingWindows() {
        JsonArray floatingArray = new JsonArray();
        for (DockFloatingWindow floatingWindow : floatingWindows) {
            rememberFloatingBoundsForNodes(floatingWindow);
            DockLayoutSerializer floatingSerializer = createLayoutSerializer(floatingWindow.getDockGraph());
            JsonObject floatingData = new JsonObject();
            floatingData.add(SNAPSHOT_FLOATING_LAYOUT_KEY, parseJsonObjectOrEmpty(floatingSerializer.serialize()));
            addOptionalNumber(floatingData, SNAPSHOT_FLOATING_X_KEY, floatingWindow.getPreferredX());
            addOptionalNumber(floatingData, SNAPSHOT_FLOATING_Y_KEY, floatingWindow.getPreferredY());
            floatingData.addProperty(SNAPSHOT_FLOATING_WIDTH_KEY, floatingWindow.getPreferredWidth());
            floatingData.addProperty(SNAPSHOT_FLOATING_HEIGHT_KEY, floatingWindow.getPreferredHeight());
            floatingData.addProperty(SNAPSHOT_FLOATING_ALWAYS_ON_TOP_KEY, floatingWindow.isAlwaysOnTop());
            floatingArray.add(floatingData);
        }
        return floatingArray;
    }

    private void restoreFloatingWindows(List<FloatingWindowSnapshot> snapshots) throws DockLayoutLoadException {
        if (snapshots == null || snapshots.isEmpty()) {
            return;
        }
        for (FloatingWindowSnapshot snapshot : snapshots) {
            restoreFloatingWindow(snapshot);
        }
    }

    private void restoreFloatingWindow(FloatingWindowSnapshot snapshot) throws DockLayoutLoadException {
        if (snapshot == null || snapshot.layout() == null) {
            return;
        }

        DockGraph floatingGraph = new DockGraph();
        DockLayoutSerializer floatingSerializer = createLayoutSerializer(floatingGraph);
        floatingSerializer.deserialize(SNAPSHOT_GSON.toJson(snapshot.layout()));
        DockElement floatingRoot = floatingGraph.getRoot();
        if (floatingRoot == null) {
            return;
        }

        DockFloatingWindow floatingWindow = new DockFloatingWindow(floatingRoot, dragService);
        floatingWindow.getDockGraph().setLocked(dockGraph.isLocked());
        if (isFinitePositive(snapshot.width()) || isFinitePositive(snapshot.height())) {
            double width = isFinitePositive(snapshot.width())
                ? snapshot.width()
                : floatingWindow.getPreferredWidth();
            double height = isFinitePositive(snapshot.height())
                ? snapshot.height()
                : floatingWindow.getPreferredHeight();
            floatingWindow.setPreferredSize(width, height);
        }
        if (isFiniteNumber(snapshot.x()) || isFiniteNumber(snapshot.y())) {
            floatingWindow.setPreferredPosition(snapshot.x(), snapshot.y());
        }
        configureFloatingWindowCallbacks(floatingWindow);
        applyFloatingWindowInitialAlwaysOnTop(
            floatingWindow,
            snapshot.alwaysOnTop(),
            snapshot.alwaysOnTop() != null ? DockFloatingPinSource.LAYOUT_LOAD : DockFloatingPinSource.WINDOW_CREATE_DEFAULT
        );
        floatingWindows.add(floatingWindow);
        if (primaryStage != null) {
            floatingWindow.show(primaryStage);
            bindFloatingShortcutScene(floatingWindow);
        }
    }

    private LayoutSnapshot tryParseLayoutSnapshot(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }

        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (!parsed.isJsonObject()) {
                return null;
            }
            JsonObject snapshotJson = parsed.getAsJsonObject();
            JsonElement mainLayoutElement = snapshotJson.get(SNAPSHOT_MAIN_LAYOUT_KEY);
            if (mainLayoutElement == null || !mainLayoutElement.isJsonObject()) {
                return null;
            }

            JsonObject mainLayout = mainLayoutElement.getAsJsonObject();
            List<FloatingWindowSnapshot> snapshots = new ArrayList<>();
            JsonElement floatingWindowsElement = snapshotJson.get(SNAPSHOT_FLOATING_WINDOWS_KEY);
            if (floatingWindowsElement != null && floatingWindowsElement.isJsonArray()) {
                for (JsonElement floatingElement : floatingWindowsElement.getAsJsonArray()) {
                    if (!floatingElement.isJsonObject()) {
                        continue;
                    }
                    JsonObject floatingJson = floatingElement.getAsJsonObject();
                    JsonElement floatingLayoutElement = floatingJson.get(SNAPSHOT_FLOATING_LAYOUT_KEY);
                    if (floatingLayoutElement == null || !floatingLayoutElement.isJsonObject()) {
                        continue;
                    }
                    snapshots.add(new FloatingWindowSnapshot(
                        floatingLayoutElement.getAsJsonObject(),
                        readOptionalFiniteDouble(floatingJson, SNAPSHOT_FLOATING_X_KEY),
                        readOptionalFiniteDouble(floatingJson, SNAPSHOT_FLOATING_Y_KEY),
                        readOptionalFiniteDouble(floatingJson, SNAPSHOT_FLOATING_WIDTH_KEY),
                        readOptionalFiniteDouble(floatingJson, SNAPSHOT_FLOATING_HEIGHT_KEY),
                        readOptionalBoolean(floatingJson, SNAPSHOT_FLOATING_ALWAYS_ON_TOP_KEY)
                    ));
                }
            }
            return new LayoutSnapshot(mainLayout, snapshots);
        } catch (JsonSyntaxException ignored) {
            return null;
        }
    }

    private void validateSnapshotLayout(LayoutSnapshot snapshot) throws DockLayoutLoadException {
        if (snapshot == null || snapshot.mainLayout() == null) {
            throw new DockLayoutLoadException("Snapshot is missing main layout data.", "$.mainLayout");
        }
        validateLayoutJson(SNAPSHOT_GSON.toJson(snapshot.mainLayout()), "$.mainLayout");
        List<FloatingWindowSnapshot> floatingSnapshots = snapshot.floatingWindows();
        if (floatingSnapshots == null || floatingSnapshots.isEmpty()) {
            return;
        }
        for (int i = 0; i < floatingSnapshots.size(); i++) {
            FloatingWindowSnapshot floatingSnapshot = floatingSnapshots.get(i);
            if (floatingSnapshot == null || floatingSnapshot.layout() == null) {
                throw new DockLayoutLoadException(
                    "Floating window snapshot is missing layout data.",
                    "$.floatingWindows[" + i + "].layout"
                );
            }
            validateLayoutJson(
                SNAPSHOT_GSON.toJson(floatingSnapshot.layout()),
                "$.floatingWindows[" + i + "].layout"
            );
        }
    }

    private void validateLayoutJson(String layoutJson, String rootPath) throws DockLayoutLoadException {
        DockLayoutSerializer validationSerializer = createLayoutSerializer(new DockGraph());
        try {
            validationSerializer.deserialize(layoutJson);
        } catch (DockLayoutLoadException e) {
            throw rebaseLoadException(e, rootPath);
        }
    }

    private DockLayoutLoadException rebaseLoadException(DockLayoutLoadException exception, String basePath) {
        if (exception == null) {
            return new DockLayoutLoadException("Layout could not be loaded.", basePath);
        }
        String location = combineJsonPath(basePath, exception.getLocation());
        return new DockLayoutLoadException(exception.getMessage(), location, exception.getCause());
    }

    private String combineJsonPath(String basePath, String nestedPath) {
        String base = (basePath == null || basePath.isBlank()) ? "$" : basePath;
        if (nestedPath == null || nestedPath.isBlank() || "$".equals(nestedPath)) {
            return base;
        }
        if (!nestedPath.startsWith("$")) {
            return base + "." + nestedPath;
        }
        String nestedSuffix = nestedPath.substring(1);
        if (nestedSuffix.isBlank()) {
            return base;
        }
        if (nestedSuffix.startsWith(".")) {
            return base + nestedSuffix;
        }
        return base + "." + nestedSuffix;
    }

    private DockLayoutSerializer createLayoutSerializer(DockGraph graph) {
        DockLayoutSerializer layoutSerializer = new DockLayoutSerializer(graph);
        if (nodeFactory != null) {
            layoutSerializer.setNodeFactory(nodeFactory);
        }
        return layoutSerializer;
    }

    private JsonObject parseJsonObjectOrEmpty(String json) {
        if (json == null || json.isBlank()) {
            return new JsonObject();
        }
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (parsed.isJsonObject()) {
                return parsed.getAsJsonObject();
            }
        } catch (JsonSyntaxException ignored) {
            // Ignore invalid data and return empty object fallback.
        }
        return new JsonObject();
    }

    private void addOptionalNumber(JsonObject object, String key, Double value) {
        if (object == null || key == null || !isFiniteNumber(value)) {
            return;
        }
        object.addProperty(key, value);
    }

    private Double readOptionalFiniteDouble(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key)) {
            return null;
        }
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            return null;
        }
        double parsed = value.getAsDouble();
        if (!Double.isFinite(parsed)) {
            return null;
        }
        return parsed;
    }

    private Boolean readOptionalBoolean(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key)) {
            return null;
        }
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            return null;
        }
        return value.getAsBoolean();
    }

    private boolean isFinitePositive(Double value) {
        return isFiniteNumber(value) && value > 0.0;
    }

    private boolean isFiniteNumber(Double value) {
        return value != null && Double.isFinite(value);
    }

    private record DockPlacementMemory(
        DockFloatingWindow hostWindow,
        DockElement preferredTarget,
        DockPosition preferredPosition,
        Integer preferredTabIndex,
        DockElement previousNeighbor,
        DockPosition positionRelativeToPreviousNeighbor,
        Integer tabIndexRelativeToPreviousNeighbor,
        DockElement nextNeighbor,
        DockPosition positionRelativeToNextNeighbor,
        Integer tabIndexRelativeToNextNeighbor
    ) {
    }

    private record LayoutSnapshot(JsonObject mainLayout, List<FloatingWindowSnapshot> floatingWindows) {
    }

    private record FloatingWindowSnapshot(
        JsonObject layout,
        Double x,
        Double y,
        Double width,
        Double height,
        Boolean alwaysOnTop
    ) {
    }
}
