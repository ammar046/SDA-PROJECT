package com.umlytics.controllers;

import com.umlytics.domain.ChatMessage;
import com.umlytics.domain.EvaluationReport;
import com.umlytics.domain.ProjectContext;
import com.umlytics.domain.StructureSuggestion;
import com.umlytics.domain.UMLModel;
import com.umlytics.enums.SenderType;
import com.umlytics.exceptions.DiagramTooSimpleException;
import com.umlytics.exceptions.EmptyDiagramException;
import com.umlytics.interfaces.IAIEngine;
import com.umlytics.interfaces.IChatRepository;
import com.umlytics.interfaces.IEvaluationRepository;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

// GRASP: Controller, Pure Fabrication
public class AIController {
    private final IAIEngine aiEngine;
    private final IChatRepository chatRepo;
    private final IEvaluationRepository evalRepo;

    public AIController(IAIEngine aiEngine, IChatRepository chatRepo, IEvaluationRepository evalRepo) {
        this.aiEngine = aiEngine;
        this.chatRepo = chatRepo;
        this.evalRepo = evalRepo;
    }

    public EvaluationReport evaluateDesign(int diagramId) {
        if (diagramId <= 0) {
            throw new DiagramTooSimpleException("At least one valid diagram is required.");
        }
        EvaluationReport report = aiEngine.evaluateDesign(new UMLModel());
        report.setDiagramId(diagramId);
        report.setGeneratedDate(new Date());
        evalRepo.save(report);
        return report;
    }

    public ChatMessage consultAI(String query, int projectId) {
        ChatMessage userMessage = new ChatMessage();
        userMessage.setContent(query);
        userMessage.setProjectId(projectId);
        userMessage.setSender(SenderType.USER);
        userMessage.setTimestamp(new Date());
        chatRepo.save(userMessage);

        ProjectContext context = new ProjectContext();
        context.setChatHistory(chatRepo.findByProject(projectId));
        String response = aiEngine.consultDesign(query, context);

        ChatMessage aiMessage = new ChatMessage();
        aiMessage.setContent(response);
        aiMessage.setProjectId(projectId);
        aiMessage.setSender(SenderType.AI);
        aiMessage.setTimestamp(new Date());
        chatRepo.save(aiMessage);
        return aiMessage;
    }

    public StructureSuggestion generateStructureSuggestions(int diagramId) {
        if (diagramId <= 0) {
            throw new EmptyDiagramException("No classes defined for structure generation.");
        }
        String response = aiEngine.generateStructure(new UMLModel());
        StructureSuggestion suggestion = new StructureSuggestion();
        suggestion.setDiagramId(diagramId);
        suggestion.setCodeSkeletons(new HashMap<>());
        suggestion.getSkeletons().put("Generated", response);
        return suggestion;
    }

    public List<ChatMessage> getChatHistory(int projectId) {
        return chatRepo.findByProject(projectId);
    }
}
