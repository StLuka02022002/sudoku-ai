package luka.teum.telegram_service.messaging;

import lombok.extern.slf4j.Slf4j;
import messaging.BaseKafkaProducerService;
import messaging.image.ImageInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaProducerService extends BaseKafkaProducerService {

    @Value("${app.kafka.topics.image-processing-info}")
    private String imageProcessingInfoTopic;

    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate) {
        super(kafkaTemplate);
    }

    public void sendImagesProcessingInfoAsync(ImageInfo imagesInfo) {
        String key = imagesInfo.getTelegramInfo().getUserId().toString();
        this.sendAsync(key, imagesInfo, imageProcessingInfoTopic);
    }

    public boolean sendImageProcessingInfo(ImageInfo imagesInfo) {
        String key = imagesInfo.getTelegramInfo().getUserId().toString();
        return this.sendSync(key, imagesInfo, imageProcessingInfoTopic);
    }

    public boolean sendWithTimeout(ImageInfo imagesInfo, long timeoutMs) {
        String key = imagesInfo.getTelegramInfo().getUserId().toString();
        return this.sendWithTimeout(key, imagesInfo, imageProcessingInfoTopic, timeoutMs);
    }

    public boolean sendWithTimeout(ImageInfo imagesInfo) {
        return this.sendWithTimeout(imagesInfo, DEFAULT_TIMEOUT_MS);
    }
}
