package com.umlytics.ui;

import com.umlytics.controllers.AIController;
import com.umlytics.domain.ChatMessage;
import com.umlytics.interfaces.ISpeechToTextService;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;
import java.util.UUID;

public class ChatPanel extends VBox {
    private AIController aiCtrl;
    private ISpeechToTextService speechSvc;
    private TextArea inputField;
    private ListView<ChatMessage> chatDisplay;
    private Label recordingIndicator;
    private FadeTransition recordingPulse;
    private int activeProjectId = 1;

    public ChatPanel() {
        setSpacing(8);
        setPadding(new Insets(10));
        chatDisplay = new ListView<>();
        inputField = new TextArea();
        inputField.setPromptText("Ask AI about your design...");
        inputField.setPrefRowCount(3);
        inputField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {
                event.consume();
                onSendMessage();
            }
        });

        Button voiceButton = new Button("Mic");
        voiceButton.setOnAction(event -> onVoiceInput());
        Button sendButton = new Button("Send");
        sendButton.setOnAction(event -> onSendMessage());
        recordingIndicator = new Label("Recording...");
        recordingIndicator.setStyle("-fx-text-fill: #d93025; -fx-font-weight: bold;");
        recordingIndicator.setVisible(false);
        recordingPulse = new FadeTransition(Duration.millis(550), recordingIndicator);
        recordingPulse.setFromValue(1.0);
        recordingPulse.setToValue(0.25);
        recordingPulse.setCycleCount(FadeTransition.INDEFINITE);
        recordingPulse.setAutoReverse(true);

        HBox inputBar = new HBox(8, inputField, voiceButton, sendButton, recordingIndicator);
        getChildren().addAll(chatDisplay, inputBar);
    }

    public void onSendMessage() {
        String text = inputField.getText();
        if (text == null || text.isBlank() || aiCtrl == null) {
            return;
        }
        try {
            aiCtrl.submitDesignQuestion(text.trim(), activeProjectId);
            displayMessages(aiCtrl.getChatHistory(activeProjectId));
            inputField.clear();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    public void onVoiceInput() {
        if (speechSvc == null) {
            new Alert(Alert.AlertType.WARNING, "Speech service is not configured.").show();
            return;
        }
        if (!speechSvc.isAvailable()) {
            new Alert(Alert.AlertType.WARNING, "Microphone is not available.").show();
            return;
        }
        try {
            recordingIndicator.setVisible(true);
            recordingPulse.playFromStart();
            speechSvc.startRecording();
            String text = speechSvc.stopRecording();
            inputField.setText(text);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        } finally {
            recordingPulse.stop();
            recordingIndicator.setOpacity(1.0);
            recordingIndicator.setVisible(false);
        }
    }

    public void displayMessages(List<ChatMessage> msgs) {
        chatDisplay.getItems().setAll(msgs);
    }

    public void setAiCtrl(AIController aiCtrl) {
        this.aiCtrl = aiCtrl;
    }

    public void setSpeechSvc(ISpeechToTextService speechSvc) {
        this.speechSvc = speechSvc;
    }

    public void setActiveProjectId(int activeProjectId) {
        this.activeProjectId = activeProjectId;
    }

    public void setActiveProjectId(UUID activeProjectId) {
        this.activeProjectId = activeProjectId == null ? 1 : activeProjectId.hashCode();
    }
}
