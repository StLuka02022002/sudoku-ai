package luka.teum.telegram_service.mapping;

import luka.teum.telegram_service.model.entity.User;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Chat;

@Component
public class UserMapping {

    public User getUserEntityByChart(Chat chat) {
        return User.builder()
                .telegramId(chat.getId())
                .username(chat.getUserName())
                .firstName(chat.getFirstName())
                .lastName(chat.getLastName())
                .build();
    }


}
