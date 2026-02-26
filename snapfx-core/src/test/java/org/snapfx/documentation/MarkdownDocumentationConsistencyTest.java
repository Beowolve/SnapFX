package org.snapfx.documentation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownDocumentationConsistencyTest {
    private static final List<String> STATUS_DOCS = List.of("STATUS.md", "DONE.md", "ROADMAP.md", "CHANGELOG.md");
    private static final Path PROJECT_ROOT = locateProjectRoot();

    private static final String ICON_CHECKMARK = "\u2705"; // ✅
    private static final String ICON_IN_PROGRESS = "\uD83D\uDEA7"; // 🚧
    private static final String ICON_PLANNED = "\uD83D\uDCCB"; // 📋
    private static final String ICON_PROPOSED = "\uD83D\uDCA1"; // 💡
    private static final String ICON_BLOCKED = "\u274C"; // ❌
    private static final String ICON_WARNING = "\u26A0\uFE0F"; // ⚠️

    private static final List<String> MOJIBAKE_MARKERS = List.of(
        "\u00E2\u0153",  // "âœ"
        "\u00F0\u0178",  // "ðŸ"
        "\u00E2\u0161",  // "âš"
        "\u00E2\u2020",  // "â†"
        "\u00C3\u00A2",  // "Ã¢"
        "\u00C3\u00B0",  // "Ã°"
        "\u00C3\u00AF",  // "Ã¯"
        "\u00EF\u00BF\u00BD", // "ï¿½"
        "\uFFFD"
    );

    private static final List<String> STATUS_ICON_PREFIXES = List.of(
        "- " + ICON_CHECKMARK + " ",
        "- " + ICON_IN_PROGRESS + " ",
        "- " + ICON_PLANNED + " ",
        "- " + ICON_PROPOSED + " ",
        "- " + ICON_BLOCKED + " ",
        "- " + ICON_WARNING + " "
    );

    private static final List<String> CHECKMARK_PREFIXES = List.of("- " + ICON_CHECKMARK + " ");
    private static final List<String> WARNING_PREFIXES = List.of("- " + ICON_WARNING + " ");

    @Test
    void testStatusDocumentationContainsNoKnownMojibakeMarkers() throws IOException {
        for (String fileName : STATUS_DOCS) {
            String content = readProjectFile(fileName);
            for (String marker : MOJIBAKE_MARKERS) {
                assertFalse(
                    content.contains(marker),
                    () -> fileName + " contains mojibake marker: " + marker
                );
            }
        }
    }

    @Test
    void testStatusMdOpenBulletsUseWarningIcon() throws IOException {
        String content = readProjectFile("STATUS.md");
        assertAllBulletsUsePrefixes(
            "STATUS.md",
            "### Open",
            content,
            WARNING_PREFIXES
        );
    }

    @Test
    void testDoneMdBuildAndDeploymentBulletsUseCheckmarkIcon() throws IOException {
        String content = readProjectFile("DONE.md");
        assertAllBulletsUsePrefixes(
            "DONE.md",
            "### Build & Deployment",
            content,
            CHECKMARK_PREFIXES
        );
    }

    @Test
    void testRoadmapMdToolingBulletsUseStatusIcons() throws IOException {
        String content = readProjectFile("ROADMAP.md");
        assertAllBulletsUsePrefixes(
            "ROADMAP.md",
            "### 7.3 Tooling",
            content,
            STATUS_ICON_PREFIXES
        );
    }

    private String readProjectFile(String fileName) throws IOException {
        return Files.readString(PROJECT_ROOT.resolve(fileName), StandardCharsets.UTF_8);
    }

    private void assertAllBulletsUsePrefixes(
        String fileName,
        String sectionHeading,
        String content,
        List<String> allowedPrefixes
    ) {
        List<String> bullets = findSectionBullets(content, sectionHeading);
        assertFalse(
            bullets.isEmpty(),
            () -> fileName + " section has no bullet entries: " + sectionHeading
        );

        for (String bullet : bullets) {
            boolean hasAllowedPrefix = allowedPrefixes.stream().anyMatch(bullet::startsWith);
            assertTrue(
                hasAllowedPrefix,
                () -> fileName + " section '" + sectionHeading + "' contains bullet without allowed icon prefix: " + bullet
            );
        }
    }

    private List<String> findSectionBullets(String content, String sectionHeading) {
        List<String> lines = content.lines().toList();
        int headingIndex = lines.indexOf(sectionHeading);
        assertTrue(headingIndex >= 0, () -> "Section not found: " + sectionHeading);

        List<String> bullets = new ArrayList<>();
        for (int i = headingIndex + 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("## ") || line.startsWith("### ")) {
                break;
            }
            if (line.startsWith("- ")) {
                bullets.add(line);
            }
        }
        return bullets;
    }

    private static Path locateProjectRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.exists(current.resolve("settings.gradle.kts"))
                && Files.exists(current.resolve("README.md"))
                && Files.exists(current.resolve("ROADMAP.md"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Could not locate repository root for markdown documentation tests.");
    }
}
