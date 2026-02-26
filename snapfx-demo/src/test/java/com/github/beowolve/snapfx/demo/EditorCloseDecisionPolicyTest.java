package com.github.beowolve.snapfx.demo;

import com.github.beowolve.snapfx.close.DockCloseBehavior;
import com.github.beowolve.snapfx.close.DockCloseDecision;
import com.github.beowolve.snapfx.close.DockCloseRequest;
import com.github.beowolve.snapfx.close.DockCloseSource;
import com.github.beowolve.snapfx.model.DockNode;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EditorCloseDecisionPolicyTest {

    @Test
    void testResolveReturnsDefaultWhenNoPromptIsRequired() {
        DockNode node = createNode("editor-1");
        AtomicInteger promptCalls = new AtomicInteger(0);

        EditorCloseDecisionPolicy policy = new EditorCloseDecisionPolicy(
            ignored -> false,
            ignored -> {
                promptCalls.incrementAndGet();
                return EditorCloseDecisionPolicy.SavePromptResult.CANCEL;
            },
            ignored -> false
        );

        DockCloseDecision decision = policy.resolve(createRequest(List.of(node)));

        assertEquals(DockCloseDecision.DEFAULT, decision);
        assertEquals(0, promptCalls.get());
    }

    @Test
    void testResolveCancelsWhenPromptResultIsCancel() {
        DockNode node = createNode("editor-1");
        EditorCloseDecisionPolicy policy = new EditorCloseDecisionPolicy(
            ignored -> true,
            ignored -> EditorCloseDecisionPolicy.SavePromptResult.CANCEL,
            ignored -> true
        );

        DockCloseDecision decision = policy.resolve(createRequest(List.of(node)));

        assertEquals(DockCloseDecision.CANCEL, decision);
    }

    @Test
    void testResolveCancelsWhenSaveFails() {
        DockNode node = createNode("editor-1");
        AtomicInteger saveCalls = new AtomicInteger(0);
        EditorCloseDecisionPolicy policy = new EditorCloseDecisionPolicy(
            ignored -> true,
            ignored -> EditorCloseDecisionPolicy.SavePromptResult.SAVE,
            ignored -> {
                saveCalls.incrementAndGet();
                return false;
            }
        );

        DockCloseDecision decision = policy.resolve(createRequest(List.of(node)));

        assertEquals(DockCloseDecision.CANCEL, decision);
        assertEquals(1, saveCalls.get());
    }

    @Test
    void testResolveReturnsDefaultWhenSaveSucceeds() {
        DockNode node = createNode("editor-1");
        AtomicInteger saveCalls = new AtomicInteger(0);
        EditorCloseDecisionPolicy policy = new EditorCloseDecisionPolicy(
            ignored -> true,
            ignored -> EditorCloseDecisionPolicy.SavePromptResult.SAVE,
            ignored -> {
                saveCalls.incrementAndGet();
                return true;
            }
        );

        DockCloseDecision decision = policy.resolve(createRequest(List.of(node)));

        assertEquals(DockCloseDecision.DEFAULT, decision);
        assertEquals(1, saveCalls.get());
    }

    @Test
    void testResolvePromptsOnlyMatchingNodesInMixedRequest() {
        DockNode editorNode = createNode("editor-1");
        DockNode otherNode = createNode("panel-1");
        AtomicInteger promptCalls = new AtomicInteger(0);

        EditorCloseDecisionPolicy policy = new EditorCloseDecisionPolicy(
            node -> node == editorNode,
            ignored -> {
                promptCalls.incrementAndGet();
                return EditorCloseDecisionPolicy.SavePromptResult.DONT_SAVE;
            },
            ignored -> true
        );

        DockCloseDecision decision = policy.resolve(createRequest(List.of(editorNode, otherNode)));

        assertEquals(DockCloseDecision.DEFAULT, decision);
        assertEquals(1, promptCalls.get());
    }

    private DockNode createNode(String id) {
        return new DockNode(id, new StackPane(), id);
    }

    private DockCloseRequest createRequest(List<DockNode> nodes) {
        return new DockCloseRequest(DockCloseSource.TITLE_BAR, nodes, null, DockCloseBehavior.HIDE);
    }
}
