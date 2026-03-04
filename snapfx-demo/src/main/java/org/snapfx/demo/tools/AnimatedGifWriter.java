package org.snapfx.demo.tools;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Utility for writing animated GIF files from buffered image frames.
 */
final class AnimatedGifWriter {
    private static final String GIF_FORMAT = "gif";
    private static final String APPLICATION_ID_NETSCAPE = "NETSCAPE";
    private static final String APPLICATION_AUTH_CODE = "2.0";
    private static final byte NETSCAPE_LOOP_SUB_BLOCK_ID = 0x1;
    private static final int MAX_DELAY_CENTISECONDS = 65_535;

    private AnimatedGifWriter() {
    }

    static void write(Path outputPath, List<BufferedImage> frames, int delayMs, boolean loopContinuously) throws IOException {
        if (outputPath == null) {
            throw new IllegalArgumentException("outputPath must not be null.");
        }
        if (frames == null || frames.isEmpty()) {
            throw new IllegalArgumentException("frames must not be null or empty.");
        }
        if (delayMs <= 0) {
            throw new IllegalArgumentException("delayMs must be > 0.");
        }

        Path parent = outputPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(GIF_FORMAT);
        if (!writers.hasNext()) {
            throw new IOException("No ImageIO GIF writer available.");
        }

        ImageWriter writer = writers.next();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(outputPath.toFile())) {
            writer.setOutput(output);
            writer.prepareWriteSequence(null);

            int baseDelayCentiseconds = toDelayCentiseconds(delayMs);
            List<EncodedFrame> encodedFrames = collapseConsecutiveFrames(frames, baseDelayCentiseconds);

            for (int index = 0; index < encodedFrames.size(); index++) {
                EncodedFrame frame = encodedFrames.get(index);
                IIOMetadata metadata = createFrameMetadata(
                    writer,
                    frame.image(),
                    frame.delayCentiseconds(),
                    loopContinuously,
                    index == 0
                );
                writer.writeToSequence(new IIOImage(frame.image(), null, metadata), (ImageWriteParam) null);
            }
            writer.endWriteSequence();
        } finally {
            writer.dispose();
        }
    }

    private static IIOMetadata createFrameMetadata(
        ImageWriter writer,
        BufferedImage frame,
        int delayCentiseconds,
        boolean loopContinuously,
        boolean includeLoopMetadata
    ) throws IOException {
        ImageTypeSpecifier imageType = ImageTypeSpecifier.createFromRenderedImage(frame);
        IIOMetadata metadata = writer.getDefaultImageMetadata(imageType, null);
        String metadataFormat = metadata.getNativeMetadataFormatName();
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metadataFormat);

        IIOMetadataNode graphicControlExtension = getOrCreateNode(root, "GraphicControlExtension");
        graphicControlExtension.setAttribute("disposalMethod", "none");
        graphicControlExtension.setAttribute("userInputFlag", "FALSE");
        graphicControlExtension.setAttribute("transparentColorFlag", "FALSE");
        graphicControlExtension.setAttribute("delayTime", Integer.toString(Math.clamp(delayCentiseconds, 1, MAX_DELAY_CENTISECONDS)));
        graphicControlExtension.setAttribute("transparentColorIndex", "0");

        if (includeLoopMetadata) {
            IIOMetadataNode applicationExtensions = getOrCreateNode(root, "ApplicationExtensions");
            IIOMetadataNode applicationExtension = new IIOMetadataNode("ApplicationExtension");
            applicationExtension.setAttribute("applicationID", APPLICATION_ID_NETSCAPE);
            applicationExtension.setAttribute("authenticationCode", APPLICATION_AUTH_CODE);
            applicationExtension.setUserObject(new byte[]{
                NETSCAPE_LOOP_SUB_BLOCK_ID,
                (byte) (loopContinuously ? 0 : 1),
                0
            });
            applicationExtensions.appendChild(applicationExtension);
        }

        metadata.setFromTree(metadataFormat, root);
        return metadata;
    }

    private static int toDelayCentiseconds(int delayMs) {
        int delayCentiseconds = Math.round(delayMs / 10.0f);
        return Math.clamp(delayCentiseconds, 1, MAX_DELAY_CENTISECONDS);
    }

    private static List<EncodedFrame> collapseConsecutiveFrames(List<BufferedImage> frames, int baseDelayCentiseconds) {
        List<EncodedFrame> collapsed = new ArrayList<>();
        BufferedImage currentFrame = frames.getFirst();
        int accumulatedDelay = baseDelayCentiseconds;

        for (int i = 1; i < frames.size(); i++) {
            BufferedImage nextFrame = frames.get(i);
            if (haveIdenticalPixels(currentFrame, nextFrame)) {
                if (accumulatedDelay <= MAX_DELAY_CENTISECONDS - baseDelayCentiseconds) {
                    accumulatedDelay += baseDelayCentiseconds;
                } else {
                    collapsed.add(new EncodedFrame(currentFrame, MAX_DELAY_CENTISECONDS));
                    accumulatedDelay = baseDelayCentiseconds;
                }
            } else {
                collapsed.add(new EncodedFrame(currentFrame, accumulatedDelay));
                currentFrame = nextFrame;
                accumulatedDelay = baseDelayCentiseconds;
            }
        }

        collapsed.add(new EncodedFrame(currentFrame, accumulatedDelay));
        return collapsed;
    }

    private static boolean haveIdenticalPixels(BufferedImage first, BufferedImage second) {
        if (first == second) {
            return true;
        }
        if (first == null || second == null) {
            return false;
        }
        if (first.getWidth() != second.getWidth() || first.getHeight() != second.getHeight()) {
            return false;
        }
        return Arrays.equals(
            first.getRGB(0, 0, first.getWidth(), first.getHeight(), null, 0, first.getWidth()),
            second.getRGB(0, 0, second.getWidth(), second.getHeight(), null, 0, second.getWidth())
        );
    }

    private static IIOMetadataNode getOrCreateNode(IIOMetadataNode root, String nodeName) {
        for (int i = 0; i < root.getLength(); i++) {
            if (root.item(i) instanceof IIOMetadataNode node && nodeName.equals(node.getNodeName())) {
                return node;
            }
        }
        IIOMetadataNode node = new IIOMetadataNode(nodeName);
        root.appendChild(node);
        return node;
    }

    private record EncodedFrame(BufferedImage image, int delayCentiseconds) {
    }
}
