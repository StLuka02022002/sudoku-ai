package luka.teum.telegram_service.command;

import luka.teum.telegram_service.command.base.BaseCommand;
import luka.teum.telegram_service.command.base.Commands;
import luka.teum.telegram_service.mapping.UserMapping;
import luka.teum.telegram_service.model.entity.User;
import luka.teum.telegram_service.service.UserService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;


@Service
public class StartCommand extends BaseCommand {

    private final String HELLO_MESSAGE = """
            Привет, %s!
            Я могу следить за всеми Вашими долгами и за всеми Вашими должниками.
            Хотите добавить долг?
            """;

    private final UserService userService;
    private final UserMapping userMapping;

    public StartCommand(UserService userService, UserMapping userMapping) {
        super(Commands.START);
        this.userService = userService;
        this.userMapping = userMapping;
    }

    @Override
    public void processMessage(AbsSender absSender, Message message, String[] strings) {
        User user = createUser(message);
        printHello(user, message, absSender);
    }

    private User createUser(Message message) {
        //TODO поменять id чат на id пользователя
        Long userTelegramId = message.getChat().getId();
        if (!userService.existsByTelegramId(userTelegramId)) {
            User user = userMapping.getUserEntityByChart(message.getChat());
            return userService.save(user);
        } else {
            return userService.getUserByTelegramId(userTelegramId);
        }
    }

    private void printHello(User user, Message message, AbsSender absSender) {
        String text = String.format(HELLO_MESSAGE, user.getFirstName());

        SendMessage answer = new SendMessage();
        answer.setChatId(String.valueOf(message.getChatId()));
        answer.setText(text);

        sendMessage(answer, absSender);
    }

}
