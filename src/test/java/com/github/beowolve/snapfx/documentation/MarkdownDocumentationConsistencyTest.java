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
            CHECKMARK_PREFIXES
        );
    }

    @Test
    void testStatusMdExampleRuntimeBulletsUseCheckmarkIcon() throws IOException {
        String content = readProjectFile("STATUS.md");
        assertAllBulletsUsePrefixes(
            "STATUS.md",
            "### Example Runtime",
            content,
            CHECKMARK_PREFIXES
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

    @Test
    void testTestingPolicyHasRulesOnlyStructure() throws IOException {
        String content = readProjectFile("TESTING_POLICY.md");
        assertContains(content, "## Scope", "TESTING_POLICY.md missing Scope section");
        assertContains(content, "## Mandatory Rules", "TESTING_POLICY.md missing Mandatory Rules section");
        assertContains(content, "## Quality Gates", "TESTING_POLICY.md missing Quality Gates section");
        assertContains(content, "## Manual Verification (When Needed)", "TESTING_POLICY.md missing Manual Verification section");
        assertContains(content, "## Test Commands", "TESTING_POLICY.md missing Test Commands section");
        assertContains(content, "## Pull Request Checklist", "TESTING_POLICY.md missing Pull Request Checklist section");
        assertContains(content, "## Ownership", "TESTING_POLICY.md missing Ownership section");
    }

    @Test
    void testTestingPolicyContainsNoTemporalStatisticsSections() throws IOException {
        String content = readProjectFile("TESTING_POLICY.md");
        assertNotContains(content, "## Current Test Statistics", "TESTING_POLICY.md must not contain temporal test statistics");
        assertNotContains(content, "### Test Distribution", "TESTING_POLICY.md must not contain temporal test distribution");
        assertNotContains(content, "### Regression Test Coverage", "TESTING_POLICY.md must not contain temporal regression coverage");
        assertNotContains(content, "## Benefits of This Policy", "TESTING_POLICY.md should remain rule-focused");
        assertNotContains(content, "**As of ", "TESTING_POLICY.md must not contain date-scoped status snapshots");
    }

    @Test
    void testDocumentationScopeSeparationIsExplicit() throws IOException {
        String statusContent = readProjectFile("STATUS.md");
        String roadmapContent = readProjectFile("ROADMAP.md");
        String doneContent = readProjectFile("DONE.md");

        assertContains(statusContent, "## Documentation Scope", "STATUS.md missing documentation scope section");
        assertContains(statusContent, "`ROADMAP.md` tracks planned work only.", "STATUS.md missing planned-work ownership note");
        assertContains(
            roadmapContent,
            "This roadmap lists planned work only; fixed issues are tracked in `STATUS.md`.",
            "ROADMAP.md missing planned-only ownership statement"
        );
        assertContains(
            doneContent,
            "`TESTING_POLICY.md` - Stable testing rules and quality gates (policy-only)",
            "DONE.md missing policy-only testing policy note"
        );
    }

    @Test
    void testRoadmapContainsNoRecentChangesSections() throws IOException {
        String content = readProjectFile("ROADMAP.md");
        assertNotContains(content, "## Recent Changes", "ROADMAP.md should not contain recent change logs");
    }

    @Test
    void testReadmeContainsDocumentationMap() throws IOException {
        String content = readProjectFile("README.md");
        assertContains(content, "## Documentation Map", "README.md missing documentation map section");
        assertContains(content, "| `TESTING_POLICY.md` | Stable testing rules and quality gates |", "README.md missing testing policy map row");
        assertContains(content, "| `STATUS.md` | Current state, open issues, and latest changes |", "README.md missing status map row");
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

    private void assertContains(String content, String expected, String failureMessage) {
        assertTrue(content.contains(expected), failureMessage);
    }

    private void assertNotContains(String content, String disallowed, String failureMessage) {
        assertFalse(content.contains(disallowed), failureMessage);
    }
}
