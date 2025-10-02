package luka.teum.telegram_service.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "telegram_id", columnDefinition = "BIGINT", unique = true, nullable = false)
    private Long telegramId;

    @Column(name = "username", columnDefinition = "VARCHAR(50)")
    private String username;

    @Column(name = "first_name", columnDefinition = "VARCHAR(100)")
    private String firstName;

    @Column(name = "last_name", columnDefinition = "VARCHAR(100)")
    private String lastName;

    @Column(name = "phone_number", columnDefinition = "VARCHAR(20)")
    private String phoneNumber;
}
