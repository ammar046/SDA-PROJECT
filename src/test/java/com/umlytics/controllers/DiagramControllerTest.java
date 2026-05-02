package com.umlytics.controllers;

import com.umlytics.domain.DiagramEdit;
import com.umlytics.domain.EvaluationReport;
import com.umlytics.domain.ProjectContext;
import com.umlytics.domain.UMLDiagram;
import com.umlytics.domain.UMLModel;
import com.umlytics.enums.ExportFormat;
import com.umlytics.exceptions.UnsupportedFileException;
import com.umlytics.exceptions.ValidationException;
import com.umlytics.interfaces.IAIEngine;
import com.umlytics.interfaces.ICodeParser;
import com.umlytics.interfaces.IDiagramRepository;
import com.umlytics.interfaces.IExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DiagramControllerTest {
    private DiagramController controller;

    @BeforeEach
    void setUp() {
        controller = new DiagramController(new InMemoryDiagramRepository(), new StubAI(), new StubParser(), new StubExport());
    }

    @Test
    void generateFromTextTooShortThrows() {
        assertThrows(ValidationException.class, () -> controller.generateFromNL(1, "short"));
    }

    @Test
    void generateFromTextValid() {
        UMLDiagram diagram = controller.generateFromNL(1, "Generate classes for library management");
        assertNotNull(diagram);
    }

    @Test
    void generateFromCodeUnsupportedFileThrows() {
        assertThrows(UnsupportedFileException.class, () -> controller.generateFromCode(1, List.of(new File("test.txt"))));
    }

    private static class InMemoryDiagramRepository implements IDiagramRepository {
        private UMLDiagram diagram;

        @Override
        public void save(UMLDiagram d) {
            this.diagram = d;
        }

        @Override
        public UMLDiagram findById(int id) {
            return diagram;
        }

        @Override
        public List<UMLDiagram> findByProject(int pid) {
            return diagram == null ? List.of() : List.of(diagram);
        }

        @Override
        public void delete(int id) {
            diagram = null;
        }

        @Override
        public void update(UMLDiagram d) {
            diagram = d;
        }
    }

    private static class StubAI implements IAIEngine {
        @Override
        public UMLModel generateFromText(String desc) {
            return new UMLModel();
        }

        @Override
        public EvaluationReport evaluateDesign(UMLModel m) {
            return new EvaluationReport();
        }

        @Override
        public String consultDesign(String q, ProjectContext ctx) {
            return "";
        }

        @Override
        public String generateStructure(UMLModel m) {
            return "";
        }

        @Override
        public UMLModel analyzeImage(byte[] data) {
            return new UMLModel();
        }
    }

    private static class StubParser implements ICodeParser {
        @Override
        public UMLModel parse(List<File> files) {
            return new UMLModel();
        }

        @Override
        public List<String> getSupportedLanguages() {
            return List.of("java");
        }
    }

    private static class StubExport implements IExportService {
        @Override
        public void export(UMLDiagram d, ExportFormat fmt, String path) {
        }

        @Override
        public List<ExportFormat> getSupportedFormats() {
            return List.of(ExportFormat.PNG, ExportFormat.PDF, ExportFormat.SVG);
        }
    }
}
