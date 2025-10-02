package luka.teum.dl_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${app.kafka.topics.images-processing-info}")
    private String imagesProcessingInfoTopic;

    @Value("${app.kafka.topics.images-processing-one-info}")
    private String imagesProcessingOneInfoTopic;

    @Value("${app.kafka.topics.solutions-processing-info}")
    private String solutionsProcessingInfoTopic;

    @Value("${app.kafka.topics.solutions-processing-one-info}")
    private String solutionsProcessingOneInfoTopic;

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
    @Bean
    public NewTopic solutionsProcessingInfoTopic() {
        return TopicBuilder.name(solutionsProcessingInfoTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
    @Bean
    public NewTopic solutionsProcessingOneInfoTopic() {
        return TopicBuilder.name(solutionsProcessingOneInfoTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
