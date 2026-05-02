package com.umlytics.controllers;

import com.umlytics.domain.ChatMessage;
import com.umlytics.domain.DesignEvaluationReport;
import com.umlytics.domain.ProjectContext;
import com.umlytics.domain.UMLModel;
import com.umlytics.exceptions.DiagramTooSimpleException;
import com.umlytics.exceptions.ValidationException;
import com.umlytics.interfaces.IAIEngine;
import com.umlytics.interfaces.IChatRepository;
import com.umlytics.interfaces.IEvaluationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AIControllerTest {
    private InMemoryChatRepo chatRepo;
    private InMemoryEvalRepo evalRepo;
    private AIController controller;

    @BeforeEach
    void setUp() {
        chatRepo = new InMemoryChatRepo();
        evalRepo = new InMemoryEvalRepo();
        controller = new AIController(new StubAI(), chatRepo, evalRepo);
    }

    @Test
    void evaluateDesignRejectsInvalidDiagram() {
        assertThrows(DiagramTooSimpleException.class, () -> controller.evaluateDesign((UUID) null));
    }

    @Test
    void submitDesignQuestionStoresBothMessages() {
        UUID projectId = UUID.randomUUID();
        ChatMessage response = controller.submitDesignQuestion("How to improve cohesion?", projectId);
        assertEquals(2, chatRepo.messages.size());
        assertEquals(projectId, response.getProjectId());
    }

    @Test
    void submitDesignQuestionRejectsInvalidInput() {
        UUID projectId = UUID.randomUUID();
        assertThrows(ValidationException.class, () -> controller.submitDesignQuestion("", projectId));
        assertThrows(ValidationException.class, () -> controller.submitDesignQuestion("hello", (UUID) null));
    }

    private static class StubAI implements IAIEngine {
        @Override
        public UMLModel generateFromText(String desc) {
            return new UMLModel();
        }

        @Override
        public DesignEvaluationReport evaluateDesign(UMLModel m) {
            DesignEvaluationReport report = new DesignEvaluationReport();
            report.setSuggestions(List.of("Improve layering"));
            return report;
        }

        @Override
        public String consultDesign(String q, ProjectContext ctx) {
            return "Mock response";
        }

        @Override
        public String generateStructure(UMLModel m) {
            return "{}";
        }

        @Override
        public UMLModel analyzeImage(byte[] data) {
            return new UMLModel();
        }
    }

    private static class InMemoryChatRepo implements IChatRepository {
        private final List<ChatMessage> messages = new ArrayList<>();

        @Override
        public void save(ChatMessage m) {
            messages.add(m);
        }

        @Override
        public List<ChatMessage> findByProject(UUID pid) {
            return messages.stream().filter(m -> pid.equals(m.getProjectId())).toList();
        }

        @Override
        public void delete(UUID id) {
        }
    }

    private static class InMemoryEvalRepo implements IEvaluationRepository {
        @Override
        public void save(DesignEvaluationReport r) {
        }

        @Override
        public List<DesignEvaluationReport> findByProject(UUID pid) {
            return List.of();
        }

        @Override
        public DesignEvaluationReport findByDiagram(UUID did) {
            return null;
        }

        @Override
        public void delete(UUID id) {
        }
    }
}
