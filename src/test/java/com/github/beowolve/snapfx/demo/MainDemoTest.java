package com.github.beowolve.snapfx.demo;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.StackPane;

import java.util.concurrent.atomic.AtomicInteger;

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

    @Test
    void testConfigureDemoShortcutsRegistersF11Accelerator() {
        Scene scene = new Scene(new StackPane(), 300, 200);
        AtomicInteger invocationCounter = new AtomicInteger(0);

        MainDemo.configureDemoShortcuts(scene, invocationCounter::incrementAndGet);

        KeyCodeCombination f11 = new KeyCodeCombination(KeyCode.F11);
        Runnable f11Action = scene.getAccelerators().get(f11);
        assertNotNull(f11Action);

        f11Action.run();
        assertEquals(1, invocationCounter.get());
        assertTrue(scene.getAccelerators().containsKey(f11));
    }
}
