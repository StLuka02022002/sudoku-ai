package luka.teum.telegram_service.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import luka.teum.telegram_service.command.base.Commands;
import luka.teum.telegram_service.telegram.TelegramBot;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.IBotCommand;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class TelegramBotInitializer {

    private final TelegramBot telegramBot;
    private final List<IBotCommand> commands;

    @EventListener({ContextRefreshedEvent.class})
    public void init() throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        try {
            commands.forEach(telegramBot::register);
            telegramBotsApi.registerBot(telegramBot);
            setMyCommands(telegramBot);
        } catch (TelegramApiException e) {
            log.error("Error in initialize bot: {}", e.getMessage(), e);
        }
    }

    private void setMyCommands(TelegramBot bot) {
        List<BotCommand> listOfCommands = Arrays.stream(Commands.values())
                .map(command -> new BotCommand("/" + command.getCommandIdentifier(),
                        command.getDescription()))
                .toList();
        try {
            bot.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error in initialize command bot: {}", e.getMessage(), e);
        }
    }
}
