package luka.teum.image_service.util;

import org.opencv.core.Point;

public class PointsUtil {

    private double squareLikeTolerance = 0.05;

    public static int getQuadrant(double pointX, double pointY,
                                  double centerX, double centerY) {
        boolean isLeft = pointX <= centerX;
        boolean isTop = pointY <= centerY;

        if (isLeft && isTop) return 0;
        if (!isLeft && isTop) return 1;
        if (isLeft) return 2;
        return 3;
    }

    public static Point[] createRectanglePoints(Point p1, Point p2) {
        double minX = Math.min(p1.x, p2.x);
        double maxX = Math.max(p1.x, p2.x);
        double minY = Math.min(p1.y, p2.y);
        double maxY = Math.max(p1.y, p2.y);

        return new Point[]{
                new Point(minX, minY),
                new Point(maxX, minY),
                new Point(minX, maxY),
                new Point(maxX, maxY)
        };
    }

    public Point[] findToRightPoints(Point[] points) {
        ImageUtil.validatePoints(points);

        Point p1 = null, p2 = null;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < points.length - 1; i++) {
            for (int j = i + 1; j < points.length; j++) {
                if (this.isSquareLike(points[i], points[j])) {
                    double distance = this.calculateAverageDistance(points[i], points[j]);
                    if (distance < minDistance) {
                        minDistance = distance;
                        p1 = points[i];
                        p2 = points[j];
                    }
                }
            }
        }

        if (p1 == null || p2 == null) {
            return new Point[0];
        }

        return createRectanglePoints(p1, p2);
    }

    private boolean isSquareLike(Point p1, Point p2) {
        double dx = Math.abs(p1.x - p2.x);
        double dy = Math.abs(p1.y - p2.y);

        return Math.abs(dx - dy) <= this.squareLikeTolerance * Math.max(dx, dy);
    }

    private double calculateAverageDistance(Point p1, Point p2) {
        double dx = Math.abs(p1.x - p2.x);
        double dy = Math.abs(p1.y - p2.y);
        return dx + dy;
    }

}
