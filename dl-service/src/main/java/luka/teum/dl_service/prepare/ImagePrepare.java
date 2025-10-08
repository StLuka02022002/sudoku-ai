package luka.teum.dl_service.prepare;

import luka.teum.image_service.util.ImageUtil;
import messaging.Solution;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

public class ImagePrepare {

    public static final Size IMAGE_DIGIT_SIZE = new Size(60, 60);

    private static final int ADAPTIVE_THRESH_BLOCK_SIZE = 9;
    private static final int ADAPTIVE_THRESH_C = 11;
    private static final int HOUGH_THRESHOLD = 150;
    private static final int HOUGH_MIN_LINE_LENGTH = 250;
    private static final int HOUGH_MAX_LINE_GAP = 50;
    private static final int LINE_THICKNESS = 13;
    private static final Scalar LINE_COLOR = new Scalar(0, 0, 255);

    private final ImageUtil imageUtil;

    public ImagePrepare() {
        this.imageUtil = new ImageUtil();
    }

    public ImagePrepare(ImageUtil imageUtil) {
        this.imageUtil = imageUtil;
    }

    public Mat[][] prepare(Mat wrappedImage) {
        Mat processedImage = this.preprocessImage(wrappedImage);
        Mat[][] splitImages = null;
        Mat[][] result = new Mat[Solution.SUDOKU_SIZE][Solution.SUDOKU_SIZE];

        try {
            splitImages = this.imageUtil.split(processedImage, Solution.SUDOKU_SIZE, Solution.SUDOKU_SIZE, 0);
            this.processDigits(splitImages, result);
        } finally {
            processedImage.release();
            this.imageUtil.releaseSubMats(splitImages);

        }
        return result;
    }

    private Mat preprocessImage(Mat wrappedImage) {
        Mat processed = new Mat();
        try {
            this.removeLines(wrappedImage, processed);
            return processed;
        } catch (Exception e) {
            processed.release();
            throw e;
        }
    }

    private void processDigits(Mat[][] splitImage, Mat[][] result) {
        for (int i = 0; i < result.length; i++) {
            for (int j = 0; j < result[i].length; j++) {
                result[i][j] = this.processSingleDigit(splitImage[i][j]);
            }
        }
    }

    private Mat processSingleDigit(Mat digitImage) {
        try {
            return this.imageUtil.extractAndResizeDigit(digitImage, IMAGE_DIGIT_SIZE);
        } catch (Exception e) {
            return new Mat();
        }
    }

    private void removeLines(Mat input, Mat output) {
        Mat gray = new Mat();
        Mat binary = new Mat();
        Mat lines = new Mat();

        try {
            Imgproc.cvtColor(input, gray, Imgproc.COLOR_RGB2GRAY);

            Imgproc.adaptiveThreshold(gray, binary, 255,
                    Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY,
                    ADAPTIVE_THRESH_BLOCK_SIZE, ADAPTIVE_THRESH_C);

            Core.bitwise_not(binary, binary);

            Imgproc.HoughLinesP(binary, lines, 1, Math.PI / 180,
                    HOUGH_THRESHOLD, HOUGH_MIN_LINE_LENGTH, HOUGH_MAX_LINE_GAP);

            this.removeDetectedLines(binary, lines);

            binary.copyTo(output);
        } finally {
            imageUtil.releaseMats(gray, binary, lines);
        }
    }

    private void removeDetectedLines(Mat image, Mat lines) {
        for (int r = 0; r < lines.rows(); r++) {
            double[] line = lines.get(r, 0);
            Point pt1 = new Point(line[0], line[1]);
            Point pt2 = new Point(line[2], line[3]);
            Imgproc.line(image, pt1, pt2, LINE_COLOR, LINE_THICKNESS, Imgproc.FILLED, 0);
        }
    }


}
