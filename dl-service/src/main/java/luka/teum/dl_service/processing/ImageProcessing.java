package luka.teum.dl_service.processing;

import lombok.extern.slf4j.Slf4j;
import luka.teum.dl_service.ai.Algorithm;
import luka.teum.dl_service.messaging.KafkaProducerService;
import luka.teum.dl_service.prepare.ImagePrepare;
import messaging.Solution;
import messaging.TelegramInfo;
import messaging.image.ImagesInfo;
import messaging.solution.SolutionsInfo;
import org.opencv.core.Mat;
import storage.Storage;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class ImageProcessing {

    private final Storage<Mat> storage;
    private final KafkaProducerService kafkaProducerService;
    private final ImagePrepare imagePrepare;
    private final Algorithm algorithm;

    public ImageProcessing(Storage<Mat> storage, KafkaProducerService kafkaProducerService) {
        this.storage = storage;
        this.kafkaProducerService = kafkaProducerService;
        this.algorithm = new Algorithm();
        this.imagePrepare = new ImagePrepare();
    }

    public ImageProcessing(Storage<Mat> storage, KafkaProducerService kafkaProducerService, String modelPath) {
        this.storage = storage;
        this.kafkaProducerService = kafkaProducerService;
        if (modelPath != null) {
            this.algorithm = new Algorithm(modelPath);
        } else {
            this.algorithm = new Algorithm();
        }
        this.imagePrepare = new ImagePrepare();
    }

    public void processing(ImagesInfo imagesInfo) {
        Set<Solution> solutions = imagesInfo.getImagesPaths().stream()
                .map(this::processSingleImage)
                .collect(Collectors.toSet());

        SolutionsInfo solutionsInfo = this.buildSolutionsInfo(solutions, imagesInfo.getTelegramInfo());
        this.kafkaProducerService.sendSolutionsProcessingInfoSync(solutionsInfo);
    }

    private Solution processSingleImage(String imagePath) {
        Mat image = null;
        try {
            image = this.storage.getData(imagePath);
            Mat[][] digits = this.imagePrepare.prepare(image);
            int[][] result = this.evaluateDigits(digits);
            return new Solution(result);
        } catch (Exception e) {
            log.error("Error processing image: {}", imagePath, e);
            return this.createEmptySolution();
        } finally {
            if (image != null) {
                image.release();
            }
        }
    }

    private int[][] evaluateDigits(Mat[][] digits) {
        int[][] result = new int[Solution.SUDOKU_SIZE][Solution.SUDOKU_SIZE];

        for (int i = 0; i < Solution.SUDOKU_SIZE; i++) {
            for (int j = 0; j < Solution.SUDOKU_SIZE; j++) {
                result[i][j] = this.evaluateSingleDigit(digits[i][j]);
            }
        }
        return result;
    }

    private int evaluateSingleDigit(Mat digit) {
        try {
            return this.algorithm.evaluateImage(digit);
        } catch (IOException e) {
            log.warn("Failed to evaluate digit, using NO_SOLUTION", e);
            return Solution.NO_SOLUTION;
        }
    }

    private Solution createEmptySolution() {
        int[][] emptyResult = new int[Solution.SUDOKU_SIZE][Solution.SUDOKU_SIZE];
        Arrays.stream(emptyResult).forEach(row -> Arrays.fill(row, Solution.NO_SOLUTION));
        return new Solution(emptyResult);
    }

    private SolutionsInfo buildSolutionsInfo(Set<Solution> solutions, TelegramInfo telegramInfo) {
        return SolutionsInfo.builder()
                .solutions(solutions)
                .countSolutions(solutions.size())
                .telegramInfo(telegramInfo)
                .build();
    }
}
