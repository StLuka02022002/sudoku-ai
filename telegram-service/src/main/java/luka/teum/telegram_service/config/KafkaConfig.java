package luka.teum.telegram_service.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    @Value("${app.kafka.topics.image-processing-info}")
    private String imageProcessingInfoTopic;

    @Value("${app.kafka.topics.sudoku-solution}")
    private String sudokuSolutionTopic;

    @Bean
    public NewTopic imageProcessingInfoTopic() {
        return TopicBuilder.name(imageProcessingInfoTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic sudokuSolutionTopic() {
        return TopicBuilder.name(sudokuSolutionTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
