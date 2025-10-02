package luka.teum.telegram_service.command.base;

import lombok.Getter;

@Getter
public enum Commands {
    HELP("help", "Информация по боту",
            "Эта команда предоставляет полную информацию о командах, доступных в боте."),
    START("start", "Начать работу с ботом",
            "Команда для получения приветственного сообщения и базовой информации о боте."),
    SOLVE("solve", "Решить судоку по изображению",
            "Команда предоставляет алгоритм действия для решения задачи судоку");

    private final String commandIdentifier;
    private final String description;
    private final String longDescription;

    Commands(String commandIdentifier, String description, String longDescription) {
        this.commandIdentifier = commandIdentifier;
        this.description = description;
        this.longDescription = longDescription;
    }
}
