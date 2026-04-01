package org.snapfx.demo;

import org.snapfx.SnapFX;
import org.snapfx.demo.factory.DemoNodeFactory;
import org.snapfx.demo.i18n.DemoLocalizationService;
import org.snapfx.model.DockNode;
import org.snapfx.model.DockSplitPane;
import org.snapfx.persistence.DockNodeFactory;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoNodeFactoryTest {
    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // JavaFX is already running.
        }
    }

    @Test
    void testUnknownNodeFallsBackToFrameworkPlaceholderByDefault() {
        DemoNodeFactory factory = new DemoNodeFactory();

        DockNode node = factory.createUnknownNode(
            new DockNodeFactory.UnknownElementContext(
                "DockNode!!!",
                "consolePanel!",
                "dock-6",
                "Console",
                "$.root.children[0].type"
            )
        );

        assertNull(node);
    }

    @Test
    void testUnknownNodeCanUseDemoSpecificFallbackWhenConfigured() {
        DemoNodeFactory factory = new DemoNodeFactory(false);

        DockNode node = factory.createUnknownNode(
            new DockNodeFactory.UnknownElementContext(
                "DockNode!!!",
                "consolePanel!",
                "dock-6",
                "Console",
                "$.root.children[0].type"
            )
        );

        assertNotNull(node);
        assertEquals("consolePanel!", node.getDockNodeId());
        assertEquals("Console", node.getTitle());
        Label content = assertInstanceOf(Label.class, node.getContent());
        assertTrue(content.getText().contains("DockNode!!!"));
    }

    @Test
    void testPropertiesNodeUpdatesTitleAndVisibleContentWhenLocaleChanges() {
        DemoLocalizationService localizationService = new DemoLocalizationService(MainDemo.class.getModule());
        DemoNodeFactory factory = new DemoNodeFactory(localizationService);

        DockNode propertiesNode = factory.createPropertiesNode();
        VBox content = assertInstanceOf(VBox.class, propertiesNode.getContent());

        assertEquals("Properties", propertiesNode.getTitle());
        assertTrue(collectLabeledTexts(content).contains("Properties"));

        localizationService.setLocale(Locale.GERMAN);

        assertEquals("Eigenschaften", propertiesNode.getTitle());
        assertTrue(collectLabeledTexts(content).contains("Eigenschaften"));
        assertTrue(collectLabeledTexts(content).contains("Typ:"));
    }

    @Test
    void testProjectExplorerKeepsTechnicalTreeLabelsAcrossLocaleChanges() {
        DemoLocalizationService localizationService = new DemoLocalizationService(MainDemo.class.getModule());
        DemoNodeFactory factory = new DemoNodeFactory(localizationService);

        DockNode projectExplorerNode = factory.createProjectExplorerNode();
        TreeView<String> projectTree = assertInstanceOf(TreeView.class, projectExplorerNode.getContent());

        TreeItem<String> root = projectTree.getRoot();
        assertNotNull(root);
        assertEquals("Project", root.getValue());
        assertEquals("src", root.getChildren().get(0).getValue());
        assertEquals("test", root.getChildren().get(1).getValue());
        assertEquals("resources", root.getChildren().get(2).getValue());

        localizationService.setLocale(Locale.GERMAN);

        assertEquals("Project", root.getValue());
        assertEquals("src", root.getChildren().get(0).getValue());
        assertEquals("test", root.getChildren().get(1).getValue());
        assertEquals("resources", root.getChildren().get(2).getValue());
    }

    @Test
    void testSnapFxLoadWithDefaultDemoFactoryRecoversUnsupportedNodeType() {
        String json = """
            {
              "mainLayout": {
                "locked": false,
                "layoutIdCounter": 9,
                "root": {
                  "id": "split-root",
                  "type": "DockSplitPane",
                  "orientation": "VERTICAL",
                  "children": [
                    {
                      "id": "dock-6",
                      "dockNodeId": "consolePanel!",
                      "type": "DockNode!!!",
                      "title": "Console",
                      "closeable": true
                    },
                    {
                      "id": "dock-5",
                      "dockNodeId": "tasks",
                      "type": "DockNode",
                      "title": "Tasks",
                      "closeable": true
                    }
                  ]
                }
              },
              "floatingWindows": []
            }
            """;
        SnapFX snapFX = new SnapFX();
        snapFX.setNodeFactory(new DemoNodeFactory());

        assertDoesNotThrow(() -> snapFX.loadLayout(json));

        DockSplitPane root = assertInstanceOf(DockSplitPane.class, snapFX.getDockGraph().getRoot());
        DockNode fallbackNode = assertInstanceOf(DockNode.class, root.getChildren().getFirst());
        Label content = assertInstanceOf(Label.class, fallbackNode.getContent());
        assertTrue(content.getText().contains("DockNode!!!"));
    }

    private List<String> collectLabeledTexts(Node root) {
        List<String> texts = new ArrayList<>();
        collectLabeledTexts(root, texts);
        return texts;
    }

    private void collectLabeledTexts(Node node, List<String> collector) {
        if (node instanceof Labeled labeled) {
            collector.add(labeled.getText());
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collectLabeledTexts(child, collector);
            }
        }
    }
}
