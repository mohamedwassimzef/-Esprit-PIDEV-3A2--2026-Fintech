package controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Utility class for switching scenes in the JavaFX application.
 * Holds a reference to the primary stage and applies the shared stylesheet.
 */
public class SceneManager {

    private static Stage primaryStage;

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Switch the current scene to the given FXML file.
     * @param fxmlPath path relative to resources, e.g. "/View/Login.fxml"
     * @param title    window title
     */
    public static void switchScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    SceneManager.class.getResource("/View/styles.css").toExternalForm()
            );

            primaryStage.setScene(scene);
            primaryStage.setTitle(title);
            primaryStage.show();
        } catch (IOException e) {
            System.out.println("Error loading scene " + fxmlPath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Switch scene and return the loader so the caller can access the controller
     * and call init() on it with extra data.
     *
     * @return the FXMLLoader (use loader.getController() to get the controller), or null on error.
     */
    public static FXMLLoader switchSceneWithLoader(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    SceneManager.class.getResource("/View/styles.css").toExternalForm()
            );

            primaryStage.setScene(scene);
            primaryStage.setTitle(title);
            primaryStage.show();
            return loader;
        } catch (IOException e) {
            System.out.println("Error loading scene " + fxmlPath + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}

