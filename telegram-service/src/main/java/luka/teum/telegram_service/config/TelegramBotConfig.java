package luka.teum.telegram_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("app.telegram-bot.settings")
public class TelegramBotConfig {

    private String name;
    private String token;
}
