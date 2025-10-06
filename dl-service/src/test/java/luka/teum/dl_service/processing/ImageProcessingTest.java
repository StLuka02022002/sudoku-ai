package luka.teum.dl_service.processing;

import luka.teum.image_service.util.ImageUtil;
import messaging.image.ImagesInfo;
import nu.pattern.OpenCV;
import org.junit.jupiter.api.Test;
import org.opencv.core.Mat;
import storage.Storage;

import java.util.Set;

class ImageProcessingTest {

    static {
        OpenCV.loadLocally();
    }

    private Set<String> data = Set.of("C:\\Code\\For Work\\sudoku-ai\\images\\wrapped\\6363172530\\8c46bd46-64bf-4955-a68b-3280b7efb48e_25a092d3-cd4a-4466-a86b-4fde3d5303f1.png",
            "C:\\Code\\For Work\\sudoku-ai\\images\\wrapped\\6363172530\\ad1f2a18-cb49-4a56-9d0e-3e433c5eb67c_0386c77d-6580-46e8-8e14-e7d1a22b3eec.png");

    private String modelPath = "C:\\Code\\For Work\\sudoku-ai\\model.tar";

    @Test
    public void solve() {
        Storage<Mat> storage = new Storage<Mat>() {
            @Override
            public Mat getData(String location) {
                ImageUtil imageUtil = new ImageUtil();
                return imageUtil.loadImage(location);
            }

            @Override
            public boolean saveData(String location, Mat data) {
                return false;
            }
        };
        ImageProcessing imageProcessing = new ImageProcessing(storage, null, modelPath);
        ImagesInfo imagesInfo = ImagesInfo.builder()
                .imagesPaths(data)
                .countImages(data.size())
                .telegramInfo(null)
                .build();
        imageProcessing.processing(imagesInfo);
    }
}