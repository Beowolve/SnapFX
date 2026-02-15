package com.github.beowolve.snapfx.demo;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;

class MainDemoTest {
    @Test
    void testAppIconResourceListContainsAllExpectedSizes() {
        List<String> resources = MainDemo.getAppIconResources();
        assertEquals(6, resources.size());
    }

    @Test
    void testAppIconResourcesExistInClasspath() {
        for (String resourcePath : MainDemo.getAppIconResources()) {
            assertNotNull(
                MainDemo.class.getResource(resourcePath),
                "Missing app icon resource: " + resourcePath
            );
        }
    }

    @Test
    void testCopyMenuIconReturnsIndependentImageViewCopy() {
        WritableImage image = new WritableImage(16, 16);
        ImageView source = new ImageView(image);
        source.setFitWidth(20);
        source.setFitHeight(18);
        source.setPreserveRatio(true);
        source.setSmooth(true);

        Node copiedNode = MainDemo.copyMenuIcon(source);

        ImageView copied = assertInstanceOf(ImageView.class, copiedNode);
        assertNotSame(source, copied);
        assertEquals(image, copied.getImage());
        assertEquals(20.0, copied.getFitWidth(), 0.0001);
        assertEquals(18.0, copied.getFitHeight(), 0.0001);
        assertEquals(source.isPreserveRatio(), copied.isPreserveRatio());
        assertEquals(source.isSmooth(), copied.isSmooth());
    }

    @Test
    void testCopyMenuIconReturnsNullForUnsupportedIconType() {
        Node copiedNode = MainDemo.copyMenuIcon(new javafx.scene.layout.Region());
        assertNull(copiedNode);
    }
}
