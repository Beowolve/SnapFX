package org.snapfx.demo.tools;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimatedGifWriterTest {
    @Test
    void writeCreatesAnimatedGifWithExpectedHeaderAndFrameCount() throws IOException {
        Path tempGif = Files.createTempFile("snapfx-main-demo-", ".gif");
        try {
            BufferedImage red = solidArgbImage(36, 24, 0xFFFF0000);
            BufferedImage blue = solidArgbImage(36, 24, 0xFF0000FF);

            AnimatedGifWriter.write(tempGif, List.of(red, blue), 90, true);

            assertTrue(Files.exists(tempGif));
            assertTrue(Files.size(tempGif) > 0);

            byte[] bytes = Files.readAllBytes(tempGif);
            assertTrue(bytes.length >= 6);
            String header = new String(bytes, 0, 6, StandardCharsets.US_ASCII);
            assertTrue("GIF89a".equals(header) || "GIF87a".equals(header));

            try (ImageInputStream input = ImageIO.createImageInputStream(tempGif.toFile())) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
                assertTrue(readers.hasNext(), "No ImageIO reader found for generated gif.");
                ImageReader reader = readers.next();
                reader.setInput(input);
                assertEquals(2, reader.getNumImages(true));
                reader.dispose();
            }
        } finally {
            Files.deleteIfExists(tempGif);
        }
    }

    @Test
    void resolveThemeSwitchSequenceReturnsAlternateAndReturnsToCurrentTheme() {
        List<String> sequence = MainDemoGifGenerator.resolveThemeSwitchSequence(List.of("Light", "Dark"), "Light");
        assertEquals(List.of("Dark", "Light"), sequence);
    }

    @Test
    void resolveThemeSwitchSequenceFallsBackWhenCurrentThemeUnknown() {
        List<String> sequence = MainDemoGifGenerator.resolveThemeSwitchSequence(List.of("Light", "Dark"), "Unknown");
        assertEquals(List.of("Dark", "Light"), sequence);
    }

    @Test
    void writeCollapsesConsecutiveDuplicateFramesAndAccumulatesDelay() throws IOException {
        Path tempGif = Files.createTempFile("snapfx-main-demo-duplicates-", ".gif");
        try {
            BufferedImage red = solidArgbImage(32, 20, 0xFFFF0000);
            BufferedImage blue = solidArgbImage(32, 20, 0xFF0000FF);

            AnimatedGifWriter.write(tempGif, List.of(red, red, blue), 90, true);

            try (ImageInputStream input = ImageIO.createImageInputStream(tempGif.toFile())) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
                assertTrue(readers.hasNext(), "No ImageIO reader found for generated gif.");
                ImageReader reader = readers.next();
                reader.setInput(input);

                assertEquals(2, reader.getNumImages(true));
                assertEquals(18, readDelayTime(reader, 0));
                assertEquals(9, readDelayTime(reader, 1));
                reader.dispose();
            }
        } finally {
            Files.deleteIfExists(tempGif);
        }
    }

    private BufferedImage solidArgbImage(int width, int height, int argbColor) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, argbColor);
            }
        }
        return image;
    }

    private int readDelayTime(ImageReader reader, int imageIndex) throws IOException {
        IIOMetadata metadata = reader.getImageMetadata(imageIndex);
        String metadataFormatName = metadata.getNativeMetadataFormatName();
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metadataFormatName);
        IIOMetadataNode graphicControl = findNode(root, "GraphicControlExtension");
        assertNotNull(graphicControl, "GraphicControlExtension metadata node missing.");
        return Integer.parseInt(graphicControl.getAttribute("delayTime"));
    }

    private IIOMetadataNode findNode(IIOMetadataNode node, String name) {
        if (name.equals(node.getNodeName())) {
            return node;
        }
        for (int i = 0; i < node.getLength(); i++) {
            if (node.item(i) instanceof IIOMetadataNode child) {
                IIOMetadataNode found = findNode(child, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
