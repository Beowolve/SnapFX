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
    private static final List<String> STATUS_DOCS = List.of("STATUS.md", "DONE.md", "ROADMAP.md", "CHANGELOG.md");

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
    void testStatusMdContainsNoDoneChangeLogSections() throws IOException {
        String content = readProjectFile("STATUS.md");
        assertNotContains(content, "### Fixed (recent)", "STATUS.md must not contain fixed history section");
        assertNotContains(content, "## Latest Changes", "STATUS.md must not contain changelog-style latest changes section");
        assertNotContains(content, "## Change History", "STATUS.md must not contain changelog history section");
    }

    @Test
    void testStatusMdContainsNoHistoricalDeltaSuffixes() throws IOException {
        String content = readProjectFile("STATUS.md");
        assertNotContains(content, "(was ", "STATUS.md must not include historical test-count delta suffixes");
        assertNotContains(content, "improved from ~", "STATUS.md must not include historical coverage delta suffixes");
        assertNotContains(content, "(Fixed:", "STATUS.md must not include fixed-date history markers");
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
        assertContains(statusContent, "`CHANGELOG.md` tracks versioned historical changes grouped by tags.", "STATUS.md missing changelog ownership note");
        assertContains(
            roadmapContent,
            "This roadmap lists planned work only; completed/fixed history is tracked in `CHANGELOG.md`.",
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
        assertNotContains(content, "## Version Track", "ROADMAP.md should not contain version-track section");
        assertNotContains(content, "see `STATUS.md` for fixed issues", "ROADMAP.md should route fixed history to CHANGELOG.md");
        assertContains(
            content,
            "Recalculate `Overall Progress` phase percentages and `Total Project Completion` when status mix changes.",
            "ROADMAP.md should document progress-percentage update rule"
        );
    }

    @Test
    void testReadmeContainsDocumentationMap() throws IOException {
        String content = readProjectFile("README.md");
        assertContains(content, "## Documentation Map", "README.md missing documentation map section");
        assertContains(content, "| `TESTING_POLICY.md` | Stable testing rules and quality gates |", "README.md missing testing policy map row");
        assertContains(content, "| `STATUS.md` | Current state and open issues |", "README.md missing status map row");
        assertContains(content, "| `CHANGELOG.md` | Versioned release history grouped by tags |", "README.md missing changelog map row");
        assertContains(content, "| `CONTRIBUTING.md` | Contribution workflow, branch strategy, and PR checklist |", "README.md missing contributing map row");
        assertContains(content, "| `RELEASING.md` | Maintainer release process, versioning, tags, and CI release flow |", "README.md missing releasing map row");
    }

    @Test
    void testReadmeQuickStartIsFrameworkFocused() throws IOException {
        String content = readProjectFile("README.md");
        assertContains(content, "## Quick Start", "README.md missing quick start section");
        assertContains(
            content,
            "Maven Central dependency coordinates will be documented here once the first Maven Central release is published.",
            "README.md quick start should include Maven Central placeholder note"
        );
        assertNotContains(content, "### Gradle Setup", "README.md quick start must not include generic Gradle setup template");
        assertNotContains(content, "### Module Configuration", "README.md quick start must not include generic module template");
        assertNotContains(content, "## Versioning", "README.md must not include repository-maintainer versioning workflow");
        assertNotContains(content, "## Branch Strategy", "README.md must not include repository branch workflow details");
        assertNotContains(content, "## CI/CD", "README.md must not include repository CI/CD workflow details");
        assertContains(content, "## Contributing", "README.md missing contributing section");
        assertContains(content, "see `CONTRIBUTING.md`.", "README.md should link to CONTRIBUTING.md");
        assertContains(content, "see `RELEASING.md`.", "README.md should link to RELEASING.md");
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
