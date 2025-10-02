package luka.teum.telegram_service.command.base;

import lombok.Getter;

@Getter
public enum Commands {
    HELP("help", "Информация по боту",
            "Эта команда предоставляет полную информацию о командах, доступных в боте."),
    START("start", "Получить приветственное сообщение",
            "Команда для получения приветственного сообщения и базовой информации о боте.");
//    MY_DATA("my_data", "Получить свои данные",
//            "Эта команда отправляет информацию о вашем профиле, например, ваше имя и ID."),
//    DELETE_MY_DATA("delete_my_data", "Удалить свои данные",
//            "Удаляет ваши данные из базы данных бота."),
//    SETTINGS("settings", "Настройки",
//            "Команда для доступа к настройкам бота, включая уведомления, обновление данных и режим тишины."),
//    CREATE_DEBT("create_debt", "Создать долг",
//            "Команда для создания долга Вашего должника или создания Вашего долга"),
//    //TODO сделана для теста
//    CREATE_DEBT_SETTINGS("create_debt_settings","Создать настройки долга",
//            "Команда для создания настроек долга, таких как: время напоминания, частота напоминания, тип напоминания");

    private final String commandIdentifier;
    private final String description;
    private final String longDescription;

    Commands(String commandIdentifier, String description, String longDescription) {
        this.commandIdentifier = commandIdentifier;
        this.description = description;
        this.longDescription = longDescription;
    }
}
