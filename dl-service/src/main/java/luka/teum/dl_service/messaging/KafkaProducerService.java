package luka.teum.dl_service.messaging;

import messaging.BaseKafkaProducerService;
import messaging.solution.SolutionsInfo;
import messaging.solution.SolutionsOneInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService extends BaseKafkaProducerService {

    @Value("${app.kafka.topics.solutions-processing-info}")
    private String solutionsProcessingInfoTopic;

    @Value("${app.kafka.topics.solutions-processing-one-info}")
    private String solutionsProcessingOneInfoTopic;

    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate) {
        super(kafkaTemplate);
    }

    private void sendSolutionsProcessingInfoAsync(SolutionsInfo solutionsInfo) {
        String key = solutionsInfo.getTelegramInfo().getUserId().toString();
        this.sendAsync(key, solutionsInfo, solutionsProcessingInfoTopic);
    }

    private void sendSolutionsProcessingIneInfoAsync(SolutionsOneInfo solutionsOneInfo) {
        String key = solutionsOneInfo.getTelegramInfo().getUserId().toString();
        this.sendAsync(key, solutionsOneInfo, solutionsProcessingInfoTopic);
    }

    private void sendSolutionsProcessingInfoSync(SolutionsInfo solutionsInfo) {
        String key = solutionsInfo.getTelegramInfo().getUserId().toString();
        this.sendSync(key, solutionsInfo, solutionsProcessingInfoTopic);
    }

    private void sendSolutionsProcessingIneInfoSync(SolutionsOneInfo solutionsOneInfo) {
        String key = solutionsOneInfo.getTelegramInfo().getUserId().toString();
        this.sendSync(key, solutionsOneInfo, solutionsProcessingInfoTopic);
    }

    private void sendWithTimeout(SolutionsInfo solutionsInfo, long timeoutMs) {
        String key = solutionsInfo.getTelegramInfo().getUserId().toString();
        this.sendWithTimeout(key, solutionsInfo, solutionsProcessingInfoTopic, timeoutMs);
    }

    private void sendWithTimeout(SolutionsInfo solutionsInfo) {
        String key = solutionsInfo.getTelegramInfo().getUserId().toString();
        this.sendWithTimeout(key, solutionsInfo, solutionsProcessingInfoTopic, DEFAULT_TIMEOUT_MS);
    }

    private void sendWithTimeout(SolutionsOneInfo solutionsOneInfo) {
        String key = solutionsOneInfo.getTelegramInfo().getUserId().toString();
        this.sendWithTimeout(key, solutionsOneInfo, solutionsProcessingInfoTopic, DEFAULT_TIMEOUT_MS);
    }
}
