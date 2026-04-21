package com.umlytics.ui.dialogs;

import com.umlytics.enums.ExportFormat;
import com.umlytics.ui.MainWindow;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;

import java.io.File;

/**
 * UC12 Export — PNG / SVG / PDF export dialog.
 */
public class ExportDialog extends Dialog<Void> {

    public ExportDialog(MainWindow facade) {
        setTitle("Export Diagram");
        setHeaderText("Choose format and destination to export your diagram.");

        ButtonType exportBtn = new ButtonType("Export", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(exportBtn, ButtonType.CANCEL);

        // Format selector
        ComboBox<String> formatBox = new ComboBox<>();
        formatBox.getItems().addAll("PNG", "SVG", "PDF");
        formatBox.setValue("PNG");
        formatBox.setId("export-format-combo");

        // Path field
        TextField pathField = new TextField();
        pathField.setEditable(false);
        pathField.setPromptText("Choose save location…");
        HBox.setHgrow(pathField, Priority.ALWAYS);

        Button browseBtn = new Button("Browse…");
        browseBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Save Diagram As");
            String fmt = formatBox.getValue();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                    fmt + " Files", "*." + fmt.toLowerCase()));
            fc.setInitialFileName("diagram." + fmt.toLowerCase());
            File f = fc.showSaveDialog(null);
            if (f != null) pathField.setText(f.getAbsolutePath());
        });

        // Disable export until path chosen
        javafx.scene.Node expButton = getDialogPane().lookupButton(exportBtn);
        expButton.setDisable(true);
        pathField.textProperty().addListener((_o, _ov, nv) -> expButton.setDisable(nv.trim().isEmpty()));

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(12));
        grid.add(new Label("Format:"), 0, 0);
        grid.add(formatBox, 1, 0);
        grid.add(new Label("Save to:"), 0, 1);
        grid.add(new HBox(6, pathField, browseBtn), 1, 1);

        getDialogPane().setContent(grid);
        getDialogPane().setPrefWidth(480);

        // Handle export on OK
        expButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            event.consume();
            String path = pathField.getText().trim();
            if (path.isEmpty()) return;

            String fmt = formatBox.getValue();
            ExportFormat format = ExportFormat.valueOf(fmt);

            try {
                facade.getDiagramController().exportDiagram(format, path);
                Alert info = new Alert(Alert.AlertType.INFORMATION,
                        "Exported successfully to:\n" + path, ButtonType.OK);
                info.setTitle("Export Complete");
                info.setHeaderText(null);
                info.showAndWait();
                MainWindow.showToast("Export complete");
                close();
            } catch (Exception ex) {
                Alert err = new Alert(Alert.AlertType.ERROR,
                        "Export failed: " + ex.getMessage(), ButtonType.OK);
                err.setTitle("Export Error"); err.showAndWait();
            }
        });
    }
}
