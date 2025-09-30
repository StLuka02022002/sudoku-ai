package luka.teum.image_service.data;

import org.opencv.core.Point;

public class RightTriangle implements Comparable<RightTriangle> {

    private Point p1, p2, p3;
    private Point squarePoint;
    private double x1, x2, y1, y2;
    private double area;
    private boolean isRight, isSquare;

    public RightTriangle(Point p1, Point p2, Point p3) {
        if (p1 == null || p2 == null || p3 == null) {
            throw new IllegalArgumentException("All points must be non-null");
        }
        init(p1, p2, p3);
    }

    private void init(Point p1, Point p2, Point p3) {
        this.x1 = Math.min(p1.x, Math.min(p2.x, p3.x));
        this.x2 = Math.max(p1.x, Math.max(p2.x, p3.x));
        this.y1 = Math.min(p1.y, Math.min(p2.y, p3.y));
        this.y2 = Math.max(p1.y, Math.max(p2.y, p3.y));
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
        this.squarePoint = new Point(x2, y2);
        double d1 = x2 - x1;
        double d2 = y2 - y1;
        this.isSquare = Math.abs(d1 - d2) <= 0.05 * d1;
        this.isRight = this.isRight(p1, p2, p3);
        this.area = d1 * d2 / 2;
    }

    private boolean isRight(Point p1, Point p2, Point p3) {
        Line line1 = new Line(p1, p2);
        Line line2 = new Line(p2, p3);
        Line line3 = new Line(p3, p1);

        boolean isHorizontal1 = line1.isHorizontal() && !line1.isVertical();
        boolean isHorizontal2 = line2.isHorizontal() && !line2.isVertical();
        boolean isHorizontal3 = line2.isHorizontal() && !line3.isVertical();
        boolean isVertical1 = !line1.isHorizontal() && line1.isVertical();
        boolean isVertical2 = !line2.isHorizontal() && line2.isVertical();
        boolean isVertical3 = !line2.isHorizontal() && line3.isVertical();

        boolean isHorizontal = isHorizontal1 && !isHorizontal2 || isHorizontal2 && !isHorizontal3 || isHorizontal3;
        boolean isVertical = isVertical1 && !isVertical2 || isVertical2 && !isVertical3 || isVertical3;

        return isHorizontal && isVertical;
    }

    public boolean isSquare() {
        return this.isSquare;
    }

    public Point[] toPoints() {
        return new Point[]{
                new Point(x1, y1),
                new Point(x2, y1),
                new Point(x1, y2),
                this.squarePoint,
        };
    }

    public boolean isRight() {
        return this.isRight;
    }

    public Point getP1() {
        return p1;
    }

    public Point getP2() {
        return p2;
    }

    public Point getP3() {
        return p3;
    }

    public Point getSquarePoint() {
        return squarePoint;
    }

    public double getX1() {
        return x1;
    }

    public double getX2() {
        return x2;
    }

    public double getY1() {
        return y1;
    }

    public double getY2() {
        return y2;
    }

    public double getArea() {
        return this.area;
    }

    @Override
    public String toString() {
        return "data.RightTriangle{" +
                "p1=" + p1 +
                ", p2=" + p2 +
                ", p3=" + p3 +
                ", isRight=" + isRight +
                ", area=" + area +
                '}';
    }

    @Override
    public int compareTo(RightTriangle other) {
        return Double.compare(this.area, other.area);
    }

    private static class Line {
        private final Point p1, p2;
        private boolean horizontal, vertical;

        public Line(Point p1, Point p2) {
            this.p1 = p1;
            this.p2 = p2;
            init();
        }

        private void init() {
            this.horizontal = Math.abs(p1.y - p2.y) < 5;
            this.vertical = Math.abs(p1.x - p2.x) < 5;
        }

        public Point getP1() {
            return p1;
        }

        public Point getP2() {
            return p2;
        }

        public boolean isHorizontal() {
            return horizontal;
        }

        public boolean isVertical() {
            return vertical;
        }
    }
}
