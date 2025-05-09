package restaurant_parallelproject;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

public class Restaurant_ParallelProject extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Launch the Server GUI in a separate thread
        Platform.runLater(() -> {
            try {
                new Server().start(new Stage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Launch the Customer Client GUI
        Platform.runLater(() -> {
            try {
                new CustomerClient().start(new Stage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Launch the Chef Client GUI
        Platform.runLater(() -> {
            try {
                new ChefClient().start(new Stage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}