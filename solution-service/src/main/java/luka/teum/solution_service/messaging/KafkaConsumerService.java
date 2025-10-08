package luka.teum.solution_service.messaging;

import lombok.extern.slf4j.Slf4j;
import luka.teum.solution_service.solver.SudokuSolver;
import messaging.Solution;
import messaging.TelegramInfo;
import messaging.solution.SolutionsInfo;
import messaging.solution.SolutionsOneInfo;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class KafkaConsumerService {

    private final KafkaProducerService kafkaProducerService;
    private final SudokuSolver solver;

    public KafkaConsumerService(KafkaProducerService kafkaProducerService) {
        this.kafkaProducerService = kafkaProducerService;
        this.solver = new SudokuSolver();
    }

    @KafkaListener(
            topics = "${app.kafka.topics.solutions-processing-info}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void processSolutions(@Payload SolutionsInfo solutionsInfo,
                                 @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                 @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
                                 @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                 Acknowledgment ack) {
        log.info("Received image processing request. Key: {}, Partition: {}, Topic: {}",
                key, partition, topic);
        log.debug("Image details: {}", solutionsInfo);

        try {
            Set<Solution> solutions = solutionsInfo.getSolutions().stream()
                    .map(solver::solve)
                    .filter(Solution::isSolved)
                    .collect(Collectors.toSet());

            solutions.forEach(solution -> {
                SolutionsOneInfo answer = this.buildSolutionONeInfo(solution, solutionsInfo.getTelegramInfo());
                kafkaProducerService.sendSudokuSolutionSync(answer);
            });

            log.debug("Successfully processed solutions: {}", solutions.size());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error submitting images processing task: {}. Error: {}",
                    solutionsInfo.getSolutions().size(), e.getMessage(), e);
        }
    }

    @KafkaListener(
            topics = "${app.kafka.topics.solutions-processing-one-info}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void processSolutionsOne(@Payload SolutionsOneInfo solutionsOneInfo,
                                    @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                    @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
                                    @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                    Acknowledgment ack) {

    }

    private SolutionsOneInfo buildSolutionONeInfo(Solution solution, TelegramInfo telegramInfo) {
        return SolutionsOneInfo.builder()
                .solution(solution)
                .solutionId(0)
                .countSolution(1)
                .telegramInfo(telegramInfo)
                .build();
    }
}
