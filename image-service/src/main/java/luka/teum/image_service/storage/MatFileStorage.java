package luka.teum.image_service.storage;

import lombok.extern.slf4j.Slf4j;
import luka.teum.image_service.util.ImageUtil;
import org.opencv.core.Mat;
import org.springframework.stereotype.Component;
import storage.BaseFileStorage;

import java.io.File;

@Slf4j
@Component
public class MatFileStorage extends BaseFileStorage<Mat> {

    private final ImageUtil imageUtil;

    public MatFileStorage(ImageUtil imageUtil) {
        super();
        this.imageUtil = imageUtil;
        log.debug("MatFileStorage initialized with ImageUtil dependency");
    }

    public MatFileStorage() {
        super();
        this.imageUtil = new ImageUtil();
        log.debug("MatFileStorage initialized with default constructor");
    }

    @Override
    protected Mat loadDataFromFile(File file) throws Exception {
        Mat image = imageUtil.loadImage(file.getAbsolutePath());
        if (image.empty()) {
            log.error("Loaded image is empty from location: {}", file.getAbsolutePath());
            throw new IllegalArgumentException("Loaded image is empty");
        }

        log.debug("Successfully loaded image. Size: {}x{}, channels: {}",
                image.cols(), image.rows(), image.channels());
        return image;
    }

    @Override
    protected boolean saveDataToFile(String filePath, Mat mat) throws Exception {
        return imageUtil.saveImage(filePath, mat);
    }

    @Override
    protected void validateInputData(Mat mat) {
        if (mat == null) {
            log.error("Attempt to save null Mat object");
            throw new IllegalArgumentException("Mat object cannot be null");
        }

        if (mat.empty()) {
            log.error("Attempt to save empty Mat object");
            throw new IllegalArgumentException("Mat object cannot be empty");
        }

        if (mat.rows() == 0 || mat.cols() == 0) {
            log.error("Attempt to save Mat with zero dimensions: {}x{}", mat.rows(), mat.cols());
            throw new IllegalArgumentException("Mat object has invalid dimensions");
        }
    }

    @Override
    public boolean saveData(String location, Mat mat) {
        boolean result = super.saveData(location, mat);

        if (mat != null) {
            long approximateMemory = (long) mat.rows() * mat.cols() * mat.channels() * 4;
            if (approximateMemory > 10_000_000) {
                log.warn("Large image processed - approx memory: {} MB", approximateMemory / 1_000_000);
            }
        }

        return result;
    }
}