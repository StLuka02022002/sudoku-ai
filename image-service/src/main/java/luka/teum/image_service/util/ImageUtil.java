package luka.teum.image_service.util;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.List;

public class ImageUtil {

    private static final Logger log = LoggerFactory.getLogger(ImageUtil.class);

    public Mat loadImage(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }

        Mat image = Imgcodecs.imread(filePath);
        if (image.empty()) {
            image.release();
            throw new RuntimeException("Failed to load image from path: " + filePath);
        }
        return image;
    }

    public boolean saveImage(String filePath, Mat mat) {
        return Imgcodecs.imwrite(filePath, mat);
    }

    public Mat warpImage(Mat image, Point[] points) {
        validateImage(image);
        validatePoints(points);

        Mat clonedImage = image.clone();
        Size outputSize = new Size(points[1].x - points[0].x, points[2].y - points[1].y);
        Mat warpedImage = new Mat(clonedImage.height(), clonedImage.width(), clonedImage.type());

        try {
            Imgproc.warpPerspective(clonedImage, warpedImage,
                    this.createPerspectiveTransform(points, outputSize), outputSize);

            return warpedImage;
        } catch (Exception e) {
            throw new RuntimeException("Failed to warp image: " + e.getMessage(), e);
        } finally {
            clonedImage.release();
        }
    }

    private Mat createPerspectiveTransform(Point[] points, Size outputSize) {
        MatOfPoint2f destinationPoints = new MatOfPoint2f(
                new Point(0, 0),
                new Point(outputSize.width, 0),
                new Point(0, outputSize.height),
                new Point(outputSize.width, outputSize.height));

        MatOfPoint2f src = new MatOfPoint2f(points);
        MatOfPoint2f dst = new MatOfPoint2f(destinationPoints);

        try {
            return Imgproc.getPerspectiveTransform(src, dst);
        } finally {
            src.release();
            dst.release();
        }
    }

    public BufferedImage getBufferedImageFromMat(Mat data) {
        validateImage(data);

        Mat processData = this.normalizeMatDepth(data);
        if (processData.empty()) {
            throw new IllegalArgumentException("Unsupported matrix depth: " + data.depth());
        }
        try {
            int imageType = this.getBufferedImageType(processData);
            if (imageType == -1) {
                throw new IllegalArgumentException("Unsupported number of channels: " + processData.channels());
            }
            return this.createBufferedImage(processData, imageType);
        } finally {
            processData.release();
        }
    }

    public Mat contrastEnhancement(Mat data, double contrast) {
        validateImage(data);

        if (data.channels() != 3) {
            throw new IllegalArgumentException("Input image must have 3 channels (BGR)");
        }

        if (contrast <= 0) {
            throw new IllegalArgumentException("Contrast factor must be positive");
        }


        double mean = this.calculateLuminanceMean(data);
        Mat lut = this.createContrastLUT(contrast, mean);
        try {
            Mat result = new Mat();
            Core.LUT(data, lut, result);
            return result;
        } finally {
            lut.release();
        }
    }

    private double calculateLuminanceMean(Mat data) {
        Scalar meanBGR = Core.mean(data);
        return meanBGR.val[0] * 0.114 + meanBGR.val[1] * 0.587 + meanBGR.val[2] * 0.299;
    }

    private Mat createContrastLUT(double contrast, double mean) {
        byte[] lutData = new byte[256];
        for (int i = 0; i < 256; i++) {
            int color = (int) (contrast * (i - mean) + mean);
            lutData[i] = (byte) (Math.max(0, Math.min(255, color)));
        }
        Mat lut = new Mat(1, 256, CvType.CV_8UC1);
        lut.put(0, 0, lutData);
        return lut;
    }

    public Mat normalizeMatDepth(Mat data) {
        return switch (data.depth()) {
            case CvType.CV_8U -> data;
            case CvType.CV_16U -> {
                Mat data_16 = new Mat();
                data.convertTo(data_16, CvType.CV_8U, 255.0 / 65535);
                yield data_16;
            }
            case CvType.CV_32F -> {
                Mat data_32 = new Mat();
                data.convertTo(data_32, CvType.CV_8U, 255);
                yield data_32;
            }
            default -> new Mat();
        };
    }

    public int getBufferedImageType(Mat data) {
        return switch (data.channels()) {
            case 1 -> BufferedImage.TYPE_BYTE_GRAY;
            case 3 -> BufferedImage.TYPE_3BYTE_BGR;
            case 4 -> BufferedImage.TYPE_4BYTE_ABGR;
            default -> -1;
        };
    }

    private BufferedImage createBufferedImage(Mat data, int imageType) {
        int bufferSize = data.channels() * data.cols() * data.rows();
        byte[] buffer = new byte[bufferSize];

        data.get(0, 0, buffer);
        if (data.channels() == 4) {
            this.convertBGRAToABGR(buffer);
        }

        BufferedImage image = new BufferedImage(data.cols(), data.rows(), imageType);
        byte[] dataImage = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(buffer, 0, dataImage, 0, buffer.length);

        return image;
    }

    public void convertBGRAToABGR(byte[] buffer) {
        for (int i = 0; i < buffer.length; i += 4) {
            byte alpha = buffer[i + 3];
            buffer[i + 3] = buffer[i + 2];
            buffer[i + 2] = buffer[i + 1];
            buffer[i + 1] = buffer[i];
            buffer[i] = alpha;
        }
    }

    public Mat[][] split(Mat image, int rows, int cols, int border) {
        validateImage(image);

        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("Rows and columns must be positive integers");
        }

        int width = image.cols();
        int height = image.rows();

        if (width < rows * 4 || height < cols * 4) {
            throw new IllegalArgumentException(
                    String.format("Image too small for splitting: %dx%d into %dx%d",
                            width, height, rows, cols));
        }

        Mat clonedImage = image.clone();
        Mat[][] result = new Mat[rows][cols];
        int rowStep = height / rows;
        int colStep = width / cols;

        try {
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    Mat m = clonedImage.submat(i * rowStep + border,
                            (i + 1) * rowStep - border,
                            j * colStep + border,
                            (j + 1) * colStep - border);
                    result[i][j] = m;
                }
            }
            return result;

        } catch (Exception e) {
            this.releaseSubMats(result);
            throw new RuntimeException("Failed to split image: " + e.getMessage(), e);
        } finally {
            clonedImage.release();
        }
    }

    public void releaseSubMats(Mat[][] mats) {
        if (mats == null) return;

        for (Mat[] row : mats) {
            for (Mat mat : row) {
                if (mat != null) {
                    mat.release();
                }
            }
        }
    }

    public Mat extractDigit(Mat image) {
        return this.extractDigit(image, 5);
    }

    public Mat extractDigit(Mat image, int padding) {
        if (image.empty()) {
            throw new IllegalArgumentException("Input image is empty");
        }

        Rect boundingBox = this.findDigitBoundingBox(image);

        if (boundingBox == null) {
            log.debug("No digit found in image");
            return new Mat();
        }

        Rect paddedBox = this.addPadding(boundingBox, padding, image.size());

        return image.submat(paddedBox);
    }

    public Mat extractDigitAdvanced(Mat image, int padding) {
        Mat gray = new Mat();
        Mat binary = new Mat();
        Mat kernel = new Mat();
        Mat hierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<>();
        try {
            if (image.channels() > 1) {
                Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
            } else {
                gray = image.clone();
            }

            Imgproc.adaptiveThreshold(gray, binary, 255,
                    Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                    Imgproc.THRESH_BINARY_INV, 11, 2);

            kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2));
            Imgproc.morphologyEx(binary, binary, Imgproc.MORPH_CLOSE, kernel);


            Imgproc.findContours(binary, contours, hierarchy,
                    Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);


            Rect bestRect = this.findBestRect(contours);

            Mat result = new Mat();
            if (bestRect != null) {
                Rect paddedRect = this.addPadding(bestRect, padding, image.size());
                result = image.submat(paddedRect);
            }
            return result;
        } finally {
            gray.release();
            binary.release();
            kernel.release();
            hierarchy.release();
            for (MatOfPoint contour : contours) {
                contour.release();
            }
        }
    }

    public Mat extractAndResizeDigit(Mat image, Size targetSize) {
        Mat digit = this.extractDigit(image, 3);
        Mat resized = new Mat();
        try {
            if (digit.empty()) {
                return new Mat();
            }

            Mat result = Mat.zeros(targetSize, digit.type());

            Rect roi = this.getRectWithNewSize(targetSize, digit);

            Imgproc.resize(digit, resized, new Size(roi.width, roi.height));
            resized.copyTo(result.submat(roi));
            return result;
        } finally {
            digit.release();
            resized.release();
        }
    }

    private Rect findDigitBoundingBox(Mat image) {
        Mat gray = new Mat();
        Mat binary = new Mat();
        Mat hierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<>();
        try {
            if (image.channels() > 1) {
                Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
            } else {
                gray = image.clone();
            }

            Imgproc.threshold(gray, binary, 0, 255,
                    Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);


            Imgproc.findContours(binary, contours, hierarchy,
                    Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);


            return this.findLargestRect(contours);

        } finally {
            gray.release();
            binary.release();
            hierarchy.release();
            for (MatOfPoint contour : contours) {
                contour.release();
            }
        }
    }

    private Rect findLargestRect(List<MatOfPoint> contours) {
        double maxArea = 0;
        Rect largestRect = null;

        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);
            if (area > maxArea) {
                maxArea = area;
                largestRect = Imgproc.boundingRect(contour);
            }
        }
        return largestRect;
    }

    private Rect findBestRect(List<MatOfPoint> contours) {
        Rect bestRect = null;
        double bestScore = 0;

        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);
            Rect rect = Imgproc.boundingRect(contour);

            double aspectRatio = (double) rect.width / rect.height;
            double score = area * (1 - Math.abs(0.7 - aspectRatio));

            if (score > bestScore && area > 50) {
                bestScore = score;
                bestRect = rect;
            }
        }
        return bestRect;
    }

    private Rect addPadding(Rect rect, int padding, Size imageSize) {
        int x = Math.max(0, rect.x - padding);
        int y = Math.max(0, rect.y - padding);
        int width = Math.min((int) imageSize.width - x, rect.width + 2 * padding);
        int height = Math.min((int) imageSize.height - y, rect.height + 2 * padding);

        return new Rect(x, y, width, height);
    }

    private Rect getRectWithNewSize(Size targetSize, Mat digit) {
        double scale = Math.min(targetSize.width / digit.cols(),
                targetSize.height / digit.rows());

        Size newSize = new Size(digit.cols() * scale, digit.rows() * scale);

        int x = (int) ((targetSize.width - newSize.width) / 2);
        int y = (int) ((targetSize.height - newSize.height) / 2);

        return new Rect(x, y, (int) newSize.width, (int) newSize.height);
    }

    public static void showImage(Mat img, String title) {
        BufferedImage bufferedImage = new ImageUtil().getBufferedImageFromMat(img);
        if (bufferedImage == null) return;

        JFrame window = new JFrame(title);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        ImageIcon imageIcon = new ImageIcon(bufferedImage);
        JLabel label = new JLabel(imageIcon);
        JScrollPane pane = new JScrollPane(label);

        int w = bufferedImage.getWidth();
        int h = bufferedImage.getWidth();

        window.setSize(w, h);
        window.setContentPane(pane);
        window.pack();

        window.setLocationRelativeTo(null);
        window.setVisible(true);
    }

    public void releaseMats(Mat... mats) {
        for (Mat mat : mats) {
            if (mat != null) {
                mat.release();
            }
        }
    }

    public static void validateImage(Mat image) {
        if (image == null || image.empty()) {
            throw new IllegalArgumentException("Input image cannot be null or empty");
        }
    }

    public static void validatePoints(Point[] points) {
        if (points == null || points.length != 4) {
            throw new IllegalArgumentException("Points array must contain exactly 4 points");
        }

        for (int i = 0; i < points.length; i++) {
            if (points[i] == null) {
                throw new IllegalArgumentException("Point at index " + i + " cannot be null");
            }
        }
    }
}
