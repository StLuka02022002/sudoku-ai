package luka.teum.dl_service.ai.util;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.PerformanceListener;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.learning.config.IUpdater;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ModelUtil {

    public static final int MAIN_SEED = 123;
    private static final double DEFAULT_LEARNING_RATE = 0.001;
    private static final double DEFAULT_L2_REGULARIZATION = 1e-4;
    private static final double DEFAULT_MOMENTUM = 0.9;
    private static final int DEFAULT_ITERATION_LISTENER_FREQ = 100;
    private static final int DEFAULT_PERFORMANCE_LISTENER_FREQ = 1000;

    public static enum ModelPreset {
        FAST_TRAINING(1e-3, new Adam(0.01)),
        HIGH_ACCURACY(1e-5, new Adam(0.001)),
        BALANCED(1e-4, new Nesterovs(0.001, 0.9));

        private final double l2;
        private final IUpdater updater;

        ModelPreset(double l2, IUpdater updater) {
            this.l2 = l2;
            this.updater = updater;
        }
    }

    public static MultiLayerNetwork buildModel(int inputSize, int hiddenSize, int outputSize) {
        return buildModel(inputSize, hiddenSize, outputSize, DEFAULT_LEARNING_RATE, DEFAULT_L2_REGULARIZATION);
    }

    public static MultiLayerNetwork buildModel(int inputSize, int hiddenSize, int outputSize,
                                               double learningRate, double l2) {
        return buildModel(inputSize, hiddenSize, outputSize, new Nesterovs(learningRate, DEFAULT_MOMENTUM), l2);
    }

    public static MultiLayerNetwork buildModel(int inputSize, int hiddenSize, int outputSize, ModelPreset preset) {
        return buildModel(inputSize, hiddenSize, outputSize, preset.updater, preset.l2);
    }

    public static MultiLayerNetwork buildModel(int inputSize, int hiddenSize, int outputSize, IUpdater updater, double l2Regularization) {
        log.info("Building model architecture: Input[{}] -> Hidden[{}] -> Output[{}]", inputSize, hiddenSize, outputSize);

        long startTime = System.nanoTime();

        MultiLayerConfiguration configuration = new NeuralNetConfiguration.Builder()
                .seed(MAIN_SEED)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(updater)
                .l2(l2Regularization)
                .list()
                .layer(createHiddenLayer(inputSize, hiddenSize))
                .layer(createOutputLayer(hiddenSize, outputSize))
                .build();

        MultiLayerNetwork model = new MultiLayerNetwork(configuration);
        model.init();

        long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        log.info("Model built successfully in {} ms", duration);

        return model;
    }

    private static DenseLayer createHiddenLayer(int inputSize, int outputSize) {
        return new DenseLayer.Builder()
                .nIn(inputSize)
                .nOut(outputSize)
                .activation(Activation.RELU)
                .weightInit(WeightInit.XAVIER_UNIFORM)
                .build();
    }

    private static OutputLayer createOutputLayer(int inputSize, int outputSize) {
        return new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                .nIn(inputSize)
                .nOut(outputSize)
                .activation(Activation.SOFTMAX)
                .weightInit(WeightInit.XAVIER_UNIFORM)
                .build();
    }

    public static TrainingResult trainingModel(MultiLayerNetwork model,
                                               DataSetIterator trainData,
                                               DataSetIterator testData) {
        return trainingModel(model, trainData, testData, DEFAULT_ITERATION_LISTENER_FREQ);
    }

    public static TrainingResult trainingModel(MultiLayerNetwork model,
                                               DataSetIterator trainData,
                                               DataSetIterator testData,
                                               int logFrequency) {
        log.info("Starting model training...");

        long startTime = System.nanoTime();

        if (model.params() == null) {
            model.init();
        }

        model.setListeners(
                new ScoreIterationListener(logFrequency),
                new PerformanceListener.Builder()
                        .reportSample(true)
                        .reportIteration(true)
                        .setFrequency(logFrequency * 10)
                        .build()
        );

        model.fit(trainData);

        Evaluation evaluation = model.evaluate(testData);

        long duration = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTime);

        log.info("Training completed in {} seconds", duration);
        log.info("Model evaluation results:\n{}", evaluation.stats());

        return new TrainingResult(model, evaluation, duration);
    }


    public static boolean saveModel(MultiLayerNetwork model, String filePath) {
        if (model == null) {
            log.error("Cannot save: model is null");
            return false;
        }

        try {
            Path path = Paths.get(filePath);

            Path parentDir = path.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            ModelSerializer.writeModel(model, filePath, true);
            log.info("Model successfully saved to: {}", path.toAbsolutePath());
            return true;

        } catch (IOException e) {
            log.error("Failed to save model to: {}. Error: {}", filePath, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error while saving model: {}", e.getMessage(), e);
            return false;
        }
    }

    public static MultiLayerNetwork loadModel(String filePath) {
        try {
            MultiLayerNetwork model = ModelSerializer.restoreMultiLayerNetwork(filePath);
            log.info("Model successfully loaded from: {}", filePath);
            return model;
        } catch (IOException e) {
            log.error("Failed to load model from: {}. Error: {}", filePath, e.getMessage(), e);
            return null;
        }
    }

    @Data
    public static class TrainingResult {
        private final MultiLayerNetwork model;
        private final Evaluation evaluation;
        private final long trainingTimeSeconds;

        public TrainingResult(MultiLayerNetwork model, Evaluation evaluation, long trainingTimeSeconds) {
            this.model = model;
            this.evaluation = evaluation;
            this.trainingTimeSeconds = trainingTimeSeconds;
        }
    }

    public static TrainingResult createAndTrainModel(int inputSize, int hiddenSize, int outputSize,
                                                     DataSetIterator trainData, DataSetIterator testData) {
        MultiLayerNetwork model = buildModel(inputSize, hiddenSize, outputSize);
        return trainingModel(model, trainData, testData);
    }
}
