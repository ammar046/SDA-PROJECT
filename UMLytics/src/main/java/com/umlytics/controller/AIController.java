package com.umlytics.controller;

import com.umlytics.domain.ChatMessage;
import com.umlytics.domain.EvaluationReport;
import com.umlytics.domain.valueobjects.ProjectContext;
import com.umlytics.domain.valueobjects.UMLModel;
import com.umlytics.domain.valueobjects.StructureSuggestion;
import com.umlytics.enums.SenderType;
import com.umlytics.interfaces.*;
import com.umlytics.persistence.ChatRepositoryImpl;
import com.umlytics.persistence.EvaluationRepositoryImpl;
import com.umlytics.service.ai.LLMAPIEngine;
import com.umlytics.service.parser.JavaCodeParser;
import com.umlytics.service.speech.SpeechToTextServiceImpl;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * System operations for AI generation and Code Parsing.
 * GRASP: Controller, Low Coupling (interacts with services)
 */
public class AIController {

    private final IAIEngine            aiEngine;
    private final ICodeParser          codeParser;
    private final ISpeechToTextService speechService;
    private final IChatRepository      chatRepo;
    private final IEvaluationRepository evaluationRepo;

    public AIController() {
        this.aiEngine       = new LLMAPIEngine();
        this.codeParser     = new JavaCodeParser();
        this.speechService  = new SpeechToTextServiceImpl();
        this.chatRepo       = new ChatRepositoryImpl();
        this.evaluationRepo = new EvaluationRepositoryImpl();
    }

    // ---- UC1 & UC4: Generate via Prompt or Speech ----
    public UMLModel generateFromPrompt(String prompt) {
        return aiEngine.generateFromText(prompt);
    }
    
    public void startVoiceRecording() {
        speechService.startRecording();
    }
    
    public String stopVoiceRecording() {
        return speechService.stopRecording();
    }

    // ---- UC2: Chat Assistant ----
    public String sendChatMessage(int projectId, String msgText, ProjectContext ctx) {
        // Save user message
        ChatMessage userMsg = new ChatMessage(0, msgText, SenderType.USER, new Date(), projectId);
        chatRepo.save(userMsg);
        
        // Refresh context history
        ctx.setChatHistory(chatRepo.findByProject(projectId));
        
        // Consult AI
        String aiResponse = aiEngine.consultDesign(msgText, ctx);
        
        // Save AI message
        ChatMessage aiMsg = new ChatMessage(0, aiResponse, SenderType.AI, new Date(), projectId);
        chatRepo.save(aiMsg);
        
        return aiResponse;
    }

    public List<ChatMessage> getChatHistory(int projectId) {
        return chatRepo.findByProject(projectId);
    }

    // ---- UC3: Parse Code ----
    public UMLModel generateFromCode(List<File> files) {
        return codeParser.parse(files);
    }

    // ---- UC8: Image Analysis ----
    public UMLModel generateFromImage(byte[] imageBytes) {
        return aiEngine.analyzeImage(imageBytes);
    }

    // ---- UC9: Evaluate Design ----
    public EvaluationReport evaluateDiagram(UMLModel model, int diagramId, int projectId) {
        EvaluationReport report = aiEngine.evaluateDesign(model);
        report.setDiagramId(diagramId);
        report.setProjectId(projectId);
        evaluationRepo.save(report);
        return report;
    }

    public EvaluationReport getEvaluation(int diagramId) {
        return evaluationRepo.findByDiagram(diagramId);
    }

    // ---- UC12: Generate Code Skeletons ----
    public StructureSuggestion generateCodeSkeletons(UMLModel model, int diagramId) {
        String codeSkeletonsText = aiEngine.generateStructure(model);
        
        Map<String, String> map = new HashMap<>();
        map.put("GeneratedCode", codeSkeletonsText); // Normally parsed by regex for files

        StructureSuggestion ss = new StructureSuggestion(0, diagramId, map, new Date());
        return ss;
    }

    /** True when a microphone is present and audio recording is supported. */
    public boolean isSpeechAvailable() {
        return speechService.isAvailable();
    }
}

