package luka.teum.dl_service.ai.stat;

import lombok.extern.slf4j.Slf4j;
import luka.teum.dl_service.ai.util.DataSetUtil;
import luka.teum.dl_service.ai.util.ModelUtil;
import luka.teum.image_service.util.ImageUtil;
import messaging.Solution;
import org.datavec.image.loader.NativeImageLoader;
import org.deeplearning4j.datasets.iterator.utilty.ListDataSetIterator;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import org.nd4j.linalg.factory.Nd4j;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Slf4j
public class DigitalClassifier {

    public static final String BASE_PATH = "images\\";
    public static final String MODEL_NAME = "model.tar";
    public static final String TRAINING_PATH = "training\\";
    public static final String TESTING_PATH = "testing\\";
    public static final int HEIGHT = 60;
    public static final int WIGHT = 60;
    public static final int INPUT_SIZE = HEIGHT * WIGHT;
    public static final int HIDDEN_SIZE = 1000;
    public static final int OUTPUT_SIZE = Solution.SUDOKU_SIZE + 1;
    public static final ImageUtil imageUtil = new ImageUtil();

    public static void main(String[] args) throws URISyntaxException, IOException {
        prepareModel();
        //trainModel();
        //OpenCV.loadLocally();
        //newTrain();
        //prepareImage("C:\\Code\\For Work\\sudoku-ai\\images\\testing");
    }

    public static void prepareModel() {
        String modelPath = getModelPath();
        prepareModel(modelPath);
    }

    public static void prepareModel(String modelPath) {
        MultiLayerNetwork model = ModelUtil.loadModel(modelPath);
        if (model == null) {
            trainModel();
        }
    }

    public static void trainModel() {
        try {
            String trainPath = BASE_PATH + TRAINING_PATH;
            String testingPath = BASE_PATH + TESTING_PATH;
            DataSetIterator train = DataSetUtil.getDataSetIterator(trainPath, WIGHT, HEIGHT);
            DataSetIterator test = DataSetUtil.getDataSetIterator(testingPath, WIGHT, HEIGHT);
            MultiLayerNetwork model = ModelUtil.buildModel(INPUT_SIZE, HIDDEN_SIZE, OUTPUT_SIZE);
            ModelUtil.TrainingResult result = ModelUtil.trainingModel(model, train, test);
            ModelUtil.saveModel(model, getModelPath());
            log.info("Result: {}", result);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getModelPath() {
        return MODEL_NAME;
    }

    public static int getCountDataSetSize(String directoryPath) {
        return getCountDataSetSize(new File(directoryPath));
    }

    public static int getCountDataSetSize(File directory) {
        if (!directory.exists()) {
            return 0;
        }
        if (directory.isFile()) {
            String fileName = directory.getName();
            int index = fileName.lastIndexOf(".");
            String ext = fileName.substring(index != -1 ? index : 0);
            return ext.equalsIgnoreCase(".jpg") ? 1 : 0;
        }
        File[] files = directory.listFiles();
        int count = 0;
        if (files != null) {
            for (File file : files) {
                count += getCountDataSetSize(file);
            }
        }
        return count;
    }

    public static void newTrain() throws IOException {
        File ftnOath = new File("C:\\Code\\For Work\\sudoku-ai\\Fnt");
        File[] files = ftnOath.listFiles();
        ImageUtil imageUtil = new ImageUtil();
        NativeImageLoader nativeImageLoader = new NativeImageLoader(60, 60);
        ImagePreProcessingScaler imagePreProcessingScaler = new ImagePreProcessingScaler(0, 1);
        List<DataSet> dataSets = new ArrayList<>();
        for (File file : files) {
            int digit = getDigit(file.getName()) - 1;
            if (digit > 9) {
                continue;
            }
            File[] images = file.listFiles();
            INDArray inputData = Nd4j.create(images.length, 60 * 60);
            INDArray outputData = Nd4j.create(images.length, Solution.SUDOKU_SIZE + 1);
            System.out.println(images.length);
            int n = 0;
            for (File image : images) {
                Mat mat = imageUtil.loadImage(image.getAbsolutePath());
                Imgproc.resize(mat, mat, new Size(60, 60));
                Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
                Imgproc.threshold(mat, mat, 127, 255, Imgproc.THRESH_BINARY_INV);
                INDArray inputImage = nativeImageLoader.asRowVector(mat);
                imagePreProcessingScaler.transform(inputImage);
                inputData.putRow(n, inputImage);
                outputData.put(n, digit, 1.0);
                n++;
            }
            dataSets.addAll(new DataSet(inputData, outputData).asList());
        }
        Collections.shuffle(dataSets, new Random(System.currentTimeMillis()));

        DataSetIterator train = new ListDataSetIterator<>(dataSets, 10);
        DataSetIterator test = DataSetUtil.getDataSetIterator(BASE_PATH + TESTING_PATH, WIGHT, HEIGHT);
        MultiLayerNetwork model = ModelUtil.buildModel(INPUT_SIZE, HIDDEN_SIZE, OUTPUT_SIZE);
        ModelUtil.TrainingResult result = ModelUtil.trainingModel(model, train, test);
        System.out.println(result);
        ModelUtil.saveModel(model, getModelPath());
    }

    public static int getDigit(String name) {
        int size = name.length();
        String number = name.substring(size - 2);
        return Integer.parseInt(number);
    }

    public static void prepareImage(String directoryPath) {
        File file = new File(directoryPath);
        File[] files = file.listFiles();
        for (File image : files) {
            File[] images = image.listFiles();
            for (File nuwImage : images) {
                try {
                    prepareImage(nuwImage);
                } catch (Exception e) {
                    System.out.println("Error " + nuwImage.getName() + ": " + e.getMessage());
                }

            }
            System.out.println(image.getName());
        }
    }

    public static void prepareImage(File fIle) {
        Mat mat = imageUtil.loadImage(fIle.getAbsolutePath());
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
        //Imgproc.threshold(mat, mat, 127, 255, Imgproc.THRESH_BINARY_INV);
        //Imgproc.resize(mat, mat, new Size(60, 60));
        //Mat newMat = extractAndResizeDigit(mat, new Size(60, 60));
        imageUtil.saveImage(fIle.getAbsolutePath(), mat);
    }

    public static Mat extractDigit(Mat image) {
        return extractDigit(image, 5);
    }

    public static Mat extractDigit(Mat image, int padding) {
        if (image.empty()) {
            throw new IllegalArgumentException("Input image is empty");
        }

        Rect boundingBox = findDigitBoundingBox(image);

        if (boundingBox == null) {
            System.err.println("No digit found in image");
            return new Mat();
        }

        Rect paddedBox = addPadding(boundingBox, padding, image.size());

        return image.submat(paddedBox);
    }

    private static Rect findDigitBoundingBox(Mat image) {
        Mat gray = new Mat();
        if (image.channels() > 1) {
            Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
        } else {
            gray = image;
        }

        Mat binary = new Mat();
        Imgproc.threshold(gray, binary, 0, 255,
                Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);

        //showImage(binary,"binary");

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(binary, contours, hierarchy,
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        double maxArea = 0;
        Rect largestRect = null;

        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);
            if (area > maxArea) {
                maxArea = area;
                largestRect = Imgproc.boundingRect(contour);
            }
        }

        //gray.release();
        binary.release();
        hierarchy.release();
        for (MatOfPoint contour : contours) {
            contour.release();
        }

        return largestRect;
    }

    private static Rect addPadding(Rect rect, int padding, Size imageSize) {
        int x = Math.max(0, rect.x - padding);
        int y = Math.max(0, rect.y - padding);
        int width = Math.min((int) imageSize.width - x, rect.width + 2 * padding);
        int height = Math.min((int) imageSize.height - y, rect.height + 2 * padding);

        return new Rect(x, y, width, height);
    }

    public static Mat extractDigitAdvanced(Mat image, int padding) {
        Mat gray = new Mat();
        if (image.channels() > 1) {
            Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
        } else {
            gray = image.clone();
        }

        Mat binary = new Mat();
        Imgproc.adaptiveThreshold(gray, binary, 255,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY_INV, 11, 2);

        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2));
        Imgproc.morphologyEx(binary, binary, Imgproc.MORPH_CLOSE, kernel);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(binary, contours, hierarchy,
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

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

        Mat result = new Mat();
        if (bestRect != null) {
            Rect paddedRect = addPadding(bestRect, padding, image.size());
            result = image.submat(paddedRect);
        }

        gray.release();
        binary.release();
        kernel.release();
        hierarchy.release();
        for (MatOfPoint contour : contours) {
            contour.release();
        }

        return result;
    }

    public static Mat extractAndResizeDigit(Mat image, Size targetSize) {
        Mat digit = extractDigit(image, 3);

        if (digit.empty()) {
            return new Mat();
        }

        Mat resized = new Mat();
        double scale = Math.min((double) targetSize.width / digit.cols(),
                (double) targetSize.height / digit.rows());

        Size newSize = new Size(digit.cols() * scale, digit.rows() * scale);
        Imgproc.resize(digit, resized, newSize);

        Mat result = Mat.zeros(targetSize, digit.type());

        int x = (int) ((targetSize.width - newSize.width) / 2);
        int y = (int) ((targetSize.height - newSize.height) / 2);

        Rect roi = new Rect(x, y, (int) newSize.width, (int) newSize.height);
        resized.copyTo(result.submat(roi));

        digit.release();
        resized.release();

        return result;
    }

    public static void showImage(Mat img, String title) {
        BufferedImage im = new ImageUtil().getBufferedImageFromMat(img);
        if (im == null) return;
        int w = 1000, h = 600;
        JFrame window = new JFrame(title);
        window.setSize(w, h);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ImageIcon imageIcon = new ImageIcon(im);
        JLabel label = new JLabel(imageIcon);
        JScrollPane pane = new JScrollPane(label);
        window.setContentPane(pane);
        if (im.getWidth() < w && im.getHeight() < h) {
            window.pack();
        }
        window.setLocationRelativeTo(null);
        window.setVisible(true);
    }
}
