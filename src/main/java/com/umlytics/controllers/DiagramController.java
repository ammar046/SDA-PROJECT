package com.umlytics.controllers;

import com.umlytics.domain.DiagramEdit;
import com.umlytics.domain.UMLDiagram;
import com.umlytics.domain.UMLModel;
import com.umlytics.enums.ExportFormat;
import com.umlytics.enums.SourceType;
import com.umlytics.exceptions.UnsupportedFormatException;
import com.umlytics.exceptions.UnsupportedFileException;
import com.umlytics.exceptions.ValidationException;
import com.umlytics.interfaces.IAIEngine;
import com.umlytics.interfaces.ICodeParser;
import com.umlytics.interfaces.IDiagramRepository;
import com.umlytics.interfaces.IExportService;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    public UMLDiagram generateFromNL(int projectId, String desc) {
        if (desc == null || desc.trim().length() <= 10) {
            throw new ValidationException("Description too short. Provide at least 10 characters.");
        }
        UMLModel model = aiEngine.generateFromText(desc);
        UMLDiagram diagram = model.toUMLDiagram();
        diagram.setProjectId(projectId);
        diagram.setSourceType(SourceType.NL);
        diagramRepo.save(diagram);
        return diagram;
    }

    public UMLDiagram generateFromCode(int projectId, List<File> files) {
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
        diagram.setSourceType(SourceType.CODE);
        diagramRepo.save(diagram);
        return diagram;
    }

    public void refineDiagram(int diagramId, DiagramEdit edit) {
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

    public void exportDiagram(int diagramId, ExportFormat fmt, String path) {
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

    public void saveDiagram(UMLDiagram diagram) {
        if (diagram == null) {
            return;
        }
        if (diagram.getDiagramId() <= 0) {
            diagramRepo.save(diagram);
        } else {
            diagramRepo.update(diagram);
        }
    }

    public List<UMLDiagram> listProjectDiagrams(int projectId) {
        return diagramRepo.findByProject(projectId);
    }

    public UMLDiagram analyzeUploadedImage(int projectId, byte[] img) {
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
        diagram.setSourceType(SourceType.UPLOAD);
        diagramRepo.save(diagram);
        return diagram;
    }

    private void pruneInvalidRelationships(UMLDiagram diagram) {
        Set<Integer> classIds = new HashSet<>();
        diagram.getClasses().forEach(c -> classIds.add(c.getClassId()));
        diagram.getRelationships().removeIf(rel ->
                rel.getSource() == null
                        || rel.getTarget() == null
                        || rel.getSource() == rel.getTarget()
                        || !classIds.contains(rel.getSource().getClassId())
                        || !classIds.contains(rel.getTarget().getClassId()));
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
