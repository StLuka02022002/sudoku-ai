package messaging.image;

import messaging.TelegramInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageOneInfo {

    private String imagePath;
    private Integer imageId;
    private Integer countImages;
    private TelegramInfo telegramInfo;
}
