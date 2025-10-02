package luka.teum.telegram_service.telegram;

import luka.teum.telegram_service.config.TelegramBotConfig;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class TelegramBot extends TelegramLongPollingCommandBot {

    private final TelegramBotConfig telegramBotConfig;

    public TelegramBot(TelegramBotConfig telegramBotConfig) {
        super(telegramBotConfig.getToken());
        this.telegramBotConfig = telegramBotConfig;
    }

    @Override
    public String getBotUsername() {
        return telegramBotConfig.getName();
    }

    @Override
    public void processNonCommandUpdate(Update update) {

    }
}
