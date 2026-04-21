package com.umlytics.ui.dialogs;

import com.umlytics.domain.UMLDiagram;
import com.umlytics.domain.valueobjects.UMLModel;
import com.umlytics.enums.SourceType;
import com.umlytics.ui.MainWindow;
import com.umlytics.ui.panels.ToolbarPanel;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

/**
 * UC2 — Generate UML diagram from natural language description.
 */
public class GenerateFromTextDialog extends Dialog<String> {

    public GenerateFromTextDialog(MainWindow facade) {
        setTitle("Generate Diagram from Text");
        setHeaderText("Describe your system and UMLytics AI will generate a diagram.");

        ButtonType generateBtn = new ButtonType("Generate", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn   = ButtonType.CANCEL;
        getDialogPane().getButtonTypes().addAll(generateBtn, cancelBtn);

        // TextArea prompt
        TextArea promptArea = new TextArea();
        promptArea.setPromptText("e.g. A library system with Book, Member, Loan classes. "
                + "Book has title and ISBN. Member has name and membershipId. "
                + "Loan connects Member to Book with a borrow date.");
        promptArea.setPrefRowCount(5);
        promptArea.setWrapText(true);
        promptArea.setId("generate-text-prompt");

        Label hint = new Label("Describe your system:");
        hint.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 12px;");

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setVisible(false);
        spinner.setPrefSize(28, 28);

        Label status = new Label("");
        status.setStyle("-fx-text-fill: #6a9955; -fx-font-size: 12px;");

        VBox content = new VBox(8, hint, promptArea, spinner, status);
        content.setPadding(new Insets(10));
        content.setPrefWidth(500);
        getDialogPane().setContent(content);

        // Disable generate button when text is empty
        javafx.scene.Node genButton = getDialogPane().lookupButton(generateBtn);
        genButton.setDisable(true);
        promptArea.textProperty().addListener((_o, _ov, nv) ->
                genButton.setDisable(nv.trim().isEmpty()));

        // Override default close behaviour — handle generation async
        genButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            event.consume(); // prevent dialog from closing yet
            String text = promptArea.getText().trim();
            if (text.isEmpty()) return;

            genButton.setDisable(true);
            spinner.setVisible(true);
            status.setText("Generating…");

            new Thread(() -> {
                try {
                    UMLModel model = facade.getAIController().generateFromPrompt(text);
                    Platform.runLater(() -> {
                        int projectId = facade.getProjectController().getCurrentProject() != null
                                ? facade.getProjectController().getCurrentProject().getProjectId() : 1;
                        String title = "Generated: " + text.substring(0, Math.min(20, text.length()));
                        UMLDiagram d = facade.getDiagramController().createDiagram(projectId, title, SourceType.NL);
                        ToolbarPanel.applyAutoLayout(model, d);
                        facade.getDiagramController().setCurrentDiagram(d);
                        facade.getDiagramController().saveCurrentDiagram();
                        facade.getMainCanvas().renderDiagram(d);
                        facade.refreshProjectExplorer();
                        MainWindow.showToast("Diagram generated");
                        close();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        status.setText("Error: " + ex.getMessage());
                        spinner.setVisible(false);
                        genButton.setDisable(false);
                    });
                }
            }).start();
        });
    }
}
