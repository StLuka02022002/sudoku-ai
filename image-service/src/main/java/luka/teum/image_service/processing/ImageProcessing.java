package luka.teum.image_service.processing;

import lombok.extern.slf4j.Slf4j;
import luka.teum.image_service.algorithm.Algorithms;
import luka.teum.image_service.algorithm.ImageAlgorithm;
import luka.teum.image_service.messaging.KafkaProducerService;
import luka.teum.image_service.util.ImageUtil;
import messaging.image.ImageInfo;
import messaging.image.ImagesInfo;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import storage.Storage;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class ImageProcessing {

    private final Storage<Mat> storage;
    private final KafkaProducerService kafkaProducerService;

    private static final String WRAPPED_IMAGE_PREFIX = "wrapped\\";
    private static final String PREPARE_IMAGE_PREFIX = "prepare\\";

    public ImageProcessing(Storage<Mat> storage, KafkaProducerService kafkaProducerService) {
        this.storage = storage;
        this.kafkaProducerService = kafkaProducerService;
    }

    public void processing(ImageInfo imageInfo) {
        Mat image = null;
        Algorithms algorithms = new Algorithms(this.defaultPrepareProcess(imageInfo));
        try {
            image = storage.getData(imageInfo.getImagePath());

            Point[][] points = algorithms.algorithm(image);
            Set<String> imagesPaths = this.processImagePoints(image, imageInfo, points);

            if (!imagesPaths.isEmpty()) {
                this.sendImagesInfo(imageInfo, imagesPaths);
            } else {
                log.warn("No images were successfully processed for: {}", imageInfo.getImagePath());
            }

        } catch (Exception e) {
            log.error("Error processing image: {}", imageInfo.getImagePath(), e);
        } finally {
            if (image != null) {
                image.release();
            }
        }
    }

    private Set<String> processImagePoints(Mat image, ImageInfo imageInfo, Point[][] points) {
        return Arrays.stream(points)
                .map(pointsArray -> this.processSingleImage(image, imageInfo, pointsArray))
                .filter(path -> path != null && !path.isEmpty())
                .collect(Collectors.toSet());
    }

    private ImageAlgorithm.PrepareProcess defaultPrepareProcess(ImageInfo imageInfo) {
        return mat -> {
            String fileName = generateImageFileName(PREPARE_IMAGE_PREFIX, imageInfo);

            if (storage.saveData(fileName, mat)) {
                log.debug("Success to save prepare image: {}", fileName);
            } else {
                log.debug("Failed to save prepare image: {}", fileName);
            }
        };
    }

    private String processSingleImage(Mat image, ImageInfo imageInfo, Point[] points) {
        Mat warpedImage = null;
        ImageUtil imageUtil = new ImageUtil();
        try {
            warpedImage = imageUtil.warpImage(image, points);
            String fileName = this.generateImageFileName(WRAPPED_IMAGE_PREFIX, imageInfo);

            if (storage.saveData(fileName, warpedImage)) {
                return fileName;
            }

            log.warn("Failed to save warped image: {}", fileName);
            return null;

        } catch (Exception e) {
            log.error("Error processing single image points", e);
            return null;
        } finally {
            if (warpedImage != null) {
                warpedImage.release();
            }
        }
    }

    private void sendImagesInfo(ImageInfo imageInfo, Set<String> imagesPaths) {
        ImagesInfo imagesInfo = ImagesInfo.builder()
                .telegramInfo(imageInfo.getTelegramInfo())
                .imagesPaths(imagesPaths)
                .countImages(imagesPaths.size())
                .build();

        kafkaProducerService.sendImageProcessingInfo(imagesInfo);
    }

    private String generateImageFileName(String prefix, ImageInfo imageInfo) {
        String imagePath = imageInfo.getImagePath().replace('/', '\\');
        String fileName = imagePath.substring(imagePath.lastIndexOf('\\') + 1);

        int dotIndex = fileName.lastIndexOf('.');
        String imageName = (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);

        return prefix +
                imageInfo.getTelegramInfo().getUserId() + "\\" +
                imageName + "_" +
                UUID.randomUUID();
    }
}
