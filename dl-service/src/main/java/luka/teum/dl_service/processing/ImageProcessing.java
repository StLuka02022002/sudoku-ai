package luka.teum.dl_service.processing;

import lombok.extern.slf4j.Slf4j;
import luka.teum.dl_service.ai.Algorithm;
import luka.teum.dl_service.prepare.ImagePrepare;
import luka.teum.image_service.messaging.KafkaProducerService;
import luka.teum.image_service.util.ImageUtil;
import messaging.Solution;
import messaging.image.ImagesInfo;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import storage.Storage;

import java.io.File;
import java.io.IOException;

@Slf4j
public class ImageProcessing {

    private final Storage<Mat> storage;
    private final KafkaProducerService kafkaProducerService;
    private final ImagePrepare imagePrepare;
    private final Algorithm algorithm;

    public ImageProcessing(Storage<Mat> storage, KafkaProducerService kafkaProducerService, String modelPath) {
        this.storage = storage;
        this.kafkaProducerService = kafkaProducerService;
        if (modelPath != null) {
            this.algorithm = new Algorithm(modelPath);
        } else {
            this.algorithm = new Algorithm();
        }
        this.imagePrepare = new ImagePrepare();
    }

    public void processing(ImagesInfo imagesInfo) {
        int count = 0;
        for (String imagePath :
                imagesInfo.getImagesPaths()) {
            Mat image = null;
            try {
                image = storage.getData(imagePath);
                File file = new File("C:\\Code\\For Work\\sudoku-ai\\images\\digits\\" + count);
                file.mkdir();
                Mat[][] digits = imagePrepare.prepare(image);
                int[][] result = new int[Solution.SUDOKU_SIZE][Solution.SUDOKU_SIZE];
                for (int i = 0; i < Solution.SUDOKU_SIZE; i++) {
                    for (int j = 0; j < Solution.SUDOKU_SIZE; j++) {
                        try {
                            result[i][j] = algorithm.evaluateImage(digits[i][j]);
                            if (digits[i][j].empty()) {
                                new ImageUtil().saveImage(file.getPath() + "\\" + i + j + ".png", Mat.ones(new Size(60, 60), CvType.CV_8U));
                            } else {
                                new ImageUtil().saveImage(file.getPath() + "\\" + i + j + ".png", digits[i][j]);
                            }
                        } catch (IOException e) {
                            result[i][j] = Solution.NO_SOLUTION;
                            new ImageUtil().saveImage(file.getPath() + "\\" + i + j + ".png", Mat.ones(new Size(60, 60), 50));
                        }
                    }
                }
                System.out.println(file.getAbsolutePath());
                Solution solution = new Solution(result);
                System.out.println(solution.getSolution());
                count++;
            } finally {
                if (image != null) {
                    image.release();
                }
            }
        }
    }
}
