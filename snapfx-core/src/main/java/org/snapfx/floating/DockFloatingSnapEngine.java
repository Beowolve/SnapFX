package org.snapfx.floating;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;

import java.util.List;

final class DockFloatingSnapEngine {

    Point2D snap(
        double requestedX,
        double requestedY,
        double snapDistance,
        List<Double> xCandidates,
        List<Double> yCandidates
    ) {
        if (!Double.isFinite(requestedX)
            || !Double.isFinite(requestedY)
            || !Double.isFinite(snapDistance)
            || snapDistance < 0.0) {
            return new Point2D(requestedX, requestedY);
        }
        return new Point2D(
            snapAxis(requestedX, snapDistance, xCandidates),
            snapAxis(requestedY, snapDistance, yCandidates)
        );
    }

    double snapAxis(double requestedValue, double snapDistance, List<Double> candidates) {
        if (!Double.isFinite(requestedValue)
            || !Double.isFinite(snapDistance)
            || snapDistance < 0.0) {
            return requestedValue;
        }
        return resolveAxis(requestedValue, snapDistance, candidates).value();
    }

    void addAlignmentCandidates(
        List<Rectangle2D> targetBounds,
        double windowWidth,
        double windowHeight,
        List<Double> xCandidates,
        List<Double> yCandidates
    ) {
        if (targetBounds == null
            || targetBounds.isEmpty()
            || !Double.isFinite(windowWidth)
            || !Double.isFinite(windowHeight)
            || windowWidth <= 0.0
            || windowHeight <= 0.0
            || xCandidates == null
            || yCandidates == null) {
            return;
        }
        for (Rectangle2D bounds : targetBounds) {
            addEdgeAlignmentCandidates(bounds, windowWidth, windowHeight, xCandidates, yCandidates);
        }
    }

    void addEdgeCandidates(
        List<Rectangle2D> targetBounds,
        List<Double> leftEdgeCandidates,
        List<Double> rightEdgeCandidates,
        List<Double> topEdgeCandidates,
        List<Double> bottomEdgeCandidates
    ) {
        if (targetBounds == null
            || targetBounds.isEmpty()
            || leftEdgeCandidates == null
            || rightEdgeCandidates == null
            || topEdgeCandidates == null
            || bottomEdgeCandidates == null) {
            return;
        }
        for (Rectangle2D bounds : targetBounds) {
            if (bounds == null) {
                continue;
            }
            leftEdgeCandidates.add(bounds.getMinX());
            rightEdgeCandidates.add(bounds.getMaxX());
            topEdgeCandidates.add(bounds.getMinY());
            bottomEdgeCandidates.add(bounds.getMaxY());
        }
    }

    void addOverlapAwareCandidates(
        List<Rectangle2D> targetBounds,
        double requestedX,
        double requestedY,
        double windowWidth,
        double windowHeight,
        double tolerance,
        List<Double> xCandidates,
        List<Double> yCandidates
    ) {
        if (targetBounds == null
            || targetBounds.isEmpty()
            || !Double.isFinite(requestedX)
            || !Double.isFinite(requestedY)
            || !Double.isFinite(windowWidth)
            || !Double.isFinite(windowHeight)
            || windowWidth <= 0.0
            || windowHeight <= 0.0
            || !Double.isFinite(tolerance)
            || tolerance < 0.0
            || xCandidates == null
            || yCandidates == null) {
            return;
        }

        for (Rectangle2D bounds : targetBounds) {
            if (bounds == null) {
                continue;
            }
            double targetMinX = bounds.getMinX();
            double targetMaxX = bounds.getMaxX();
            double targetMinY = bounds.getMinY();
            double targetMaxY = bounds.getMaxY();

            boolean verticalOverlap = rangesOverlap(
                requestedY,
                requestedY + windowHeight,
                targetMinY,
                targetMaxY,
                tolerance
            );
            if (verticalOverlap) {
                xCandidates.add(targetMinX);
                xCandidates.add(targetMaxX - windowWidth);
                xCandidates.add(targetMinX - windowWidth);
                xCandidates.add(targetMaxX);
            }

            boolean horizontalOverlap = rangesOverlap(
                requestedX,
                requestedX + windowWidth,
                targetMinX,
                targetMaxX,
                tolerance
            );
            if (horizontalOverlap) {
                yCandidates.add(targetMinY);
                yCandidates.add(targetMaxY - windowHeight);
                yCandidates.add(targetMinY - windowHeight);
                yCandidates.add(targetMaxY);
            }
        }
    }

    void addOverlapAwareEdgeCandidates(
        List<Rectangle2D> targetBounds,
        double requestedX,
        double requestedY,
        double windowWidth,
        double windowHeight,
        double tolerance,
        List<Double> leftEdgeCandidates,
        List<Double> rightEdgeCandidates,
        List<Double> topEdgeCandidates,
        List<Double> bottomEdgeCandidates
    ) {
        if (targetBounds == null
            || targetBounds.isEmpty()
            || !Double.isFinite(requestedX)
            || !Double.isFinite(requestedY)
            || !Double.isFinite(windowWidth)
            || !Double.isFinite(windowHeight)
            || windowWidth <= 0.0
            || windowHeight <= 0.0
            || !Double.isFinite(tolerance)
            || tolerance < 0.0
            || leftEdgeCandidates == null
            || rightEdgeCandidates == null
            || topEdgeCandidates == null
            || bottomEdgeCandidates == null) {
            return;
        }

        for (Rectangle2D bounds : targetBounds) {
            if (bounds == null) {
                continue;
            }
            double targetMinX = bounds.getMinX();
            double targetMaxX = bounds.getMaxX();
            double targetMinY = bounds.getMinY();
            double targetMaxY = bounds.getMaxY();

            boolean verticalOverlap = rangesOverlap(
                requestedY,
                requestedY + windowHeight,
                targetMinY,
                targetMaxY,
                tolerance
            );
            if (verticalOverlap) {
                leftEdgeCandidates.add(targetMinX);
                leftEdgeCandidates.add(targetMaxX);
                rightEdgeCandidates.add(targetMinX);
                rightEdgeCandidates.add(targetMaxX);
            }

            boolean horizontalOverlap = rangesOverlap(
                requestedX,
                requestedX + windowWidth,
                targetMinX,
                targetMaxX,
                tolerance
            );
            if (horizontalOverlap) {
                topEdgeCandidates.add(targetMinY);
                topEdgeCandidates.add(targetMaxY);
                bottomEdgeCandidates.add(targetMinY);
                bottomEdgeCandidates.add(targetMaxY);
            }
        }
    }

    double inferShadowInset(double leftInset, double rightInset, double bottomInset) {
        double smallestInset = Math.min(leftInset, Math.min(rightInset, bottomInset));
        if (!Double.isFinite(smallestInset) || smallestInset <= 1.0) {
            return 0.0;
        }
        return smallestInset - 1.0;
    }

    private SnapAxisResult resolveAxis(double requestedValue, double snapDistance, List<Double> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return new SnapAxisResult(requestedValue, Double.POSITIVE_INFINITY);
        }
        SnapAxisResult best = new SnapAxisResult(requestedValue, Double.POSITIVE_INFINITY);
        for (Double candidate : candidates) {
            best = chooseBetterCandidate(best, requestedValue, snapDistance, candidate);
        }
        return best;
    }

    private SnapAxisResult chooseBetterCandidate(
        SnapAxisResult currentBest,
        double requestedValue,
        double snapDistance,
        Double candidate
    ) {
        if (candidate == null) {
            return currentBest;
        }
        return chooseBetterCandidate(currentBest, requestedValue, snapDistance, candidate.doubleValue());
    }

    private SnapAxisResult chooseBetterCandidate(
        SnapAxisResult currentBest,
        double requestedValue,
        double snapDistance,
        double candidate
    ) {
        if (!Double.isFinite(candidate)) {
            return currentBest;
        }
        double distance = Math.abs(requestedValue - candidate);
        if (distance > snapDistance || distance >= currentBest.distance()) {
            return currentBest;
        }
        return new SnapAxisResult(candidate, distance);
    }

    private void addEdgeAlignmentCandidates(
        Rectangle2D bounds,
        double windowWidth,
        double windowHeight,
        List<Double> xCandidates,
        List<Double> yCandidates
    ) {
        if (bounds == null) {
            return;
        }
        xCandidates.add(bounds.getMinX());
        xCandidates.add(bounds.getMaxX() - windowWidth);
        yCandidates.add(bounds.getMinY());
        yCandidates.add(bounds.getMaxY() - windowHeight);
    }

    private boolean rangesOverlap(
        double firstStart,
        double firstEnd,
        double secondStart,
        double secondEnd,
        double tolerance
    ) {
        return firstEnd >= secondStart - tolerance && secondEnd >= firstStart - tolerance;
    }

    private record SnapAxisResult(double value, double distance) {
    }
}
