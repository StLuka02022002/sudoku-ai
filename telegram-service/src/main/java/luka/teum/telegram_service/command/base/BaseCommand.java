package luka.teum.telegram_service.command.base;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import luka.teum.telegram_service.model.entity.User;
import luka.teum.telegram_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.IBotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Getter
public abstract class BaseCommand implements IBotCommand {
    private final Commands command;

    @Autowired
    private UserService userService;

    public BaseCommand(Commands command) {
        this.command = command;
    }

    @Override
    public String getCommandIdentifier() {
        return command.getCommandIdentifier();
    }

    @Override
    public String getDescription() {
        return command.getDescription();
    }

    public User getUserByTelegramId(Message message, AbsSender absSender) {
        //TODO поменять id чат на id пользователя
        Long userTelegramId = message.getChatId();
        User user = userService.getUserByTelegramId(userTelegramId);
        if (user == null) {
            log.error("User with userTelegramId:{} not found on the command /{}", userTelegramId, getCommandIdentifier());
            //TODO возможно нужно убрать
            String text = "Мы не нашли Вас в нашей базе!";
            SendMessage answer = new SendMessage();
            answer.setChatId(message.getChatId());
            answer.setText(text);
            sendMessage(answer, absSender);
        }
        return user;
    }

    public void sendMessage(SendMessage answer, AbsSender absSender) {
        try {
            absSender.execute(answer);
        } catch (TelegramApiException e) {
            log.error("Error occurred in /{} command", getCommandIdentifier(), e);
        }
    }
}
