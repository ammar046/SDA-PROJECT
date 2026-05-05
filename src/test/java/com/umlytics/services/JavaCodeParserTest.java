package com.umlytics.services;

import com.umlytics.domain.UMLModel;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    void parseSkipsBrokenFileAndKeepsGoodOnes() throws Exception {
        File good = File.createTempFile("Good", ".java");
        File bad = File.createTempFile("Bad", ".java");
        try (FileWriter w = new FileWriter(good)) {
            w.write("package demo; public class Good { String name; }");
        }
        try (FileWriter w = new FileWriter(bad)) {
            w.write("this is not valid java {{{");
        }
        JavaCodeParser parser = new JavaCodeParser();
        UMLModel model = parser.parse(List.of(good, bad));
        assertEquals(1, model.getClasses().size());
        assertFalse(model.getParseNotes().isEmpty());
        good.delete();
        bad.delete();
    }

    @Test
    void parseThrowsWhenAllFilesFail() throws Exception {
        File bad = File.createTempFile("OnlyBad", ".java");
        try (FileWriter w = new FileWriter(bad)) {
            w.write("broken !!!");
        }
        JavaCodeParser parser = new JavaCodeParser();
        assertThrows(com.umlytics.exceptions.ParsingException.class, () -> parser.parse(List.of(bad)));
        bad.delete();
    }
}
