package gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class MainApp extends Application {
    private static MainController mainController;

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainUI.fxml"));
        Scene scene = new Scene(loader.load());

        mainController = loader.getController();

        // Attach CSS file (same folder as MyUI.fxml)
        scene.getStylesheets().add(getClass().getResource("/css/style-light.css").toExternalForm());
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/app.png")));

        stage.setTitle("S-emulator");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();  // launches JavaFX app
    }

    public static MainController getMainController() {
        return mainController;
    }
}