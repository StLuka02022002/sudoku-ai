package luka.teum.telegram_service.service;

import luka.teum.telegram_service.model.entity.User;

import java.util.List;
import java.util.UUID;

public interface UserService {

    List<User> getAll();

    User get(UUID id);

    User save(User t);

    User update(UUID id, User t);

    void delete(UUID id);

    boolean exists(UUID id);

    User getUserByTelegramId(Long telegramId);

    boolean existsByTelegramId(Long telegramId);
}
