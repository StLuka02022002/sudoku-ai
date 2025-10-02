package luka.teum.telegram_service.repository;

import luka.teum.telegram_service.model.entity.User;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends org.springframework.data.jpa.repository.JpaRepository<User, UUID> {

    Optional<User> findByTelegramId(Long id);

    boolean existsByTelegramId(Long id);
}
