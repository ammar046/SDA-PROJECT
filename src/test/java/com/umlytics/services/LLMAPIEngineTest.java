package com.umlytics.services;

import com.umlytics.domain.UMLModel;
import com.umlytics.exceptions.AIEngineException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class LLMAPIEngineTest {
    @Test
    void generateFromTextThrowsWhenApiNotConfigured() {
        LLMAPIEngine engine = new LLMAPIEngine();
        assertThrows(AIEngineException.class, () ->
                engine.generateFromText("Create a small library system with books and members."));
    }

    @Test
    void generateFromTextRejectsBlankInput() {
        LLMAPIEngine engine = new LLMAPIEngine();
        assertThrows(AIEngineException.class, () -> engine.generateFromText(" "));
    }

    @Test
    void evaluateDesignThrowsWhenApiNotConfigured() {
        LLMAPIEngine engine = new LLMAPIEngine();
        assertThrows(AIEngineException.class, () -> engine.evaluateDesign(new UMLModel()));
    }
}
