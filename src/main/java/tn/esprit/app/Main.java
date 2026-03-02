package tn.esprit.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));

        if (loader.getLocation() == null) {
            throw new RuntimeException(
                    "Login.fxml not found! Make sure Login.fxml is in src/main/resources/");
        }

        Parent root = loader.load();

        primaryStage.setTitle("FinTech Portal – Login");
        primaryStage.setScene(new Scene(root, 900, 600));
        primaryStage.setResizable(false);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
