package com.umlytics.ui;

import javafx.scene.text.TextFlow;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaSyntaxHighlighterTest {

    @Test
    void fillTextFlowHandlesJavaSample() {
        TextFlow flow = new TextFlow();
        JavaSyntaxHighlighter.fillTextFlow(flow, "public class Foo {\n  private int x;\n}");
        assertFalse(flow.getChildren().isEmpty());
    }

    @Test
    void fillTextFlowAcceptsNullOrEmpty() {
        TextFlow flow = new TextFlow();
        JavaSyntaxHighlighter.fillTextFlow(flow, null);
        assertTrue(flow.getChildren().isEmpty());
        JavaSyntaxHighlighter.fillTextFlow(flow, "");
        assertTrue(flow.getChildren().isEmpty());
    }
}
