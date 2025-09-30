package luka.teum.image_service.parellel;

import luka.teum.image_service.data.RightTriangle;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TriangleParallel {

    public interface ProcessPoint {
        List<RightTriangle> processPointI(Point[] points, int i, double maxS, double minS);
    }

    public interface ProcessPointWithArray {
        void processPointI(List<RightTriangle> rightTriangles, Point[] points, int i, double maxS, double minS);
    }

    private final ProcessPoint processPointI;
    private final ProcessPointWithArray processPointIWithArray;

    public TriangleParallel() {
        this.processPointI = this::defaultProcessPointI;
        this.processPointIWithArray = this::defaultProcessPointIWithArray;
    }

    public TriangleParallel(ProcessPoint processPointI, ProcessPointWithArray processPointWithArray) {
        this.processPointI = processPointI;
        this.processPointIWithArray = processPointWithArray;
    }

    private List<RightTriangle> defaultProcessPointI(Point[] points, int i, double maxS, double minS) {
        List<RightTriangle> trianglesForI = new ArrayList<>();

        for (int j = i + 1; j < points.length - 1; j++) {
            for (int k = j + 1; k < points.length; k++) {
                RightTriangle triangle = new RightTriangle(points[i], points[j], points[k]);
                if (triangle.isRight() && triangle.getArea() < maxS && triangle.getArea() > minS) {
                    trianglesForI.add(triangle);
                }
            }
        }

        return trianglesForI;
    }

    private void defaultProcessPointIWithArray(List<RightTriangle> rightTriangles, Point[] points, int i, double maxS, double minS) {
        for (int j = i + 1; j < points.length - 1; j++) {
            for (int k = j + 1; k < points.length; k++) {
                RightTriangle triangle = new RightTriangle(points[i], points[j], points[k]);
                if (triangle.isRight() && triangle.getArea() < maxS && triangle.getArea() > minS) {
                    rightTriangles.add(triangle);
                }
            }
        }
    }

    public List<RightTriangle> outerRightTriangle(Point[] points, double maxS, double minS) {
        this.validateInput(points, maxS, minS);

        if (points.length < 20) {
            return this.outerRightTriangleSequential(points, maxS, minS);
        } else if (points.length < 100) {
            return this.outerRightTriangleParallelStream(points, maxS, minS);
        } else if (points.length < 1000) {
            return this.outerRightTriangleForkJoin(points, maxS, minS);
        } else {
            throw new IllegalArgumentException("Points size is so big");
        }
    }

    private void validateInput(Point[] points, double maxS, double minS) {
        if (points == null) {
            throw new IllegalArgumentException("Points array cannot be null");
        }
        if (maxS <= minS) {
            throw new IllegalArgumentException("maxS must be greater than minS");
        }
        if (minS < 0) {
            throw new IllegalArgumentException("minS must be non-negative");
        }
        for (int i = 0; i < points.length; i++) {
            if (points[i] == null) {
                throw new IllegalArgumentException("Point at index " + i + " is null");
            }
        }
    }

    private List<RightTriangle> outerRightTriangleSequential(Point[] points, double maxS, double minS) {
        return IntStream.range(0, points.length - 2)
                .mapToObj(i -> this.processPointI.processPointI(points, i, maxS, minS))
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private List<RightTriangle> outerRightTriangleParallelStream(Point[] points, double maxS, double minS) {
        return IntStream.range(0, points.length - 2)
                .parallel()
                .mapToObj(i -> this.processPointI.processPointI(points, i, maxS, minS))
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private List<RightTriangle> outerRightTriangleForkJoin(Point[] points, double maxS, double minS) {
        ForkJoinPool forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());

        try {
            TriangleCalculationTask task = new TriangleCalculationTask(points, 0, points.length, maxS, minS, this.processPointIWithArray);
            return forkJoinPool.invoke(task);
        } finally {
            forkJoinPool.shutdown();
        }
    }

    private static class TriangleCalculationTask extends RecursiveTask<List<RightTriangle>> {
        private final Point[] points;
        private final int start;
        private final int end;
        private final double maxS;
        private final double minS;
        private static final int THRESHOLD = 15;
        private final ProcessPointWithArray processPointIWithArray;

        public TriangleCalculationTask(Point[] points, int start, int end,
                                       double maxS, double minS, ProcessPointWithArray processPointIWithArray) {
            this.points = points;
            this.start = start;
            this.end = end;
            this.maxS = maxS;
            this.minS = minS;
            this.processPointIWithArray = processPointIWithArray;
        }

        @Override
        protected List<RightTriangle> compute() {
            int length = this.end - this.start;

            if (length <= THRESHOLD) {
                return this.computeSequentially();
            }

            int mid = this.start + length / 2;
            TriangleCalculationTask leftTask = new TriangleCalculationTask(this.points, this.start, mid, this.maxS, this.minS, this.processPointIWithArray);
            TriangleCalculationTask rightTask = new TriangleCalculationTask(this.points, mid, this.end, this.maxS, this.minS, this.processPointIWithArray);

            leftTask.fork();
            List<RightTriangle> rightResult = rightTask.compute();
            List<RightTriangle> leftResult = leftTask.join();

            leftResult.addAll(rightResult);
            return leftResult;
        }

        private List<RightTriangle> computeSequentially() {
            List<RightTriangle> result = new ArrayList<>();

            for (int i = this.start; i < this.end - 2; i++) {
                processPointIWithArray.processPointI(result, this.points, i, this.maxS, this.minS);
            }

            return result;
        }
    }
}