package messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelegramInfo {

    private Long userId;
    private Long messageId;
    private Long chartId;

    private String username;
    private String info;
}
