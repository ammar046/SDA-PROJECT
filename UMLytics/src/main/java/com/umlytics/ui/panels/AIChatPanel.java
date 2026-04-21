package com.umlytics.ui.panels;

import com.umlytics.domain.ChatMessage;
import com.umlytics.domain.EvaluationReport;
import com.umlytics.domain.UMLDiagram;
import com.umlytics.domain.valueobjects.ProjectContext;
import com.umlytics.domain.valueobjects.StructureSuggestion;
import com.umlytics.domain.valueobjects.UMLModel;
import com.umlytics.enums.SenderType;
import com.umlytics.ui.MainWindow;
import com.umlytics.ui.dialogs.CodeSkeletonDialog;
import com.umlytics.ui.dialogs.EvaluationResultDialog;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

/**
 * AI Chat interaction panel with bubble-style messages.
 * GRASP: Pure Fabrication, Low Coupling
 */
public class AIChatPanel extends VBox {

    private final MainWindow facade;
    private final VBox chatContainer;
    private final ScrollPane scrollPane;
    private final TextField inputField;
    private final Button btnMic;

    private boolean isRecording = false;
    private Label thinkingBubble = null;
    private Timeline thinkingAnim = null;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public AIChatPanel(MainWindow facade) {
        this.facade = facade;
        setSpacing(8);
        setPadding(new Insets(10));
        getStyleClass().add("ai-chat-panel");
        setMinWidth(220);

        Label title = new Label("🤖 AI Design Assistant");
        title.getStyleClass().add("panel-header");
        title.setMaxWidth(Double.MAX_VALUE);

        // Chat bubble container
        chatContainer = new VBox(8);
        chatContainer.setPadding(new Insets(8));

        scrollPane = new ScrollPane(chatContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("chat-scroll-pane");
        scrollPane.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Input row
        inputField = new TextField();
        inputField.setPromptText("Ask about design or type 'generate …'");
        inputField.setId("ai-chat-input");
        HBox.setHgrow(inputField, Priority.ALWAYS);
        inputField.setOnAction(e -> sendMessage());

        Button btnSend = new Button("Send");
        btnSend.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; "
                + "-fx-background-radius: 6; -fx-cursor: hand;");
        btnSend.setOnAction(e -> sendMessage());

        btnMic = new Button("🎤");
        btnMic.setStyle("-fx-background-color: #2d2d2d; -fx-text-fill: #d4d4d4; "
                + "-fx-background-radius: 6; -fx-cursor: hand;");
        btnMic.setOnAction(e -> handleMic());

        HBox inputBox = new HBox(6, inputField, btnMic, btnSend);
        inputBox.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(title, scrollPane, inputBox);

        // Welcome message
        appendAIBubble("Welcome! I'm your UMLytics AI assistant.\n"
                + "Type 'generate …' to create a diagram, or ask anything about your design.");
    }

    // =====================================================================
    // Message sending
    // =====================================================================

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        inputField.clear();

        appendUserBubble(text);

        if (facade.getProjectController().getCurrentProject() == null) {
            appendAIBubble("Please create or open a project first.");
            return;
        }

        if (text.toLowerCase().startsWith("generate")) {
            handleGenerate(text);
        } else {
            handleChat(text);
        }
    }

    private void handleGenerate(String text) {
        showThinking();
        new Thread(() -> {
            try {
                UMLModel model = facade.getAIController().generateFromPrompt(text);
                Platform.runLater(() -> {
                    hideThinking();
                    int pid = facade.getProjectController().getCurrentProject().getProjectId();
                    String title = "Generated: " + text.substring(0, Math.min(20, text.length()));
                    UMLDiagram d = facade.getDiagramController().createDiagram(pid, title,
                            com.umlytics.enums.SourceType.NL);
                    ToolbarPanel.applyAutoLayout(model, d);
                    facade.getDiagramController().setCurrentDiagram(d);
                    facade.getDiagramController().saveCurrentDiagram();
                    facade.getMainCanvas().renderDiagram(d);
                    facade.refreshProjectExplorer();
                    appendAIBubble("Diagram generated with " + model.getClasses().size() + " classes. "
                            + "You can drag nodes to rearrange them.");
                    MainWindow.showToast("Diagram generated");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    hideThinking();
                    appendAIBubble("Error generating diagram: " + ex.getMessage());
                });
            }
        }).start();
    }

    private void handleChat(String text) {
        showThinking();
        new Thread(() -> {
            try {
                ProjectContext ctx = new ProjectContext(
                        facade.getProjectController().getCurrentProject(),
                        facade.getDiagramController().getCurrentDiagram(),
                        new ArrayList<>());
                String resp = facade.getAIController().sendChatMessage(
                        facade.getProjectController().getCurrentProject().getProjectId(), text, ctx);
                Platform.runLater(() -> {
                    hideThinking();
                    appendAIBubble(resp);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    hideThinking();
                    appendAIBubble("Error: " + ex.getMessage());
                });
            }
        }).start();
    }

    // =====================================================================
    // Voice input
    // =====================================================================

    private void handleMic() {
        if (!facade.getAIController().isSpeechAvailable()) {
            MainWindow.showToast("Voice unavailable — type instead");
            return;
        }
        if (!isRecording) {
            facade.getAIController().startVoiceRecording();
            btnMic.setText("⏹ Stop");
            btnMic.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; "
                    + "-fx-background-radius: 6; -fx-cursor: hand;");
            isRecording = true;
        } else {
            String transcript = facade.getAIController().stopVoiceRecording();
            btnMic.setText("🎤");
            btnMic.setStyle("-fx-background-color: #2d2d2d; -fx-text-fill: #d4d4d4; "
                    + "-fx-background-radius: 6; -fx-cursor: hand;");
            isRecording = false;
            if (!transcript.trim().isEmpty()) {
                inputField.setText(transcript);
                sendMessage();
            }
        }
    }

    // =====================================================================
    // Evaluation & Code generation
    // =====================================================================

    public void runEvaluation() {
        UMLDiagram d = facade.getDiagramController().getCurrentDiagram();
        if (d == null) { appendAIBubble("Open a diagram to evaluate."); return; }

        appendSystemBubble("Evaluating design quality…");
        showThinking();

        new Thread(() -> {
            try {
                UMLModel m = new UMLModel(d.getClasses(), d.getRelationships(), d.serialize());
                EvaluationReport rep = facade.getAIController().evaluateDiagram(m, d.getDiagramId(), d.getProjectId());
                Platform.runLater(() -> {
                    hideThinking();
                    new EvaluationResultDialog(rep).showAndWait();
                    appendAIBubble("Evaluation complete. Overall: "
                            + String.format("%.0f%%", rep.getOverallScore()));
                    MainWindow.showToast("Evaluation complete");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    hideThinking();
                    appendAIBubble("Evaluation error: " + ex.getMessage());
                });
            }
        }).start();
    }

    public void generateCode() {
        UMLDiagram d = facade.getDiagramController().getCurrentDiagram();
        if (d == null) { appendAIBubble("Open a diagram to generate code."); return; }

        appendSystemBubble("Generating Java code skeletons…");
        showThinking();

        new Thread(() -> {
            try {
                UMLModel m = new UMLModel(d.getClasses(), d.getRelationships(), d.serialize());
                StructureSuggestion sgt = facade.getAIController().generateCodeSkeletons(m, d.getDiagramId());
                String combined = String.join("\n\n", sgt.getSkeletons().values());
                Platform.runLater(() -> {
                    hideThinking();
                    new CodeSkeletonDialog(combined).showAndWait();
                    appendAIBubble("Code skeletons generated for " + sgt.getSkeletons().size() + " class(es).");
                    MainWindow.showToast("Code skeletons generated");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    hideThinking();
                    appendAIBubble("Code generation error: " + ex.getMessage());
                });
            }
        }).start();
    }

    // =====================================================================
    // Chat history
    // =====================================================================

    public void loadHistory(int projectId) {
        try {
            for (ChatMessage msg : facade.getAIController().getChatHistory(projectId)) {
                if (msg.getSender() == SenderType.USER) appendUserBubble(msg.getContent());
                else appendAIBubble(msg.getContent());
            }
        } catch (Exception ex) {
            System.err.println("[AIChatPanel] History load error: " + ex.getMessage());
        }
    }

    // =====================================================================
    // Public API used by MainCanvas / other panels
    // =====================================================================

    public void appendSysMessage(String msg) { appendSystemBubble(msg); }

    public void appendSystemBubble(String text) {
        Platform.runLater(() -> {
            Label lbl = new Label(text);
            lbl.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px; -fx-font-style: italic;");
            lbl.setWrapText(true);
            chatContainer.getChildren().add(lbl);
            scrollToBottom();
        });
    }

    public void focusWithText(String text) {
        inputField.requestFocus();
        inputField.setText(text);
    }

    // =====================================================================
    // Bubble builders
    // =====================================================================

    private void appendUserBubble(String text) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_RIGHT);

        VBox bubble = new VBox(2);
        bubble.maxWidthProperty().bind(widthProperty().multiply(0.85));
        bubble.setAlignment(Pos.CENTER_RIGHT);

        Label msg = new Label(text);
        msg.getStyleClass().add("chat-bubble-user");
        msg.setWrapText(true);
        msg.setMaxWidth(340);

        Label time = new Label(LocalTime.now().format(TIME_FMT));
        time.getStyleClass().add("chat-timestamp");
        time.setAlignment(Pos.CENTER_RIGHT);

        bubble.getChildren().addAll(msg, time);
        row.getChildren().add(bubble);

        chatContainer.getChildren().add(row);
        scrollToBottom();
    }

    private void appendAIBubble(String text) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);

        VBox bubble = new VBox(2);
        bubble.maxWidthProperty().bind(widthProperty().multiply(0.85));

        Label msg = new Label(text);
        msg.getStyleClass().add("chat-bubble-ai");
        msg.setWrapText(true);
        msg.setMaxWidth(340);

        Label time = new Label(LocalTime.now().format(TIME_FMT));
        time.getStyleClass().add("chat-timestamp");

        bubble.getChildren().addAll(msg, time);
        row.getChildren().add(bubble);

        chatContainer.getChildren().add(row);
        scrollToBottom();
    }

    // =====================================================================
    // Thinking indicator
    // =====================================================================

    private void showThinking() {
        Platform.runLater(() -> {
            if (thinkingBubble != null) return;
            thinkingBubble = new Label("AI is thinking.");
            thinkingBubble.getStyleClass().add("thinking-bubble");
            chatContainer.getChildren().add(thinkingBubble);

            // Animate dots
            int[] dotCount = {0};
            thinkingAnim = new Timeline(new KeyFrame(Duration.millis(500), e -> {
                dotCount[0] = (dotCount[0] + 1) % 4;
                String dots = ".".repeat(dotCount[0]);
                thinkingBubble.setText("AI is thinking" + dots);
            }));
            thinkingAnim.setCycleCount(Timeline.INDEFINITE);
            thinkingAnim.play();
            scrollToBottom();
        });
    }

    private void hideThinking() {
        if (thinkingAnim != null) { thinkingAnim.stop(); thinkingAnim = null; }
        if (thinkingBubble != null) {
            chatContainer.getChildren().remove(thinkingBubble);
            thinkingBubble = null;
        }
    }

    private void scrollToBottom() {
        scrollPane.setVvalue(1.0);
    }
}
