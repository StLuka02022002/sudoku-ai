package luka.teum.dl_service.messaging;

import messaging.image.ImagesInfo;
import messaging.image.ImagesOneInfo;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

public class KafkaConsumerService {

    @KafkaListener(
            topics = "${app.kafka.topics.solutions-processing-info}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void processSolutions(@Payload ImagesInfo imagesInfo,
                                 @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                 @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
                                 @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                 Acknowledgment ack) {
    }

    @KafkaListener(
            topics = "${app.kafka.topics.solutions-processing-one-info}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void processSolutionsOne(@Payload ImagesOneInfo imagesOneInfo,
                                    @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                    @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
                                    @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                    Acknowledgment ack) {

    }


}
