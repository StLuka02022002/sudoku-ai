package luka.teum.image_service.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import messaging.TelegramInfo;
import messaging.image.ImageInfo;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    @Value("${app.kafka.topics.image-processing-info}")
    private String imageProcessingInfoTopic;

    @Value("${app.kafka.topics.images-processing-info}")
    private String imagesProcessingInfoTopic;

    @Value("${app.kafka.topics.images-processing-one-info}")
    private String imagesProcessingOneInfoTopic;

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Random random = new Random();

    @Bean
    public NewTopic imageProcessingInfoTopic() {
        return TopicBuilder.name(imageProcessingInfoTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic imagesProcessingInfoTopic() {
        return TopicBuilder.name(imagesProcessingInfoTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic imagesProcessingOneInfoTopic() {
        return TopicBuilder.name(imagesProcessingOneInfoTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

//    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.SECONDS)
//    public void sendData() {
//        try {
//
//            TelegramInfo telegramInfo = TelegramInfo.builder()
//                    .userId(random.nextLong())
//                    .username(UUID.randomUUID().toString())
//                    .messageId(random.nextLong())
//                    .chartId(random.nextLong())
//                    .info(UUID.randomUUID().toString())
//                    .build();
//
//            ImageInfo imageInfo = ImageInfo.builder()
//                    .imagePath("C:\\Code\\Neyro\\opencv\\Info.PNG")
//                    .telegramInfo(telegramInfo)
//                    .build();
//
//            SendResult<String, Object> result = kafkaTemplate.send(imageProcessingInfoTopic,
//                            telegramInfo.getUserId().toString(), imageInfo)
//                    .get();
//            log.info("Message sent successfully to partition: {}",
//                    result.getRecordMetadata().partition());
//        } catch (Exception e) {
//            log.error("Failed to send message synchronously", e);
//        }
//    }
}
