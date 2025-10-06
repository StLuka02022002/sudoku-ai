package luka.teum.image_service.util;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class ImageUtil {

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

    public Mat[][] split(Mat image, int rows, int cols) {
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
        int border = 0;

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

    private void releaseSubMats(Mat[][] mats) {
        if (mats == null) return;

        for (Mat[] row : mats) {
            for (Mat mat : row) {
                if (mat != null) {
                    mat.release();
                }
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
