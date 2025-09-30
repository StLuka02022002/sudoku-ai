package luka.teum.image_service.algorithm;

import org.opencv.core.Mat;
import org.opencv.core.Point;

public interface ImageAlgorithm {

    Point[] algorithm(Mat data);

    Point[] algorithm(Mat data, int typeVersion);
}
