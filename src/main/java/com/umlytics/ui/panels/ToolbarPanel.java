package com.umlytics.ui.panels;

import com.umlytics.ui.MainWindow;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

// GRASP: Controller (Facade boundary)
public class ToolbarPanel extends ToolBar {

    private final MainWindow facade;

    public ToolbarPanel(MainWindow facade) {
        this.facade = facade;
        getStyleClass().add("toolbar-panel");

        // ── Project group ──
        Button btnNew  = btn("New Project",   "New project  Ctrl+N");
        Button btnOpen = btn("Open Project",  "Open existing project");
        Button btnSave = btn("Save",          "Save diagram  Ctrl+S");

        btnNew.setOnAction(e  -> facade.promptNewProject());
        btnOpen.setOnAction(e -> handleOpen());
        btnSave.setOnAction(e -> {
            facade.getEditorPanel().saveCurrentDiagram();
            facade.refreshProjectExplorer();
            facade.onSaved();
            MainWindow.showToast("Saved ✓");
        });

        // ── Diagram group ──
        Button btnNewDiag = btn("+ New Diagram", "Add blank diagram");
        Button btnText    = btn("From Text",     "Generate diagram from text description  Ctrl+G");
        Button btnCode    = btn("From Code",     "Reverse-engineer Java source  UC3");
        Button btnImage   = btn("Analyse Image", "Upload & analyse UML image  UC8");

        btnNewDiag.setOnAction(e -> {
            java.util.UUID pid = facade.getActiveProjectId();
            if (pid == null) {
                var all = facade.getProjectController().getAllProjects();
                if (all.isEmpty()) {
                    MainWindow.showToast("Create a project first");
                    return;
                }
                pid = all.get(0).getProjectId();
                facade.setActiveProjectId(pid);
            }
            facade.newDiagramForProject(pid);
        });
        btnText.setOnAction(e    -> facade.getEditorPanel().showGenerateFromTextDialog());
        btnCode.setOnAction(e    -> handleGenCode());
        btnImage.setOnAction(e   -> handleUploadImage());

        // ── AI group ──
        Button btnEval    = btn("Evaluate", "Run AI quality evaluation  UC5");
        Button btnSuggest = btn("Gen Code", "Generate code skeleton  UC7");

        btnEval.setOnAction(e    -> facade.getChatPanel().runEvaluation());
        btnSuggest.setOnAction(e -> facade.getChatPanel().generateStructure());

        // ── View group ──
        Button btnFit     = btn("Fit Screen", "Fit all nodes in view");
        Button btnZoomIn  = btn("+",          "Zoom in");
        Button btnZoomOut = btn("–",          "Zoom out");
        Button btnExport  = btn("Export",     "Export diagram  Ctrl+Shift+E");

        btnFit.setOnAction(e     -> facade.getEditorPanel().fitToScreen());
        btnZoomIn.setOnAction(e  -> facade.getEditorPanel().zoomIn());
        btnZoomOut.setOnAction(e -> facade.getEditorPanel().zoomOut());
        btnExport.setOnAction(e  -> facade.getEditorPanel().showExportDialog());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getItems().addAll(
            btnNew, btnOpen, btnSave,
            new Separator(),
            btnNewDiag, btnText, btnCode, btnImage,
            new Separator(),
            btnEval, btnSuggest,
            spacer,
            btnFit, btnZoomIn, btnZoomOut,
            new Separator(),
            btnExport
        );
    }

    private Button btn(String label, String tip) {
        Button b = new Button(label);
        b.getStyleClass().add("tool-btn");
        b.setTooltip(new Tooltip(tip));
        return b;
    }

    private void handleOpen() {
        var all = facade.getProjectController().getAllProjects();
        if (all.isEmpty()) { MainWindow.showToast("No projects — create one first"); return; }
        ChoiceDialog<com.umlytics.domain.Project> dlg = new ChoiceDialog<>(all.get(0), all);
        dlg.setTitle("Open Project");
        dlg.setHeaderText("Select project:");
        dlg.showAndWait().ifPresent(p -> {
            facade.getProjectController().openProject(p.getProjectId());
            facade.setActiveProjectId(p.getProjectId());
            facade.refreshProjectExplorer();
            MainWindow.showToast("Opened: " + p.getName());
        });
    }

    private void handleGenCode() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Java Source Files");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Java Files", "*.java"));
        List<File> files = fc.showOpenMultipleDialog(null);
        if (files == null || files.isEmpty()) return;
        java.util.UUID pid = facade.getActiveProjectId();
        if (pid == null) {
            var all = facade.getProjectController().getAllProjects();
            if (all.isEmpty()) {
                MainWindow.showToast("Create a project first");
                return;
            }
            pid = all.get(0).getProjectId();
            facade.setActiveProjectId(pid);
        }
        try {
            var d = facade.getDiagramController().generateFromCode(pid, files);
            facade.openDiagram(d);
            facade.refreshProjectExplorer();
            MainWindow.showToast("Diagram from " + files.size() + " file(s) ✓");
        } catch (Exception ex) {
            MainWindow.showToast("Error: " + ex.getMessage());
        }
    }

    private void handleUploadImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select UML Diagram Image");
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Images", "*.png","*.jpg","*.jpeg"));
        File f = fc.showOpenDialog(null);
        if (f == null) return;
        try {
            byte[] bytes = java.nio.file.Files.readAllBytes(f.toPath());
            java.util.UUID pid = facade.getActiveProjectId();
            if (pid == null) {
                var all = facade.getProjectController().getAllProjects();
                if (all.isEmpty()) {
                    MainWindow.showToast("Create a project first");
                    return;
                }
                pid = all.get(0).getProjectId();
                facade.setActiveProjectId(pid);
            }
            com.umlytics.domain.UMLDiagram d = facade.getDiagramController().analyzeUploadedImage(pid, bytes);
            d.setTitle("From Image: " + f.getName());
            facade.openDiagram(d);
            facade.refreshProjectExplorer();
            MainWindow.showToast("Image analysed ✓  " + f.getName());
        } catch (Exception ex) {
            MainWindow.showToast("Error: " + ex.getMessage());
        }
    }
}
