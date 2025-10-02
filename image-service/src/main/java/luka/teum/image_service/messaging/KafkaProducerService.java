package luka.teum.image_service.messaging;

import lombok.extern.slf4j.Slf4j;
import messaging.BaseKafkaProducerService;
import messaging.image.ImagesOneInfo;
import messaging.image.ImagesInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

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

    public void sendImageProcessingOneInfoAsync(ImagesOneInfo imagesOneInfo) {
        String key = imagesOneInfo.getTelegramInfo().getUserId().toString();
        this.sendAsync(key, imagesOneInfo, imagesProcessingOneInfoTopic);
    }

    public boolean sendImageProcessingInfo(ImagesInfo imagesInfo) {
        String key = imagesInfo.getTelegramInfo().getUserId().toString();
        return this.sendSync(key, imagesInfo, imagesProcessingInfoTopic);
    }

    public boolean sendImageProcessingOneInfo(ImagesOneInfo imagesOneInfo) {
        String key = imagesOneInfo.getTelegramInfo().getUserId().toString();
        return this.sendSync(key, imagesOneInfo, imagesProcessingOneInfoTopic);
    }

    public boolean sendWithTimeout(ImagesInfo imagesInfo, long timeoutMs) {
        String key = imagesInfo.getTelegramInfo().getUserId().toString();
        return this.sendWithTimeout(key, imagesInfo, imagesProcessingInfoTopic, timeoutMs);
    }

    public boolean sendWithTimeout(ImagesInfo imagesInfo) {
        return this.sendWithTimeout(imagesInfo, DEFAULT_TIMEOUT_MS);
    }

    public boolean sendWithTimeout(ImagesOneInfo imagesOneInfo, long timeoutMs) {
        String key = imagesOneInfo.getTelegramInfo().getUserId().toString();
        return this.sendWithTimeout(key, imagesOneInfo, imagesProcessingOneInfoTopic, timeoutMs);
    }

    public boolean sendWithTimeout(ImagesOneInfo imagesOneInfo) {
        return this.sendWithTimeout(imagesOneInfo, DEFAULT_TIMEOUT_MS);
    }
}
