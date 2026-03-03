/**package tn.esprit.services;

import org.opencv.core.*;
import org.opencv.face.LBPHFaceRecognizer;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * FaceRecognitionService - webcam face enrolment + LBPH recognition (OpenCV).
 *
 * pom.xml dependency:
 *   <dependency>
 *       <groupId>org.openpnp</groupId>
 *       <artifactId>opencv</artifactId>
 *       <version>4.9.0-0</version>
 *   </dependency>
 *
 * In Main.java add: nu.pattern.OpenCV.loadLocally();
 *
 * DB: ALTER TABLE user ADD COLUMN IF NOT EXISTS face_registered BOOLEAN DEFAULT FALSE;
 *
 * Flow:
 *   enrollFace(userId)  - opens webcam, captures 20 frames, trains LBPH model,
 *                         saves to ~/fintech_faces/{userId}/model.xml
 *   recognise(userId)   - opens webcam, predicts against model, returns true if
 *                         confidence < THRESHOLD (lower = better match in LBPH)

public class FaceRecognitionService {

    private static final double CONFIDENCE_THRESHOLD = 80.0;
    private static final int    TRAINING_FRAMES      = 20;
    private static final String FACES_DIR =
            System.getProperty("user.home") + "/fintech_faces/";

    public boolean enrollFace(int userId) {
        CascadeClassifier detector = loadCascade();
        if (detector == null) return false;

        VideoCapture cam = new VideoCapture(0);
        if (!cam.isOpened()) {
            System.err.println("[Face] Cannot open webcam."); return false;
        }

        String dir = FACES_DIR + userId + "/";
        new File(dir).mkdirs();

        List<Mat> images = new ArrayList<>();
        Mat frame = new Mat();
        int captured = 0;

        System.out.println("[Face] Look at the camera - capturing " + TRAINING_FRAMES + " frames...");
        while (captured < TRAINING_FRAMES) {
            cam.read(frame);
            if (frame.empty()) continue;
            Mat face = detectFace(frame, detector);
            if (face != null) {
                Imgcodecs.imwrite(dir + "f" + captured + ".png", face);
                images.add(face);
                captured++;
                try { Thread.sleep(120); } catch (InterruptedException ignored) {}
            }
        }
        cam.release();
        if (images.size() < TRAINING_FRAMES) return false;

        LBPHFaceRecognizer rec = LBPHFaceRecognizer.create();
        int[] lblArr = new int[images.size()];
        Arrays.fill(lblArr, userId);
        rec.train(images, new MatOfInt(lblArr));
        rec.save(dir + "model.xml");
        System.out.println("[Face] Model saved for user " + userId);
        return true;
    }

    public boolean recognise(int userId) {
        String modelPath = FACES_DIR + userId + "/model.xml";
        if (!new File(modelPath).exists()) {
            System.err.println("[Face] No model for user " + userId); return false;
        }

        CascadeClassifier detector = loadCascade();
        if (detector == null) return false;

        VideoCapture cam = new VideoCapture(0);
        if (!cam.isOpened()) { System.err.println("[Face] Cannot open webcam."); return false; }

        LBPHFaceRecognizer rec = LBPHFaceRecognizer.create();
        rec.read(modelPath);

        Mat frame = new Mat();
        boolean matched = false;
        int attempts = 0;
        while (!matched && attempts < 40) {
            cam.read(frame);
            if (!frame.empty()) {
                Mat face = detectFace(frame, detector);
                if (face != null) {
                    int[] lbl = {0};
                    double[] conf = {0};
                    rec.predict(face, lbl, conf);
                    System.out.printf("[Face] confidence=%.1f%n", conf[0]);
                    if (conf[0] < CONFIDENCE_THRESHOLD) matched = true;
                }
            }
            attempts++;
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        }
        cam.release();
        return matched;
    }

    private Mat detectFace(Mat frame, CascadeClassifier detector) {
        Mat grey = new Mat();
        Imgproc.cvtColor(frame, grey, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(grey, grey);
        MatOfRect faces = new MatOfRect();
        detector.detectMultiScale(grey, faces, 1.1, 5, 0, new Size(80, 80), new Size());
        Rect[] rects = faces.toArray();
        if (rects.length == 0) return null;
        Rect best = rects[0];
        for (Rect r : rects) if (r.area() > best.area()) best = r;
        Mat cropped = new Mat(grey, best);
        Mat resized = new Mat();
        Imgproc.resize(cropped, resized, new Size(100, 100));
        return resized;
    }

    private CascadeClassifier loadCascade() {
        try {
            InputStream is = getClass().getResourceAsStream("/haarcascade_frontalface_default.xml");
            if (is == null)
                is = Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream("haarcascade_frontalface_default.xml");
            if (is == null) {
                System.err.println("[Face] haarcascade_frontalface_default.xml not on classpath.");
                return null;
            }
            File tmp = File.createTempFile("haarcascade", ".xml");
            tmp.deleteOnExit();
            Files.copy(is, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return new CascadeClassifier(tmp.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("[Face] Failed to load cascade: " + e.getMessage());
            return null;
        }
    }
}*/
package tn.esprit.services;

/**
 * Face recognition is currently unavailable (opencv-face contrib module not on classpath).
 * Both methods return false so the rest of the app compiles and runs normally.
 */
public class FaceRecognitionService {

    public boolean enrollFace(int userId) {
        System.err.println("[Face] Face recognition not available.");
        return false;
    }

    public boolean recognise(int userId) {
        System.err.println("[Face] Face recognition not available.");
        return false;
    }
}
