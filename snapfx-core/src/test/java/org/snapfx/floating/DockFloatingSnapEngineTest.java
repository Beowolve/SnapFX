package org.snapfx.floating;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DockFloatingSnapEngineTest {

    @Test
    void testSnapToLeftAndTopEdgesWithinDistance() {
        DockFloatingSnapEngine engine = new DockFloatingSnapEngine();
        List<Double> xCandidates = new ArrayList<>();
        List<Double> yCandidates = new ArrayList<>();
        engine.addAlignmentCandidates(
            List.of(new Rectangle2D(0.0, 0.0, 1920.0, 1080.0)),
            200.0,
            120.0,
            xCandidates,
            yCandidates
        );

        Point2D snapped = engine.snap(
            8.0,
            7.0,
            12.0,
            xCandidates,
            yCandidates
        );

        assertEquals(0.0, snapped.getX(), 0.0001);
        assertEquals(0.0, snapped.getY(), 0.0001);
    }

    @Test
    void testSnapToRightAndBottomEdgesWithinDistance() {
        DockFloatingSnapEngine engine = new DockFloatingSnapEngine();
        List<Double> xCandidates = new ArrayList<>();
        List<Double> yCandidates = new ArrayList<>();
        engine.addAlignmentCandidates(
            List.of(new Rectangle2D(0.0, 0.0, 1920.0, 1080.0)),
            200.0,
            120.0,
            xCandidates,
            yCandidates
        );

        Point2D snapped = engine.snap(
            1711.0,
            952.0,
            12.0,
            xCandidates,
            yCandidates
        );

        assertEquals(1720.0, snapped.getX(), 0.0001);
        assertEquals(960.0, snapped.getY(), 0.0001);
    }

    @Test
    void testNoSnapWhenOutsideDistance() {
        DockFloatingSnapEngine engine = new DockFloatingSnapEngine();
        List<Double> xCandidates = new ArrayList<>();
        List<Double> yCandidates = new ArrayList<>();
        engine.addAlignmentCandidates(
            List.of(new Rectangle2D(0.0, 0.0, 1920.0, 1080.0)),
            200.0,
            120.0,
            xCandidates,
            yCandidates
        );

        Point2D snapped = engine.snap(
            20.0,
            20.0,
            12.0,
            xCandidates,
            yCandidates
        );

        assertEquals(20.0, snapped.getX(), 0.0001);
        assertEquals(20.0, snapped.getY(), 0.0001);
    }

    @Test
    void testNearestCandidateWinsAcrossMultipleTargets() {
        DockFloatingSnapEngine engine = new DockFloatingSnapEngine();
        List<Double> xCandidates = new ArrayList<>();
        List<Double> yCandidates = new ArrayList<>();
        engine.addAlignmentCandidates(
            List.of(
                new Rectangle2D(0.0, 0.0, 1920.0, 1080.0),
                new Rectangle2D(300.0, 50.0, 800.0, 600.0)
            ),
            200.0,
            120.0,
            xCandidates,
            yCandidates
        );

        Point2D snapped = engine.snap(
            289.0,
            100.0,
            15.0,
            xCandidates,
            yCandidates
        );

        assertEquals(300.0, snapped.getX(), 0.0001);
        assertEquals(100.0, snapped.getY(), 0.0001);
    }

    @Test
    void testOverlapAwareCandidatesRequirePerpendicularOverlap() {
        DockFloatingSnapEngine engine = new DockFloatingSnapEngine();
        List<Double> xCandidates = new ArrayList<>();
        List<Double> yCandidates = new ArrayList<>();
        engine.addOverlapAwareCandidates(
            List.of(new Rectangle2D(300.0, 800.0, 640.0, 420.0)),
            289.0,
            80.0,
            640.0,
            420.0,
            15.0,
            xCandidates,
            yCandidates
        );

        Point2D snapped = engine.snap(289.0, 80.0, 15.0, xCandidates, yCandidates);
        assertEquals(289.0, snapped.getX(), 0.0001);
        assertEquals(80.0, snapped.getY(), 0.0001);
    }

    @Test
    void testInferShadowInsetCompensatesBorder() {
        DockFloatingSnapEngine engine = new DockFloatingSnapEngine();

        assertEquals(5.0, engine.inferShadowInset(6.0, 6.0, 6.0), 0.0001);
        assertEquals(0.0, engine.inferShadowInset(1.0, 1.0, 1.0), 0.0001);
        assertEquals(0.0, engine.inferShadowInset(Double.NaN, 6.0, 6.0), 0.0001);
    }
}
