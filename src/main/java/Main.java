import controller.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import tn.esprit.SpringBootApp;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Main extends Application {

    private static ConfigurableApplicationContext springContext;
    private static final CountDownLatch springLatch = new CountDownLatch(1);

    @Override
    public void init() {
        Thread springThread = new Thread(() -> {
            try {
                springContext = new SpringApplicationBuilder(SpringBootApp.class)
                        .properties(
                            "server.port=8081",
                            "spring.main.banner-mode=off",
                            "spring.main.web-application-type=servlet"
                        )
                        .headless(false)
                        .run(new String[0]);
                System.out.println("[Spring] ✓ Tomcat started on port 8081");
                System.out.println("[Spring]   Webhook: https://iridaceous-misty-vivaciously.ngrok-free.dev/webhook/boldsign");
            } catch (Exception e) {
                System.out.println("[Spring] ✗ Failed to start: " + e.getMessage());
                e.printStackTrace();
            } finally {
                springLatch.countDown();
            }
        }, "spring-boot-thread");
        springThread.setDaemon(true);
        springThread.start();

        try {
            if (!springLatch.await(30, TimeUnit.SECONDS))
                System.out.println("[Spring] Warning: timed out after 30s — continuing anyway.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        SceneManager.setPrimaryStage(stage);
        SceneManager.switchScene("/View/Login.fxml", "Fintech Insurance - Login");
    }

    @Override
    public void stop() {
        if (springContext != null) {
            springContext.close();
            System.out.println("[Spring] Context closed.");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
