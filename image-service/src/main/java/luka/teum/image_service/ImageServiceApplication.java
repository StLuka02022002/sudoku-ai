package luka.teum.image_service;

import nu.pattern.OpenCV;
import org.opencv.core.Core;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ImageServiceApplication {

    static {
        OpenCV.loadLocally();
    }

    public static void main(String[] args) {
        System.out.println(Core.VERSION);
        SpringApplication.run(ImageServiceApplication.class, args);
    }
}
