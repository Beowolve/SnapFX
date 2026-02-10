package com.github.beowolve.snapfx.demo;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Utility class for loading icons from resources.
 */
public class IconUtil {

    private IconUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Loads an icon from the resources/images/16 folder.
     *
     * @param iconName The name of the icon file (e.g., "folder.png")
     * @return ImageView with the loaded icon, or null if icon not found
     */
    public static ImageView loadIcon(String iconName) {
        try {
            var iconUrl = IconUtil.class.getResource("/images/16/" + iconName);
            if (iconUrl != null) {
                Image image = new Image(iconUrl.toExternalForm());
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(16);
                imageView.setFitHeight(16);
                return imageView;
            }
        } catch (Exception e) {
            // Icon not found, return null (no icon will be displayed)
        }
        return null;
    }
}

