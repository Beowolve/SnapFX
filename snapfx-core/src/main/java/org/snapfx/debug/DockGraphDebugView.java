package org.snapfx.debug;

import org.snapfx.dnd.DockDragData;
import org.snapfx.dnd.DockDragService;
import org.snapfx.model.*;
import org.snapfx.theme.DockThemeStyleClasses;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * Debug view that visualizes the current DockGraph structure.
 * <p>
 * It also highlights active D&amp;D state (dragged node, current drop target, drop position)
 * by observing the {@link DockDragService}.
 */
public class DockGraphDebugView extends BorderPane {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final int MAX_LOG_ENTRIES = 500;
    /** Placeholder label for absent drag target/state values. */
    public static final String NONE = "<none>";

    private final DockGraph dockGraph;
    private final DockDragService dragService;

    private final TreeTableView<DockElement> treeTable;
    private final ListView<DragLogEntry> activityLog;
    private final ObservableList<DragLogEntry> logEntries;

    private final ObjectProperty<DockElement> draggedElement;
    private final ObjectProperty<DockElement> dropTarget;
    private final ObjectProperty<DockPosition> dropPosition;

    private final BooleanProperty autoExportOnDrop;

    private int dragSequenceNumber = 0;

    /**
     * Creates the debug tree/log view for a dock graph.
     *
     * @param dockGraph dock graph to visualize
     * @param dragService drag service used for live drag-state diagnostics
     */
    public DockGraphDebugView(DockGraph dockGraph, DockDragService dragService) {
        this.dockGraph = Objects.requireNonNull(dockGraph, "dockGraph");
        this.dragService = Objects.requireNonNull(dragService, "dragService");

        this.draggedElement = new SimpleObjectProperty<>();
        this.dropTarget = new SimpleObjectProperty<>();
        this.dropPosition = new SimpleObjectProperty<>();
        this.autoExportOnDrop = new SimpleBooleanProperty(false);

        this.logEntries = FXCollections.observableArrayList();
        this.activityLog = new ListView<>(logEntries);
        this.activityLog.setCellFactory(lv -> new DragLogCell());
        this.activityLog.setPrefHeight(150);

        this.treeTable = new TreeTableView<>();
        treeTable.getStyleClass().add(DockThemeStyleClasses.DOCK_DEBUG_PANEL);
        treeTable.setShowRoot(true);
        treeTable.setTableMenuButtonVisible(true);

        configureColumns();
        treeTable.setRowFactory(tv -> new DockElementRow());

        setPadding(new Insets(8));
        setTop(createHeader());

        // Create split pane with tree and log
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.getItems().addAll(treeTable, createLogPanel());
        splitPane.setDividerPositions(0.7);
        setCenter(splitPane);

        rebuildTree();

        // Rebuild on structural changes.
        // We don't have a dedicated model change event yet.
        dockGraph.lockedProperty().addListener((obs, o, n) -> rebuildTree());
        dockGraph.revisionProperty().addListener((obs, o, n) -> {
            rebuildTree();
            logEntry(DragLogType.LAYOUT_CHANGE, "Layout revision changed to " + n);
        });

        // Keep D&D state in sync
        this.dragService.currentDragProperty().addListener((obs, old, cur) -> onDragDataChanged(old, cur));
        onDragDataChanged(null, this.dragService.currentDragProperty().get());
    }

    private void configureColumns() {
        TreeTableColumn<DockElement, String> elementCol = new TreeTableColumn<>("Element");
        elementCol.setPrefWidth(320);
        elementCol.setCellValueFactory(param -> {
            DockElement el = param.getValue() == null ? null : param.getValue().getValue();
            return new SimpleStringProperty(formatElement(el));
        });

        TreeTableColumn<DockElement, String> idCol = new TreeTableColumn<>("ID");
        idCol.setPrefWidth(120);
        idCol.setCellValueFactory(param -> {
            DockElement el = param.getValue() == null ? null : param.getValue().getValue();
            String id = (el == null) ? "" : shortenId(el.getId());
            return new SimpleStringProperty(id);
        });

        TreeTableColumn<DockElement, String> infoCol = new TreeTableColumn<>("Info");
        infoCol.setPrefWidth(180);
        infoCol.setCellValueFactory(param -> {
            DockElement el = param.getValue() == null ? null : param.getValue().getValue();
            return new SimpleStringProperty(formatInfo(el));
        });

        treeTable.getColumns().setAll(List.of(elementCol, idCol, infoCol));
    }

    private void onDragDataChanged(DockDragData oldData, DockDragData newData) {
        boolean dragStarted = oldData == null && newData != null;
        boolean dragEnded = oldData != null && newData == null;
        boolean dragChanged = newData != null && oldData != null && newData.getDraggedNode() != oldData.getDraggedNode();
        boolean targetChanged = newData != null && (oldData == null || !Objects.equals(oldData.getDropTarget(), newData.getDropTarget()));
        boolean positionChanged = newData != null && (oldData == null || !Objects.equals(oldData.getDropPosition(), newData.getDropPosition()));

        if (dragStarted) {
            dragSequenceNumber++;
            logEntry(DragLogType.DRAG_START,
                "Started dragging '" + safeTitle(newData.getDraggedNode()) + "' (seq#" + dragSequenceNumber + ")");
        }
        if (targetChanged) {
            logEntry(DragLogType.TARGET_CHANGE, "Target changed to " + describeTarget(newData));
        }
        if (positionChanged) {
            logEntry(DragLogType.POSITION_CHANGE, "Drop position changed to " + describePosition(newData));
        }
        if (dragEnded) {
            logEntry(DragLogType.DROP, buildDropSummary(oldData) + " (seq#" + dragSequenceNumber + ")");
            if (autoExportOnDrop.get()) {
                copySnapshotToClipboard(buildSnapshot());
            }
        }
        if (dragChanged) {
            logEntry(DragLogType.DRAG_CANCEL, "Drag changed unexpectedly");
        }
        updateDragProperties(newData);
        treeTable.refresh();
    }

    private String describeTarget(DockDragData data) {
        if (data.getDropTarget() == null) return "none";
        String base = data.getDropTarget().getClass().getSimpleName();
        if (data.getDropTarget() instanceof DockNode dn) {
            base += " '" + safeTitle(dn) + "'";
        }
        return base;
    }

    private String describePosition(DockDragData data) {
        return data.getDropPosition() == null ? "none" : data.getDropPosition().name();
    }

    private String buildDropSummary(DockDragData oldData) {
        return String.format("Dropped '%s' on %s at position %s",
                safeTitle(oldData.getDraggedNode()),
                oldData.getDropTarget() == null ? "none" : oldData.getDropTarget().getClass().getSimpleName(),
                oldData.getDropPosition() == null ? "none" : oldData.getDropPosition().name()
        );
    }

    private void updateDragProperties(DockDragData newData) {
        if (newData == null) {
            draggedElement.set(null);
            dropTarget.set(null);
            dropPosition.set(null);
        } else {
            draggedElement.set(newData.getDraggedNode());
            dropTarget.set(newData.getDropTarget());
            dropPosition.set(newData.getDropPosition());
        }
    }

    private BorderPane createLogPanel() {
        BorderPane logPanel = new BorderPane();

        Label logTitle = new Label("D&D Activity Log");
        logTitle.setStyle("-fx-font-weight: bold;");

        Button clearLog = new Button("Clear Log");
        clearLog.setOnAction(e -> {
            logEntries.clear();
            logEntry(DragLogType.SYSTEM, "Log cleared");
        });

        HBox logHeader = new HBox(8, logTitle, new Region(), clearLog);
        HBox.setHgrow(logHeader.getChildren().get(1), Priority.ALWAYS);
        logHeader.setPadding(new Insets(4, 0, 4, 0));

        logPanel.setTop(logHeader);
        logPanel.setCenter(activityLog);

        return logPanel;
    }

    private void logEntry(DragLogType type, String message) {
        DragLogEntry entry = new DragLogEntry(
            LocalTime.now(),
            type,
            message,
            dockGraph.getRevision()
        );
        logEntries.add(entry);

        // Limit log size
        while (logEntries.size() > MAX_LOG_ENTRIES) {
            logEntries.removeFirst();
        }

        // Auto-scroll to bottom
        if (!logEntries.isEmpty()) {
            activityLog.scrollTo(logEntries.size() - 1);
        }
    }

    private HBox createHeader() {
        Label title = new Label("DockGraph (Debug)");
        title.setStyle("-fx-font-weight: bold;");

        Separator sep = new Separator();
        sep.setOrientation(Orientation.VERTICAL);

        Label locked = new Label();
        locked.textProperty().bind(Bindings.createStringBinding(
            () -> dockGraph.isLocked() ? "LOCKED" : "UNLOCKED",
            dockGraph.lockedProperty()
        ));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refresh = new Button("Refresh");
        refresh.setOnAction(e -> rebuildTree());

        Button export = new Button("Export");
        export.setOnAction(e -> exportSnapshot());

        CheckBox autoExport = new CheckBox("Auto export on drop");
        autoExport.selectedProperty().bindBidirectional(autoExportOnDrop);

        HBox header = new HBox(8, title, sep, locked, spacer, autoExport, refresh, export);
        header.setPadding(new Insets(0, 0, 8, 0));
        return header;
    }

    /**
     * Rebuilds the tree-table content from the current dock graph state.
     */
    public void rebuildTree() {
        TreeItem<DockElement> rootItem = createTreeItem(dockGraph.getRoot());
        treeTable.setRoot(rootItem);
        rootItem.setExpanded(true);
        expandAll();
    }

    private TreeItem<DockElement> createTreeItem(DockElement element) {
        if (element == null) {
            return new TreeItem<>(null);
        }

        TreeItem<DockElement> item = new TreeItem<>(element);

        if (element instanceof DockContainer container) {
            for (DockElement child : container.getChildren()) {
                item.getChildren().add(createTreeItem(child));
            }
        }

        return item;
    }

    private void exportSnapshot() {
        String snapshot = buildSnapshot();
        copySnapshotToClipboard(snapshot);

        Alert choice = new Alert(Alert.AlertType.CONFIRMATION);
        choice.setTitle("Export");
        choice.setHeaderText("Snapshot was copied to the clipboard.");
        choice.setContentText("Do you also want to save it to a file?");

        ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);
        choice.getButtonTypes().setAll(saveButton, cancelButton);

        choice.showAndWait().ifPresent(bt -> {
            if (bt == saveButton) {
                saveSnapshotToFile(snapshot);
            }
        });
    }

    private void copySnapshotToClipboard(String snapshot) {
        ClipboardContent content = new ClipboardContent();
        content.putString(snapshot);
        Clipboard.getSystemClipboard().setContent(content);
    }

    private void saveSnapshotToFile(String snapshot) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save DockGraph Snapshot");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text files", "*.txt"));
        fileChooser.setInitialFileName("snapfx-dockgraph-snapshot.txt");

        File file = fileChooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (file == null) {
            return;
        }

        try {
            Files.writeString(file.toPath(), snapshot, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("Export failed");
            err.setHeaderText(null);
            err.setContentText(ex.getMessage());
            err.showAndWait();
        }
    }

    private String buildSnapshot() {
        StringBuilder sb = new StringBuilder();
        appendSnapshotHeader(sb);
        appendDragInfo(sb);
        sb.append('\n');
        sb.append("Tree:\n");
        appendElement(sb, dockGraph.getRoot(), 0);
        appendActivityLog(sb);
        return sb.toString();
    }

    private void appendSnapshotHeader(StringBuilder sb) {
        sb.append("SnapFX DockGraph Snapshot\n");
        sb.append("locked=").append(dockGraph.isLocked()).append('\n');
        sb.append("revision=").append(dockGraph.getRevision()).append('\n');
    }

    private void appendDragInfo(StringBuilder sb) {
        DockDragData drag = dragService.currentDragProperty().get();
        if (drag == null) {
            sb.append("drag=<none>\n");
        } else {
            appendDragDetails(sb, drag);
        }
    }

    private void appendDragDetails(StringBuilder sb, DockDragData drag) {
        DockNode dragged = drag.getDraggedNode();
        DockElement target = drag.getDropTarget();
        sb.append("drag.title=").append(safeTitle(dragged)).append('\n');
        sb.append("drag.nodeId=").append(nullToEmpty(dragged != null ? dragged.getId() : null)).append('\n');
        sb.append("drag.path=").append(pathOf(dragged)).append('\n');
        sb.append("drag.dropTarget=").append(target == null ? NONE : target.getClass().getSimpleName()).append('\n');
        sb.append("drag.dropTargetId=").append(nullToEmpty(target != null ? target.getId() : null)).append('\n');
        sb.append("drag.dropTargetPath=").append(pathOf(target)).append('\n');
        sb.append("drag.dropTargetTitle=").append(target instanceof DockNode tn ? safeTitle(tn) : "").append('\n');
        sb.append("drag.dropPosition=").append(drag.getDropPosition() == null ? NONE : drag.getDropPosition().name()).append('\n');
        sb.append("drag.mouseX=").append(drag.getMouseX()).append('\n');
        sb.append("drag.mouseY=").append(drag.getMouseY()).append('\n');
    }

    private void appendActivityLog(StringBuilder sb) {
        sb.append("\n\nD&D Activity Log (").append(logEntries.size()).append(" entries):\n");
        sb.append("─".repeat(80)).append('\n');
        if (logEntries.isEmpty()) {
            sb.append("  <no entries>\n");
        } else {
            for (DragLogEntry entry : logEntries) {
                sb.append(entry.timestamp().format(TIME_FORMATTER))
                  .append(" [").append(String.format("%-15s", entry.type().name())).append("]")
                  .append(" rev=").append(entry.revision())
                  .append(" | ").append(entry.message())
                  .append('\n');
            }
        }
    }

    private String pathOf(DockElement el) {
        if (el == null) {
            return NONE;
        }
        StringBuilder sb = new StringBuilder();
        DockElement current = el;
        while (current.getParent() != null) {
            DockContainer parent = current.getParent();
            int idx = parent.getChildren().indexOf(current);
            sb.insert(0, "/" + idx);
            current = parent;
        }
        if (sb.isEmpty()) {
            return "/";
        }
        return sb.toString();
    }

    private void appendElement(StringBuilder sb, DockElement el, int depth) {
        String indent = "  ".repeat(Math.max(0, depth));
        if (el == null) {
            sb.append(indent).append("<empty>\n");
            return;
        }

        sb.append(indent)
            .append(el.getClass().getSimpleName())
            .append(" id=").append(nullToEmpty(el.getId()))
            .append(" label=").append(formatElement(el));

        if (el.equals(draggedElement.get())) {
            sb.append(" [DRAGGED]");
        }
        if (el.equals(dropTarget.get())) {
            sb.append(" [TARGET");
            if (dropPosition.get() != null) {
                sb.append(" pos=").append(dropPosition.get());
            }
            sb.append(']');
        }

        sb.append('\n');

        if (el instanceof DockContainer container) {
            for (DockElement child : container.getChildren()) {
                appendElement(sb, child, depth + 1);
            }
        }
    }

    private String safeTitle(DockNode node) {
        if (node == null) {
            return "<null>";
        }
        String title = node.getTitle();
        return title == null || title.isEmpty() ? "<untitled>" : title;
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private String formatElement(DockElement el) {
        if (el == null) {
            return "<empty>";
        }
        return switch (el) {
            case DockNode node -> "DockNode title='" + node.getTitle() + "'";
            case DockSplitPane split -> "DockSplitPane orientation=" + split.getOrientation() + " children=" + split.getChildren().size();
            case DockTabPane tab -> "DockTabPane tabs=" + tab.getChildren().size() + " selected=" + tab.getSelectedIndex();
            default -> el.getClass().getSimpleName();
        };
    }

    private String formatInfo(DockElement el) {
        if (el == null) {
            return "";
        }
        return switch (el) {
            case DockNode node -> "closeable=" + node.isCloseable();
            case DockContainer container -> "children=" + container.getChildren().size();
            default -> "";
        };
    }

    private String shortenId(String id) {
        if (id == null || id.length() < 8) {
            return id == null ? "" : id;
        }
        return id.substring(0, 8);
    }

    private final class DockElementRow extends TreeTableRow<DockElement> {
        @Override
        protected void updateItem(DockElement item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setStyle("");
                setTooltip(null);
                return;
            }

            boolean isDragged = item.equals(draggedElement.get());
            boolean isTarget = item.equals(dropTarget.get());

            StringBuilder style = new StringBuilder();
            if (isDragged) {
                style.append("-fx-background-color: rgba(255, 193, 7, 0.35);");
            }

            if (isTarget) {
                style.append("-fx-background-color: rgba(33, 150, 243, 0.25);");
                if (dropPosition.get() != null) {
                    style.append("-fx-border-color: rgba(33, 150, 243, 0.85);");
                    style.append("-fx-border-width: 0 0 0 4;");
                }
            }

            setStyle(style.toString());

            if (isTarget && dropPosition.get() != null) {
                setTooltip(new Tooltip("Drop: " + dropPosition.get()));
            } else {
                setTooltip(null);
            }
        }
    }

    /**
     * Enables or disables automatic snapshot export after drop completion.
     *
     * @param enabled {@code true} to auto-export on drop
     */
    public void setAutoExportOnDrop(boolean enabled) {
        this.autoExportOnDrop.set(enabled);
    }

    /**
     * Expands all currently visible tree items.
     */
    public void expandAll() {
        expandTreeItem(treeTable.getRoot());
    }

    private void expandTreeItem(TreeItem<DockElement> item) {
        if (item == null) {
            return;
        }
        item.setExpanded(true);
        for (TreeItem<DockElement> child : item.getChildren()) {
            expandTreeItem(child);
        }
    }

    /**
     * Types of drag and drop activities that can be logged.
     */
    private enum DragLogType {
        DRAG_START,
        TARGET_CHANGE,
        POSITION_CHANGE,
        DROP,
        DRAG_CANCEL,
        LAYOUT_CHANGE,
        SYSTEM
    }

    /**
     * A single log entry for a drag and drop activity.
     */
    private record DragLogEntry(
        LocalTime timestamp,
        DragLogType type,
        String message,
        long revision
    ) {}

    /**
     * Custom cell renderer for the activity log.
     */
    private static class DragLogCell extends ListCell<DragLogEntry> {
        @Override
        protected void updateItem(DragLogEntry entry, boolean empty) {
            super.updateItem(entry, empty);

            if (empty || entry == null) {
                setText(null);
                setStyle("");
                return;
            }

            String timeStr = entry.timestamp().format(TIME_FORMATTER);
            String text = String.format("[%s] %s - %s (rev:%d)",
                timeStr,
                entry.type().name(),
                entry.message(),
                entry.revision()
            );
            setText(text);

            // Color code by type
            String style = switch (entry.type()) {
                case DRAG_START -> "-fx-text-fill: #2196F3;";
                case TARGET_CHANGE -> "-fx-text-fill: #FF9800;";
                case POSITION_CHANGE -> "-fx-text-fill: #FF9800;";
                case DROP -> "-fx-text-fill: #4CAF50; -fx-font-weight: bold;";
                case DRAG_CANCEL -> "-fx-text-fill: #F44336;";
                case LAYOUT_CHANGE -> "-fx-text-fill: #9C27B0;";
                case SYSTEM -> "-fx-text-fill: #757575;";
            };
            setStyle(style);
        }
    }
}
