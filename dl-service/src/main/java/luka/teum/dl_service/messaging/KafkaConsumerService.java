package luka.teum.dl_service.messaging;

import lombok.extern.slf4j.Slf4j;
import luka.teum.dl_service.processing.ImageProcessing;
import luka.teum.image_service.storage.MatFileStorage;
import messaging.image.ImagesInfo;
import messaging.image.ImagesOneInfo;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaConsumerService {

    private final MatFileStorage matFileStorage;
    private final KafkaProducerService kafkaProducerService;
    private final ImageProcessing imageProcessing;

    public KafkaConsumerService(KafkaProducerService kafkaProducerService) {
        this.matFileStorage = new MatFileStorage();
        this.kafkaProducerService = kafkaProducerService;
        this.imageProcessing = new ImageProcessing(matFileStorage, kafkaProducerService);
    }

    @KafkaListener(
            topics = "${app.kafka.topics.images-processing-info}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void processSolutions(@Payload ImagesInfo imagesInfo,
                                 @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                 @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
                                 @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                 Acknowledgment ack) {
        log.info("Received image processing request. Key: {}, Partition: {}, Topic: {}",
                key, partition, topic);
        log.debug("Image details: {}", imagesInfo);

        try {
            imageProcessing.processing(imagesInfo);
            log.debug("Successfully processed images: {}", imagesInfo.getImagesPaths().size());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error submitting images processing task: {}. Error: {}",
                    imagesInfo.getImagesPaths().size(), e.getMessage(), e);
        }
    }

    @KafkaListener(
            topics = "${app.kafka.topics.images-processing-one-info}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void processSolutionsOne(@Payload ImagesOneInfo imagesOneInfo,
                                    @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                    @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
                                    @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                    Acknowledgment ack) {

    }
}
