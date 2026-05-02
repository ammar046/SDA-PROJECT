package com.umlytics.services;

import com.umlytics.domain.DesignEvaluationReport;
import com.umlytics.domain.UMLModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LLMAPIEngineTest {
    @Test
    void generateFromTextReturnsModel() {
        LLMAPIEngine engine = new LLMAPIEngine();
        UMLModel model = engine.generateFromText("Create a small library system with books and members.");
        assertNotNull(model);
    }

    @Test
    void generateFromTextRejectsBlankInput() {
        LLMAPIEngine engine = new LLMAPIEngine();
        assertThrows(RuntimeException.class, () -> engine.generateFromText(" "));
    }

    @Test
    void evaluateDesignReturnsReport() {
        LLMAPIEngine engine = new LLMAPIEngine();
        DesignEvaluationReport report = engine.evaluateDesign(new UMLModel());
        assertNotNull(report);
    }
}
