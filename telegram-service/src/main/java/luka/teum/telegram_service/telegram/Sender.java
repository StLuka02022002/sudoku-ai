package luka.teum.telegram_service.telegram;

import lombok.extern.slf4j.Slf4j;
import luka.teum.telegram_service.messaging.KafkaProducerService;
import luka.teum.telegram_service.service.ImageService;
import messaging.TelegramInfo;
import messaging.image.ImageInfo;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
public class Sender {

    private static final String SUCCESS_MESSAGE = "✅ Фото успешно получено и отправлено в обработку!";
    private static final String ERROR_MESSAGE = "❌ Произошла ошибка при обработке фото. Попробуйте еще раз.";
    private static final String INFO_MESSAGE = "📤 Отправляю фото в обработку...";

    private final ImageService imageService;
    private final DefaultAbsSender defaultAbsSender;
    private final KafkaProducerService kafkaProducerService;

    public Sender(DefaultAbsSender defaultAbsSender, KafkaProducerService kafkaProducerService) {
        this.defaultAbsSender = defaultAbsSender;
        this.imageService = new ImageService(defaultAbsSender);
        this.kafkaProducerService = kafkaProducerService;
    }

    public void photoProcess(Message message) {
        Long userId = message.getFrom().getId();
        Long chatId = message.getChatId();
        Integer messageId = message.getMessageId();
        Integer processingMessageId = null;

        log.info("Processing photo from user: {}, chat: {}", userId, chatId);

        try {
            processingMessageId = this.sendMessage(chatId, INFO_MESSAGE);

            String imagePath = imageService.process(userId, message.getPhoto());

            if (imagePath != null) {
                this.sendToKafka(userId, chatId, messageId, imagePath);
                if (processingMessageId != null) {
                    this.editMessage(chatId, processingMessageId, SUCCESS_MESSAGE);
                } else {
                    this.sendMessage(chatId, SUCCESS_MESSAGE);
                }
                log.info("Photo successfully processed for user: {}, path: {}", userId, imagePath);
            } else {
                if (processingMessageId != null) {
                    this.editMessage(chatId, processingMessageId, ERROR_MESSAGE);
                } else {
                    this.sendMessage(chatId, ERROR_MESSAGE);
                }
                log.error("Photo processing failed for user: {}", userId);
            }

        } catch (Exception e) {
            log.error("Unexpected error processing photo for user: {}", userId, e);
            if (processingMessageId != null) {
                this.editMessage(chatId, processingMessageId, ERROR_MESSAGE);
            } else {
                this.sendMessage(chatId, ERROR_MESSAGE);
            }
        }
    }

    private void sendToKafka(Long userId, Long chatId, Integer messageId, String imagePath) {
        try {
            ImageInfo imageInfo = this.buildImageInfo(imagePath, userId, chatId, messageId);

            kafkaProducerService.sendImageProcessingInfo(imageInfo);
            log.debug("Message sent to Kafka for user: {}", userId);

        } catch (Exception e) {
            log.error("Failed to send message to Kafka for user: {}", userId, e);
            throw new RuntimeException("Kafka sending failed", e);
        }
    }

    private TelegramInfo buildTelegramInfo(Long userId, Long chatId, Integer messageId) {
        return TelegramInfo.builder()
                .userId(userId)
                .chartId(chatId)
                .messageId(messageId.longValue())
                .info("image_processing")
                .build();
    }

    private ImageInfo buildImageInfo(String imagePath, Long userId, Long chatId, Integer messageId) {
        TelegramInfo telegramInfo = this.buildTelegramInfo(userId, chatId, messageId);
        return ImageInfo.builder()
                .imagePath(imagePath)
                .telegramInfo(telegramInfo)
                .build();
    }

    private Integer sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage(chatId.toString(), text);
        try {
            Message sendMessage = defaultAbsSender.execute(message);
            return sendMessage.getMessageId();
        } catch (TelegramApiException e) {
            log.error("Failed to send message to chat: {}", chatId, e);
            throw new RuntimeException("Message sending failed", e);
        }
    }

    private void editMessage(Long chatId, Integer messageId, String newText) {
        try {
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId.toString());
            editMessage.setMessageId(messageId);
            editMessage.setText(newText);

            defaultAbsSender.execute(editMessage);
        } catch (TelegramApiException e) {
            log.error("Failed to edit message in chat: {}, messageId: {}", chatId, messageId, e);
        }
    }
}
