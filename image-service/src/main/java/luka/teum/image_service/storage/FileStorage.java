package luka.teum.image_service.storage;

import exception.ImageNotFoundException;
import lombok.extern.slf4j.Slf4j;
import luka.teum.image_service.util.ImageUtil;
import org.opencv.core.Mat;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Component
public class FileStorage implements Storage {

    private static final String BASE_DIRECTORY = "images\\";
    private static final String FILE_IMAGE_EXTENSION = ".png";
    private static final String[] SUPPORTED_EXTENSIONS = {".png", ".jpg", ".jpeg", ".bmp", ".tiff"};

    private final ImageUtil imageUtil;

    public FileStorage(ImageUtil imageUtil) {
        this.imageUtil = imageUtil;
        log.debug("FileStorage initialized with ImageUtil dependency");
    }

    public FileStorage() {
        this.imageUtil = new ImageUtil();
        log.debug("FileStorage initialized with default constructor");
    }

    @Override
    public Mat getData(String location) {
        log.info("Attempting to load image from location: {}", location);

        validateInputLocation(location);
        File imageFile = validateImageFile(location);

        try {
            Mat image = imageUtil.loadImage(imageFile.getAbsolutePath());
            if (image.empty()) {
                log.error("Loaded image is empty from location: {}", location);
                throw new ImageNotFoundException("Loaded image is empty from location: " + location);
            }

            log.debug("Successfully loaded image. Size: {}x{}, channels: {}",
                    image.cols(), image.rows(), image.channels());
            return image;

        } catch (Exception e) {
            log.error("Failed to load image from location: {}. Error: {}", location, e.getMessage(), e);
            throw new ImageNotFoundException("Failed to load image from location: " + location, e);
        }
    }

    @Override
    public boolean saveData(String location, Mat mat) {
        log.info("Attempting to save image to location: {}", location);

        this.validateInputMat(mat);
        String filePath = this.buildFilePath(location);

        try {
            this.createParentDirectories(filePath);

            boolean success = imageUtil.saveImage(filePath, mat);
            if (success) {
                log.info("Image successfully saved to: {}", filePath);
                log.debug("Saved image details - Size: {}x{}, channels: {}",
                        mat.cols(), mat.rows(), mat.channels());
            } else {
                log.error("Failed to save image to: {}", filePath);
            }
            return success;

        } catch (Exception e) {
            log.error("Exception occurred while saving image to: {}. Error: {}",
                    filePath, e.getMessage(), e);
            return false;
        } finally {
            if (mat != null) {
                long approximateMemory = (long) mat.rows() * mat.cols() * mat.channels() * 4;
                if (approximateMemory > 10_000_000) {
                    log.warn("Large image processed - approx memory: {} MB", approximateMemory / 1_000_000);
                }
            }
        }
    }

    private void validateInputLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            log.error("Image location is null or empty");
            throw new IllegalArgumentException("Image location cannot be null or empty");
        }

        if (location.contains("..")) {
            log.warn("Potential path traversal attempt detected: {}", location);
        }
    }


    private File validateImageFile(String location) {
        File imageFile = new File(location);

        if (!imageFile.exists()) {
            log.error("Image file does not exist: {}", location);
            throw new ImageNotFoundException("Image file not found: " + location);
        }

        if (!imageFile.isFile()) {
            log.error("Path is not a file: {}", location);
            throw new ImageNotFoundException("Path is not a file: " + location);
        }

        if (!imageFile.canRead()) {
            log.error("No read permissions for file: {}", location);
            throw new ImageNotFoundException("No read permissions for file: " + location);
        }

        if (imageFile.length() == 0) {
            log.error("Image file is empty: {}", location);
            throw new ImageNotFoundException("Image file is empty: " + location);
        }

        if (!isSupportedExtension(location)) {
            log.warn("Unsupported file extension for: {}", location);
        }

        return imageFile;
    }

    private void validateInputMat(Mat mat) {
        if (mat == null) {
            log.error("Attempt to save null Mat object");
            throw new IllegalArgumentException("Mat object cannot be null");
        }

        if (mat.empty()) {
            log.error("Attempt to save empty Mat object");
            throw new IllegalArgumentException("Mat object cannot be empty");
        }

        if (mat.rows() == 0 || mat.cols() == 0) {
            log.error("Attempt to save Mat with zero dimensions: {}x{}", mat.rows(), mat.cols());
            throw new IllegalArgumentException("Mat object has invalid dimensions");
        }
    }

    private String buildFilePath(String location) {
        for (String ext : SUPPORTED_EXTENSIONS) {
            if (location.toLowerCase().endsWith(ext)) {
                log.debug("Using existing file extension for: {}", location);
                return location;
            }
        }

        String filePath = BASE_DIRECTORY + location + FILE_IMAGE_EXTENSION;
        log.debug("Added default extension to file path: {}", filePath);
        return filePath;
    }

    private void createParentDirectories(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        Path parentDir = path.getParent();

        if (parentDir != null && !Files.exists(parentDir)) {
            log.info("Creating parent directories for: {}", parentDir);
            Files.createDirectories(parentDir);
        }
    }

    private boolean isSupportedExtension(String filename) {
        String lowerCaseFilename = filename.toLowerCase();
        for (String ext : SUPPORTED_EXTENSIONS) {
            if (lowerCaseFilename.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
}