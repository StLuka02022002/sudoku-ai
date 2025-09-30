package luka.teum.image_service.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import messaging.image.ImageOneInfo;
import messaging.image.ImagesInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    public final int DEFAULT_TIMEOUT_MS = 2 * 60 * 1000;


    @Value("${app.kafka.topics.images-processing-info}")
    private String imagesProcessingInfoTopic;

    @Value("${app.kafka.topics.images-processing-one-info}")
    private String imagesProcessingOneInfoTopic;

    private void sendImageProcessingAsync(String key, Object data, String topic) {
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, key, data);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Sent message successfully to topic: {}, partition: {}, offset: {}",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send message to topic: {}. Error: {}",
                        topic, ex.getMessage(), ex);
            }
        });
    }

    public void sendImagesProcessingInfoAsync(ImagesInfo imagesInfo) {
        String key = imagesInfo.getTelegramInfo().getUserId().toString();
        this.sendImageProcessingAsync(key, imagesInfo, imagesProcessingInfoTopic);
    }

    public void sendImageProcessingOneInfoAsync(ImageOneInfo imagesOneInfo) {
        String key = imagesOneInfo.getTelegramInfo().getUserId().toString();
        this.sendImageProcessingAsync(key, imagesOneInfo, imagesProcessingOneInfoTopic);
    }

    public boolean sendImageProcessing(String key, Object data, String topic) {
        try {

            SendResult<String, Object> result = kafkaTemplate.send(topic, key, data)
                    .get();
            log.debug("Message sent successfully to partition: {}",
                    result.getRecordMetadata().partition());
            return true;
        } catch (Exception e) {
            log.error("Failed to send message synchronously", e);
            return false;
        }
    }

    public boolean sendImageProcessingInfo(ImagesInfo imagesInfo) {
        String key = imagesInfo.getTelegramInfo().getUserId().toString();
        return this.sendImageProcessing(key, imagesInfo, imagesProcessingInfoTopic);
    }

    public boolean sendImageProcessingOneInfo(ImageOneInfo imageOneInfo) {
        String key = imageOneInfo.getTelegramInfo().getUserId().toString();
        return this.sendImageProcessing(key, imageOneInfo, imagesProcessingOneInfoTopic);
    }

    private boolean sendWithTimeout(String key, Object data, String topic, long timeoutMs) {
        try {
            SendResult<String, Object> result = kafkaTemplate.send(topic, key, data)
                    .get(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
            log.info("Message sent successfully. Key: {}, Partition: {}, Offset: {}",
                    key,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            return true;
        } catch (Exception e) {
            log.error("Failed to send message with timeout", e);
            return false;
        }
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
