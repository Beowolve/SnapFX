# AI Agent Definitions

These instructions are shared for all AI agents working on this workspace.

## Core Workflow
- Always add or update unit tests for new behavior, bug fixes, or refactors.
- Always run unit tests after code changes when possible (`./gradlew test`).
- Always update `STATUS.md`, `DONE.md`, and `ROADMAP.md` after changes.
- Update other `*.md` files if the change affects their content or accuracy.
- Keep changes scoped and avoid unrelated edits.
- After successful changes, prepare a small, focused commit (stage relevant files and propose a commit message).
- Always `git add` new files when preparing a commit.
- Before creating a commit, always show the proposed commit message to the user and get confirmation.
- Keep commits minimally mixed: one logical topic/fix per commit; avoid bundling unrelated changes.
- Default to one commit per fix/feature so changes stay clearly separated and release notes remain precise.
- For fix commits, use a multi-line commit message body with at least one explanation line per fix.
- Use git-cliff-compatible commit prefixes in the subject line (`feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `perf:`, `ci:`, `build:`, `chore:`); domain prefixes like `floating:`, `dnd:`, `ui:`, `demo:` are also allowed when they map to cliff groups.
- If a fixed collaboration rule is agreed with the user, add it to this `AGENTS.md` immediately.
- Treat `AGENTS.md` as the source of truth for all persistent collaboration rules in this workspace.
- As soon as a focused, sensible commit is ready, proactively show the proposed commit message without waiting for the user to ask.
- If `MainDemo` visuals change (layout, window composition, look and feel, styling, icons, controls), always run `./scripts/update-main-demo-preview.ps1` and include the updated `docs/images/main-demo.png`.
- Versioning is controlled by `gradle-jgitver` in `build.gradle.kts`; do not reintroduce custom version calculators in the build script.
- Use tag-driven releases (`vX.Y.Z`) as the only release source of truth; do not create tags per commit.
- Branch workflow policy: while base implementation is ongoing, work directly on `main`; once `develop` exists, integrate features into `develop` and cut release tags from `main` after merge.
- Status documentation consistency rule: in `STATUS.md`, `DONE.md`, and `ROADMAP.md`, status bullets must always include a status icon prefix (`✅`, `🚧`, `📋`, `💡`, `❌`, `⚠️`), never plain `- Added/Updated/Completed/...`.
- Markdown encoding rule: preserve UTF-8 and avoid shell text-rewrite commands that can alter Unicode; prefer `apply_patch` for markdown edits.

## DnD-Specific Rules
- Any drag-and-drop change must include model-level tests for the affected behavior.
- If UI-level behavior cannot be tested automatically, document manual verification steps.

## Coding Standards
- Follow existing code style and conventions.
- Use descriptive variable and method names.
- Add JavaDoc comments for new public methods and classes.
- Avoid introducing new dependencies unless necessary.
- Write all code in Java 21 and ensure compatibility with JavaFX 21.
- Write all code and documentation in English.
- Prefer explicit imports over fully-qualified JavaFX class names; use fully-qualified names only when unavoidable.
- Prefer `getFirst()`, `getLast()`, and `isEmpty()` over direct index access in production code when available; tests may use index access for clarity.
- Use `Math.clamp(...)` for range clamping instead of nested `Math.min(...)` and `Math.max(...)`.
- Keep function complexity at a reasonable level, aligned with SonarQube guidance (target low cognitive complexity; refactor methods approaching high complexity, e.g. around 15+ cognitive complexity).
- For UI callback assignments (for example `setOnAction`, `setOnMouseClicked`, `setOnKeyPressed`), extract logic to a named method once the lambda exceeds a single simple statement.
- Control visuals (dock/floating button icons, glyphs, close symbols) must be defined via stylesheet classes, not hardcoded vector/icon factories in Java code.
- Keep close-button visuals consistent with tab close styling (same glyph family/path unless explicitly changed by the user).
