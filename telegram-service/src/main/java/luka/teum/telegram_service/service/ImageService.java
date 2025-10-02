package luka.teum.telegram_service.service;

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

@Service
public class ImageService {
    private static final String INPUT_IMAGE_PREFIX = "input\\";
    private final DefaultAbsSender defaultAbsSender;
    private final Storage<java.io.File> storage;

    public ImageService(DefaultAbsSender defaultAbsSender) {
        this.defaultAbsSender = defaultAbsSender;
        this.storage = new FileStorage();
    }

    public boolean process(Long id, List<PhotoSize> photos) {
        PhotoSize photo = photos.stream()
                .max(Comparator.comparing(photoSize -> photoSize.getWidth() * photoSize.getHeight()))
                .orElse(photos.get(photos.size() - 1));
        return this.process(id, photo);
    }

    public boolean process(Long id, PhotoSize photo) {
        String fileId = photo.getFileId();
        GetFile getFile = new GetFile();
        getFile.setFileId(fileId);
        try {
            File file = defaultAbsSender.execute(getFile);
            String filePath = file.getFilePath();

            java.io.File downloadedFile = defaultAbsSender.downloadFile(filePath);
            String fileName = INPUT_IMAGE_PREFIX + id + "\\" + UUID.randomUUID();
            return storage.saveData(fileName, downloadedFile);
        } catch (TelegramApiException e) {
            return false;
        }
    }
}
