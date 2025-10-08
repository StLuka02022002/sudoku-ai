package luka.teum.dl_service.ai;

import luka.teum.dl_service.ai.stat.DigitalClassifier;
import luka.teum.dl_service.ai.util.ModelUtil;
import luka.teum.dl_service.prepare.ImagePrepare;
import messaging.Solution;
import org.datavec.image.loader.NativeImageLoader;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.BooleanIndexing;
import org.nd4j.linalg.indexing.conditions.Conditions;
import org.opencv.core.Mat;

import java.io.IOException;

public class Algorithm {

    private static final MultiLayerNetwork INSTANT;
    private static final int WIDTH = (int) ImagePrepare.IMAGE_DIGIT_SIZE.width;
    private static final int HEIGHT = (int) ImagePrepare.IMAGE_DIGIT_SIZE.height;
    private static final int OUTPUT_CLASSES = Solution.SUDOKU_SIZE + 1;

    static {
        INSTANT = initializeModel();
    }

    private final MultiLayerNetwork model;
    private final NativeImageLoader nativeImageLoader;
    private final ImagePreProcessingScaler imagePreProcessingScaler;


    public Algorithm() {
        this.model = INSTANT.clone();
        this.nativeImageLoader = new NativeImageLoader(HEIGHT, WIDTH);
        this.imagePreProcessingScaler = new ImagePreProcessingScaler(0, 1);
    }

    public Algorithm(String modelPath) {
        this.model = ModelUtil.loadModel(modelPath);
        this.nativeImageLoader = new NativeImageLoader(HEIGHT, WIDTH);
        this.imagePreProcessingScaler = new ImagePreProcessingScaler(0, 1);
    }

    private static MultiLayerNetwork initializeModel() {
        MultiLayerNetwork model = ModelUtil.loadModel(DigitalClassifier.getModelPath());
        if (model == null) {
            DigitalClassifier.prepareModel();
            model = ModelUtil.loadModel(DigitalClassifier.getModelPath());
        }
        return model;
    }

    public int evaluateImage(Mat image) throws IOException {
        if (image.empty()) {
            return Solution.NO_SOLUTION;
        }
        try (INDArray input = Nd4j.create(1, WIDTH * HEIGHT);
             INDArray imageData = nativeImageLoader.asRowVector(image)) {

            this.imagePreProcessingScaler.transform(imageData);
            input.putRow(0, imageData);
            DataSet dataSet = new DataSet(input, Nd4j.create(1, OUTPUT_CLASSES));

            try (INDArray predicted = model.output(dataSet.get(0).getFeatures(), false);
                 INDArray predictedValue = BooleanIndexing.firstIndex(predicted, Conditions.equals(predicted.maxNumber()))) {

                return Integer.parseInt(predictedValue.toString());
            } catch (NumberFormatException e) {
                return Solution.NO_SOLUTION;
            }
        }
    }
}
