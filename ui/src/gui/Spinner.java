package gui;

import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class Spinner extends StackPane {
    private final ProgressIndicator spinner;

    public Spinner() {
        setStyle("-fx-background-color: rgba(0, 0, 0, 0.3);"); // semi-transparent background
        setVisible(false); // hidden by default

        spinner = new ProgressIndicator();
        spinner.setPrefSize(50, 50);

        getChildren().add(spinner);
    }

    /** Show overlay immediately */
    public void show() {
        setVisible(true);
    }

    /** Hide overlay immediately */
    public void hide() {
        setVisible(false);
    }

    /**
     * Run a task with overlay shown. 
     * It ensures the overlay is visible at least 0.5s (so user notices it).
     */
    public void runWithOverlay(Node parent, Runnable task) {
        if (getParent() == null && parent instanceof StackPane) {
            ((StackPane) parent).getChildren().add(this);
        }

        show();

        Task<Void> backgroundTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                task.run();
                return null;
            }
        };

        backgroundTask.setOnSucceeded(e -> {
            // Ensure spinner shows for at least 0.5s
            PauseTransition delay = new PauseTransition(Duration.millis(2000));
            delay.setOnFinished(ev -> hide());
            delay.play();
        });

        new Thread(backgroundTask).start();
    }
}
