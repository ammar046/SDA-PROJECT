package com.umlytics.ui.panels;

import com.umlytics.domain.Project;
import com.umlytics.enums.SourceType;
import com.umlytics.ui.MainWindow;
import com.umlytics.ui.dialogs.ExportDialog;
import com.umlytics.ui.dialogs.GenerateFromTextDialog;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Top toolbar with project, diagram, AI, and view action groups.
 * GRASP: Controller (Facade boundary)
 */
public class ToolbarPanel extends ToolBar {

    private final MainWindow facade;

    public ToolbarPanel(MainWindow facade) {
        this.facade = facade;
        getStyleClass().add("toolbar-panel");

        // ---- LEFT: Project actions ----
        Button btnNewProj  = createToolButton("◻ New Project",    "Create a new project (Ctrl+N)");
        Button btnOpenProj = createToolButton("📂 Open Project",   "Open an existing project");
        Button btnSave     = createToolButton("💾 Save",           "Save current diagram (Ctrl+S)");

        btnNewProj.setOnAction(e -> handleNewProject());
        btnOpenProj.setOnAction(e -> handleOpenProject());
        btnSave.setOnAction(e -> handleSave());

        // ---- MIDDLE: Diagram actions ----
        Button btnNewDiagram  = createToolButton("＋ New Diagram",        "Add a blank diagram");
        Button btnGenText     = createToolButton("⊞ From Text",           "Generate diagram from natural language (UC2)");
        Button btnGenCode     = createToolButton("{} From Code",          "Generate diagram from Java source (UC3)");
        Button btnUploadImage = createToolButton("🖼 Upload Image",       "Analyse UML diagram image (UC8)");

        btnNewDiagram.setOnAction(e -> handleNewDiagram());
        btnGenText.setOnAction(e -> new GenerateFromTextDialog(facade).showAndWait());
        btnGenCode.setOnAction(e -> handleGenerateFromCode());
        btnUploadImage.setOnAction(e -> handleUploadImage());

        // ---- RIGHT: AI + View ----
        Button btnEval     = createToolButton("⚙ Evaluate",      "Evaluate design quality (UC9)");
        Button btnCode     = createToolButton("≡ Gen Code",       "Generate Java code skeletons (UC12)");
        Button btnFit      = createToolButton("◎ Fit Screen",     "Fit all nodes in view");
        Button btnZoomIn   = createToolButton("+ Zoom",           "Zoom in (Ctrl+scroll)");
        Button btnZoomOut  = createToolButton("− Zoom",           "Zoom out (Ctrl+scroll)");
        Button btnExport   = createToolButton("📤 Export",        "Export diagram (Ctrl+Shift+E)");

        btnEval.setOnAction(e -> facade.getAiChatPanel().runEvaluation());
        btnCode.setOnAction(e -> facade.getAiChatPanel().generateCode());
        btnFit.setOnAction(e -> facade.getMainCanvas().fitToScreen());
        btnZoomIn.setOnAction(e -> facade.getMainCanvas().zoomIn());
        btnZoomOut.setOnAction(e -> facade.getMainCanvas().zoomOut());
        btnExport.setOnAction(e -> new ExportDialog(facade).showAndWait());

        // Spacer between right-align
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getItems().addAll(
            btnNewProj, btnOpenProj, btnSave,
            new Separator(),
            btnNewDiagram, btnGenText, btnGenCode, btnUploadImage,
            new Separator(),
            btnEval, btnCode,
            spacer,
            btnFit, btnZoomIn, btnZoomOut,
            new Separator(),
            btnExport
        );
    }

    private Button createToolButton(String label, String tooltip) {
        Button b = new Button(label);
        b.getStyleClass().add("tool-btn");
        b.setTooltip(new Tooltip(tooltip));
        b.setPrefWidth(110);
        b.setPrefHeight(30);
        return b;
    }

    // ---- Handlers ----

    private void handleNewProject() {
        TextInputDialog dlg = new TextInputDialog("My Project");
        dlg.setTitle("New Project");
        dlg.setHeaderText("Enter project name:");
        dlg.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                facade.getProjectController().createProject(name.trim(), "");
                facade.refreshProjectExplorer();
                MainWindow.showToast("Project \"" + name.trim() + "\" created");
            }
        });
    }

    private void handleOpenProject() {
        List<Project> projects = facade.getProjectController().getAllProjects();
        if (projects.isEmpty()) {
            MainWindow.showToast("No projects found — create one first");
            return;
        }
        ChoiceDialog<Project> dlg = new ChoiceDialog<>(projects.get(0), projects);
        dlg.setTitle("Open Project");
        dlg.setHeaderText("Select a project to open:");
        dlg.setContentText("Project:");
        dlg.showAndWait().ifPresent(p -> {
            facade.getProjectController().openProject(p.getProjectId());
            facade.refreshProjectExplorer();
            MainWindow.showToast("Opened project: " + p.getName());
        });
    }

    private void handleSave() {
        facade.getDiagramController().saveCurrentDiagram();
        MainWindow.showToast("Saved ✓");
        facade.onSaved();
    }

    private void handleNewDiagram() {
        Project p = facade.getProjectController().getCurrentProject();
        if (p == null) { MainWindow.showToast("No project open"); return; }
        facade.getDiagramController().createDiagram(
                p.getProjectId(), "Diagram " + System.currentTimeMillis(), SourceType.MANUAL);
        facade.refreshProjectExplorer();
        MainWindow.showToast("New diagram created");
    }

    private void handleGenerateFromCode() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Java Source Files");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Java Files", "*.java"));
        List<File> files = fc.showOpenMultipleDialog(null);
        if (files == null || files.isEmpty()) return;

        facade.getAiChatPanel().appendSystemBubble("Parsing " + files.size() + " Java file(s)...");

        new Thread(() -> {
            try {
                com.umlytics.domain.valueobjects.UMLModel model =
                        facade.getAIController().generateFromCode(files);
                javafx.application.Platform.runLater(() -> {
                    Project p = facade.getProjectController().getCurrentProject();
                    if (p == null) return;
                    com.umlytics.domain.UMLDiagram d = facade.getDiagramController().createDiagram(
                            p.getProjectId(), "Code: " + files.get(0).getName(), com.umlytics.enums.SourceType.CODE);
                    applyAutoLayout(model, d);
                    facade.getDiagramController().setCurrentDiagram(d);
                    facade.getDiagramController().saveCurrentDiagram();
                    facade.getMainCanvas().renderDiagram(d);
                    facade.refreshProjectExplorer();
                    MainWindow.showToast("Diagram generated from code");
                });
            } catch (RuntimeException ex) {
                javafx.application.Platform.runLater(() -> {
                    Alert err = new Alert(Alert.AlertType.ERROR, "Parse failed: " + ex.getMessage(), ButtonType.OK);
                    err.setTitle("Code Parse Error"); err.showAndWait();
                });
            }
        }).start();
    }

    private void handleUploadImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select UML Diagram Image");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File file = fc.showOpenDialog(null);
        if (file == null) return;

        facade.getAiChatPanel().appendSystemBubble("Analysing image: " + file.getName() + "...");

        new Thread(() -> {
            try {
                byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
                com.umlytics.domain.valueobjects.UMLModel model =
                        facade.getAIController().generateFromImage(bytes);
                javafx.application.Platform.runLater(() -> {
                    showImageMergeDialog(model);
                });
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() ->
                    facade.getAiChatPanel().appendSystemBubble("Image analyse failed: " + ex.getMessage()));
            }
        }).start();
    }

    private void showImageMergeDialog(com.umlytics.domain.valueobjects.UMLModel model) {
        Alert dlg = new Alert(Alert.AlertType.CONFIRMATION,
                "How should the extracted classes be applied?",
                new ButtonType("Replace Current", ButtonBar.ButtonData.YES),
                new ButtonType("Merge into Current", ButtonBar.ButtonData.NO),
                ButtonType.CANCEL);
        dlg.setTitle("Apply Extracted Diagram");
        dlg.setHeaderText("Image analysis complete — " + model.getClasses().size() + " classes found.");
        dlg.showAndWait().ifPresent(bt -> {
            if (bt.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) return;
            boolean replace = bt.getButtonData() == ButtonBar.ButtonData.YES;
            Project p = facade.getProjectController().getCurrentProject();
            if (p == null) return;

            com.umlytics.domain.UMLDiagram d;
            if (replace || facade.getDiagramController().getCurrentDiagram() == null) {
                d = facade.getDiagramController().createDiagram(
                        p.getProjectId(), "Image Analysis", SourceType.UPLOAD);
            } else {
                d = facade.getDiagramController().getCurrentDiagram();
            }

            applyAutoLayout(model, d);
            facade.getDiagramController().setCurrentDiagram(d);
            facade.getDiagramController().saveCurrentDiagram();
            facade.getMainCanvas().renderDiagram(d);
            facade.refreshProjectExplorer();
            MainWindow.showToast("Image analyzed");
        });
    }

    /** Auto-layout classes in a 4-column grid starting at (60, 60). */
    public static void applyAutoLayout(com.umlytics.domain.valueobjects.UMLModel model,
                                       com.umlytics.domain.UMLDiagram d) {
        List<com.umlytics.domain.UMLClass> classes = model.getClasses();
        for (int i = 0; i < classes.size(); i++) {
            com.umlytics.domain.UMLClass cls = classes.get(i);
            int col = i % 4;
            int row = i / 4;
            cls.setPositionX(60 + col * 240);
            cls.setPositionY(60 + row * 200);
            d.addUMLClass(cls);
        }
        model.getRelationships().forEach(d::addRelationship);
    }
}
