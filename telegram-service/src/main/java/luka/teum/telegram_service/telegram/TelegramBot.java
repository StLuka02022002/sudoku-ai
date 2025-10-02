package luka.teum.telegram_service.telegram;

import lombok.extern.slf4j.Slf4j;
import luka.teum.telegram_service.config.TelegramBotConfig;
import luka.teum.telegram_service.messaging.KafkaProducerService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingCommandBot {

    private final TelegramBotConfig telegramBotConfig;
    private final Sender sender;

    public TelegramBot(TelegramBotConfig telegramBotConfig, KafkaProducerService kafkaProducerService) {
        super(telegramBotConfig.getToken());
        this.telegramBotConfig = telegramBotConfig;
        this.sender = new Sender(this, kafkaProducerService);
    }

    @Override
    public String getBotUsername() {
        return telegramBotConfig.getName();
    }

    @Override
    public void processNonCommandUpdate(Update update) {
        if (this.isPhotoMessage(update)) {
            sender.photoProcess(update.getMessage());
        }
    }

    private boolean isPhotoMessage(Update update) {
        return update.hasMessage() &&
                update.getMessage().hasPhoto() &&
                !update.getMessage().getPhoto().isEmpty();
    }

    @Override
    public void onRegister() {
        super.onRegister();
        log.info("Telegram bot successfully registered: {}", getBotUsername());
    }

    @Override
    public void processInvalidCommandUpdate(Update update) {
        log.warn("Invalid command received from update: {}", update.getUpdateId());
        try {
            execute(new SendMessage(update.getMessage()
                    .getChatId().toString(), "❌ Неизвестная команда"));
        } catch (TelegramApiException e) {
            log.error("Failed to send message to chat: {}", update.getMessage().getChatId(), e);
            throw new RuntimeException("Message sending failed", e);
        }
    }
}
