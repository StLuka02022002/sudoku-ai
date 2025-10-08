package luka.teum.dl_service.processing;

import lombok.extern.slf4j.Slf4j;
import luka.teum.dl_service.ai.Algorithm;
import luka.teum.dl_service.messaging.KafkaProducerService;
import luka.teum.dl_service.prepare.ImagePrepare;
import luka.teum.image_service.util.ImageUtil;
import messaging.Solution;
import messaging.TelegramInfo;
import messaging.image.ImagesInfo;
import messaging.solution.SolutionsInfo;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import storage.Storage;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class ImageProcessing {

    private static final String DIGIT_IMAGE_PREFIX = "digits\\";

    private final Storage<Mat> storage;
    private final KafkaProducerService kafkaProducerService;
    private final ImageUtil imageUtil;
    private final ImagePrepare imagePrepare;
    private final Algorithm algorithm;

    public ImageProcessing(Storage<Mat> storage, KafkaProducerService kafkaProducerService) {
        this.storage = storage;
        this.kafkaProducerService = kafkaProducerService;
        this.algorithm = new Algorithm();
        this.imageUtil = new ImageUtil();
        this.imagePrepare = new ImagePrepare(imageUtil);
    }

    public ImageProcessing(Storage<Mat> storage, KafkaProducerService kafkaProducerService, String modelPath) {
        this.storage = storage;
        this.kafkaProducerService = kafkaProducerService;
        if (modelPath != null) {
            this.algorithm = new Algorithm(modelPath);
        } else {
            this.algorithm = new Algorithm();
        }
        this.imageUtil = new ImageUtil();
        this.imagePrepare = new ImagePrepare(imageUtil);
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
        Mat[][] digits = null;
        try {
            image = this.storage.getData(imagePath);
            digits = this.imagePrepare.prepare(image);
            int[][] result = this.evaluateDigits(digits);
            this.saveImagesDigits(digits, imagePath);
            return new Solution(result);
        } catch (Exception e) {
            log.error("Error processing image: {}", imagePath, e);
            return this.createEmptySolution();
        } finally {
            imageUtil.releaseMats(image);
            imageUtil.releaseSubMats(digits);
        }
    }

    private void saveImagesDigits(Mat[][] digits, String imagePath) {
        for (int i = 0; i < digits.length; i++) {
            for (int j = 0; j < digits[0].length; j++) {
                String path = this.generateImageFileName(DIGIT_IMAGE_PREFIX, imagePath, i * 10 + j);
                if (digits[i][j] == null || digits[i][j].empty()) {
                    this.storage.saveData(path, new Mat(ImagePrepare.IMAGE_DIGIT_SIZE, CvType.CV_8U));
                } else {
                    this.storage.saveData(path, digits[i][j]);
                }
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

    private String generateImageFileName(String prefix, String wrappedImagePath, int digit) {
        String imagePath = wrappedImagePath.replace('/', '\\');
        int dotIndex = imagePath.indexOf('\\');
        String imageName = (dotIndex == -1) ? imagePath : imagePath.substring(dotIndex + 1);

        dotIndex = imageName.lastIndexOf('.');
        imageName = (dotIndex == -1) ? imageName : imageName.substring(0, dotIndex);

        return prefix + imageName + "\\" +
                this.digitToString(digit) + "_"
                + UUID.randomUUID();
    }

    private String digitToString(int digit) {
        if (digit < 10) {
            return "0" + digit;
        } else return String.valueOf(digit);
    }
}
