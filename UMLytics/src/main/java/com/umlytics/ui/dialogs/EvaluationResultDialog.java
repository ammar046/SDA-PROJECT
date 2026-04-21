package com.umlytics.ui.dialogs;

import com.umlytics.domain.EvaluationReport;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * UC5 — Evaluation result dialog with colour-coded progress bars.
 */
public class EvaluationResultDialog extends Dialog<Void> {

    public EvaluationResultDialog(EvaluationReport report) {
        setTitle("Design Evaluation Report");
        setHeaderText(null);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        getDialogPane().setPrefWidth(480);

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));

        Label heading = new Label("Design Evaluation Report");
        heading.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #d4d4d4;");

        content.getChildren().addAll(
            heading,
            buildScoreRow("Coupling",         report.getCouplingScore()),
            buildScoreRow("Cohesion",         report.getCohesionScore()),
            buildScoreRow("SOLID Compliance", report.getSolidScore()),
            new Separator(),
            buildSuggestions(report),
            new Separator(),
            buildOverallLabel(report)
        );

        getDialogPane().setContent(content);

        // Style the dialog pane
        getDialogPane().setStyle("-fx-background-color: #1e1e2e;");
    }

    private VBox buildScoreRow(String label, double score) {
        VBox row = new VBox(4);
        HBox header = new HBox(8);

        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: #d4d4d4; -fx-font-size: 13px; -fx-font-weight: 500;");
        Label pct = new Label(String.format("%.0f%%", score));
        pct.setStyle("-fx-text-fill: #888888; -fx-font-size: 12px;");

        header.getChildren().addAll(lbl, pct);

        ProgressBar bar = new ProgressBar(score / 100.0);
        bar.setPrefWidth(420);
        bar.setPrefHeight(10);

        // Colour code by score
        if (score < 50)      bar.getStyleClass().add("eval-bar-bad");
        else if (score < 75) bar.getStyleClass().add("eval-bar-ok");
        else                 bar.getStyleClass().add("eval-bar-good");

        row.getChildren().addAll(header, bar);
        return row;
    }

    private VBox buildSuggestions(EvaluationReport report) {
        VBox box = new VBox(6);
        Label title = new Label("Suggestions");
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #c0c0c0;");

        ListView<String> list = new ListView<>();
        list.getItems().addAll(report.getSuggestions().isEmpty()
                ? java.util.List.of("No critical issues found.")
                : report.getSuggestions());
        list.setPrefHeight(Math.min(120, report.getSuggestions().size() * 28 + 20));
        list.setStyle("-fx-background-color: #252526; -fx-control-inner-background: #252526;");
        list.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : "• " + item);
                setStyle("-fx-text-fill: #c0c0c0; -fx-font-size: 12px;");
            }
        });

        box.getChildren().addAll(title, list);
        return box;
    }

    private Label buildOverallLabel(EvaluationReport report) {
        double overall = report.getOverallScore();
        Label lbl = new Label(String.format("Overall Score: %.0f%%", overall));
        String colour = overall < 50 ? "#e24b4a" : overall < 75 ? "#ef9f27" : "#639922";
        lbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + colour + ";");
        return lbl;
    }
}
