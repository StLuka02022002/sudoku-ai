package luka.teum.image_service.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import messaging.BaseKafkaProducerService;
import messaging.image.ImageOneInfo;
import messaging.image.ImagesInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class KafkaProducerService extends BaseKafkaProducerService {

    @Value("${app.kafka.topics.images-processing-info}")
    private String imagesProcessingInfoTopic;

    @Value("${app.kafka.topics.images-processing-one-info}")
    private String imagesProcessingOneInfoTopic;

    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate) {
        super(kafkaTemplate);
    }

    public void sendImagesProcessingInfoAsync(ImagesInfo imagesInfo) {
        String key = imagesInfo.getTelegramInfo().getUserId().toString();
        this.sendAsync(key, imagesInfo, imagesProcessingInfoTopic);
    }

    public void sendImageProcessingOneInfoAsync(ImageOneInfo imagesOneInfo) {
        String key = imagesOneInfo.getTelegramInfo().getUserId().toString();
        this.sendAsync(key, imagesOneInfo, imagesProcessingOneInfoTopic);
    }

    public boolean sendImageProcessingInfo(ImagesInfo imagesInfo) {
        String key = imagesInfo.getTelegramInfo().getUserId().toString();
        return this.sendSync(key, imagesInfo, imagesProcessingInfoTopic);
    }

    public boolean sendImageProcessingOneInfo(ImageOneInfo imageOneInfo) {
        String key = imageOneInfo.getTelegramInfo().getUserId().toString();
        return this.sendSync(key, imageOneInfo, imagesProcessingOneInfoTopic);
    }

    public boolean sendWithTimeout(ImagesInfo imagesInfo, long timeoutMs) {
        String key = imagesInfo.getTelegramInfo().getUserId().toString();
        return this.sendWithTimeout(key, imagesInfo, imagesProcessingInfoTopic, timeoutMs);
    }

    public boolean sendWithTimeout(ImagesInfo imagesInfo) {
        return this.sendWithTimeout(imagesInfo, DEFAULT_TIMEOUT_MS);
    }

    public boolean sendWithTimeout(ImageOneInfo imageOneInfo, long timeoutMs) {
        String key = imageOneInfo.getTelegramInfo().getUserId().toString();
        return this.sendWithTimeout(key, imageOneInfo, imagesProcessingOneInfoTopic, timeoutMs);
    }

    public boolean sendWithTimeout(ImageOneInfo imageOneInfo) {
        return this.sendWithTimeout(imageOneInfo, DEFAULT_TIMEOUT_MS);
    }
}
