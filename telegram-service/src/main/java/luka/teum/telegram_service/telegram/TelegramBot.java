package luka.teum.telegram_service.telegram;

import luka.teum.telegram_service.config.TelegramBotConfig;
import luka.teum.telegram_service.messaging.KafkaProducerService;
import luka.teum.telegram_service.service.ImageService;
import messaging.TelegramInfo;
import messaging.image.ImageInfo;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class TelegramBot extends TelegramLongPollingCommandBot {

    private final TelegramBotConfig telegramBotConfig;
    private final KafkaProducerService kafkaProducerService;
    private final ImageService imageService;

    public TelegramBot(TelegramBotConfig telegramBotConfig, KafkaProducerService kafkaProducerService) {
        super(telegramBotConfig.getToken());
        this.telegramBotConfig = telegramBotConfig;
        this.kafkaProducerService = kafkaProducerService;
        this.imageService = new ImageService(this);
    }

    @Override
    public String getBotUsername() {
        return telegramBotConfig.getName();
    }

    @Override
    public void processNonCommandUpdate(Update update) {
        if (update.hasMessage() && update.getMessage().hasPhoto()) {
            Message message = update.getMessage();
            Long userId = message.getFrom().getId();
            String result = imageService.process(userId, message.getPhoto());
            if (result != null) {
                TelegramInfo telegramInfo = TelegramInfo.builder()
                        .userId(userId)
                        .chartId(message.getChatId())
                        .messageId(message.getMessageId().longValue())
                        .info("info")
                        .build();
                ImageInfo imageInfo = ImageInfo.builder()
                        .imagePath(result)
                        .telegramInfo(telegramInfo)
                        .build();
                kafkaProducerService.sendImageProcessingInfo(imageInfo);
            }
            String answer = result != null ? "Фото скачано!" : "Печалька";
            SendMessage response = new SendMessage();
            response.setChatId(message.getChatId().toString());
            response.setText(answer);
            try {
                execute(response);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
