package luka.teum.telegram_service.command;

import luka.teum.telegram_service.command.base.BaseCommand;
import luka.teum.telegram_service.command.base.Commands;
import luka.teum.telegram_service.mapping.UserMapping;
import luka.teum.telegram_service.model.entity.User;
import luka.teum.telegram_service.service.UserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;


@Component
public class StartCommand extends BaseCommand {

    public static final String HELLO_MESSAGE = """
            Привет, 🧠 любитель головоломок! %s!
                        
            Я — твой личный помощник по решению судоку. Просто отправь мне фотографию или скриншот с незаконченной сеткой судоку, и я быстро найду решение!
                        
            ✨ Что я умею:
            • Решать классические судоку 9x9 по изображению
            • Отображать решение в удобном, читаемом формате
            • Предлагать альтернативные действия
                        
            Чтобы начать, просто загрузи фото или воспользуйся кнопкой «📤 Загрузить судоку» ниже.
                        
            Нужна помощь? Команда /help расскажет обо всем подробнее!
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
        Long userTelegramId = message.getFrom().getId();
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
        answer.setParseMode("HTML");

        sendMessage(answer, absSender);
    }

}
