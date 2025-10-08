package luka.teum.telegram_service.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import messaging.Solution;
import messaging.TelegramInfo;
import messaging.solution.SolutionsOneInfo;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final DefaultAbsSender defaultAbsSender;

    @KafkaListener(
            topics = "${app.kafka.topics.sudoku-solution}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void processSolutions(@Payload SolutionsOneInfo solutionsOneInfo,
                                 @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                 @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
                                 @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                 Acknowledgment ack) {
        log.info("Received image processing request. Key: {}, Partition: {}, Topic: {}",
                key, partition, topic);
        log.debug("Solutions details: {}", solutionsOneInfo);


        try {
            if (solutionsOneInfo.getSolution().isSolved()) {
                this.sendSolution(solutionsOneInfo);
            } else {
                this.sendNoSolution(solutionsOneInfo);
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error submitting images processing task: {}. Error: {}",
                    solutionsOneInfo.getSolution().isSolved(), e.getMessage(), e);
        }
    }

    private void sendSolution(SolutionsOneInfo solutionsOneInfo) throws TelegramApiException {
        TelegramInfo telegramInfo = solutionsOneInfo.getTelegramInfo();
        Solution solution = solutionsOneInfo.getSolution();

        String formattedSolution = this.formatSudokuSolution(solution);
        String messageText = this.buildSuccessMessage(formattedSolution, solutionsOneInfo);

        this.sendTelegramMessage(telegramInfo, messageText);
    }

    private void sendSolutionWithTable(SolutionsOneInfo solutionInfo) throws TelegramApiException {
        TelegramInfo telegramInfo = solutionInfo.getTelegramInfo();
        String messageText = this.buildSuccessMessageWithTable(solutionInfo);
        this.sendTelegramMessage(telegramInfo, messageText);
    }

    private void sendNoSolution(SolutionsOneInfo solutionsOneInfo) throws TelegramApiException {
        TelegramInfo telegramInfo = solutionsOneInfo.getTelegramInfo();
        String messageText = this.buildNoSolutionMessage(solutionsOneInfo);

        this.sendTelegramMessage(telegramInfo, messageText);
    }

    private String formatSudokuSolution(Solution solution) {
        int[][] digits = solution.getDigits();
        StringBuilder sb = new StringBuilder();

        sb.append("<pre>");
        for (int i = 0; i < digits.length; i++) {
            if (i % 3 == 0 && i != 0) {
                sb.append("------+------+------\n");
            }

            for (int j = 0; j < digits[i].length; j++) {
                if (j % 3 == 0 && j != 0) {
                    sb.append("| ");
                }

                int digit = digits[i][j];
                String displayDigit = (digit == Solution.NO_SOLUTION) ? "✗" : String.valueOf(digit);
                sb.append(displayDigit).append(" ");
            }
            sb.append("\n");
        }
        sb.append("</pre>");

        return sb.toString();
    }

    private String formatSudokuSolutionTable(Solution solution) {
        int[][] digits = solution.getDigits();
        StringBuilder sb = new StringBuilder();

        sb.append("<code>\n");
        sb.append("--------------------\n");

        for (int i = 0; i < digits.length; i++) {
            sb.append("| ");
            for (int j = 0; j < digits[i].length; j++) {
                int digit = digits[i][j];
                String displayDigit = (digit == Solution.NO_SOLUTION) ? "✗" : String.valueOf(digit);
                sb.append(displayDigit).append(" ");

                if (j % 3 == 2 && j != 8) {
                    sb.append("| ");
                }
            }
            sb.append("| \n");

            if (i % 3 == 2 && i != 8) {
                sb.append("|------|------|------|\n");
            }
        }

        sb.append("--------------------\n");
        sb.append("</code>");

        return sb.toString();
    }

    private String buildSuccessMessage(String formattedSolution, SolutionsOneInfo solutionInfo) {
        return "<b>🎯 Sudoku Solved!</b>\n\n" +
                formattedSolution + "\n" +
                this.getSolutionStats(solutionInfo) +
                "\n<i>Generated by Sudoku Solver</i>";
    }

    private String buildSuccessMessageWithTable(SolutionsOneInfo solutionInfo) {
        String formattedTable = this.formatSudokuSolutionTable(solutionInfo.getSolution());
        return "<b>🎯 Sudoku Solved!</b>\n" +
                formattedTable + "\n" +
                this.getSolutionStats(solutionInfo) +
                "\n<i>Generated by Sudoku Solver</i>";
    }

    private String buildNoSolutionMessage(SolutionsOneInfo solutionInfo) {
        return "<b>❌ Unable to Solve Sudoku</b>\n\n" +
                "Sorry, I couldn't find a solution for this Sudoku puzzle.\n\n" +
                this.getSolutionStats(solutionInfo) +
                "\n<i>Please check if the image was clear and try again</i>";
    }

    private String getSolutionStats(SolutionsOneInfo solutionInfo) {
        StringBuilder stats = new StringBuilder();
        stats.append("<b>📊 Statistics:</b>\n");

        if (solutionInfo.getCountSolution() != null) {
            stats.append("• Total solutions processed: ").append(solutionInfo.getCountSolution()).append("\n");
        }

        if (solutionInfo.getSolutionId() != null) {
            stats.append("• Solution ID: ").append(solutionInfo.getSolutionId()).append("\n");
        }

        return stats.toString();
    }

    private void sendTelegramMessage(TelegramInfo telegramInfo, String messageText) throws TelegramApiException, TelegramApiException {
        SendMessage message = SendMessage.builder()
                .chatId(telegramInfo.getChartId().toString())
                .text(messageText)
                .parseMode("HTML")
                .build();

        if (telegramInfo.getUserId() != null) {
            message.setReplyToMessageId(telegramInfo.getMessageId() != null ?
                    telegramInfo.getMessageId().intValue() : null);
        }

        this.defaultAbsSender.execute(message);
    }
}
