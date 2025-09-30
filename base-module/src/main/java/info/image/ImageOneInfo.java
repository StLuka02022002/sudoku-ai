package info.image;

import info.TelegramInfo;
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
