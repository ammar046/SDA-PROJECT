package com.umlytics.controllers;

import com.umlytics.domain.ClassSuggestion;
import com.umlytics.domain.ChatMessage;
import com.umlytics.domain.DesignEvaluationReport;
import com.umlytics.domain.ProjectContext;
import com.umlytics.domain.UMLDiagram;
import com.umlytics.domain.UMLModel;
import com.umlytics.enums.SenderType;
import com.umlytics.exceptions.DiagramTooSimpleException;
import com.umlytics.exceptions.EmptyDiagramException;
import com.umlytics.exceptions.ValidationException;
import com.umlytics.interfaces.IAIEngine;
import com.umlytics.interfaces.IChatRepository;
import com.umlytics.interfaces.IDiagramRepository;
import com.umlytics.interfaces.IEvaluationRepository;
import com.umlytics.services.AiDiagramPayload;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// GRASP: Controller, Pure Fabrication
public class AIController {
    private final IAIEngine aiEngine;
    private final IChatRepository chatRepo;
    private final IEvaluationRepository evalRepo;
    private final IDiagramRepository diagramRepo;

    public AIController(IAIEngine aiEngine, IChatRepository chatRepo, IEvaluationRepository evalRepo,
                        IDiagramRepository diagramRepo) {
        this.aiEngine = aiEngine;
        this.chatRepo = chatRepo;
        this.evalRepo = evalRepo;
        this.diagramRepo = diagramRepo;
    }

    public DesignEvaluationReport evaluateDesign(UUID diagramId) {
        if (diagramId == null) {
            throw new DiagramTooSimpleException("No diagram selected for evaluation.");
        }
        UMLDiagram diagram = diagramRepo.findById(diagramId);
        if (diagram == null) {
            throw new DiagramTooSimpleException("Diagram not found: " + diagramId);
        }
        if (diagram.getClasses().size() < 2) {
            throw new DiagramTooSimpleException("Evaluation requires at least 2 classes.");
        }
        UMLModel model = new UMLModel();
        model.setClasses(new java.util.ArrayList<>(diagram.getClasses()));
        model.setRelationships(new java.util.ArrayList<>(diagram.getRelationships()));
        model.setRawJson(AiDiagramPayload.evaluationPayload(diagram));

        DesignEvaluationReport report = aiEngine.evaluateDesign(model);
        report.setReportId(UUID.randomUUID());
        report.setDiagramId(diagramId);
        report.setEvaluationDate(java.time.LocalDateTime.now());
        evalRepo.save(report);
        return report;
    }

    public DesignEvaluationReport evaluateDesign(int diagramId) {
        return evaluateDesign(UUID.nameUUIDFromBytes(("legacy-diagram-" + diagramId).getBytes()));
    }

    public ChatMessage submitDesignQuestion(String questionText, UUID projectId) {
        return submitDesignQuestion(questionText, projectId, null, null);
    }

    /**
     * @param diagramId when non-null, used only if {@code liveDiagram} is null — loads from repository (may be stale).
     * @param liveDiagram when non-null, the AI sees this exact canvas state (preferred).
     */
    public ChatMessage submitDesignQuestion(String questionText, UUID projectId, UUID diagramId, UMLDiagram liveDiagram) {
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
        if (liveDiagram != null) {
            context.setCurrentDiagram(liveDiagram);
        } else if (diagramId != null) {
            UMLDiagram d = diagramRepo.findById(diagramId);
            if (d != null) {
                context.setCurrentDiagram(d);
            }
        }
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

    /** @deprecated Prefer {@link #submitDesignQuestion(String, UUID, UUID, UMLDiagram)} with the live canvas. */
    public ChatMessage submitDesignQuestion(String questionText, UUID projectId, UUID diagramId) {
        return submitDesignQuestion(questionText, projectId, diagramId, null);
    }

    public ChatMessage submitDesignQuestion(String questionText, int projectId) {
        return submitDesignQuestion(questionText, UUID.nameUUIDFromBytes(("legacy-project-" + projectId).getBytes()));
    }

    public ClassSuggestion generateStructureSuggestions(UUID diagramId) {
        if (diagramId == null) {
            throw new EmptyDiagramException("No diagram selected for structure generation.");
        }
        UMLDiagram diagram = diagramRepo.findById(diagramId);
        if (diagram == null || diagram.getClasses().isEmpty()) {
            throw new EmptyDiagramException("No classes defined. Add classes to the diagram first.");
        }
        UMLModel model = new UMLModel();
        model.setClasses(new java.util.ArrayList<>(diagram.getClasses()));
        model.setRelationships(new java.util.ArrayList<>(diagram.getRelationships()));

        String response = aiEngine.generateStructure(model);
        ClassSuggestion suggestion = new ClassSuggestion();
        suggestion.setSuggestionId(UUID.randomUUID());
        suggestion.setDiagramId(diagramId);
        suggestion.setSkeletonCode(ClassSuggestion.combineSkeletonResponse(response, AiDiagramPayload.classNameSet(model)));
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
