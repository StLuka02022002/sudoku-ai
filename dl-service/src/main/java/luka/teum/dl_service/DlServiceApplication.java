package luka.teum.dl_service;

import nu.pattern.OpenCV;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DlServiceApplication {
    static {
        OpenCV.loadLocally();
    }

    public static void main(String[] args) {
        SpringApplication.run(DlServiceApplication.class, args);
    }

}
