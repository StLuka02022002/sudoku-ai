package luka.teum.telegram_service.storage;

import exception.ImageNotFoundException;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
public class FileStorage implements Storage {

    private static final String BASE_DIRECTORY = "images\\";
    private static final String FILE_IMAGE_EXTENSION = ".png";
    private static final String[] SUPPORTED_EXTENSIONS = {".png", ".jpg", ".jpeg", ".bmp", ".tiff"};

    @Override
    public File getData(String location) {

        return validateImageFile(location);
    }

    @Override
    public boolean saveData(String location, File file) {
        String fileName = this.buildFilePath(location);
        try {
            this.createParentDirectories(fileName);
            File outputFile = new File(fileName);
            Files.move(file.toPath(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            return false;
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

    private void createParentDirectories(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        Path parentDir = path.getParent();

        if (parentDir != null && !Files.exists(parentDir)) {
            log.info("Creating parent directories for: {}", parentDir);
            Files.createDirectories(parentDir);
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
