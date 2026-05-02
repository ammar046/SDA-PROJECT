package com.umlytics.ui;

import com.umlytics.controllers.AIController;
import com.umlytics.domain.EvaluationReport;
import com.umlytics.domain.StructureSuggestion;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.stage.FileChooser;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EvaluationPanel extends VBox {
    private AIController aiCtrl;
    private final Label couplingScoreLabel;
    private final Label cohesionScoreLabel;
    private final Label solidScoreLabel;
    private final ListView<String> evaluationSuggestionsList;
    private final SplitPane structureViewer;
    private final ListView<String> structureClassNames;
    private final TextFlow structureCodeFlow;
    private final ScrollPane structureCodeScroll;
    private final Map<String, String> structureSkeletons = new LinkedHashMap<>();
    private final Label statusLabel;
    private EvaluationReport lastReport;
    private int activeDiagramId = 1;

    public EvaluationPanel() {
        setSpacing(10);
        setPadding(new Insets(12));
        Button evaluateButton = new Button("Evaluate Design");
        evaluateButton.setOnAction(event -> onEvaluate());
        Button generateStructureButton = new Button("Generate Structure Suggestions");
        generateStructureButton.setOnAction(event -> onGenerateSuggestions());
        Button exportReportButton = new Button("Export Report PDF");
        exportReportButton.setOnAction(event -> onExportReport());

        couplingScoreLabel = new Label("Coupling: -");
        cohesionScoreLabel = new Label("Cohesion: -");
        solidScoreLabel = new Label("SOLID: -");
        HBox scoreRow = new HBox(16, couplingScoreLabel, cohesionScoreLabel, solidScoreLabel);
        HBox actionsRow = new HBox(10, evaluateButton, generateStructureButton, exportReportButton);

        evaluationSuggestionsList = new ListView<>();
        evaluationSuggestionsList.setPrefHeight(220);

        structureClassNames = new ListView<>();
        structureClassNames.setMinWidth(160);
        structureClassNames.setPrefWidth(200);

        structureCodeFlow = new TextFlow();
        structureCodeFlow.setLineSpacing(2);
        structureCodeFlow.getStyleClass().add("structure-code-view");
        structureCodeScroll = new ScrollPane(structureCodeFlow);
        structureCodeScroll.setFitToWidth(true);
        structureCodeScroll.setPrefHeight(280);
        structureCodeScroll.getStyleClass().add("structure-code-scroll");

        structureViewer = new SplitPane(structureClassNames, structureCodeScroll);
        structureViewer.setVisible(false);
        structureViewer.setManaged(false);

        structureClassNames.getSelectionModel().selectedItemProperty().addListener((obs, prev, name) -> {
            structureCodeFlow.getChildren().clear();
            if (name == null) {
                return;
            }
            String code = structureSkeletons.getOrDefault(name, "");
            JavaSyntaxHighlighter.fillTextFlow(structureCodeFlow, code);
        });

        statusLabel = new Label("Ready");
        getChildren().addAll(actionsRow, scoreRow, evaluationSuggestionsList, structureViewer, statusLabel);
    }

    public void onEvaluate() {
        if (aiCtrl == null) {
            statusLabel.setText("AI service is not available.");
            return;
        }
        try {
            EvaluationReport report = aiCtrl.evaluateDesign(activeDiagramId);
            displayReport(report);
            statusLabel.setText("Evaluation generated.");
        } catch (Exception e) {
            statusLabel.setText("Evaluation failed.");
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    public void onGenerateSuggestions() {
        if (aiCtrl == null) {
            statusLabel.setText("AI service is not available.");
            return;
        }
        try {
            StructureSuggestion structureSuggestion = aiCtrl.generateStructureSuggestions(activeDiagramId);
            Map<String, String> skel = structureSuggestion.getSkeletons();
            structureSkeletons.clear();
            if (skel != null) {
                structureSkeletons.putAll(skel);
            }
            showStructureViewerPanel();
            structureClassNames.getItems().setAll(new ArrayList<>(structureSkeletons.keySet()));
            if (structureClassNames.getItems().isEmpty()) {
                structureClassNames.getItems().add("(no skeletons)");
                structureSkeletons.put("(no skeletons)", "// No code skeletons were returned.");
            }
            structureClassNames.getSelectionModel().selectFirst();
            statusLabel.setText("Structure suggestions generated.");
        } catch (Exception e) {
            statusLabel.setText("Suggestion generation failed.");
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    public void onExportReport() {
        if (lastReport == null) {
            new Alert(Alert.AlertType.WARNING, "Run evaluation first.").show();
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Evaluation Report");
        chooser.setInitialFileName("evaluation-report-" + activeDiagramId + ".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File file = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            exportReportToPdf(lastReport, file);
            statusLabel.setText("Report exported: " + file.getName());
        } catch (Exception e) {
            statusLabel.setText("Report export failed.");
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    public void displayReport(EvaluationReport r) {
        this.lastReport = r;
        showEvaluationSuggestionMode();
        couplingScoreLabel.setText("Coupling: " + r.getCouplingScore());
        cohesionScoreLabel.setText("Cohesion: " + r.getCohesionScore());
        solidScoreLabel.setText("SOLID: " + r.getSolidScore());
        List<String> items = r.getSuggestions();
        evaluationSuggestionsList.getItems().setAll(items != null ? items : List.of());
    }

    public void setAiCtrl(AIController aiCtrl) {
        this.aiCtrl = aiCtrl;
    }

    public void setActiveDiagramId(int activeDiagramId) {
        this.activeDiagramId = activeDiagramId;
    }

    private void showEvaluationSuggestionMode() {
        evaluationSuggestionsList.setVisible(true);
        evaluationSuggestionsList.setManaged(true);
        structureViewer.setVisible(false);
        structureViewer.setManaged(false);
    }

    private void showStructureViewerPanel() {
        evaluationSuggestionsList.setVisible(false);
        evaluationSuggestionsList.setManaged(false);
        structureViewer.setVisible(true);
        structureViewer.setManaged(true);
    }

    private void exportReportToPdf(EvaluationReport report, File output) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.setLeading(16f);
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                content.newLineAtOffset(50, 750);
                content.showText("UMLytics Evaluation Report");
                content.newLine();
                content.newLine();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.showText("Diagram ID: " + report.getDiagramId());
                content.newLine();
                content.showText("Coupling: " + report.getCouplingScore());
                content.newLine();
                content.showText("Cohesion: " + report.getCohesionScore());
                content.newLine();
                content.showText("SOLID: " + report.getSolidScore());
                content.newLine();
                content.newLine();
                content.showText("Suggestions:");
                List<String> sug = report.getSuggestions();
                if (sug != null) {
                    for (String suggestion : sug) {
                        content.newLine();
                        content.showText("- " + suggestion);
                    }
                }
                content.endText();
            }
            document.save(output);
        }
    }
}
