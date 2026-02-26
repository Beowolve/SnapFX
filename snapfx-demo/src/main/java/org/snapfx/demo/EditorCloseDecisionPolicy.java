package org.snapfx.demo;

import org.snapfx.close.DockCloseDecision;
import org.snapfx.close.DockCloseRequest;
import org.snapfx.model.DockNode;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Decides whether a close request should proceed for editor-like nodes.
 * This class is UI-agnostic and can be tested independently from JavaFX dialogs.
 */
final class EditorCloseDecisionPolicy {
    enum SavePromptResult {
        SAVE,
        DONT_SAVE,
        CANCEL
    }

    private final Predicate<DockNode> promptRequired;
    private final Function<DockNode, SavePromptResult> promptHandler;
    private final Predicate<DockNode> saveHandler;

    EditorCloseDecisionPolicy(
        Predicate<DockNode> promptRequired,
        Function<DockNode, SavePromptResult> promptHandler,
        Predicate<DockNode> saveHandler
    ) {
        this.promptRequired = promptRequired == null ? node -> false : promptRequired;
        this.promptHandler = promptHandler == null ? node -> SavePromptResult.CANCEL : promptHandler;
        this.saveHandler = saveHandler == null ? node -> true : saveHandler;
    }

    DockCloseDecision resolve(DockCloseRequest request) {
        if (request == null || request.nodes().isEmpty()) {
            return DockCloseDecision.DEFAULT;
        }

        for (DockNode node : request.nodes()) {
            if (!promptRequired.test(node)) {
                continue;
            }

            SavePromptResult promptResult = Objects.requireNonNullElse(
                promptHandler.apply(node),
                SavePromptResult.CANCEL
            );

            if (promptResult == SavePromptResult.CANCEL) {
                return DockCloseDecision.CANCEL;
            }

            if (promptResult == SavePromptResult.SAVE && !saveHandler.test(node)) {
                return DockCloseDecision.CANCEL;
            }
        }
        return DockCloseDecision.DEFAULT;
    }
}
