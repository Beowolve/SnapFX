package com.github.beowolve.snapfx.documentation;

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
    private static final List<String> STATUS_DOCS = List.of("STATUS.md", "DONE.md", "ROADMAP.md");
    private static final List<String> MOJIBAKE_MARKERS = List.of("âœ", "ðŸ", "âš", "â†", "ï¸", "�");
    private static final List<String> STATUS_ICON_PREFIXES = List.of("- ✅ ", "- 🚧 ", "- 📋 ", "- 💡 ", "- ❌ ", "- ⚠️ ");

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
    void testStatusMdFixedRecentBulletsUseCheckmarkIcon() throws IOException {
        String content = readProjectFile("STATUS.md");
        assertAllBulletsUsePrefixes(
            "STATUS.md",
            "### Fixed (recent)",
            content,
            List.of("- ✅ ")
        );
    }

    @Test
    void testStatusMdExampleRuntimeBulletsUseCheckmarkIcon() throws IOException {
        String content = readProjectFile("STATUS.md");
        assertAllBulletsUsePrefixes(
            "STATUS.md",
            "### Example Runtime",
            content,
            List.of("- ✅ ")
        );
    }

    @Test
    void testDoneMdBuildAndDeploymentBulletsUseCheckmarkIcon() throws IOException {
        String content = readProjectFile("DONE.md");
        assertAllBulletsUsePrefixes(
            "DONE.md",
            "### Build & Deployment",
            content,
            List.of("- ✅ ")
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
        return Files.readString(Path.of(fileName), StandardCharsets.UTF_8);
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
}
