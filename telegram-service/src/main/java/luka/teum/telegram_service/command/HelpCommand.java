package luka.teum.telegram_service.command;

import luka.teum.telegram_service.command.base.BaseCommand;
import luka.teum.telegram_service.command.base.Commands;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;

@Service
public class HelpCommand extends BaseCommand {

    //TODO переделать сообщение

    public static final String HELP_TEXT = """
            Этот бот создан для работы с долгами. Вы можете использовать следующие команды:
            """;

    public HelpCommand() {
        super(Commands.HELP);
    }

    @Override
    public void processMessage(AbsSender absSender, Message message, String[] strings) {
        SendMessage answer = new SendMessage();
        answer.setChatId(message.getChatId());
        answer.setText(getHelpText());
        sendMessage(answer, absSender);
    }

    private String getHelpText() {
        StringBuilder stringBuilder = new StringBuilder(HELP_TEXT + "\n");
        for (Commands value : Commands.values()) {
            stringBuilder.append("/")
                    .append(value.getCommandIdentifier())
                    .append(" - ")
                    .append(value.getDescription())
                    .append("\n");
        }
        return stringBuilder.toString();
    }
}
