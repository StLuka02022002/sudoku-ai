package luka.teum.telegram_service.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import storage.BaseFileStorage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@Slf4j
@Component
public class FileStorage extends BaseFileStorage<File> {

    public FileStorage() {
        super();
    }

    @Override
    protected File loadDataFromFile(File file) throws Exception {
        return file;
    }

    @Override
    protected boolean saveDataToFile(String filePath, File file) throws Exception {
        File outputFile = new File(filePath);
        Files.move(file.toPath(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    @Override
    protected void validateInputData(File data) {
        if (data == null) {
            log.error("Attempt to save null File object");
            throw new IllegalArgumentException("File object cannot be null");
        }

        if (!data.exists()) {
            log.error("Attempt to save non-existent file: {}", data.getAbsolutePath());
            throw new IllegalArgumentException("File does not exist: " + data.getAbsolutePath());
        }

        if (!data.isFile()) {
            log.error("Attempt to save directory as file: {}", data.getAbsolutePath());
            throw new IllegalArgumentException("Path is not a file: " + data.getAbsolutePath());
        }

        if (data.length() == 0) {
            log.error("Attempt to save empty file: {}", data.getAbsolutePath());
            throw new IllegalArgumentException("File is empty: " + data.getAbsolutePath());
        }

        if (!data.canRead()) {
            log.error("No read permissions for file: {}", data.getAbsolutePath());
            throw new IllegalArgumentException("No read permissions for file: " + data.getAbsolutePath());
        }
    }

    @Override
    public boolean saveData(String location, File file) {
        boolean result = super.saveData(location, file);

        if (file != null && file.exists()) {
            long fileSize = file.length();
            if (fileSize > 10_000_000) {
                log.warn("Large file processed - size: {} MB", fileSize / 1_000_000);
            }
        }

        return result;
    }
}