package com.github.beowolve.snapfx.demo;

import com.github.beowolve.snapfx.BuildInfo;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Renders and shows the About dialog used by {@link MainDemo}.
 */
public final class AboutDialog {
    private static final String CONTENT_LOGO_RESOURCE = "/images/128/snapfx.png";
    private static final String DIALOG_ICON_RESOURCE = "/images/64/snapfx.png";
    private static final String YUSUKE_AUTHOR_URL = "http://p.yusukekamiyamane.com/";
    private static final String YUSUKE_LICENSE_URL = "http://creativecommons.org/licenses/by/3.0/deed.de";
    private static final String FLATICON_CREDIT_URL = "https://www.flaticon.com/free-icons/logout";
    private static final String EASTER_EGG_RUNNING_KEY = "snapfx.about.easterEgg.running";
    private static final Duration EASTER_EGG_POP_DURATION = Duration.millis(210);
    private static final Duration EASTER_EGG_SPIN_DURATION = Duration.millis(780);

    private AboutDialog() {
    }

    /**
     * Shows the About dialog.
     *
     * @param owner      owner window for modality; can be {@code null}
     * @param linkOpener callback used for opening external links
     */
    public static void show(Window owner, Consumer<String> linkOpener) {
        Objects.requireNonNull(linkOpener, "linkOpener");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        if (owner != null) {
            alert.initOwner(owner);
        }

        alert.setTitle("About SnapFX");
        alert.setHeaderText(null);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setGraphic(null);
        dialogPane.setPrefWidth(620);
        dialogPane.setContent(createContent(linkOpener));

        applyDialogIcon(alert);
        alert.showAndWait();
    }

    static String getContentLogoResource() {
        return CONTENT_LOGO_RESOURCE;
    }

    static String getDialogIconResource() {
        return DIALOG_ICON_RESOURCE;
    }

    static String getFlaticonCreditUrl() {
        return FLATICON_CREDIT_URL;
    }

    static String getYusukeAuthorUrl() {
        return YUSUKE_AUTHOR_URL;
    }

    static String getYusukeLicenseUrl() {
        return YUSUKE_LICENSE_URL;
    }

    private static VBox createContent(Consumer<String> linkOpener) {
        Label titleLabel = new Label("SnapFX Docking Framework");
        titleLabel.setStyle(MainDemo.FX_FONT_WEIGHT_BOLD + "-fx-font-size: 18px;");

        Label versionLabel = new Label("Version " + BuildInfo.getVersion());
        versionLabel.setStyle(MainDemo.FX_FONT_WEIGHT_BOLD);

        Label descriptionLabel = new Label(
            "A high-performance, lightweight JavaFX docking framework\n" +
            "designed for professional IDE-like applications."
        );
        descriptionLabel.setWrapText(true);

        Label easterEggLabel = new Label("Easter egg unlocked: docking energy is now at 100%.");
        easterEggLabel.setWrapText(true);
        easterEggLabel.setVisible(false);
        easterEggLabel.setManaged(false);
        easterEggLabel.setOpacity(0.0);
        easterEggLabel.setStyle("-fx-text-fill: #2873b8; -fx-font-style: italic;");

        Node logoNode = createLogoNode();
        if (logoNode instanceof ImageView logoView) {
            installEasterEggAnimation(logoView, easterEggLabel);
        }

        VBox headerText = new VBox(6, titleLabel, versionLabel, descriptionLabel);
        headerText.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(headerText, Priority.ALWAYS);

        HBox header = new HBox(16, logoNode, headerText);
        header.setAlignment(Pos.CENTER_LEFT);

        Label licenseTitle = new Label("Icon Credits");
        licenseTitle.setStyle(MainDemo.FX_FONT_WEIGHT_BOLD);

        FlowPane yusukeCredits = new FlowPane(5, 5);
        yusukeCredits.getChildren().addAll(
            new Label("Some icons by"),
            createHyperlink("Yusuke Kamiyamane", YUSUKE_AUTHOR_URL, linkOpener),
            new Label("licensed under"),
            createHyperlink("Creative Commons Attribution 3.0", YUSUKE_LICENSE_URL, linkOpener),
            new Label(".")
        );

        Hyperlink flaticonCredits = createHyperlink(
            "Logout icons created by Pixel perfect - Flaticon",
            FLATICON_CREDIT_URL,
            linkOpener
        );

        VBox content = new VBox(
            12,
            header,
            easterEggLabel,
            new Separator(),
            licenseTitle,
            yusukeCredits,
            flaticonCredits
        );
        content.setPadding(new Insets(14));
        return content;
    }

    private static Node createLogoNode() {
        ImageView logoView = createImageView(CONTENT_LOGO_RESOURCE, 110);
        if (logoView != null) {
            return logoView;
        }

        Label fallback = new Label("SnapFX");
        fallback.setStyle(MainDemo.FX_FONT_WEIGHT_BOLD + "-fx-font-size: 28px;");
        return fallback;
    }

    private static Hyperlink createHyperlink(String text, String url, Consumer<String> linkOpener) {
        Hyperlink hyperlink = new Hyperlink(text);
        hyperlink.setOnAction(e -> linkOpener.accept(url));
        return hyperlink;
    }

    private static void applyDialogIcon(Alert alert) {
        Image dialogIcon = loadImage(DIALOG_ICON_RESOURCE);
        if (dialogIcon == null) {
            return;
        }

        alert.setOnShown(e -> {
            Window window = alert.getDialogPane().getScene().getWindow();
            if (window instanceof Stage stage) {
                stage.getIcons().setAll(dialogIcon);
            }
        });
    }

    private static void installEasterEggAnimation(ImageView logoView, Label easterEggLabel) {
        logoView.setOnMouseClicked(event -> {
            if (event.getButton() != MouseButton.PRIMARY || event.getClickCount() < 3) {
                return;
            }
            if (Boolean.TRUE.equals(logoView.getProperties().get(EASTER_EGG_RUNNING_KEY))) {
                return;
            }

            logoView.getProperties().put(EASTER_EGG_RUNNING_KEY, Boolean.TRUE);
            easterEggLabel.setManaged(true);
            easterEggLabel.setVisible(true);

            ScaleTransition scaleUp = new ScaleTransition(EASTER_EGG_POP_DURATION, logoView);
            scaleUp.setToX(1.12);
            scaleUp.setToY(1.12);
            scaleUp.setInterpolator(Interpolator.EASE_OUT);

            ScaleTransition scaleDown = new ScaleTransition(EASTER_EGG_POP_DURATION, logoView);
            scaleDown.setToX(1.0);
            scaleDown.setToY(1.0);
            scaleDown.setInterpolator(Interpolator.EASE_IN);

            RotateTransition rotate = new RotateTransition(EASTER_EGG_SPIN_DURATION, logoView);
            rotate.setByAngle(360);
            rotate.setInterpolator(Interpolator.EASE_BOTH);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(180), easterEggLabel);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            PauseTransition hold = new PauseTransition(Duration.millis(920));
            FadeTransition fadeOut = new FadeTransition(Duration.millis(340), easterEggLabel);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            SequentialTransition revealText = new SequentialTransition(fadeIn, hold, fadeOut);
            SequentialTransition pulse = new SequentialTransition(scaleUp, scaleDown);
            ParallelTransition celebration = new ParallelTransition(rotate, pulse, revealText);
            celebration.setOnFinished(finishEvent -> {
                logoView.setRotate(0.0);
                logoView.setScaleX(1.0);
                logoView.setScaleY(1.0);
                easterEggLabel.setManaged(false);
                easterEggLabel.setVisible(false);
                easterEggLabel.setOpacity(0.0);
                logoView.getProperties().remove(EASTER_EGG_RUNNING_KEY);
            });
            celebration.playFromStart();
        });
    }

    private static ImageView createImageView(String resourcePath, double fitWidth) {
        Image image = loadImage(resourcePath);
        if (image == null) {
            return null;
        }
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(fitWidth);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        return imageView;
    }

    private static Image loadImage(String resourcePath) {
        var imageUrl = AboutDialog.class.getResource(resourcePath);
        if (imageUrl == null) {
            return null;
        }
        return new Image(imageUrl.toExternalForm());
    }
}
