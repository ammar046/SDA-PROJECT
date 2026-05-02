package com.umlytics.controllers;

import com.umlytics.domain.ClassSuggestion;
import com.umlytics.domain.ChatMessage;
import com.umlytics.domain.DesignEvaluationReport;
import com.umlytics.domain.ProjectContext;
import com.umlytics.domain.UMLModel;
import com.umlytics.enums.SenderType;
import com.umlytics.exceptions.DiagramTooSimpleException;
import com.umlytics.exceptions.EmptyDiagramException;
import com.umlytics.exceptions.ValidationException;
import com.umlytics.interfaces.IAIEngine;
import com.umlytics.interfaces.IChatRepository;
import com.umlytics.interfaces.IEvaluationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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

    public DesignEvaluationReport evaluateDesign(UUID diagramId) {
        if (diagramId == null) {
            throw new DiagramTooSimpleException("Valid diagram ID required.");
        }
        DesignEvaluationReport report = aiEngine.evaluateDesign(new UMLModel());
        report.setReportId(UUID.randomUUID());
        report.setDiagramId(diagramId);
        report.setEvaluationDate(LocalDateTime.now());
        evalRepo.save(report);
        return report;
    }

    public DesignEvaluationReport evaluateDesign(int diagramId) {
        return evaluateDesign(UUID.nameUUIDFromBytes(("legacy-diagram-" + diagramId).getBytes()));
    }

    public ChatMessage submitDesignQuestion(String questionText, UUID projectId) {
        if (projectId == null) {
            throw new ValidationException("Open a project first.");
        }
        if (questionText == null || questionText.isBlank()) {
            throw new ValidationException("Query cannot be empty.");
        }
        ChatMessage userMessage = new ChatMessage();
        userMessage.setMessageId(UUID.randomUUID());
        userMessage.setContent(questionText.trim());
        userMessage.setProjectId(projectId);
        userMessage.setSender(SenderType.USER);
        userMessage.setTimestamp(LocalDateTime.now());
        chatRepo.save(userMessage);

        ProjectContext context = new ProjectContext();
        context.setChatHistory(chatRepo.findByProject(projectId));
        String response = aiEngine.consultDesign(questionText.trim(), context);

        ChatMessage aiMessage = new ChatMessage();
        aiMessage.setMessageId(UUID.randomUUID());
        aiMessage.setContent(response);
        aiMessage.setProjectId(projectId);
        aiMessage.setSender(SenderType.AI);
        aiMessage.setTimestamp(LocalDateTime.now());
        chatRepo.save(aiMessage);
        return aiMessage;
    }

    public ChatMessage submitDesignQuestion(String questionText, int projectId) {
        return submitDesignQuestion(questionText, UUID.nameUUIDFromBytes(("legacy-project-" + projectId).getBytes()));
    }

    public ClassSuggestion generateStructureSuggestions(UUID diagramId) {
        if (diagramId == null) {
            throw new EmptyDiagramException("No diagram selected.");
        }
        String response = aiEngine.generateStructure(new UMLModel());
        ClassSuggestion suggestion = new ClassSuggestion();
        suggestion.setSuggestionId(UUID.randomUUID());
        suggestion.setDiagramId(diagramId);
        suggestion.setSkeletonCode(response);
        suggestion.setAccepted(false);
        return suggestion;
    }

    public ClassSuggestion generateStructureSuggestions(int diagramId) {
        return generateStructureSuggestions(UUID.nameUUIDFromBytes(("legacy-diagram-" + diagramId).getBytes()));
    }

    public ClassSuggestion generateClassSuggestions(UUID diagramId) {
        return generateStructureSuggestions(diagramId);
    }

    public ClassSuggestion generateClassSuggestions(int diagramId) {
        return generateStructureSuggestions(diagramId);
    }

    public List<ChatMessage> getChatHistory(UUID projectId) {
        return chatRepo.findByProject(projectId);
    }

    public List<ChatMessage> getChatHistory(int projectId) {
        return getChatHistory(UUID.nameUUIDFromBytes(("legacy-project-" + projectId).getBytes()));
    }
}
