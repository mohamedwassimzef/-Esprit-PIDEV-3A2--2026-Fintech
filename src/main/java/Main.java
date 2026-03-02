import controller.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import tn.esprit.SpringBootApp;

public class Main extends Application {

    private static ConfigurableApplicationContext springContext;
    private Thread springThread;

    @Override
    public void init() {
        springThread = new Thread(() -> {
            springContext = new SpringApplicationBuilder(SpringBootApp.class).run();
        });
        springThread.setName("spring-boot-thread");
        springThread.setDaemon(true); // dies automatically when JavaFX exits
        springThread.start();
    }

    @Override
    public void start(Stage stage) throws Exception {
        SceneManager.setPrimaryStage(stage);
        SceneManager.switchScene("/View/Login.fxml", "Fintech Insurance - Login");
    }

    @Override
    public void stop() {
        if (springContext != null) springContext.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
