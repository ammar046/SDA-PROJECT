package com.umlytics.controllers;

import com.umlytics.domain.DiagramEdit;
import com.umlytics.domain.UMLDiagram;
import com.umlytics.domain.UMLModel;
import com.umlytics.enums.ExportFormat;
import com.umlytics.enums.SourceType;
import com.umlytics.exceptions.UnsupportedFileException;
import com.umlytics.exceptions.ValidationException;
import com.umlytics.interfaces.IAIEngine;
import com.umlytics.interfaces.ICodeParser;
import com.umlytics.interfaces.IDiagramRepository;
import com.umlytics.interfaces.IExportService;

import java.io.File;
import java.util.List;

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
        UMLDiagram diagram = diagramRepo.findById(diagramId);
        if (diagram == null) {
            return;
        }
        edit.apply(diagram);
        diagramRepo.update(diagram);
    }

    public void exportDiagram(int diagramId, ExportFormat fmt, String path) {
        UMLDiagram diagram = diagramRepo.findById(diagramId);
        if (diagram != null) {
            exportSvc.export(diagram, fmt, path);
        }
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
        UMLModel model = aiEngine.analyzeImage(img);
        UMLDiagram diagram = model.toUMLDiagram();
        diagram.setProjectId(projectId);
        diagram.setSourceType(SourceType.UPLOAD);
        diagramRepo.save(diagram);
        return diagram;
    }
}
