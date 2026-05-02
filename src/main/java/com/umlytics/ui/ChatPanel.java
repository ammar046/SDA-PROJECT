package com.umlytics.ui;

import com.umlytics.controllers.AIController;
import com.umlytics.domain.ChatMessage;
import com.umlytics.interfaces.ISpeechToTextService;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public class ChatPanel extends VBox {
    private AIController aiCtrl;
    private ISpeechToTextService speechSvc;
    private TextArea inputField;
    private ListView<ChatMessage> chatDisplay;
    private int activeProjectId = 1;

    public ChatPanel() {
        setSpacing(8);
        setPadding(new Insets(10));
        chatDisplay = new ListView<>();
        inputField = new TextArea();
        inputField.setPromptText("Ask AI about your design...");
        inputField.setPrefRowCount(3);

        Button voiceButton = new Button("Mic");
        voiceButton.setOnAction(event -> onVoiceInput());
        Button sendButton = new Button("Send");
        sendButton.setOnAction(event -> onSendMessage());

        HBox inputBar = new HBox(8, inputField, voiceButton, sendButton);
        getChildren().addAll(chatDisplay, inputBar);
    }

    public void onSendMessage() {
        String text = inputField.getText();
        if (text == null || text.isBlank() || aiCtrl == null) {
            return;
        }
        aiCtrl.consultAI(text.trim(), activeProjectId);
        displayMessages(aiCtrl.getChatHistory(activeProjectId));
        inputField.clear();
    }

    public void onVoiceInput() {
        if (speechSvc == null || !speechSvc.isAvailable()) {
            return;
        }
        speechSvc.startRecording();
        String text = speechSvc.stopRecording();
        inputField.setText(text);
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
}
