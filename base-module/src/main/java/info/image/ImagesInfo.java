package info.image;

import info.TelegramInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImagesInfo {

    private Set<String> imagesPaths;
    private Integer countImages;
    private TelegramInfo telegramInfo;
}
