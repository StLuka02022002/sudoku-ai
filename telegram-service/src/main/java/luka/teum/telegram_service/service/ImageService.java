package luka.teum.telegram_service.service;

import lombok.extern.slf4j.Slf4j;
import luka.teum.telegram_service.storage.FileStorage;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import storage.Storage;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ImageService {

    private static final String INPUT_IMAGE_PREFIX = "input\\";

    private final DefaultAbsSender defaultAbsSender;
    private final Storage<java.io.File> storage;

    public ImageService(DefaultAbsSender defaultAbsSender) {
        this.defaultAbsSender = defaultAbsSender;
        this.storage = new FileStorage();
    }

    public String process(Long userId, List<PhotoSize> photos) {
        if (photos == null || photos.isEmpty()) {
            log.warn("Empty photos list for user: {}", userId);
            return null;
        }

        PhotoSize bestPhoto = this.selectBestQualityPhoto(photos);
        return process(userId, bestPhoto);
    }

    public String process(Long userId, PhotoSize photo) {
        if (photo == null) {
            log.warn("Null photo provided for user: {}", userId);
            return null;
        }

        try {
            java.io.File downloadedFile = this.downloadPhoto(photo);
            if (downloadedFile == null) {
                return null;
            }

            String fileName = this.generateFileName(userId);
            boolean saved = storage.saveData(fileName, downloadedFile);

            if (!saved) {
                log.error("Failed to save file for user: {}", userId);
                this.cleanupTempFile(downloadedFile);
            }

            return saved ? fileName : null;

        } catch (Exception e) {
            log.error("Telegram API error for user: {}", userId, e);
            return null;
        }
    }

    private PhotoSize selectBestQualityPhoto(List<PhotoSize> photos) {
        return photos.stream()
                .max(Comparator.comparing(photo -> photo.getWidth() * photo.getHeight()))
                .orElseGet(() -> {
                    log.debug("Using last photo as fallback");
                    return photos.get(photos.size() - 1);
                });
    }

    private java.io.File downloadPhoto(PhotoSize photo) throws TelegramApiException {
        GetFile getFile = new GetFile(photo.getFileId());
        File file = defaultAbsSender.execute(getFile);

        if (file == null || file.getFilePath() == null) {
            log.error("Invalid file response from Telegram API");
            return null;
        }

        return defaultAbsSender.downloadFile(file.getFilePath());
    }

    private String generateFileName(Long userId) {
        return INPUT_IMAGE_PREFIX + userId + "\\" + UUID.randomUUID();
    }

    private void cleanupTempFile(java.io.File file) {
        if (file != null && file.exists()) {
            try {
                if (!file.delete()) {
                    log.warn("Failed to delete temporary file: {}", file.getAbsolutePath());
                }
            } catch (SecurityException e) {
                log.warn("Security exception when deleting temp file: {}", e.getMessage());
            }
        }
    }
}
