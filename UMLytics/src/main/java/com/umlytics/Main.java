package com.umlytics;

import com.umlytics.ui.MainWindow;
import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Entry point for UMLytics.
 * Initializes the API key from environment, dot-env file, or user prompt.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        ensureApiKey();
        MainWindow mainWindow = new MainWindow(primaryStage);
        mainWindow.initialize();
    }

    private void ensureApiKey() {
        // 1. Check Env
        String key = System.getenv("GEMINI_API_KEY");
        if (key != null && !key.isBlank()) return;

        // 2. Check ~/umlytics.env file
        Path envPath = Path.of(System.getProperty("user.home"), "umlytics.env");
        if (Files.exists(envPath)) {
            try {
                key = Files.readString(envPath).trim();
                if (!key.isBlank()) {
                    System.setProperty("GEMINI_API_KEY", key);
                    return;
                }
            } catch (IOException ignored) {}
        }

        // 3. Prompt user
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("API Key Required");
        dialog.setHeaderText("Welcome to UMLytics AI");
        dialog.setContentText("Please enter your Google Gemini API Key:\n(It will be saved locally to ~/umlytics.env)");

        dialog.showAndWait().ifPresent(input -> {
            if (!input.trim().isBlank()) {
                System.setProperty("GEMINI_API_KEY", input.trim());
                try {
                    Files.writeString(envPath, input.trim());
                } catch (IOException e) {
                    Alert alert = new Alert(Alert.AlertType.WARNING, "Could not save API key permanently.");
                    alert.show();
                }
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
