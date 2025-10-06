package luka.teum.dl_service.prepare;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import luka.teum.dl_service.ai.stat.DigitalClassifier;
import luka.teum.image_service.util.ImageUtil;
import messaging.Solution;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

@Data
@RequiredArgsConstructor
public class ImagePrepare {

    private final ImageUtil imageUtil;
    private double contrast = 3.0;
    private double thresh = 10.0;
    private double maxVal = 255.0;

    public ImagePrepare() {
        this.imageUtil = new ImageUtil();
    }

    public Mat[][] prepare(Mat wrappedImage) {
        this.removeLine(wrappedImage);
        Mat[][] result = imageUtil.split(wrappedImage, Solution.SUDOKU_SIZE, Solution.SUDOKU_SIZE);
        for (int i = 0; i < result.length; i++) {
            for (int j = 0; j < result[0].length; j++) {
                result[i][j] = DigitalClassifier.extractAndResizeDigit(result[i][j], new Size(60, 60));
            }
        }
        return result;
    }

    private void prepareImage(Mat image) {
        Mat mat = null;
        try {
            mat = this.imageUtil.contrastEnhancement(image, this.contrast);
            Imgproc.cvtColor(mat, image, Imgproc.COLOR_BGR2GRAY);
            Imgproc.threshold(image, image, this.thresh, this.maxVal, Imgproc.THRESH_BINARY_INV);
            this.resizeImage(image);
        } finally {
            if (mat != null) {
                mat.release();
            }
        }
    }

    private void resizeImage(Mat image) {
        final int count = Core.countNonZero(image);
        if (count <= 50) {
            image.release();
        } else {
            Imgproc.resize(image, image, new Size(60, 60));
        }
    }

    private void removeLine(Mat wrapped) {
        final Mat lines = new Mat();

        Imgproc.cvtColor(wrapped, wrapped, Imgproc.COLOR_RGB2GRAY);

        Imgproc.adaptiveThreshold(wrapped, wrapped, 255,
                Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 9, 11);

        Core.bitwise_not(wrapped, wrapped);
        Imgproc.HoughLinesP(wrapped, lines, 1,
                Math.PI / 180, 150, 250, 50
        );

        for (int r = 0; r < lines.rows(); r++) {
            double[] l = lines.get(r, 0);
            Imgproc.line(wrapped, new Point(l[0], l[1]), new Point(l[2], l[3]),
                    new Scalar(0, 0, 255), 13, Imgproc.FILLED, 0
            );
        }

        lines.release();
    }
}
