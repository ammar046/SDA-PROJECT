package com.umlytics.controllers;

import com.umlytics.domain.DesignEvaluationReport;
import com.umlytics.domain.ProjectContext;
import com.umlytics.domain.UMLDiagram;
import com.umlytics.domain.UMLModel;
import com.umlytics.enums.ExportFormat;
import com.umlytics.exceptions.UnsupportedFormatException;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DiagramControllerTest {
    private DiagramController controller;
    private InMemoryDiagramRepository repository;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        repository = new InMemoryDiagramRepository();
        controller = new DiagramController(repository, new StubAI(), new StubParser(), new StubExport());
        projectId = UUID.randomUUID();
    }

    @Test
    void generateFromTextTooShortThrows() {
        assertThrows(ValidationException.class, () -> controller.generateFromText(projectId, "short"));
    }

    @Test
    void generateFromTextValid() {
        UMLDiagram diagram = controller.generateFromText(projectId, "Generate classes for library management");
        assertNotNull(diagram);
    }

    @Test
    void generateFromCodeUnsupportedFileThrows() {
        assertThrows(UnsupportedFileException.class, () -> controller.generateFromCode(projectId, List.of(new File("test.txt"))));
    }

    @Test
    void exportDiagramRejectsNullFormat() {
        UMLDiagram diagram = new UMLDiagram();
        diagram.setDiagramId(UUID.randomUUID());
        repository.save(diagram);
        assertThrows(UnsupportedFormatException.class, () -> controller.exportDiagram(diagram.getDiagramId(), null, "x.png"));
    }

    @Test
    void exportDiagramRejectsBlankPath() {
        UMLDiagram diagram = new UMLDiagram();
        diagram.setDiagramId(UUID.randomUUID());
        repository.save(diagram);
        assertThrows(ValidationException.class, () -> controller.exportDiagram(diagram.getDiagramId(), ExportFormat.PNG, " "));
    }

    @Test
    void analyzeUploadedImageRejectsInvalidPayloads() {
        assertThrows(UnsupportedFileException.class, () -> controller.analyzeUploadedImage(projectId, new byte[0]));
        assertThrows(UnsupportedFileException.class, () -> controller.analyzeUploadedImage(projectId, new byte[]{0x01, 0x02, 0x03}));
    }

    @Test
    void refineDiagramRejectsNullEdit() {
        UMLDiagram diagram = new UMLDiagram();
        diagram.setDiagramId(UUID.randomUUID());
        repository.save(diagram);
        assertThrows(ValidationException.class, () -> controller.modifyClassDefinition(diagram.getDiagramId(), null));
    }

    private static class InMemoryDiagramRepository implements IDiagramRepository {
        private UMLDiagram diagram;

        @Override
        public void save(UMLDiagram d) {
            this.diagram = d;
        }

        @Override
        public UMLDiagram findById(UUID id) {
            return diagram;
        }

        @Override
        public List<UMLDiagram> findByProject(UUID pid) {
            return diagram == null ? List.of() : List.of(diagram);
        }

        @Override
        public void delete(UUID id) {
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
        public DesignEvaluationReport evaluateDesign(UMLModel m) {
            return new DesignEvaluationReport();
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
