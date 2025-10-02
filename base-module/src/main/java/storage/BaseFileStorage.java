package storage;

import exception.ImageNotFoundException;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public abstract class BaseFileStorage<T> implements Storage<T> {

    protected static final String BASE_DIRECTORY = "images\\";
    protected static final String FILE_IMAGE_EXTENSION = ".png";
    protected static final String[] SUPPORTED_EXTENSIONS = {".png", ".jpg", ".jpeg", ".bmp", ".tiff"};

    protected BaseFileStorage() {
        log.debug("{} initialized", this.getClass().getSimpleName());
    }

    protected abstract T loadDataFromFile(File file) throws Exception;

    protected abstract boolean saveDataToFile(String filePath, T data) throws Exception;

    protected abstract void validateInputData(T data);

    @Override
    public T getData(String location) {
        log.info("Attempting to load data from location: {}", location);

        this.validateInputLocation(location);
        File dataFile = this.getValidateDataFile(location);

        try {
            T data = this.loadDataFromFile(dataFile);
            log.debug("Successfully loaded data from: {}", location);
            return data;

        } catch (Exception e) {
            log.error("Failed to load data from location: {}. Error: {}", location, e.getMessage(), e);
            throw new ImageNotFoundException("Failed to load data from location: " + location, e);
        }
    }

    @Override
    public boolean saveData(String location, T data) {
        log.info("Attempting to save data to location: {}", location);

        this.validateInputData(data);
        this.validateInputLocation(location);
        String filePath = this.createFilePath(location);

        try {
            this.createParentDirectories(filePath);
            boolean success = this.saveDataToFile(filePath, data);

            if (success) {
                log.info("Data successfully saved to: {}", filePath);
            } else {
                log.error("Failed to save data to: {}", filePath);
            }
            return success;

        } catch (Exception e) {
            log.error("Exception occurred while saving data to: {}. Error: {}",
                    filePath, e.getMessage(), e);
            return false;
        }
    }

    protected void validateInputLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            log.error("Data location is null or empty");
            throw new IllegalArgumentException("Data location cannot be null or empty");
        }

        if (location.contains("..")) {
            log.warn("Potential path traversal attempt detected: {}", location);
        }

        if (location.length() > 500) {
            log.warn("Data location path is unusually long: {} characters", location.length());
        }
    }

    protected File getValidateDataFile(String location) {
        String filePath = this.createFilePath(location);
        File dataFile = new File(filePath);

        if (!dataFile.exists()) {
            log.error("Data file does not exist: {}", filePath);
            throw new ImageNotFoundException("Data file not found: " + filePath);
        }

        if (!dataFile.isFile()) {
            log.error("Path is not a file: {}", filePath);
            throw new ImageNotFoundException("Path is not a file: " + filePath);
        }

        if (!dataFile.canRead()) {
            log.error("No read permissions for file: {}", filePath);
            throw new ImageNotFoundException("No read permissions for file: " + filePath);
        }

        if (dataFile.length() == 0) {
            log.error("Data file is empty: {}", filePath);
            throw new ImageNotFoundException("Data file is empty: " + filePath);
        }

        if (!isSupportedExtension(location)) {
            log.warn("Unsupported file extension for: {}", location);
        }

        log.debug("File validation successful - Name: {}, Size: {} bytes, Path: {}",
                dataFile.getName(), dataFile.length(), dataFile.getAbsolutePath());
        return dataFile;
    }

    protected String createFilePath(String location) {
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

    protected void createParentDirectories(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        Path parentDir = path.getParent();

        if (parentDir != null && !Files.exists(parentDir)) {
            log.info("Creating parent directories for: {}", parentDir);
            Files.createDirectories(parentDir);

            if (!Files.isWritable(parentDir)) {
                throw new IOException("Cannot write to directory: " + parentDir);
            }
        }
    }

    protected boolean isSupportedExtension(String filename) {
        String lowerCaseFilename = filename.toLowerCase();
        for (String ext : SUPPORTED_EXTENSIONS) {
            if (lowerCaseFilename.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
}