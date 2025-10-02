package luka.teum.telegram_service.command;

import luka.teum.telegram_service.command.base.BaseCommand;
import luka.teum.telegram_service.command.base.Commands;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;

@Component
public class HelpCommand extends BaseCommand {

    //TODO переделать сообщение

    public static final String HELP_TEXT = """
            🛠️ <b>Справка по использованию бота судоку</b>

            <b>Как это работает:</b>
            1. Сделайте фото или скриншот головоломки судоку. Убедитесь, что сетка хорошо видна и освещение достаточное.
            2. Отправьте изображение мне в этот чат или нажмите кнопку «📤 Загрузить судоку».
            3. Я обработаю изображение, распознаю цифры и найду решение.
            4. Получите ответ! Я пришлю решенную сетку в виде текста и, возможно, изображения.

            <b>Доступные команды:</b>
            %s
            <b>Советы для лучшего распознавания:</b>
            • Используйте четкие изображения с хорошим контрастом.
            • Старайтесь, чтобы сетка была прямой и занимала большую часть кадра.
            • Избегайте бликов и теней на сетке.

            Если у вас возникли проблемы или бот выдает ошибку, попробуйте сделать фото еще раз. Удачи! 🧩
            """;

    public HelpCommand() {
        super(Commands.HELP);
    }

    @Override
    public void processMessage(AbsSender absSender, Message message, String[] strings) {
        SendMessage answer = new SendMessage();
        answer.setChatId(message.getChatId());
        answer.setText(getHelpText());
        answer.setParseMode("HTML");
        sendMessage(answer, absSender);
    }

    private String getHelpText() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Commands value : Commands.values()) {
            stringBuilder.append("/")
                    .append(value.getCommandIdentifier())
                    .append(" - ")
                    .append(value.getDescription())
                    .append("\n");
        }
        return String.format(HELP_TEXT, stringBuilder);
    }
}
