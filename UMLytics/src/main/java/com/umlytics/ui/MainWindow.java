package com.umlytics.ui;

import com.umlytics.controller.AIController;
import com.umlytics.controller.DiagramController;
import com.umlytics.controller.ProjectController;
import com.umlytics.domain.Project;
import com.umlytics.domain.UMLDiagram;
import com.umlytics.ui.canvas.MainCanvas;
import com.umlytics.ui.panels.AIChatPanel;
import com.umlytics.ui.panels.ProjectExplorerPanel;
import com.umlytics.ui.panels.ShapePalettePanel;
import com.umlytics.ui.panels.ToolbarPanel;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Facade that integrates all UI panels with Business Logic Controllers.
 * GoF: Facade — centralises access to ProjectController, DiagramController,
 *      AIController for all UI panels.
 * GRASP: Controller (Facade / Boundary)
 */
public class MainWindow {

    private final Stage primaryStage;

    // Subsystem Controllers
    private final ProjectController projectCtrl;
    private final DiagramController diagramCtrl;
    private final AIController      aiCtrl;

    // UI Panels
    private ProjectExplorerPanel projectPanel;
    private MainCanvas           mainCanvas;
    private AIChatPanel          aiChatPanel;
    private ToolbarPanel         toolbarPanel;
    private ShapePalettePanel    shapePalettePanel;

    // Status bar labels
    private Label projectLabel;
    private Label diagramLabel;
    private Label nodeCountLabel;
    private Label zoomLabel;
    private Label lastSavedLabel;

    // Toast overlay
    private static StackPane toastOverlay;

    // Root layout (for toast positioning)
    private static StackPane rootStack;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public MainWindow(Stage stage) {
        this.primaryStage = stage;
        this.projectCtrl  = new ProjectController();
        this.diagramCtrl  = new DiagramController();
        this.aiCtrl       = new AIController();
    }

    public void initialize() {
        // Load / create default project
        if (projectCtrl.getAllProjects().isEmpty()) {
            projectCtrl.createProject("Default Project", "Auto-generated project");
        }
        Project initialProject = projectCtrl.getAllProjects().get(0);
        projectCtrl.openProject(initialProject.getProjectId());

        // Build panels
        toolbarPanel      = new ToolbarPanel(this);
        projectPanel      = new ProjectExplorerPanel(this);
        shapePalettePanel = new ShapePalettePanel();
        mainCanvas        = new MainCanvas(this);
        aiChatPanel       = new AIChatPanel(this);

        // Register canvas as diagram change observer
        diagramCtrl.addListener(mainCanvas);

        // Left sidebar: palette (top) + explorer (bottom)
        SplitPane leftSidebar = new SplitPane();
        leftSidebar.setOrientation(javafx.geometry.Orientation.VERTICAL);
        leftSidebar.getItems().addAll(shapePalettePanel, projectPanel);
        leftSidebar.setDividerPositions(0.45);
        leftSidebar.setPrefWidth(210);
        leftSidebar.setMinWidth(160);

        VBox leftOuter = new VBox(leftSidebar);
        VBox.setVgrow(leftSidebar, Priority.ALWAYS);

        // Horizontal split: left | canvas | chat
        SplitPane hSplit = new SplitPane();
        hSplit.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
        hSplit.getItems().addAll(leftOuter, mainCanvas, aiChatPanel);
        hSplit.setDividerPositions(0.20, 0.77);

        // Status bar
        HBox statusBar = buildStatusBar();

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-bg");
        root.setTop(toolbarPanel);
        root.setCenter(hSplit);
        root.setBottom(statusBar);

        // Toast overlay wraps the entire scene
        toastOverlay = new StackPane();
        toastOverlay.setPickOnBounds(false);
        toastOverlay.setAlignment(Pos.BOTTOM_CENTER);
        StackPane.setMargin(toastOverlay, new Insets(0, 0, 48, 0));

        rootStack = new StackPane(root, toastOverlay);

        Scene scene = new Scene(rootStack, 1440, 860);
        try {
            scene.getStylesheets().add(
                    getClass().getResource("/css/dark-theme.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("[MainWindow] CSS not found: " + e.getMessage());
        }

        // Keyboard shortcuts
        registerShortcuts(scene);

        primaryStage.setTitle("UMLytics — AI-Powered Design Intelligence");
        primaryStage.setScene(scene);
        primaryStage.show();

        refreshProjectExplorer();
        updateStatusBar();
    }

    // =====================================================================
    // Status bar
    // =====================================================================

    private HBox buildStatusBar() {
        projectLabel   = new Label("Project: —");
        diagramLabel   = new Label("Diagram: —");
        nodeCountLabel = new Label("Nodes: 0");
        zoomLabel      = new Label("Zoom: 100%");
        lastSavedLabel = new Label("Not saved yet");

        // Separator label helper
        Label s1 = sep(), s2 = sep(), s3 = sep(), s4 = sep();

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(8,
                projectLabel, s1, diagramLabel, s2, nodeCountLabel, s3, zoomLabel,
                spacer, s4, lastSavedLabel);
        bar.getStyleClass().add("status-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(4, 12, 4, 12));

        return bar;
    }

    private Label sep() {
        Label l = new Label("|");
        l.setStyle("-fx-text-fill: #444444; -fx-font-size: 11px;");
        return l;
    }

    public void updateStatusBar() {
        Platform.runLater(() -> {
            Project p = projectCtrl.getCurrentProject();
            UMLDiagram d = diagramCtrl.getCurrentDiagram();
            projectLabel.setText("Project: " + (p != null ? p.getName() : "—"));
            diagramLabel.setText("Diagram: " + (d != null ? d.getTitle() : "—"));
            nodeCountLabel.setText("Nodes: " + (d != null ? d.getClasses().size() : 0));
            if (mainCanvas != null) {
                int pct = (int) Math.round(mainCanvas.getZoomLevel() * 100);
                zoomLabel.setText("Zoom: " + pct + "%");
            }
        });
    }

    /** Called by canvas when zoom changes */
    public void onZoomChanged(double zoom) {
        Platform.runLater(() -> zoomLabel.setText("Zoom: " + (int) Math.round(zoom * 100) + "%"));
    }

    /** Called by canvas/toolbar when diagram is rendered */
    public void onCanvasUpdated(UMLDiagram d) {
        Platform.runLater(() -> {
            diagramLabel.setText("Diagram: " + (d != null ? d.getTitle() : "—"));
            nodeCountLabel.setText("Nodes: " + (d != null ? d.getClasses().size() : 0));
        });
    }

    /** Called by toolbar Save button */
    public void onSaved() {
        Platform.runLater(() ->
            lastSavedLabel.setText("Saved " + LocalTime.now().format(TIME_FMT)));
    }

    // =====================================================================
    // Toast notifications (static — accessible from anywhere)
    // =====================================================================

    public static void showToast(String message) {
        if (toastOverlay == null) return;
        Platform.runLater(() -> {
            Label toast = new Label(message);
            toast.getStyleClass().add("toast-label");
            toastOverlay.getChildren().add(toast);
            StackPane.setAlignment(toast, Pos.BOTTOM_CENTER);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), toast);
            fadeIn.setFromValue(0); fadeIn.setToValue(1);

            FadeTransition fadeOut = new FadeTransition(Duration.millis(400), toast);
            fadeOut.setFromValue(1); fadeOut.setToValue(0);
            fadeOut.setDelay(Duration.seconds(2));
            fadeOut.setOnFinished(e -> toastOverlay.getChildren().remove(toast));

            fadeIn.play();
            fadeIn.setOnFinished(e -> fadeOut.play());
        });
    }

    // =====================================================================
    // Keyboard shortcuts
    // =====================================================================

    private void registerShortcuts(Scene scene) {
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN),
            () -> showNewProjectDialog());

        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN),
            () -> {
                diagramCtrl.saveCurrentDiagram();
                showToast("Saved ✓");
                onSaved();
            });

        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN),
            () -> mainCanvas.requestFocus());   // canvas handles Ctrl+Z itself

        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN,
                    KeyCombination.SHIFT_DOWN),
            () -> new com.umlytics.ui.dialogs.ExportDialog(this).showAndWait());

        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.A, KeyCombination.CONTROL_DOWN),
            () -> { mainCanvas.requestFocus(); });  // canvas handles Ctrl+A itself
    }

    private void showNewProjectDialog() {
        TextInputDialog dlg = new TextInputDialog("My Project");
        dlg.setTitle("New Project");
        dlg.setHeaderText("Enter project name:");
        dlg.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                projectCtrl.createProject(name.trim(), "");
                refreshProjectExplorer();
                showToast("Project \"" + name.trim() + "\" created");
            }
        });
    }

    // =====================================================================
    // Refresh
    // =====================================================================

    public void refreshProjectExplorer() {
        if (projectCtrl.getCurrentProject() != null) {
            projectPanel.loadProject(
                    projectCtrl.getCurrentProject(),
                    diagramCtrl.getProjectDiagrams(projectCtrl.getCurrentProject().getProjectId()));
        }
        updateStatusBar();
    }

    public void showMessage(String title, String content, Alert.AlertType type) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setContentText(content);
        a.show();
    }

    // =====================================================================
    // Accessors
    // =====================================================================

    public ProjectController   getProjectController()  { return projectCtrl; }
    public DiagramController   getDiagramController()  { return diagramCtrl; }
    public AIController        getAIController()       { return aiCtrl; }
    public MainCanvas          getMainCanvas()         { return mainCanvas; }
    public AIChatPanel         getAiChatPanel()        { return aiChatPanel; }
    public ProjectExplorerPanel getProjectPanel()      { return projectPanel; }
}
