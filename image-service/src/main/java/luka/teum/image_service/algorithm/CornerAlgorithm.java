package luka.teum.image_service.algorithm;

import lombok.Setter;
import luka.teum.image_service.data.RightTriangle;
import luka.teum.image_service.parellel.TriangleParallel;
import luka.teum.image_service.util.ImageUtil;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CornerAlgorithm implements ImageAlgorithm {

    public static final int V1 = 0;
    public static final int V2 = 1;
    public static final int V3 = 2;
    public static final int V4 = 3;

    private final ImageUtil imageUtil;
    private final TriangleParallel triangleParallel;
    @Setter
    private PrepareProcess prepareProcess;
    private double contrast = 3.0;
    private double thresh = 225.0;
    private double maxVal = 255.0;
    private int maxCorners = 500;
    double qualityLevel = 0.01;
    double minDistance = 10;
    double maxS = Double.MAX_VALUE;
    double minS = 0;
    double minSV2 = 80000;

    public CornerAlgorithm(ImageUtil imageUtil, TriangleParallel triangleParallel) {
        if (imageUtil == null) {
            throw new IllegalArgumentException("util.ImageUtil cannot be null");
        }
        if (triangleParallel == null) {
            throw new IllegalArgumentException("TriangleParallel cannot be null");
        }
        this.imageUtil = imageUtil;
        this.triangleParallel = triangleParallel;
    }

    public CornerAlgorithm() {
        this.imageUtil = new ImageUtil();
        this.triangleParallel = new TriangleParallel();
    }


    @Override
    public Point[] algorithm(Mat data) {
        ImageUtil.validateImage(data);

        Mat image = data.clone();
        try {
            return this.algorithmV1(data);
        } finally {
            image.release();
        }
    }

    private Point[] algorithmV1(Mat data) {
        Mat prepareImage = this.prepareImage(data);
        try {
            List<RightTriangle> rightTriangles = this.outerRightTriangle(prepareImage);

            if (rightTriangles.isEmpty()) {
                return new Point[0];
            }

            RightTriangle max = this.getRightTriangleWithMaxS(rightTriangles);

            return max.toPoints();
        } finally {
            prepareImage.release();
        }

    }

    private Point[] algorithmV2(Mat data) {
        Mat prepareImage = this.prepareImage(data);
        try {
            List<RightTriangle> rightTriangles = this.outerRightTriangle(prepareImage);
            if (rightTriangles.isEmpty()) {
                return new Point[0];
            }

            Point[] points = this.getUnicuePoint(rightTriangles, false);

            rightTriangles = triangleParallel.outerRightTriangle(points, Double.MAX_VALUE, this.minSV2);

            if (rightTriangles.isEmpty()) {
                return new Point[0];
            }

            RightTriangle max = this.getRightTriangleWithMaxS(rightTriangles);

            return max.toPoints();
        } finally {
            prepareImage.release();
        }
    }

    @Override
    public Point[] algorithm(Mat data, int typeVersion) {
        Mat image = data.clone();
        switch (typeVersion) {
            case V1 -> {
                contrast = 3.0;
                thresh = 225.0;
                maxVal = 255.0;
                maxCorners = 500;
                qualityLevel = 0.01;
                minDistance = 10;
                maxS = Double.MAX_VALUE;
                minS = 60000;
                return algorithmV1(image);
            }
            case V2 -> {
                contrast = 3.0;
                thresh = 225.0;
                maxVal = 255.0;
                maxCorners = 500;
                qualityLevel = 0.01;
                minDistance = 10;
                maxS = 10000;
                minS = 1000;
                minSV2 = 80000;
                return algorithmV2(image);
            }
        }
        return new Point[0];
    }

    private Mat prepareImage(Mat data) {
        Mat image = this.imageUtil.contrastEnhancement(data, this.contrast);
        Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2GRAY);
        Imgproc.threshold(image, image, this.thresh, this.maxVal, Imgproc.THRESH_BINARY);
        if (prepareProcess != null) {
            this.prepareProcess.prepareProcess(image);
        }
        return image;
    }

    private List<RightTriangle> outerRightTriangle(Mat imageG) {
        MatOfPoint corners = new MatOfPoint();
        Imgproc.goodFeaturesToTrack(imageG, corners, this.maxCorners, this.qualityLevel, this.minDistance);

        Point[] points = corners.toArray();
        if (points.length < 3) {
            return List.of();
        }

        try {
            return triangleParallel.outerRightTriangle(points, this.maxS, this.minS);
        } finally {
            corners.release();
        }
    }


    private RightTriangle getRightTriangleWithMaxS(List<RightTriangle> rightTriangles) {
        double maxS = 0;
        RightTriangle maxRightTriangle = null;
        for (RightTriangle rightTriangle : rightTriangles) {
            if (rightTriangle.getArea() > maxS) {
                maxS = rightTriangle.getArea();
                maxRightTriangle = rightTriangle;
            }
        }
        return maxRightTriangle;
    }

    private Point[] getUnicuePoint(List<RightTriangle> rightTriangles, boolean includeSquarePoint) {
        if (rightTriangles == null || rightTriangles.isEmpty()) {
            return new Point[0];
        }
        int estimatedSize = rightTriangles.size() * (includeSquarePoint ? 4 : 3);
        Set<Point> points = new HashSet<>(estimatedSize);

        for (RightTriangle triangle : rightTriangles) {
            if (triangle == null) continue;

            points.add(triangle.getP1());
            points.add(triangle.getP2());
            points.add(triangle.getP3());

            if (includeSquarePoint) {
                points.add(triangle.getSquarePoint());
            }
        }
        return points.toArray(new Point[0]);
    }
}
