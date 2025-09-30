package luka.teum.image_service.messaging;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import luka.teum.image_service.processing.ImageProcessing;
import luka.teum.image_service.storage.FileStorage;
import messaging.image.ImageInfo;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class KafkaConsumerService {

    private final FileStorage fileStorage;
    private final KafkaProducerService kafkaProducerService;
    private final ExecutorService executorService;

    public KafkaConsumerService(FileStorage fileStorage, KafkaProducerService kafkaProducerService) {
        this.fileStorage = fileStorage;
        this.kafkaProducerService = kafkaProducerService;
        this.executorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors()
        );
    }

    @KafkaListener(
            topics = "${app.kafka.topics.image-processing-info}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void processImages(@Payload ImageInfo imageInfo,
                              @Header(KafkaHeaders.RECEIVED_KEY) String key,
                              @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
                              @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                              Acknowledgment ack) {

        log.info("Received image processing request. Key: {}, Partition: {}, Topic: {}",
                key, partition, topic);
        log.debug("Image details: {}", imageInfo);

        try {
            ImageProcessing imageProcessing = new ImageProcessing(fileStorage, kafkaProducerService);

            executorService.submit(() -> {
                try {
                    imageProcessing.processing(imageInfo);
                    log.debug("Successfully processed image: {}", imageInfo.getImagePath());
                } catch (Exception e) {
                    log.error("Error processing image: {}", imageInfo.getImagePath(), e);
                }
            });

            ack.acknowledge();

        } catch (Exception e) {
            log.error("Error submitting image processing task: {}", imageInfo.getImagePath(), e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
