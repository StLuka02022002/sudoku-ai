package luka.teum.telegram_service.service;

import lombok.RequiredArgsConstructor;
import luka.teum.telegram_service.model.entity.User;
import luka.teum.telegram_service.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository repository;

    @Override
    public List<User> getAll() {
        return repository.findAll();
    }

    @Override
    public User get(UUID id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public User save(User user) {
        return repository.save(user);
    }

    @Override
    public User update(UUID id, User user) {
        user.setId(id);
        return repository.save(user);
    }

    @Override
    public void delete(UUID id) {
        if (exists(id)) {
            repository.deleteById(id);
        }
    }

    @Override
    public boolean exists(UUID id) {
        return repository.existsById(id);
    }

    public User getUserByTelegramId(Long telegramId) {
        return repository.findByTelegramId(telegramId).orElse(null);
    }

    @Override
    public boolean existsByTelegramId(Long telegramId) {
        return repository.existsByTelegramId(telegramId);
    }
}
