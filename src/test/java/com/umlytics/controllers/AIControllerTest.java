package com.umlytics.controllers;

import com.umlytics.domain.ChatMessage;
import com.umlytics.domain.EvaluationReport;
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
        assertThrows(DiagramTooSimpleException.class, () -> controller.evaluateDesign(0));
    }

    @Test
    void consultAIStoresBothMessages() {
        ChatMessage response = controller.consultAI("How to improve cohesion?", 42);
        assertEquals(2, chatRepo.messages.size());
        assertEquals(42, response.getProjectId());
    }

    @Test
    void consultAIRejectsInvalidInput() {
        assertThrows(ValidationException.class, () -> controller.consultAI("", 42));
        assertThrows(ValidationException.class, () -> controller.consultAI("hello", 0));
    }

    private static class StubAI implements IAIEngine {
        @Override
        public UMLModel generateFromText(String desc) {
            return new UMLModel();
        }

        @Override
        public EvaluationReport evaluateDesign(UMLModel m) {
            EvaluationReport report = new EvaluationReport();
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
        public List<ChatMessage> findByProject(int pid) {
            return messages.stream().filter(m -> m.getProjectId() == pid).toList();
        }

        @Override
        public void delete(int id) {
        }
    }

    private static class InMemoryEvalRepo implements IEvaluationRepository {
        @Override
        public void save(EvaluationReport r) {
        }

        @Override
        public List<EvaluationReport> findByProject(int pid) {
            return List.of();
        }

        @Override
        public EvaluationReport findByDiagram(int did) {
            return null;
        }

        @Override
        public void delete(int id) {
        }
    }
}
