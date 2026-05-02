package com.umlytics.ui;

import com.umlytics.controllers.AIController;
import com.umlytics.controllers.DiagramController;
import com.umlytics.controllers.ProjectController;
import com.umlytics.domain.Project;
import com.umlytics.domain.UMLDiagram;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.util.List;

// GRASP: Controller (Facade / Boundary)
public class MainWindow extends Application {
    private ProjectController projectCtrl;
    private DiagramController diagramCtrl;
    private AIController aiCtrl;
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
        Tab tab = new Tab("AI Chat", chatPanel);
        workspaceTabs.getTabs().add(tab);
        workspaceTabs.getSelectionModel().select(tab);
    }

    public void showEvaluationPanel() {
        EvaluationPanel evaluationPanel = new EvaluationPanel();
        evaluationPanel.setAiCtrl(aiCtrl);
        Tab tab = new Tab("Evaluation", evaluationPanel);
        workspaceTabs.getTabs().add(tab);
        workspaceTabs.getSelectionModel().select(tab);
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
        generateFromText.setOnAction(event -> showDiagramEditor());
        generateFromCode.setOnAction(event -> showDiagramEditor());
        uploadImage.setOnAction(event -> showDiagramEditor());
        export.setOnAction(event -> showDiagramEditor());
        evaluateDesign.setOnAction(event -> showEvaluationPanel());
        consultAI.setOnAction(event -> showChatPanel());
        generateSuggestions.setOnAction(event -> showEvaluationPanel());

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
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN), this::showDiagramEditor);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN), this::showDiagramEditor);
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
}
