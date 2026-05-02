package com.umlytics.ui;

import com.umlytics.controllers.AIController;
import com.umlytics.controllers.DiagramController;
import com.umlytics.controllers.ProjectController;
import com.umlytics.domain.Project;
import com.umlytics.domain.UMLDiagram;
import com.umlytics.enums.ExportFormat;
import com.umlytics.interfaces.ISpeechToTextService;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToolBar;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

// GRASP: Controller (Facade / Boundary)
public class MainWindow extends Application {
    private ProjectController projectCtrl;
    private DiagramController diagramCtrl;
    private AIController aiCtrl;
    private ISpeechToTextService speechSvc;
    private Project activeProject;
    private TabPane workspaceTabs;
    private ProjectDashboardPanel dashboardPanel;

    public MainWindow() {
    }

    public MainWindow(ProjectController projectCtrl, DiagramController diagramCtrl, AIController aiCtrl) {
        this.projectCtrl = projectCtrl;
        this.diagramCtrl = diagramCtrl;
        this.aiCtrl = aiCtrl;
    }

    public MainWindow(ProjectController projectCtrl, DiagramController diagramCtrl, AIController aiCtrl, ISpeechToTextService speechSvc) {
        this(projectCtrl, diagramCtrl, aiCtrl);
        this.speechSvc = speechSvc;
    }

    public void openProject() {
        if (dashboardPanel != null) {
            dashboardPanel.refreshProjects();
        }
    }

    public void createProject() {
        if (dashboardPanel != null) {
            dashboardPanel.onCreateProject();
        }
    }

    public void showDiagramEditor() {
        showDiagramEditor(null, "Diagram");
    }

    public void showDiagramEditor(UMLDiagram diagram, String title) {
        DiagramEditorPanel editorPanel = new DiagramEditorPanel(diagramCtrl);
        if (diagram != null) {
            editorPanel.renderDiagram(diagram);
        }
        Tab tab = new Tab(title == null || title.isBlank() ? "Diagram" : title, editorPanel);
        workspaceTabs.getTabs().add(tab);
        workspaceTabs.getSelectionModel().select(tab);
    }

    public void showChatPanel() {
        ChatPanel chatPanel = new ChatPanel();
        chatPanel.setAiCtrl(aiCtrl);
        chatPanel.setSpeechSvc(speechSvc);
        if (activeProject != null) {
            chatPanel.setActiveProjectId(activeProject.getProjectId());
        }
        Tab tab = new Tab("AI Chat", chatPanel);
        workspaceTabs.getTabs().add(tab);
        workspaceTabs.getSelectionModel().select(tab);
    }

    public void showEvaluationPanel() {
        showEvaluationPanel(false);
    }

    public void showEvaluationPanel(boolean triggerSuggestions) {
        EvaluationPanel evaluationPanel = new EvaluationPanel();
        evaluationPanel.setAiCtrl(aiCtrl);
        DiagramEditorPanel activeEditor = getActiveEditor();
        if (activeEditor != null && activeEditor.getCurrentDiagram() != null) {
            evaluationPanel.setActiveDiagramId(activeEditor.getCurrentDiagram().getDiagramId());
        }
        Tab tab = new Tab("Evaluation", evaluationPanel);
        workspaceTabs.getTabs().add(tab);
        workspaceTabs.getSelectionModel().select(tab);
        if (triggerSuggestions) {
            evaluationPanel.onGenerateSuggestions();
        }
    }

    public void initialize() {
        if (dashboardPanel != null) {
            dashboardPanel.setProjectCtrl(projectCtrl);
            dashboardPanel.setOpenProjectHandler(this::openProjectWorkspace);
            dashboardPanel.refreshProjects();
        }
    }

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();

        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");
        MenuItem newProject = new MenuItem("New Project");
        MenuItem openProject = new MenuItem("Open Project");
        MenuItem exit = new MenuItem("Exit");
        fileMenu.getItems().addAll(newProject, openProject, exit);
        Menu diagramMenu = new Menu("Diagram");
        MenuItem generateFromText = new MenuItem("Generate from Text");
        MenuItem generateFromCode = new MenuItem("Generate from Code");
        MenuItem uploadImage = new MenuItem("Upload Image");
        MenuItem export = new MenuItem("Export");
        diagramMenu.getItems().addAll(generateFromText, generateFromCode, uploadImage, export);
        Menu aiMenu = new Menu("AI");
        MenuItem evaluateDesign = new MenuItem("Evaluate Design");
        MenuItem consultAI = new MenuItem("Consult AI");
        MenuItem generateSuggestions = new MenuItem("Generate Suggestions");
        aiMenu.getItems().addAll(evaluateDesign, consultAI, generateSuggestions);
        Menu helpMenu = new Menu("Help");
        menuBar.getMenus().addAll(fileMenu, diagramMenu, aiMenu, helpMenu);

        ToolBar toolBar = new ToolBar();
        toolBar.getItems().addAll(new Label("Quick Actions"));

        TabPane tabPane = new TabPane();
        this.workspaceTabs = tabPane;
        this.dashboardPanel = new ProjectDashboardPanel();
        dashboardPanel.setProjectCtrl(projectCtrl);
        dashboardPanel.setOpenProjectHandler(this::openProjectWorkspace);
        dashboardPanel.refreshProjects();
        Tab dashboard = new Tab("Dashboard", dashboardPanel);
        dashboard.setClosable(false);
        tabPane.getTabs().add(dashboard);

        newProject.setOnAction(event -> createProject());
        openProject.setOnAction(event -> openProject());
        exit.setOnAction(event -> primaryStage.close());
        generateFromText.setOnAction(event -> handleGenerateFromText(primaryStage));
        generateFromCode.setOnAction(event -> handleGenerateFromCode(primaryStage));
        uploadImage.setOnAction(event -> handleUploadImage(primaryStage));
        export.setOnAction(event -> exportActiveDiagram(primaryStage));
        evaluateDesign.setOnAction(event -> showEvaluationPanel());
        consultAI.setOnAction(event -> showChatPanel());
        generateSuggestions.setOnAction(event -> showEvaluationPanel(true));

        HBox statusBar = new HBox(24);
        statusBar.getChildren().addAll(
                new Label("DB: Connected"),
                new Label("AI: Ready"),
                new Label("Last saved: -")
        );

        BorderPane topPane = new BorderPane();
        topPane.setTop(menuBar);
        topPane.setCenter(toolBar);

        root.setTop(topPane);
        root.setCenter(tabPane);
        root.setBottom(statusBar);

        primaryStage.setTitle("UMLytics — AI-Powered UML Design Intelligence");
        Scene scene = new Scene(root, 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/css/drawio-theme.css").toExternalForm());
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN), this::createProject);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN), this::openProject);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN), () -> exportActiveDiagram(primaryStage));
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.S && e.isControlDown() && !e.isAltDown() && !e.isMetaDown()) {
                if (e.getTarget() instanceof TextInputControl) {
                    return;
                }
                saveActiveDiagram(primaryStage);
                e.consume();
            }
        });
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void openProjectWorkspace(Project project) {
        if (project == null) {
            return;
        }
        this.activeProject = projectCtrl.retrieveProject(project.getProjectId());
        List<UMLDiagram> diagrams = diagramCtrl.listProjectDiagrams(project.getProjectId());
        if (diagrams.isEmpty()) {
            UMLDiagram empty = new UMLDiagram();
            empty.setProjectId(project.getProjectId());
            empty.setTitle(project.getName() + " Diagram");
            showDiagramEditor(empty, project.getName());
            return;
        }
        for (UMLDiagram diagram : diagrams) {
            showDiagramEditor(diagram, project.getName() + ": " + diagram.getTitle());
        }
        new Alert(Alert.AlertType.INFORMATION, "Loaded " + diagrams.size() + " diagram(s) for " + project.getName()).show();
    }

    private void handleGenerateFromText(Stage owner) {
        if (!ensureActiveProject()) {
            return;
        }
        Dialog<String> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Generate UML from Text");
        dialog.setHeaderText("Describe the system design");
        TextArea area = new TextArea();
        area.setPromptText("Enter description...");
        area.setWrapText(true);
        area.setPrefRowCount(8);
        dialog.getDialogPane().setContent(area);
        ButtonType generate = new ButtonType("Generate", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(generate, ButtonType.CANCEL);
        dialog.setResultConverter(buttonType -> buttonType == generate ? area.getText() : null);
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().isBlank()) {
            return;
        }
        try {
            UMLDiagram diagram = diagramCtrl.generateFromNL(activeProject.getProjectId(), result.get());
            showDiagramEditor(diagram, "Generated: " + diagram.getTitle());
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Generation failed: " + e.getMessage()).show();
        }
    }

    private void handleGenerateFromCode(Stage owner) {
        if (!ensureActiveProject()) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Java Source Files");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Java Files", "*.java"));
        List<File> files = chooser.showOpenMultipleDialog(owner);
        if (files == null || files.isEmpty()) {
            return;
        }
        try {
            UMLDiagram diagram = diagramCtrl.generateFromCode(activeProject.getProjectId(), files);
            showDiagramEditor(diagram, "Code Import: " + diagram.getTitle());
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Code parsing failed: " + e.getMessage()).show();
        }
    }

    private void handleUploadImage(Stage owner) {
        if (!ensureActiveProject()) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select UML Image");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(owner);
        if (file == null) {
            return;
        }
        try {
            byte[] data = Files.readAllBytes(file.toPath());
            UMLDiagram diagram = diagramCtrl.analyzeUploadedImage(activeProject.getProjectId(), data);
            showDiagramEditor(diagram, "Image Import: " + diagram.getTitle());
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Image analysis failed: " + e.getMessage()).show();
        }
    }

    /**
     * Persists the diagram in the active editor tab (Ctrl+S), with user feedback.
     */
    public void saveActiveDiagram(Stage owner) {
        DiagramEditorPanel editor = getActiveEditor();
        if (editor == null) {
            new Alert(Alert.AlertType.WARNING, "Open a diagram tab first.").show();
            return;
        }
        try {
            editor.onSaveDiagram();
            new Alert(Alert.AlertType.INFORMATION, "Diagram saved.").show();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Save failed: " + e.getMessage()).show();
        }
    }

    private void exportActiveDiagram(Stage owner) {
        DiagramEditorPanel editor = getActiveEditor();
        if (editor == null) {
            new Alert(Alert.AlertType.WARNING, "Open a diagram tab first.").show();
            return;
        }
        UMLDiagram diagram = editor.getCurrentDiagram();
        if (diagram == null) {
            new Alert(Alert.AlertType.WARNING, "No diagram to export.").show();
            return;
        }
        ChoiceDialog<ExportFormat> formatDialog = new ChoiceDialog<>(ExportFormat.PNG, FXCollections.observableArrayList(ExportFormat.values()));
        formatDialog.initOwner(owner);
        formatDialog.setTitle("Export Diagram");
        formatDialog.setHeaderText("Choose export format");
        Optional<ExportFormat> formatResult = formatDialog.showAndWait();
        if (formatResult.isEmpty()) {
            return;
        }
        ExportFormat format = formatResult.get();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Diagram");
        String ext = format.name().toLowerCase();
        chooser.setInitialFileName((diagram.getTitle() == null || diagram.getTitle().isBlank() ? "diagram" : diagram.getTitle()) + "." + ext);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(format.name() + " files", "*." + ext));
        File out = chooser.showSaveDialog(owner);
        if (out == null) {
            return;
        }
        try {
            if (diagram.getDiagramId() <= 0) {
                diagramCtrl.saveDiagram(diagram);
            }
            diagramCtrl.exportDiagram(diagram.getDiagramId(), format, out.getAbsolutePath());
            new Alert(Alert.AlertType.INFORMATION, "Exported to: " + out.getAbsolutePath()).show();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Export failed: " + e.getMessage()).show();
        }
    }

    private DiagramEditorPanel getActiveEditor() {
        Tab selected = workspaceTabs.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return null;
        }
        if (selected.getContent() instanceof DiagramEditorPanel editor) {
            return editor;
        }
        return null;
    }

    private boolean ensureActiveProject() {
        if (activeProject == null) {
            new Alert(Alert.AlertType.WARNING, "Open a project first from Dashboard.").show();
            return false;
        }
        return true;
    }
}
