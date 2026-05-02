package com.umlytics;

import com.umlytics.controllers.AIController;
import com.umlytics.controllers.DiagramController;
import com.umlytics.controllers.ProjectController;
import com.umlytics.db.DatabaseManager;
import com.umlytics.interfaces.IAIEngine;
import com.umlytics.interfaces.IChatRepository;
import com.umlytics.interfaces.ICodeParser;
import com.umlytics.interfaces.IDiagramRepository;
import com.umlytics.interfaces.IEvaluationRepository;
import com.umlytics.interfaces.IExportService;
import com.umlytics.interfaces.IProjectRepository;
import com.umlytics.interfaces.ISpeechToTextService;
import com.umlytics.repository.ChatRepositoryImpl;
import com.umlytics.repository.DiagramRepositoryImpl;
import com.umlytics.repository.DesignEvaluationRepositoryImpl;
import com.umlytics.repository.ProjectRepositoryImpl;
import com.umlytics.services.DiagramExportService;
import com.umlytics.services.JavaCodeParser;
import com.umlytics.services.LLMAPIEngine;
import com.umlytics.services.SpeechToTextServiceImpl;
import com.umlytics.ui.MainWindow;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        DatabaseManager.getInstance().initialize();

        IProjectRepository projectRepo = new ProjectRepositoryImpl();
        IDiagramRepository diagramRepo = new DiagramRepositoryImpl();
        IChatRepository chatRepo = new ChatRepositoryImpl();
        IEvaluationRepository evalRepo = new DesignEvaluationRepositoryImpl();
        IAIEngine aiEngine = new LLMAPIEngine();
        ICodeParser codeParser = new JavaCodeParser();
        IExportService exportSvc = new DiagramExportService();
        ISpeechToTextService speechSvc = new SpeechToTextServiceImpl();

        ProjectController projectCtrl = new ProjectController(projectRepo, diagramRepo);
        DiagramController diagramCtrl = new DiagramController(diagramRepo, aiEngine, codeParser, exportSvc);
        AIController aiCtrl = new AIController(aiEngine, chatRepo, evalRepo);

        MainWindow mainWindow = new MainWindow(projectCtrl, diagramCtrl, aiCtrl, speechSvc);
        mainWindow.start(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
