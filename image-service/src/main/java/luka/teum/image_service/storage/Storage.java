package luka.teum.image_service.storage;

import org.opencv.core.Mat;

public interface Storage {

    Mat getData(String location);

    boolean saveData(String location, Mat mat);
}
