package luka.teum.solution_service.messaging;

import messaging.BaseKafkaProducerService;
import messaging.solution.SolutionsOneInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService extends BaseKafkaProducerService {

    @Value("${app.kafka.topics.sudoku-solution}")
    private String sudokuSolutionTopic;

    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate) {
        super(kafkaTemplate);
    }

    public void sendSudokuSolutionAsync(SolutionsOneInfo solutionsInfo) {
        String key = solutionsInfo.getTelegramInfo().getUserId().toString();
        this.sendAsync(key, solutionsInfo, sudokuSolutionTopic);
    }

    public void sendSudokuSolutionSync(SolutionsOneInfo solutionsInfo) {
        String key = solutionsInfo.getTelegramInfo().getUserId().toString();
        this.sendSync(key, solutionsInfo, sudokuSolutionTopic);
    }

    public void sendWithTimeout(SolutionsOneInfo solutionsInfo, long timeoutMs) {
        String key = solutionsInfo.getTelegramInfo().getUserId().toString();
        this.sendWithTimeout(key, solutionsInfo, sudokuSolutionTopic, timeoutMs);
    }


    public void sendWithTimeout(SolutionsOneInfo solutionsOneInfo) {
        String key = solutionsOneInfo.getTelegramInfo().getUserId().toString();
        this.sendWithTimeout(key, solutionsOneInfo, sudokuSolutionTopic, DEFAULT_TIMEOUT_MS);
    }
}
