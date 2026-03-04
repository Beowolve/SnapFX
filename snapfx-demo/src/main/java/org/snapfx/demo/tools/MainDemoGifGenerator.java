package org.snapfx.demo.tools;

import org.snapfx.SnapFX;
import org.snapfx.demo.MainDemo;
import org.snapfx.demo.factory.DockNodeType;
import org.snapfx.model.DockContainer;
import org.snapfx.model.DockElement;
import org.snapfx.model.DockNode;
import org.snapfx.view.DockNodeView;
import javafx.animation.KeyFrame;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.image.WritableImage;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Utility launcher that starts {@link MainDemo} and captures an animated GIF preview with
 * representative interactions (drag and drop, sidebar context-menu action, and theme switching).
 */
public class MainDemoGifGenerator extends Application {
    private static final String DEFAULT_OUTPUT = "docs/images/main-demo.gif";
    private static final int CAPTURE_INTERVAL_MS = 85;
    private static final int START_DELAY_MS = 1400;
    private static final int DRAG_STEPS = 20;
    private static final int DRAG_STEP_DELAY_MS = 60;
    private static final int MEDIUM_PAUSE_MS = 800;
    private static final int VISUAL_READY_MAX_RETRIES = 12;
    private static final int VISUAL_READY_RETRY_DELAY_MS = 250;
    private static final int MIN_VISIBLE_IMAGE_VIEWS = 6;
    private static final String STYLE_SIDEBAR_STRIP_LEFT = "dock-sidebar-strip-left";
    private static final String STYLE_SIDEBAR_STRIP_RIGHT = "dock-sidebar-strip-right";
    private static final String STYLE_SIDEBAR_ICON_BUTTON = "dock-sidebar-icon-button";
    private static final String STYLE_DOCK_NODE_HEADER = "dock-node-header";

    private final List<BufferedImage> frames = new ArrayList<>();
    private final Robot robot = new Robot();

    private boolean captureFailed;
    private Timeline captureTimeline;
    private SequentialTransition scenario;
    private Stage stage;
    private SnapFX snapFX;
    private Path outputPath;
    private boolean originalAlwaysOnTop;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        MainDemo demo = new MainDemo();
        demo.start(stage);
        this.snapFX = resolveSnapFxForAutomation(demo);

        if (snapFX == null) {
            failAndExit(new IllegalStateException("MainDemo did not initialize SnapFX."));
            return;
        }

        originalAlwaysOnTop = stage.isAlwaysOnTop();
        stage.setAlwaysOnTop(true);
        ensureStageForeground();

        outputPath = resolveOutputPath(getParameters().getUnnamed());
        PauseTransition delay = new PauseTransition(Duration.millis(START_DELAY_MS));
        delay.setOnFinished(event -> startCaptureScenario(0));
        delay.play();
    }

    @Override
    public void stop() {
        if (captureFailed) {
            System.exit(1);
        }
    }

    static List<String> resolveThemeSwitchSequence(List<String> availableThemes, String currentTheme) {
        if (availableThemes == null || availableThemes.isEmpty()) {
            return List.of();
        }

        String resolvedCurrent = currentTheme;
        if (resolvedCurrent == null || resolvedCurrent.isBlank() || !availableThemes.contains(resolvedCurrent)) {
            resolvedCurrent = availableThemes.getFirst();
        }

        for (String theme : availableThemes) {
            if (!theme.equals(resolvedCurrent)) {
                return List.of(theme, resolvedCurrent);
            }
        }
        return List.of(resolvedCurrent);
    }

    private void startCaptureScenario(int readinessRetryCount) {
        ensureStageForeground();

        if (!isSceneVisualReady() && readinessRetryCount < VISUAL_READY_MAX_RETRIES) {
            PauseTransition retryDelay = new PauseTransition(Duration.millis(VISUAL_READY_RETRY_DELAY_MS));
            retryDelay.setOnFinished(event -> startCaptureScenario(readinessRetryCount + 1));
            retryDelay.play();
            return;
        }

        String currentTheme = resolveThemeNameByStylesheetPath(snapFX.getThemeStylesheetResourcePath());
        List<String> themeSequence = resolveThemeSwitchSequence(SnapFX.getAvailableThemeNames(), currentTheme);
        String middleTheme = themeSequence.isEmpty() ? null : themeSequence.getFirst();
        String endTheme = themeSequence.size() > 1 ? themeSequence.getLast() : null;

        captureTimeline = new Timeline(new KeyFrame(Duration.millis(CAPTURE_INTERVAL_MS), e -> captureFrame()));
        captureTimeline.setCycleCount(Timeline.INDEFINITE);
        captureTimeline.play();
        captureFrame();

        scenario = new SequentialTransition();
        scenario.getChildren().add(waitStep(MEDIUM_PAUSE_MS));

        scenario.getChildren().add(
            createDockHeaderContextMenuMoveOperation(DockNodeType.CONSOLE.getId(), 1)
        );
        scenario.getChildren().add(waitStep(MEDIUM_PAUSE_MS));

        scenario.getChildren().add(
            createDragOperation(
                "Drag Console from left sidebar",
                () -> sideBarIconCenter(
                    Side.LEFT,
                    DockNodeType.CONSOLE.getId(),
                    DockNodeType.CONSOLE.getDefaultTitle()
                ),
                () -> dockLayoutPoint(0.58, 0.66)
            )
        );
        scenario.getChildren().add(waitStep(MEDIUM_PAUSE_MS));
        appendThemeSwitchStep(scenario, middleTheme);

        scenario.getChildren().add(
            createDragOperation(
                "Drag Project Explorer to left sidebar",
                () -> dockHeaderPoint(DockNodeType.PROJECT_EXPLORER.getId(), 0.5),
                () -> sideBarStripCenter(Side.LEFT)
            )
        );
        scenario.getChildren().add(waitStep(MEDIUM_PAUSE_MS));

        scenario.getChildren().add(
            createDragOperation(
                "Drag Project Explorer from left sidebar",
                () -> sideBarIconCenter(
                    Side.LEFT,
                    DockNodeType.PROJECT_EXPLORER.getId(),
                    DockNodeType.PROJECT_EXPLORER.getDefaultTitle()
                ),
                () -> dockLayoutPoint(0.62, 0.42)
            )
        );
        scenario.getChildren().add(waitStep(MEDIUM_PAUSE_MS));

        appendThemeSwitchStep(scenario, endTheme);
        scenario.getChildren().add(actionStep(this::finishAndExit));
        scenario.play();
    }

    private void appendThemeSwitchStep(SequentialTransition sequence, String themeName) {
        if (themeName == null || themeName.isBlank()) {
            return;
        }
        sequence.getChildren().add(actionStep(() -> applyTheme(themeName)));
        sequence.getChildren().add(waitStep(MEDIUM_PAUSE_MS));
    }

    private void applyTheme(String themeName) {
        Map<String, String> namedThemes = SnapFX.getAvailableThemeStylesheets();
        String stylesheetPath = namedThemes.get(themeName);
        if (stylesheetPath != null) {
            snapFX.setThemeStylesheet(stylesheetPath);
        }
    }

    private SnapFX resolveSnapFxForAutomation(MainDemo demo) {
        if (demo == null) {
            return null;
        }
        try {
            var accessor = MainDemo.class.getDeclaredMethod("getSnapFXForAutomation");
            accessor.setAccessible(true);
            Object value = accessor.invoke(demo);
            if (value instanceof SnapFX snapFxValue) {
                return snapFxValue;
            }
            return null;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to access MainDemo SnapFX automation instance.", ex);
        }
    }

    private String resolveThemeNameByStylesheetPath(String stylesheetResourcePath) {
        if (stylesheetResourcePath == null || stylesheetResourcePath.isBlank()) {
            return SnapFX.getDefaultThemeName();
        }
        for (Map.Entry<String, String> entry : SnapFX.getAvailableThemeStylesheets().entrySet()) {
            if (entry.getValue().equals(stylesheetResourcePath)) {
                return entry.getKey();
            }
        }
        return SnapFX.getDefaultThemeName();
    }

    private SequentialTransition createDockHeaderContextMenuMoveOperation(String dockNodeId, int moveDownCount) {
        SequentialTransition transition = new SequentialTransition();
        transition.getChildren().add(actionStep(() -> {
            Point2D headerPoint = dockHeaderPoint(dockNodeId, 0.5);
            robot.mouseMove(headerPoint.getX(), headerPoint.getY());
            robot.mousePress(MouseButton.SECONDARY);
            robot.mouseRelease(MouseButton.SECONDARY);
        }));
        transition.getChildren().add(waitStep(420));
        for (int i = 0; i < moveDownCount; i++) {
            transition.getChildren().add(actionStep(() -> tapKey(KeyCode.DOWN)));
            transition.getChildren().add(waitStep(140));
        }
        transition.getChildren().add(actionStep(() -> tapKey(KeyCode.ENTER)));
        return transition;
    }

    private SequentialTransition createDragOperation(String label, Supplier<Point2D> startSupplier, Supplier<Point2D> endSupplier) {
        SequentialTransition transition = new SequentialTransition();
        Point2D[] points = new Point2D[2];

        transition.getChildren().add(actionStep(() -> {
            points[0] = requirePoint(startSupplier.get(), label + " start");
            points[1] = requirePoint(endSupplier.get(), label + " end");
            robot.mouseMove(points[0].getX(), points[0].getY());
            robot.mousePress(MouseButton.PRIMARY);
        }));

        for (int i = 1; i <= DRAG_STEPS; i++) {
            final int step = i;
            transition.getChildren().add(waitStep(DRAG_STEP_DELAY_MS));
            transition.getChildren().add(actionStep(() -> {
                Point2D start = requirePoint(points[0], label + " start");
                Point2D end = requirePoint(points[1], label + " end");
                double t = step / (double) DRAG_STEPS;
                double x = start.getX() + (end.getX() - start.getX()) * t;
                double y = start.getY() + (end.getY() - start.getY()) * t;
                robot.mouseMove(x, y);
            }));
        }

        transition.getChildren().add(waitStep(DRAG_STEP_DELAY_MS));
        transition.getChildren().add(actionStep(() -> robot.mouseRelease(MouseButton.PRIMARY)));
        return transition;
    }

    private PauseTransition actionStep(Runnable action) {
        PauseTransition transition = new PauseTransition(Duration.millis(1));
        transition.setOnFinished(event -> {
            if (captureFailed) {
                return;
            }
            try {
                ensureStageForeground();
                action.run();
            } catch (Exception ex) {
                failAndExit(ex);
            }
        });
        return transition;
    }

    private PauseTransition waitStep(int millis) {
        return new PauseTransition(Duration.millis(millis));
    }

    private void tapKey(KeyCode keyCode) {
        robot.keyPress(keyCode);
        robot.keyRelease(keyCode);
    }

    private void ensureStageForeground() {
        if (stage == null) {
            return;
        }
        if (stage.isIconified()) {
            stage.setIconified(false);
        }
        stage.toFront();
        stage.requestFocus();
    }

    private boolean isSceneVisualReady() {
        if (stage == null || stage.getScene() == null || stage.getScene().getRoot() == null) {
            return false;
        }
        Parent root = stage.getScene().getRoot();
        return countVisibleImageViews(root) >= MIN_VISIBLE_IMAGE_VIEWS;
    }

    private int countVisibleImageViews(Node node) {
        if (node == null || !node.isVisible()) {
            return 0;
        }
        int count = node instanceof ImageView ? 1 : 0;
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                count += countVisibleImageViews(child);
            }
        }
        return count;
    }

    private Point2D dockHeaderPoint(String dockNodeId, double horizontalRatio) {
        DockNode dockNode = findDockNodeById(snapFX.getDockGraph().getRoot(), dockNodeId);
        if (dockNode != null) {
            DockNodeView nodeView = snapFX.getLayoutEngine().getDockNodeView(dockNode);
            Node header = nodeView == null ? null : nodeView.getHeader();
            Bounds bounds = resolveScreenBoundsWithRetry(header);
            if (bounds != null) {
                double x = bounds.getMinX() + bounds.getWidth() * Math.clamp(horizontalRatio, 0.0, 1.0);
                double y = bounds.getMinY() + bounds.getHeight() * 0.5;
                return new Point2D(x, y);
            }
        }

        Point2D fallback = firstVisibleDockHeaderPoint(horizontalRatio);
        if (fallback != null) {
            return fallback;
        }
        throw new IllegalStateException("Dock header point unavailable for nodeId '" + dockNodeId + "'.");
    }

    private Point2D sideBarStripCenter(Side side) {
        String sideClass = side == Side.LEFT
            ? STYLE_SIDEBAR_STRIP_LEFT
            : STYLE_SIDEBAR_STRIP_RIGHT;
        Node strip = findVisibleNodeByStyleClass(sideClass);
        if (strip == null) {
            throw new IllegalStateException("Sidebar strip not visible for side: " + side);
        }
        return centerOnScreen(strip);
    }

    private Point2D dockLayoutPoint(double horizontalRatio, double verticalRatio) {
        Parent layoutRoot = snapFX.buildLayout();
        Bounds bounds = resolveScreenBoundsWithRetry(layoutRoot);
        if (bounds == null) {
            throw new IllegalStateException("Dock layout bounds are unavailable on screen.");
        }
        double x = bounds.getMinX() + bounds.getWidth() * Math.clamp(horizontalRatio, 0.0, 1.0);
        double y = bounds.getMinY() + bounds.getHeight() * Math.clamp(verticalRatio, 0.0, 1.0);
        return new Point2D(x, y);
    }

    private Point2D sideBarIconCenter(Side side, String dockNodeId, String title) {
        Point2D point = tryResolveSideBarIconCenter(side, title);
        if (point != null) {
            return point;
        }

        // Fallback: if a previous DnD step did not complete in time, enforce sidebar placement once.
        DockNode dockNode = requireDockNode(dockNodeId);
        if (snapFX.getPinnedSide(dockNode) != side) {
            snapFX.pinToSideBar(dockNode, side);
        }
        snapFX.buildLayout();

        point = tryResolveSideBarIconCenter(side, title);
        if (point != null) {
            return point;
        }

        List<String> pinnedTitles = snapFX.getSideBarNodes(side).stream()
            .map(DockNode::getTitle)
            .toList();
        throw new IllegalStateException(
            "Sidebar icon button not found for side " + side
                + ", nodeId='" + dockNodeId + "', expectedTitle='" + title
                + "', pinnedNodes=" + pinnedTitles
        );
    }

    private Point2D tryResolveSideBarIconCenter(Side side, String title) {
        List<Button> candidates = resolveSideBarIconCandidates(side);
        for (Button candidate : candidates) {
            Tooltip tooltip = candidate.getTooltip();
            if (tooltip != null && Objects.equals(tooltip.getText(), title)) {
                return centerOnScreen(candidate);
            }
        }
        if (!candidates.isEmpty()) {
            return centerOnScreen(candidates.getFirst());
        }
        return null;
    }

    private List<Button> resolveSideBarIconCandidates(Side side) {
        Parent root = requireSceneRoot();
        Set<Node> iconNodes = root.lookupAll("." + STYLE_SIDEBAR_ICON_BUTTON);
        if (iconNodes.isEmpty()) {
            return List.of();
        }
        return iconNodes.stream()
            .filter(Node::isVisible)
            .filter(node -> belongsToSideBarStrip(node, side))
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .toList();
    }

    private boolean belongsToSideBarStrip(Node node, Side side) {
        if (node == null || side == null) {
            return false;
        }
        String targetStyleClass = side == Side.LEFT ? STYLE_SIDEBAR_STRIP_LEFT : STYLE_SIDEBAR_STRIP_RIGHT;
        Node current = node;
        while (current != null) {
            if (current.getStyleClass().contains(targetStyleClass)) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private Node findVisibleNodeByStyleClass(String styleClass) {
        Parent root = requireSceneRoot();
        Set<Node> nodes = root.lookupAll("." + styleClass);
        for (Node node : nodes) {
            if (node.isVisible()) {
                return node;
            }
        }
        return null;
    }

    private Parent requireSceneRoot() {
        if (stage == null || stage.getScene() == null || stage.getScene().getRoot() == null) {
            throw new IllegalStateException("MainDemo scene root is not available.");
        }
        return stage.getScene().getRoot();
    }

    private Point2D centerOnScreen(Node node) {
        Bounds bounds = resolveScreenBoundsWithRetry(node);
        if (bounds == null) {
            throw new IllegalStateException("Node bounds are not available on screen.");
        }
        return new Point2D(bounds.getCenterX(), bounds.getCenterY());
    }

    private Bounds resolveScreenBoundsWithRetry(Node node) {
        if (node == null) {
            return null;
        }
        Bounds bounds = node.localToScreen(node.getBoundsInLocal());
        if (bounds != null) {
            return bounds;
        }
        snapFX.buildLayout();
        return node.localToScreen(node.getBoundsInLocal());
    }

    private Point2D firstVisibleDockHeaderPoint(double horizontalRatio) {
        Parent root = requireSceneRoot();
        Set<Node> headers = root.lookupAll("." + STYLE_DOCK_NODE_HEADER);
        for (Node header : headers) {
            if (!header.isVisible()) {
                continue;
            }
            Bounds bounds = resolveScreenBoundsWithRetry(header);
            if (bounds == null) {
                continue;
            }
            double x = bounds.getMinX() + bounds.getWidth() * Math.clamp(horizontalRatio, 0.0, 1.0);
            double y = bounds.getMinY() + bounds.getHeight() * 0.5;
            return new Point2D(x, y);
        }
        return null;
    }

    private DockNode requireDockNode(String dockNodeId) {
        DockNode node = findDockNodeById(snapFX.getDockGraph().getRoot(), dockNodeId);
        if (node != null) {
            return node;
        }
        for (DockNode sideNode : snapFX.getSideBarNodes(Side.LEFT)) {
            if (dockNodeId.equals(sideNode.getDockNodeId())) {
                return sideNode;
            }
        }
        for (DockNode sideNode : snapFX.getSideBarNodes(Side.RIGHT)) {
            if (dockNodeId.equals(sideNode.getDockNodeId())) {
                return sideNode;
            }
        }
        throw new IllegalStateException("Unable to resolve dock node by id: " + dockNodeId);
    }

    private DockNode findDockNodeById(DockElement element, String dockNodeId) {
        if (element == null || dockNodeId == null) {
            return null;
        }
        if (element instanceof DockNode dockNode && dockNodeId.equals(dockNode.getDockNodeId())) {
            return dockNode;
        }
        if (element instanceof DockContainer container) {
            for (DockElement child : container.getChildren()) {
                DockNode found = findDockNodeById(child, dockNodeId);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private Point2D requirePoint(Point2D point, String label) {
        if (point == null) {
            throw new IllegalStateException("Point is unavailable: " + label);
        }
        return point;
    }

    private void captureFrame() {
        if (stage == null || !stage.isShowing()) {
            return;
        }
        double width = stage.getWidth();
        double height = stage.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        WritableImage image = robot.getScreenCapture(
            null,
            stage.getX(),
            stage.getY(),
            width,
            height
        );
        frames.add(toBufferedImage(image));
    }

    private void finishAndExit() {
        try {
            if (captureTimeline != null) {
                captureTimeline.stop();
            }
            captureFrame();
            if (frames.size() < 2) {
                throw new IllegalStateException("Not enough frames captured to create GIF.");
            }
            AnimatedGifWriter.write(outputPath, frames, CAPTURE_INTERVAL_MS, true);
            System.out.println("MainDemo GIF preview updated: " + outputPath); // NOSONAR - build utility output
        } catch (Exception ex) {
            failAndExit(ex);
            return;
        } finally {
            if (stage != null) {
                stage.setAlwaysOnTop(originalAlwaysOnTop);
            }
        }
        Platform.exit();
    }

    private void failAndExit(Exception ex) {
        captureFailed = true;
        if (captureTimeline != null) {
            captureTimeline.stop();
        }
        if (scenario != null) {
            scenario.stop();
        }
        if (stage != null) {
            stage.setAlwaysOnTop(originalAlwaysOnTop);
        }
        ex.printStackTrace(System.err); // NOSONAR - build utility output
        Platform.exit();
    }

    private Path resolveOutputPath(List<String> args) {
        String outputArg = (args == null || args.isEmpty()) ? DEFAULT_OUTPUT : args.getFirst();
        return Paths.get(outputArg).toAbsolutePath();
    }

    private BufferedImage toBufferedImage(WritableImage image) {
        BufferedImage bufferedImage = new BufferedImage((int) image.getWidth(), (int) image.getHeight(), BufferedImage.TYPE_INT_RGB);
        var pixelReader = image.getPixelReader();
        for (int y = 0; y < bufferedImage.getHeight(); y++) {
            for (int x = 0; x < bufferedImage.getWidth(); x++) {
                int argb = pixelReader.getArgb(x, y);
                int alpha = (argb >>> 24) & 0xFF;
                int red = (argb >>> 16) & 0xFF;
                int green = (argb >>> 8) & 0xFF;
                int blue = argb & 0xFF;

                // Flatten semi-transparent pixels against white to avoid icon loss in GIF quantization.
                if (alpha < 0xFF) {
                    red = blendChannel(red, alpha);
                    green = blendChannel(green, alpha);
                    blue = blendChannel(blue, alpha);
                }

                int rgb = (red << 16) | (green << 8) | blue;
                bufferedImage.setRGB(x, y, rgb);
            }
        }
        return bufferedImage;
    }

    private int blendChannel(int color, int alpha) {
        return (color * alpha + 0xFF * (0xFF - alpha)) / 0xFF;
    }

    /**
     * Main entry point for the application.
     * Launches the JavaFX application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
