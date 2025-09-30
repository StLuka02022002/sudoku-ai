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
public class ImageInfo {

    private String imagePath;
    private TelegramInfo telegramInfo;
}
