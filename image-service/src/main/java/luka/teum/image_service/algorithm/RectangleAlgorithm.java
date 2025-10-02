package luka.teum.image_service.algorithm;

import lombok.Setter;
import luka.teum.image_service.util.ImageUtil;
import luka.teum.image_service.util.PointsUtil;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RectangleAlgorithm implements ImageAlgorithm {

    public static final int V1 = 0;
    public static final int V2 = 1;
    public static final int V3 = 2;
    public static final int V4 = 3;

    private final ImageUtil imageUtil;
    private final PointsUtil pointsUtil;
    @Setter
    private PrepareProcess prepareProcess;
    private double contrast = 3.0;
    private double thresh = 225.0;
    private double maxVal = 255.0;
    private double minContourArea = 1000.0;
    private double epsilonFactor = 0.04;
    private final int targetVertices = 4;


    public RectangleAlgorithm(ImageUtil imageUtil, PointsUtil pointsUtil, PrepareProcess prepareProcess) {
        if (imageUtil == null) {
            throw new IllegalArgumentException("ImageUtil cannot be null");
        }
        if (pointsUtil == null) {
            throw new IllegalArgumentException("PointsUtil cannot be null");
        }
        if (prepareProcess == null) {
            throw new IllegalArgumentException("PrepareProcess cannot be null");
        }
        this.imageUtil = imageUtil;
        this.pointsUtil = pointsUtil;
    }

    public RectangleAlgorithm() {
        this.imageUtil = new ImageUtil();
        this.pointsUtil = new PointsUtil();
    }

    public Point[] algorithm(Mat data) {
        ImageUtil.validateImage(data);

        Mat image = data.clone();
        Mat prepareImage = this.prepareImage(image);
        try {
            Point[] points = this.outerPoint(prepareImage);
            return this.pointsUtil.findToRightPoints(points);
        } finally {
            image.release();
            prepareImage.release();
        }
    }

    public Point[] algorithm(Mat data, int version) {
        switch (version) {
            case V1 -> {
                contrast = 3.0;
                thresh = 225.0;
                maxVal = 255.0;
                minContourArea = 1000.0;
                epsilonFactor = 0.04;
            }
            case V2 -> {
                contrast = 3.0;
                thresh = 250.0;
                maxVal = 255.0;
                minContourArea = 1000.0;
                epsilonFactor = 0.04;
            }
        }
        return algorithm(data);
    }

    private Mat prepareImage(Mat data) {
        Mat image = this.imageUtil.contrastEnhancement(data, this.contrast);
        Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2GRAY);
        Imgproc.threshold(image, image, this.thresh, this.maxVal, Imgproc.THRESH_BINARY);
        Imgproc.Canny(image, image, 100, 255);
        Imgproc.dilate(image, image, new Mat(), new Point(-1, -1), 1);
        if(prepareProcess != null){
            this.prepareProcess.prepareProcess(image);
        }
        return image;
    }

    private Point[] outerPoint(Mat imageG) {
        ImageUtil.validateImage(imageG);

        List<MatOfPoint> allContours = new ArrayList<>();
        Mat hierarchy = new Mat(imageG.size(), imageG.type());
        try {
            Imgproc.findContours(imageG, allContours, hierarchy,
                    Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            List<MatOfPoint> filterContours = allContours.stream()
                    .filter(contour -> Imgproc.contourArea(contour) > this.minContourArea)
                    .collect(Collectors.toList());

            if (filterContours.isEmpty()) {
                return new Point[0];
            }

            int bigContourIndex = this.getBigContourIndex(filterContours);
            Point[] points = this.getSorterPoints(filterContours.get(bigContourIndex));

            if (points.length != 4) {
                return new Point[0];
            }

            return points;
        } finally {
            allContours.forEach(Mat::release);
            hierarchy.release();
        }

    }

    private int getBigContourIndex(List<MatOfPoint> contours) {
        if (contours == null || contours.isEmpty()) {
            return -1;
        }

        double max = 0;
        int index = 0;

        for (int i = 0; i < contours.size(); i++) {
            double contourArea = Imgproc.contourArea(contours.get(i));
            if (max < contourArea) {
                max = contourArea;
                index = i;
            }
        }

        return index;
    }

    private Point[] getSorterPoints(MatOfPoint contour) {
        ImageUtil.validateImage(contour);

        MatOfPoint2f approxPolygon = this.approxPolygon(contour);

        try {
            if (approxPolygon.rows() != 4) {
                return new Point[0];
            }

            Point center = this.calculateCentroid(approxPolygon);
            if (center == null) {
                return new Point[0];
            }
            return this.sortPointsByQuadrant(approxPolygon, center);

        } finally {
            approxPolygon.release();
        }
    }

    private MatOfPoint2f approxPolygon(MatOfPoint contour) {
        MatOfPoint2f source = new MatOfPoint2f();

        try {
            contour.convertTo(source, CvType.CV_32FC2);
            return this.approxPolygonWithBinarySearch(source, this.targetVertices);
        } finally {
            source.release();
        }
    }

    private MatOfPoint2f approxPolygonWithBinarySearch(MatOfPoint2f source, int targetVertices) {
        final double PRECISION = 0.001;

        double leftEpsilon = 0.005;
        double rightEpsilon = 0.5;
        double bestEpsilon = this.epsilonFactor;
        MatOfPoint2f bestResult = new MatOfPoint2f();

        double arcLength = Imgproc.arcLength(source, true);
        int bestVertices = this.approxPolygonWithArcLength(source, bestResult, leftEpsilon * arcLength);
        if (bestVertices == targetVertices) {
            return bestResult;
        }

        int leftVertices = this.approxPolygonWithArcLength(source, bestResult, leftEpsilon * arcLength);
        if (leftVertices == targetVertices) {
            return bestResult;
        }

        int rightVertices = this.approxPolygonWithArcLength(source, bestResult, rightEpsilon * arcLength);
        if (rightVertices == targetVertices) {
            return bestResult;
        }

        if (targetVertices < rightVertices) {
            this.approxPolygonWithArcLength(source, bestResult, rightEpsilon * arcLength);
            return bestResult;
        }
        if (targetVertices > leftVertices) {
            this.approxPolygonWithArcLength(source, bestResult, leftEpsilon * arcLength);
            return bestResult;
        }

        while ((rightEpsilon - leftEpsilon) > PRECISION) {
            double midEpsilon = (rightEpsilon + leftEpsilon) / 2.0;
            int currentVertices = this.approxPolygonWithArcLength(source, bestResult, midEpsilon * arcLength);

            if (currentVertices == targetVertices) {
                return bestResult;
            }

            if (currentVertices > targetVertices) {
                leftEpsilon = midEpsilon;
            } else {
                rightEpsilon = midEpsilon;
            }
        }

        this.approxPolygonWithArcLength(source, bestResult, bestEpsilon * arcLength);
        return bestResult;

    }

    private int approxPolygonWithArcLength(MatOfPoint2f source, MatOfPoint2f destination, double epsilon) {
        Imgproc.approxPolyDP(source, destination, epsilon, true);
        return destination.rows();
    }

    private Point calculateCentroid(MatOfPoint2f polygon) {
        Moments moments = Imgproc.moments(polygon);
        if (Math.abs(moments.get_m00()) < 1e-9) {
            return null;
        }

        double centerX = moments.get_m10() / moments.get_m00();
        double centerY = moments.get_m01() / moments.get_m00();
        return new Point(centerX, centerY);
    }

    private Point[] sortPointsByQuadrant(MatOfPoint2f polygon, Point center) {
        Point[] sortedPoints = new Point[4];
        for (int i = 0; i < polygon.rows(); i++) {
            double[] data = polygon.get(i, 0);

            if (data == null || data.length < 2) {
                continue;
            }

            int quadrant = PointsUtil.getQuadrant(data[0], data[1], center.x, center.y);
            if (quadrant >= 0 && quadrant < 4) {
                sortedPoints[quadrant] = new Point(data[0], data[1]);
            }
        }
        return sortedPoints;
    }


    public double getContrast() {
        return contrast;
    }

    public void setContrast(double contrast) {
        this.contrast = contrast;
    }

    public double getThresh() {
        return thresh;
    }

    public void setThresh(double thresh) {
        this.thresh = thresh;
    }

    public double getMaxVal() {
        return maxVal;
    }

    public void setMaxVal(double maxVal) {
        this.maxVal = maxVal;
    }

    public double getMinContourArea() {
        return minContourArea;
    }

    public void setMinContourArea(double minContourArea) {
        this.minContourArea = minContourArea;
    }

    public double getEpsilonFactor() {
        return epsilonFactor;
    }

    public void setEpsilonFactor(double epsilonFactor) {
        this.epsilonFactor = epsilonFactor;
    }
}
