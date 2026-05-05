package com.umlytics.ui;

import com.umlytics.controllers.*;
import com.umlytics.db.DatabaseManager;
import com.umlytics.domain.Project;
import com.umlytics.domain.UMLDiagram;
import com.umlytics.exceptions.ValidationException;
import com.umlytics.interfaces.*;
import com.umlytics.repository.*;
import com.umlytics.services.*;
import com.umlytics.ui.panels.*;
import javafx.animation.*;
import javafx.application.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

// GRASP: Facade — single access point for all controllers and UI panels
// GoF:   Facade
public class MainWindow extends Application {

    // Controllers
    private ProjectController projectCtrl;
    private DiagramController diagramCtrl;
    private AIController      aiCtrl;

    // Panels
    private ToolbarPanel          toolbarPanel;
    private ShapePalettePanel     shapePanel;
    private ProjectExplorerPanel  explorerPanel;
    private DiagramEditorPanel    editorPanel;
    private AIChatPanel           chatPanel;

    /** Project used for new diagrams, code import, and status bar when set. */
    private UUID activeProjectId;

    // Status bar
    private Label lbProject, lbDiagram, lbNodes, lbZoom, lbSaved;

    // Toast
    private static StackPane toastOverlay;

    private static volatile MainWindow instance;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Refreshes the project explorer after diagrams are created or saved from panels that do not hold a MainWindow reference. */
    public static void notifyDiagramChanged() {
        MainWindow w = instance;
        if (w != null) {
            Platform.runLater(w::refreshProjectExplorer);
        }
    }

    @Override
    public void start(Stage stage) {
        instance = this;
        // ── Wire persistence ──────────────────────────────────────────────────
        DatabaseManager.getInstance().initialize();
        IProjectRepository    projRepo  = new ProjectRepositoryImpl();
        IDiagramRepository    diagRepo  = new DiagramRepositoryImpl();
        IChatRepository       chatRepo  = new ChatRepositoryImpl();
        IEvaluationRepository evalRepo  = new DesignEvaluationRepositoryImpl();
        ICodeParser           parser    = new JavaCodeParser();
        IExportService        exporter  = new DiagramExportService();
        IAIEngine             ai        = new LLMAPIEngine();
        ISpeechToTextService  stt       = new SpeechToTextServiceImpl();

        projectCtrl = new ProjectController(projRepo, diagRepo, chatRepo, evalRepo);
        diagramCtrl = new DiagramController(diagRepo, ai, parser, exporter);
        aiCtrl      = new AIController(ai, chatRepo, evalRepo, diagRepo);

        // Ensure default project exists
        if (projectCtrl.getAllProjects().isEmpty())
            projectCtrl.createProject("My Project", "Default workspace");

        // ── Build panels ──────────────────────────────────────────────────────
        editorPanel  = new DiagramEditorPanel(diagramCtrl);
        shapePanel   = new ShapePalettePanel();
        explorerPanel = new ProjectExplorerPanel(this);
        chatPanel    = new AIChatPanel(this, stt);
        toolbarPanel = new ToolbarPanel(this);

        // ── Left sidebar: Shapes (top) + Explorer (bottom) ────────────────────
        SplitPane leftBar = new SplitPane();
        leftBar.setOrientation(Orientation.VERTICAL);
        leftBar.getItems().addAll(shapePanel, explorerPanel);
        leftBar.setDividerPositions(0.55);
        leftBar.setPrefWidth(225);
        leftBar.setMinWidth(170);

        // ── Main 3-col split ─────────────────────────────────────────────────
        SplitPane mainSplit = new SplitPane(leftBar, editorPanel, chatPanel);
        mainSplit.setDividerPositions(0.17, 0.80);
        SplitPane.setResizableWithParent(leftBar,   false);
        SplitPane.setResizableWithParent(chatPanel, false);

        // ── Root ──────────────────────────────────────────────────────────────
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-bg");
        root.setTop(toolbarPanel);
        root.setCenter(mainSplit);
        root.setBottom(buildStatusBar());

        // ── Toast overlay ─────────────────────────────────────────────────────
        toastOverlay = new StackPane();
        toastOverlay.setPickOnBounds(false);
        toastOverlay.setAlignment(Pos.BOTTOM_CENTER);
        StackPane.setMargin(toastOverlay, new Insets(0, 0, 42, 0));

        StackPane sceneRoot = new StackPane(root, toastOverlay);

        Scene scene = new Scene(sceneRoot, 1440, 860);
        try {
            scene.getStylesheets().add(
                getClass().getResource("/css/dark-theme.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("[MainWindow] dark-theme.css not found: " + e.getMessage());
        }
        registerShortcuts(scene);

        stage.setTitle("UMLytics — AI-Powered Design Intelligence");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();

        refreshProjectExplorer();
        updateStatusBar();
    }

    // ── Status bar ────────────────────────────────────────────────────────────
    private HBox buildStatusBar() {
        lbProject = new Label("Project: —");
        lbDiagram = new Label("Diagram: —");
        lbNodes   = new Label("Nodes: 0");
        lbZoom    = new Label("Zoom: 100%");
        lbSaved   = new Label("Not saved yet");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(10, lbProject, sep(), lbDiagram, sep(), lbNodes,
                            sep(), lbZoom, spacer, sep(), lbSaved);
        bar.getStyleClass().add("status-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(3, 12, 3, 12));
        return bar;
    }

    private Label sep() {
        Label l = new Label("|");
        l.setStyle("-fx-text-fill: #333344; -fx-font-size: 11px;");
        return l;
    }

    // ── Toast ─────────────────────────────────────────────────────────────────
    /** Toast + full stack trace on stderr (avoids multi-line wall-of-text in the UI). */
    public static void showErrorToast(Throwable ex) {
        if (ex == null) {
            showToast("Error");
            return;
        }
        String m = ex.getMessage();
        if (m == null || m.isBlank()) {
            m = ex.getClass().getSimpleName();
        }
        m = m.replace('\r', ' ').replace('\n', ' ').replaceAll("\\s+", " ").trim();
        if (m.length() > 240) {
            m = m.substring(0, 240) + "…";
        }
        System.err.println("[UMLytics] " + ex.getClass().getName() + ": " + ex.getMessage());
        ex.printStackTrace(System.err);
        showToast("Error: " + m);
    }

    public static void showToast(String msg) {
        if (toastOverlay == null) return;
        Platform.runLater(() -> {
            Label toast = new Label(msg);
            toast.getStyleClass().add("toast-label");
            toastOverlay.getChildren().add(toast);
            FadeTransition in  = new FadeTransition(Duration.millis(250), toast);
            in.setFromValue(0); in.setToValue(1); in.play();
            FadeTransition out = new FadeTransition(Duration.millis(350), toast);
            out.setFromValue(1); out.setToValue(0);
            out.setDelay(Duration.seconds(2.2));
            out.setOnFinished(e -> toastOverlay.getChildren().remove(toast));
            in.setOnFinished(e -> out.play());
        });
    }

    // ── Shortcuts ─────────────────────────────────────────────────────────────
    private void registerShortcuts(Scene scene) {
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN), this::promptNewProject);
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN),
            () -> {
                editorPanel.saveCurrentDiagram();
                refreshProjectExplorer();
                onSaved();
                showToast("Saved ✓");
            });
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN), editorPanel::undo);
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN),
            editorPanel::redo);
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.G, KeyCombination.CONTROL_DOWN),
            editorPanel::showGenerateFromTextDialog);
    }

    // ── Public helpers ────────────────────────────────────────────────────────
    public void promptNewProject() {
        TextInputDialog dlg = new TextInputDialog("My Project");
        dlg.setTitle("New Project"); dlg.setHeaderText("Enter project name:");
        dlg.showAndWait().ifPresent(n -> {
            if (!n.isBlank()) {
                try {
                    Project created = projectCtrl.createProject(n.trim(), "");
                    setActiveProjectId(created.getProjectId());
                    refreshProjectExplorer();
                    showToast("Created: " + n.trim());
                } catch (ValidationException ex) {
                    showToast(ex.getMessage());
                }
            }
        });
    }

    public void openDiagram(UMLDiagram d) {
        if (d != null && d.getProjectId() != null) {
            setActiveProjectId(d.getProjectId());
        }
        editorPanel.loadDiagram(d);
        updateStatusBar();
    }

    /** Starts a blank diagram scoped to the given project (saving will persist under that project). */
    public void newDiagramForProject(UUID projectId) {
        if (projectId == null) {
            return;
        }
        setActiveProjectId(projectId);
        editorPanel.newBlankDiagram();
        UMLDiagram cur = editorPanel.getCurrentDiagram();
        if (cur != null) {
            cur.setProjectId(projectId);
        }
        updateStatusBar();
        showToast("New diagram — press Save when ready");
    }

    public void refreshProjectExplorer() {
        List<Project> all = projectCtrl.getAllProjects();
        if (activeProjectId == null && !all.isEmpty()) {
            activeProjectId = all.get(0).getProjectId();
        } else if (activeProjectId != null && projectCtrl.findProject(activeProjectId) == null) {
            activeProjectId = all.isEmpty() ? null : all.get(0).getProjectId();
        }
        editorPanel.setDefaultProjectIdForNewDiagrams(activeProjectId);
        explorerPanel.loadWorkspace(all);
        updateStatusBar();
    }

    public void setActiveProjectId(UUID projectId) {
        this.activeProjectId = projectId;
        editorPanel.setDefaultProjectIdForNewDiagrams(projectId);
    }

    public UUID getActiveProjectId() {
        return activeProjectId;
    }

    public void updateStatusBar() {
        Platform.runLater(() -> {
            String projectLabel = "—";
            if (activeProjectId != null) {
                Project p = projectCtrl.findProject(activeProjectId);
                if (p != null) {
                    projectLabel = p.getName();
                }
            }
            lbProject.setText("Project: " + projectLabel);
            lbDiagram.setText("Diagram: " + editorPanel.getCurrentDiagramName());
            lbNodes.setText("Nodes: " + editorPanel.getNodeCount());
            lbZoom.setText("Zoom: " + editorPanel.getZoomPercent() + "%");
        });
    }

    public void onZoomChanged(int pct) {
        Platform.runLater(() -> lbZoom.setText("Zoom: " + pct + "%"));
    }

    public void onSaved() {
        Platform.runLater(() -> lbSaved.setText("Saved " + LocalTime.now().format(TIME_FMT)));
    }

    // ── Accessors ─────────────────────────────────────────────────────────────
    public ProjectController  getProjectController() { return projectCtrl; }
    public DiagramController  getDiagramController() { return diagramCtrl; }
    public AIController       getAIController()      { return aiCtrl; }
    public DiagramEditorPanel getEditorPanel()       { return editorPanel; }
    public AIChatPanel        getChatPanel()         { return chatPanel; }
}
