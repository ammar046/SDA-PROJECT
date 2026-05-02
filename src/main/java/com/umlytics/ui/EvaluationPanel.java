package com.umlytics.ui;

import com.umlytics.controllers.AIController;
import com.umlytics.domain.EvaluationReport;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class EvaluationPanel extends VBox {
    private AIController aiCtrl;
    private final Label couplingScoreLabel;
    private final Label cohesionScoreLabel;
    private final Label solidScoreLabel;
    private final ListView<String> suggestions;
    private int activeDiagramId = 1;

    public EvaluationPanel() {
        setSpacing(10);
        setPadding(new Insets(12));
        Button evaluateButton = new Button("Evaluate Design");
        evaluateButton.setOnAction(event -> onEvaluate());

        couplingScoreLabel = new Label("Coupling: -");
        cohesionScoreLabel = new Label("Cohesion: -");
        solidScoreLabel = new Label("SOLID: -");
        HBox scoreRow = new HBox(16, couplingScoreLabel, cohesionScoreLabel, solidScoreLabel);

        suggestions = new ListView<>();
        getChildren().addAll(evaluateButton, scoreRow, suggestions);
    }

    public void onEvaluate() {
        if (aiCtrl == null) {
            return;
        }
        EvaluationReport report = aiCtrl.evaluateDesign(activeDiagramId);
        displayReport(report);
    }

    public void displayReport(EvaluationReport r) {
        couplingScoreLabel.setText("Coupling: " + r.getCouplingScore());
        cohesionScoreLabel.setText("Cohesion: " + r.getCohesionScore());
        solidScoreLabel.setText("SOLID: " + r.getSolidScore());
        suggestions.getItems().setAll(r.getSuggestions());
    }

    public void setAiCtrl(AIController aiCtrl) {
        this.aiCtrl = aiCtrl;
    }

    public void setActiveDiagramId(int activeDiagramId) {
        this.activeDiagramId = activeDiagramId;
    }
}
