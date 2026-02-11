package com.github.beowolve.snapfx.demo;

import com.github.beowolve.snapfx.SnapFX;
import com.github.beowolve.snapfx.model.DockNode;
import com.github.beowolve.snapfx.model.DockPosition;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeView;
import javafx.scene.control.TreeItem;
import javafx.stage.Stage;

import java.util.List;

/**
 * Simple example showing how to use SnapFX.
 * Demonstrates the minimum amount of code required for a working docking setup.
 */
public class SimpleExample extends Application {

    @Override
    public void start(Stage stage) {
        // 1. Create SnapFX instance
        SnapFX snapFX = new SnapFX();

        // 2. Create content
        TreeView<String> fileTree = createFileTree();
        TextArea editor = createEditor();
        TextArea console = createConsole();
        Label properties = createProperties();

        // 3. Dock nodes (simple API)
        DockNode files = snapFX.dock(fileTree, "Files");
        DockNode edit = snapFX.dock(editor, "Editor", files, DockPosition.RIGHT);
        snapFX.dock(console, "Console", edit, DockPosition.BOTTOM);
        snapFX.dock(properties, "Properties", edit, DockPosition.RIGHT);

        // 4. Build layout
        Parent layout = snapFX.buildLayout();

        // 5. Configure scene and stage
        Scene scene = new Scene(layout, 1000, 600);

        // Load CSS (optional)
        var cssResource = getClass().getResource("/snapfx.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
        }

        stage.setTitle("SnapFX - Simple Example");
        stage.setScene(scene);

        // 6. Initialize SnapFX (required for drag & drop)
        snapFX.initialize(stage);

        stage.show();
    }

    private TreeView<String> createFileTree() {
        TreeView<String> tree = new TreeView<>();
        TreeItem<String> root = new TreeItem<>("Project");
        root.setExpanded(true);

        TreeItem<String> src = new TreeItem<>("src");
        src.getChildren().addAll(
            List.of(
                new TreeItem<>("Main.java"),
                new TreeItem<>("App.java")
            )
        );

        root.getChildren().add(src);
        tree.setRoot(root);

        return tree;
    }

    private TextArea createEditor() {
        TextArea editor = new TextArea();
        editor.setText("""
            public class Main {
                public static void main(String[] args) {
                    // SnapFX demo
                    SnapFX snapFX = new SnapFX();

                    // Dock nodes
                    DockNode editor = snapFX.dock(
                        new TextArea(),
                        "Editor"
                    );

                    // Done!
                }
            }
            """);
        editor.setStyle("-fx-font-family: monospace;");
        return editor;
    }

    private TextArea createConsole() {
        TextArea console = new TextArea();
        console.setEditable(false);
        console.setText("""
            SnapFX Console
            ==============
            > Application started
            > Ready for input
            """);
        console.setStyle("-fx-font-family: monospace;");
        return console;
    }

    private Label createProperties() {
        Label props = new Label("""
            Properties
            ----------
            Name: Main.java
            Type: Java File
            Size: 1.2 KB
            Modified: Today
            """);
        props.setStyle("-fx-padding: 10;");
        return props;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
