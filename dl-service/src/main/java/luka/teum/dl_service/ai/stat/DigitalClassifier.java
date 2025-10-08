package luka.teum.dl_service.ai.stat;

import lombok.extern.slf4j.Slf4j;
import luka.teum.dl_service.ai.util.DataSetUtil;
import luka.teum.dl_service.ai.util.ModelUtil;
import luka.teum.image_service.util.ImageUtil;
import messaging.Solution;
import nu.pattern.OpenCV;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.opencv.core.Mat;
import org.opencv.core.Size;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;

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
        //prepareImages("C:\\Code\\For Work\\sudoku-ai\\images\\testing");
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

    public static int getDigit(String name) {
        int size = name.length();
        if (size > 1) {
            String number = name.substring(size - 2);
            return Integer.parseInt(number);
        }
        return Integer.parseInt(name);
    }

    public static void prepareImages(String directoryPath) {
        OpenCV.loadLocally();
        File directory = new File(directoryPath);
        File[] directories = getSubFiles(directory);

        Arrays.stream(directories)
                .forEach(DigitalClassifier::processSubdirectory);

    }

    private static void processSubdirectory(File subdirectory) {
        File[] images = getSubFiles(subdirectory);
        Arrays.stream(images)
                .forEach(DigitalClassifier::processSingleImage);
        logProcessedDirectory(subdirectory.getName());
    }

    private static void processSingleImage(File image) {
        try {
            prepareImage(image);
        } catch (Exception e) {
            handleImageProcessingError(image, e);
        }
    }

    private static File[] getSubFiles(File directory) {
        File[] files = directory.listFiles();
        return (files != null) ? files : new File[0];
    }

    public static void prepareImage(File fIle) {
        Mat mat = imageUtil.loadImage(fIle.getAbsolutePath());
        Mat result = imageUtil.extractAndResizeDigit(mat, new Size(60, 60));
        imageUtil.saveImage(fIle.getAbsolutePath(), result);

        result.release();
        mat.release();
    }

    private static void handleImageProcessingError(File image, Exception e) {
        System.err.println("Error processing image " + image.getName() + ": " + e.getMessage());
    }

    private static void logProcessedDirectory(String directoryName) {
        System.out.println("Processed directory: " + directoryName);
    }


}
