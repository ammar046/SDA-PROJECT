package com.umlytics.controllers;

import com.umlytics.domain.ClassSuggestion;
import com.umlytics.domain.ConceptualClass;
import com.umlytics.domain.DesignEvaluationReport;
import com.umlytics.domain.DiagramImage;
import com.umlytics.domain.DiagramEdit;
import com.umlytics.domain.UMLDiagram;
import com.umlytics.domain.UMLModel;
import com.umlytics.enums.ExportFormat;
import com.umlytics.enums.ImageFormat;
import com.umlytics.enums.SourceType;
import com.umlytics.exceptions.DiagramTooSimpleException;
import com.umlytics.exceptions.EmptyDiagramException;
import com.umlytics.exceptions.UnsupportedFormatException;
import com.umlytics.exceptions.UnsupportedFileException;
import com.umlytics.exceptions.ValidationException;
import com.umlytics.interfaces.IAIEngine;
import com.umlytics.interfaces.ICodeParser;
import com.umlytics.interfaces.IDiagramRepository;
import com.umlytics.interfaces.IExportService;

import java.io.File;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// GRASP: Controller (Use Case), Creator
public class DiagramController {
    private final IDiagramRepository diagramRepo;
    private final IAIEngine aiEngine;
    private final ICodeParser codeParser;
    private final IExportService exportSvc;

    public DiagramController(IDiagramRepository diagramRepo, IAIEngine aiEngine, ICodeParser codeParser, IExportService exportSvc) {
        this.diagramRepo = diagramRepo;
        this.aiEngine = aiEngine;
        this.codeParser = codeParser;
        this.exportSvc = exportSvc;
    }

    public UMLDiagram generateFromText(UUID projectId, String desc) {
        if (desc == null || desc.trim().length() <= 10) {
            throw new ValidationException("Description too short. Provide at least 10 characters.");
        }
        UMLModel model = aiEngine.generateFromText(desc);
        UMLDiagram diagram = model.toUMLDiagram();
        diagram.setProjectId(projectId);
        diagram.setSourceType(SourceType.NATURAL_LANGUAGE);
        diagramRepo.save(diagram);
        return diagram;
    }

    public UMLDiagram generateFromText(int projectId, String desc) {
        return generateFromText(UUID.nameUUIDFromBytes(("legacy-project-" + projectId).getBytes()), desc);
    }

    public UMLDiagram generateFromCode(UUID projectId, List<File> files) {
        if (files == null || files.isEmpty()) {
            throw new ValidationException("Select at least one .java file.");
        }
        for (File file : files) {
            if (!file.getName().endsWith(".java")) {
                throw new UnsupportedFileException("Only .java files are supported.");
            }
        }
        UMLModel model = codeParser.parse(files);
        UMLDiagram diagram = model.toUMLDiagram();
        diagram.setProjectId(projectId);
        diagram.setSourceType(SourceType.SOURCE_CODE);
        diagramRepo.save(diagram);
        return diagram;
    }

    public UMLDiagram generateFromCode(int projectId, List<File> files) {
        return generateFromCode(UUID.nameUUIDFromBytes(("legacy-project-" + projectId).getBytes()), files);
    }

    public void modifyClassDefinition(UUID diagramId, DiagramEdit edit) {
        if (edit == null) {
            throw new ValidationException("Edit payload cannot be null.");
        }
        UMLDiagram diagram = diagramRepo.findById(diagramId);
        if (diagram == null) {
            throw new ValidationException("Diagram not found.");
        }
        edit.apply(diagram);
        pruneInvalidRelationships(diagram);
        diagramRepo.update(diagram);
    }

    public void modifyClassDefinition(int diagramId, DiagramEdit edit) {
        modifyClassDefinition(UUID.nameUUIDFromBytes(("legacy-diagram-" + diagramId).getBytes()), edit);
    }

    public void refineDiagram(int diagramId, DiagramEdit edit) {
        modifyClassDefinition(diagramId, edit);
    }

    public void exportDiagram(UUID diagramId, ExportFormat fmt, String path) {
        if (fmt == null || !exportSvc.getSupportedFormats().contains(fmt)) {
            throw new UnsupportedFormatException("Unsupported export format: " + fmt);
        }
        if (path == null || path.isBlank()) {
            throw new ValidationException("Export path is required.");
        }
        UMLDiagram diagram = diagramRepo.findById(diagramId);
        if (diagram == null) {
            throw new ValidationException("Diagram not found.");
        }
        exportSvc.export(diagram, fmt, path);
    }

    public void exportDiagram(int diagramId, ExportFormat fmt, String path) {
        exportDiagram(UUID.nameUUIDFromBytes(("legacy-diagram-" + diagramId).getBytes()), fmt, path);
    }

    public void saveDiagram(UMLDiagram diagram) {
        if (diagram == null) {
            return;
        }
        if (diagram.getDiagramId() == null) {
            diagram.setDiagramId(UUID.randomUUID());
        }
        UMLDiagram existing = diagramRepo.findById(diagram.getDiagramId());
        if (existing == null) {
            diagramRepo.save(diagram);
        } else {
            diagramRepo.update(diagram);
        }
    }

    public List<UMLDiagram> listProjectDiagrams(UUID projectId) {
        return diagramRepo.findByProject(projectId);
    }

    public List<UMLDiagram> listProjectDiagrams(int projectId) {
        return listProjectDiagrams(UUID.nameUUIDFromBytes(("legacy-project-" + projectId).getBytes()));
    }

    public UMLDiagram analyzeUploadedImage(UUID projectId, byte[] img) {
        if (img == null || img.length == 0) {
            throw new UnsupportedFileException("Image is empty.");
        }
        if (img.length > 5 * 1024 * 1024) {
            throw new UnsupportedFileException("Image exceeds 5MB limit.");
        }
        if (!looksLikeSupportedImage(img)) {
            throw new UnsupportedFileException("Only PNG/JPG images are supported.");
        }
        UMLModel model = aiEngine.analyzeImage(img);
        UMLDiagram diagram = model.toUMLDiagram();
        diagram.setProjectId(projectId);
        diagram.setSourceType(SourceType.UPLOADED_IMAGE);
        diagramRepo.save(diagram);
        return diagram;
    }

    public UMLDiagram analyzeUploadedImage(int projectId, byte[] img) {
        return analyzeUploadedImage(UUID.nameUUIDFromBytes(("legacy-project-" + projectId).getBytes()), img);
    }

    public DesignEvaluationReport evaluateDesign(UUID diagramId) {
        UMLDiagram diagram = diagramRepo.findById(diagramId);
        if (diagram == null) {
            throw new ValidationException("Diagram not found.");
        }
        if (diagram.getClasses().size() < 2) {
            throw new DiagramTooSimpleException("Need at least 2 classes.");
        }
        UMLModel model = new UMLModel();
        model.setClasses(diagram.getClasses());
        model.setRelationships(diagram.getRelationships());
        DesignEvaluationReport report = aiEngine.evaluateDesign(model);
        report.setReportId(UUID.randomUUID());
        report.setDiagramId(diagramId);
        report.setProjectId(diagram.getProjectId());
        report.setEvaluationDate(LocalDateTime.now());
        return report;
    }

    public DesignEvaluationReport evaluateDesign(int diagramId) {
        return evaluateDesign(UUID.nameUUIDFromBytes(("legacy-diagram-" + diagramId).getBytes()));
    }

    public ClassSuggestion generateStructureSuggestions(UUID diagramId) {
        UMLDiagram diagram = diagramRepo.findById(diagramId);
        if (diagram == null || diagram.getClasses().isEmpty()) {
            throw new EmptyDiagramException("No classes defined.");
        }
        UMLModel model = new UMLModel();
        model.setClasses(diagram.getClasses());
        model.setRelationships(diagram.getRelationships());
        String skeleton = aiEngine.generateStructure(model);
        ClassSuggestion suggestion = new ClassSuggestion();
        suggestion.setSuggestionId(UUID.randomUUID());
        suggestion.setDiagramId(diagramId);
        suggestion.setSkeletonCode(ClassSuggestion.combineSkeletonResponse(skeleton));
        suggestion.setAccepted(false);
        return suggestion;
    }

    public ClassSuggestion generateStructureSuggestions(int diagramId) {
        return generateStructureSuggestions(UUID.nameUUIDFromBytes(("legacy-diagram-" + diagramId).getBytes()));
    }

    // GRASP: Controller — delegates persistence to repository
    public void deleteDiagram(UUID diagramId) {
        if (diagramId != null) {
            diagramRepo.delete(diagramId);
        }
    }

    /** Persists a new display title for an existing diagram. */
    public void renameDiagram(UUID diagramId, String newTitle) {
        if (diagramId == null) {
            throw new ValidationException("Diagram id is required.");
        }
        if (newTitle == null || newTitle.isBlank()) {
            throw new ValidationException("Diagram name cannot be empty.");
        }
        UMLDiagram diagram = diagramRepo.findById(diagramId);
        if (diagram == null) {
            throw new ValidationException("Diagram not found.");
        }
        diagram.setTitle(newTitle.trim());
        diagram.setLastModifiedDate(LocalDateTime.now());
        diagramRepo.update(diagram);
    }

    public void renameDiagram(int diagramId, String newTitle) {
        renameDiagram(UUID.nameUUIDFromBytes(("legacy-diagram-" + diagramId).getBytes()), newTitle);
    }

    private void pruneInvalidRelationships(UMLDiagram diagram) {
        Set<UUID> classIds = new HashSet<>();
        diagram.getClasses().forEach(c -> classIds.add(c.getClassId()));
        diagram.getRelationships().removeIf(rel ->
                rel.getSource() == null
                        || rel.getTarget() == null
                        || rel.getSource().getClassId().equals(rel.getTarget().getClassId())
                        || !classIds.contains(rel.getSource().getClassId())
                        || !classIds.contains(rel.getTarget().getClassId()));
    }

    private boolean looksLikePng(byte[] bytes) {
        return bytes.length >= 4
                && bytes[0] == (byte) 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47;
    }

    private boolean looksLikeSupportedImage(byte[] bytes) {
        if (bytes.length < 4) {
            return false;
        }
        boolean png = bytes[0] == (byte) 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47;
        boolean jpg = bytes[0] == (byte) 0xFF
                && bytes[1] == (byte) 0xD8
                && bytes[2] == (byte) 0xFF;
        return png || jpg;
    }
}
