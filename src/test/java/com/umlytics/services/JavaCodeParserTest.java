package com.umlytics.services;

import com.umlytics.domain.UMLModel;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

class JavaCodeParserTest {
    @Test
    void parseExtractsClassFromJavaFile() throws Exception {
        File file = File.createTempFile("SampleClass", ".java");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("public class SampleClass { private int x; public void run(){} }");
        }

        JavaCodeParser parser = new JavaCodeParser();
        UMLModel model = parser.parse(List.of(file));

        assertFalse(model.getClasses().isEmpty());
        file.delete();
    }
}
