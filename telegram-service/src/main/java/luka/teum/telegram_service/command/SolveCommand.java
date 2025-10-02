package luka.teum.telegram_service.command;

import luka.teum.telegram_service.command.base.BaseCommand;
import luka.teum.telegram_service.command.base.Commands;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;

@Component
public class SolveCommand extends BaseCommand {

    public static final String SOLVE_MESSAGE = """
            🧩 <b>Решатель судоку</b>
                        
            Отправьте мне фотографию или скриншот с головоломкой судоку.
                        
            <b>Требования к изображению:</b>
            • Четкое и хорошо освещенное
            • Сетка занимает большую часть кадра
            • Цифры хорошо видны
                        
            <b>Совет:</b> Сделайте фото прямо над головоломкой для лучшего результата!
            """;

    public SolveCommand() {
        super(Commands.SOLVE);
    }

    @Override
    public void processMessage(AbsSender absSender, Message message, String[] arguments) {
        SendMessage answer = new SendMessage();
        answer.setChatId(message.getChatId());
        answer.setText(SOLVE_MESSAGE);
        answer.setParseMode("HTML");

        sendMessage(answer, absSender);
    }
}
