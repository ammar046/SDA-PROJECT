package com.umlytics.ui.panels;

import com.umlytics.enums.SenderType;
import com.umlytics.interfaces.ISpeechToTextService;
import com.umlytics.ui.MainWindow;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

// GRASP: Pure Fabrication, Low Coupling
public class AIChatPanel extends VBox {

    private final MainWindow           facade;
    private final ISpeechToTextService stt;
    private final VBox                 messages;
    private final ScrollPane           scroll;
    private final TextField            input;
    private final Button               btnMic;
    private       Label                thinkingLabel;
    private       Timeline             thinkingAnim;
    private       boolean              recording = false;

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    public AIChatPanel(MainWindow facade, ISpeechToTextService stt) {
        this.facade = facade;
        this.stt    = stt;

        getStyleClass().add("ai-chat-panel");
        setMinWidth(200);
        setPrefWidth(280);

        // Header
        Label title = new Label("AI Design Assistant");
        title.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 14px; -fx-font-weight: bold; "
            + "-fx-padding: 10; -fx-border-color: #374151; -fx-border-width: 0 0 1 0;");
        title.setMaxWidth(Double.MAX_VALUE);

        // Message area
        messages = new VBox(10);
        messages.setPadding(new Insets(10));

        scroll = new ScrollPane(messages);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // Welcome bubble
        addBubble("Hi! I can help you design UML diagrams, evaluate quality, and explain SDA principles. "
            + "Type a description like \"generate a library system\" to get started.", SenderType.AI);

        // Input row
        input = new TextField();
        input.setPromptText("Ask about design or type 'generate …'");
        input.getStyleClass().add("text-field");
        HBox.setHgrow(input, Priority.ALWAYS);
        input.setOnAction(e -> send());

        Button btnSend = new Button("Send");
        btnSend.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; "
            + "-fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 8 14;");
        btnSend.setOnAction(e -> send());

        btnMic = new Button("🎤");
        btnMic.setStyle("-fx-background-color: #2d2d3a; -fx-text-fill: #d4d4d4; "
            + "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 8;");
        btnMic.setOnMousePressed(e  -> startRec());
        btnMic.setOnMouseReleased(e -> stopRec());

        HBox inputRow = new HBox(6, input, btnMic, btnSend);
        inputRow.setPadding(new Insets(8, 10, 10, 10));
        inputRow.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(title, scroll, inputRow);
    }

    // ── Public API (called by ToolbarPanel) ───────────────────────────────────
    public void runEvaluation() {
        addBubble("Running design quality evaluation…", SenderType.SYSTEM);
        new Thread(() -> {
            try {
                java.util.UUID id = facade.getEditorPanel().getCurrentDiagramId();
                if (id == null) { Platform.runLater(() -> addBubble("No diagram loaded.", SenderType.AI)); return; }
                var r = facade.getDiagramController().evaluateDesign(id);
                String msg = "📊 Evaluation Results\nCoupling: " + r.getCouplingScore()
                    + "\nCohesion: " + r.getCohesionScore()
                    + "\nSOLID:    " + r.getSolidScore()
                    + (r.getFeedbackSummary() != null ? "\n\n" + r.getFeedbackSummary() : "");
                Platform.runLater(() -> addBubble(msg, SenderType.AI));
            } catch (Exception ex) {
                Platform.runLater(() -> addBubble("Evaluation failed: " + ex.getMessage(), SenderType.AI));
            }
        }, "eval-thread").start();
    }

    public void generateStructure() {
        addBubble("Generating class structure suggestions…", SenderType.SYSTEM);
        new Thread(() -> {
            try {
                java.util.UUID id = facade.getEditorPanel().getCurrentDiagramId();
                if (id == null) { Platform.runLater(() -> addBubble("No diagram loaded.", SenderType.AI)); return; }
                var s = facade.getDiagramController().generateStructureSuggestions(id);
                Platform.runLater(() -> addSkeletonCodeBubble(s.getSkeletonCode()));
            } catch (Exception ex) {
                Platform.runLater(() -> addBubble("Error: " + ex.getMessage(), SenderType.AI));
            }
        }, "struct-thread").start();
    }

    // ── Send ──────────────────────────────────────────────────────────────────
    private void send() {
        String text = input.getText().trim();
        if (text.isEmpty()) return;
        input.clear();
        addBubble(text, SenderType.USER);
        showThinking();

        new Thread(() -> {
            try {
                java.util.UUID pid = facade.getActiveProjectId();
                if (pid == null) {
                    var all = facade.getProjectController().getAllProjects();
                    pid = all.isEmpty() ? null : all.get(0).getProjectId();
                }
                String response;
                if (text.toLowerCase().startsWith("generate") || text.toLowerCase().startsWith("create")) {
                    if (pid == null) {
                        final String err = "Create or open a project first, then try again.";
                        Platform.runLater(() -> { hideThinking(); addBubble(err, SenderType.AI); });
                        return;
                    }
                    var d = facade.getDiagramController().generateFromText(pid, text);
                    Platform.runLater(() -> {
                        facade.openDiagram(d);
                        MainWindow.notifyDiagramChanged();
                    });
                    response = "✅ Diagram generated! Check the canvas.";
                } else {
                    java.util.UUID did = facade.getEditorPanel().getCurrentDiagramId();
                    var aiMsg = facade.getAIController().submitDesignQuestion(text, pid, did);
                    response  = aiMsg.getContent();
                }
                final String r = response;
                Platform.runLater(() -> { hideThinking(); addBubble(r, SenderType.AI); });
            } catch (Exception ex) {
                Platform.runLater(() -> { hideThinking(); addBubble("Error: " + ex.getMessage(), SenderType.AI); });
            }
        }, "chat-thread").start();
    }

    // ── Bubble ────────────────────────────────────────────────────────────────
    private void addBubble(String text, SenderType role) {
        if (role == SenderType.SYSTEM) {
            Label l = new Label(text);
            l.setStyle("-fx-text-fill: #6a9955; -fx-font-style: italic; -fx-font-size: 11px; -fx-padding: 2 4;");
            l.setWrapText(true);
            messages.getChildren().add(l);
        } else {
            final boolean isUser = role == SenderType.USER;
            Region bubble;
            if (isUser) {
                Label userLbl = new Label(text);
                userLbl.setWrapText(true);
                userLbl.setMaxWidth(220);
                userLbl.getStyleClass().add("chat-bubble-user");
                bubble = userLbl;
            } else {
                // Selectable, wrapped prose — Labels made JSON/errors unreadable and un-copyable.
                TextArea ta = new TextArea(text != null ? text : "");
                ta.setEditable(false);
                ta.setWrapText(true);
                ta.setMaxWidth(260);
                ta.setFocusTraversable(true);
                ta.getStyleClass().add("chat-bubble-ai");
                int lines = Math.max(2, Math.min(22, (text != null ? text : "").split("\r?\n", -1).length + 2));
                ta.setPrefRowCount(lines);
                bubble = ta;
            }

            // Timestamp
            Label ts = new Label(LocalTime.now().format(HM));
            ts.setStyle("-fx-font-size: 9px; -fx-text-fill: #555566;");

            VBox wrap = new VBox(2, bubble, ts);
            wrap.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

            HBox row = new HBox(wrap);
            row.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            messages.getChildren().add(row);
        }
        Platform.runLater(() -> scroll.setVvalue(1.0));
    }

    /** Read-only text area + Copy — Labels are not selectable, so generated code must use this. */
    private void addSkeletonCodeBubble(String skeletonCode) {
        String body = skeletonCode != null ? skeletonCode : "";
        String fullText = "Java skeleton (copy or use the button below):\n\n" + body;
        TextArea ta = new TextArea(fullText);
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setMaxWidth(260);
        ta.getStyleClass().add("chat-bubble-ai");
        ta.setStyle("-fx-font-family: 'Consolas','Courier New',monospace; -fx-font-size: 12px;");
        int lineCount = fullText.split("\r?\n", -1).length;
        ta.setPrefRowCount(Math.min(28, Math.max(4, lineCount + 1)));

        Button copyBtn = new Button("Copy");
        copyBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; "
            + "-fx-font-size: 11px; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 4 10;");
        copyBtn.setOnAction(e -> {
            ClipboardContent cc = new ClipboardContent();
            cc.putString(body);
            Clipboard.getSystemClipboard().setContent(cc);
            MainWindow.showToast("Copied");
        });

        HBox actions = new HBox(copyBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setMaxWidth(260);

        Label ts = new Label(LocalTime.now().format(HM));
        ts.setStyle("-fx-font-size: 9px; -fx-text-fill: #555566;");

        VBox wrap = new VBox(4, actions, ta, ts);
        wrap.setAlignment(Pos.CENTER_LEFT);

        HBox row = new HBox(wrap);
        row.setAlignment(Pos.CENTER_LEFT);
        messages.getChildren().add(row);
        Platform.runLater(() -> scroll.setVvalue(1.0));
    }

    private void showThinking() {
        thinkingLabel = new Label("● ○ ○");
        thinkingLabel.setStyle("-fx-text-fill: #6a9955; -fx-font-style: italic;");
        HBox row = new HBox(thinkingLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        messages.getChildren().add(row);
        thinkingAnim = new Timeline(
            new KeyFrame(Duration.ZERO,       e -> thinkingLabel.setText("● ○ ○")),
            new KeyFrame(Duration.millis(400), e -> thinkingLabel.setText("○ ● ○")),
            new KeyFrame(Duration.millis(800), e -> thinkingLabel.setText("○ ○ ●")),
            new KeyFrame(Duration.millis(1200))
        );
        thinkingAnim.setCycleCount(Animation.INDEFINITE);
        thinkingAnim.play();
        Platform.runLater(() -> scroll.setVvalue(1.0));
    }

    private void hideThinking() {
        if (thinkingAnim != null) thinkingAnim.stop();
        if (thinkingLabel != null)
            messages.getChildren().removeIf(n ->
                n instanceof HBox hb && !hb.getChildren().isEmpty()
                && hb.getChildren().get(0) == thinkingLabel);
    }

    private void startRec() {
        if (stt == null || !stt.isAvailable()) { MainWindow.showToast("No microphone"); return; }
        recording = true; stt.startRecording();
        btnMic.setStyle(btnMic.getStyle().replace("#2d2d3a", "#b02020"));
    }

    private void stopRec() {
        if (!recording) return; recording = false;
        btnMic.setStyle(btnMic.getStyle().replace("#b02020", "#2d2d3a"));
        String t = stt.stopRecording();
        if (t != null && !t.isBlank()) { input.setText(t); input.requestFocus(); }
    }
}
