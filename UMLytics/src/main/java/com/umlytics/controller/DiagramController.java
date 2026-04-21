package com.umlytics.controller;

import com.umlytics.domain.UMLDiagram;
import com.umlytics.domain.valueobjects.DiagramEdit;
import com.umlytics.enums.ExportFormat;
import com.umlytics.enums.SourceType;
import com.umlytics.interfaces.IDiagramRepository;
import com.umlytics.interfaces.IExportService;
import com.umlytics.interfaces.DiagramChangeListener;
import com.umlytics.persistence.DiagramRepositoryImpl;
import com.umlytics.service.export.DiagramExportService;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles Diagram management and editing.
 * GRASP: Controller (Use Case), Creator
 * GoF: Subject in Observer pattern (notifies DiagramChangeListeners)
 */
public class DiagramController {

    private final IDiagramRepository diagramRepo;
    private final IExportService     exportService;
    private UMLDiagram               currentDiagram;

    // GoF: Observer — list of registered canvas listeners
    private final List<DiagramChangeListener> listeners = new ArrayList<>();

    public DiagramController() {
        this.diagramRepo   = new DiagramRepositoryImpl();
        this.exportService = new DiagramExportService();
    }

    public UMLDiagram createDiagram(int projectId, String title, SourceType type) {
        UMLDiagram d = new UMLDiagram(0, title, type, projectId);
        diagramRepo.save(d);
        this.currentDiagram = d;
        return d;
    }

    public void loadDiagram(int diagramId) {
        this.currentDiagram = diagramRepo.findById(diagramId);
    }

    public List<UMLDiagram> getProjectDiagrams(int projectId) {
        return diagramRepo.findByProject(projectId);
    }

    public void saveCurrentDiagram() {
        if (currentDiagram != null) {
            diagramRepo.update(currentDiagram);
            // GoF: Observer — notify all registered listeners
            final UMLDiagram snapshot = currentDiagram;
            listeners.forEach(l -> l.onDiagramChanged(snapshot));
        }
    }

    /** Rename a diagram by id. */
    public void renameDiagram(int id, String newTitle) {
        UMLDiagram d = diagramRepo.findById(id);
        if (d != null) {
            d.setTitle(newTitle);
            diagramRepo.update(d);
            if (currentDiagram != null && currentDiagram.getDiagramId() == id) {
                currentDiagram.setTitle(newTitle);
            }
        }
    }

    public void applyEdit(DiagramEdit edit) {
        if (currentDiagram != null) {
            edit.apply(currentDiagram);
        }
    }

    public void deleteDiagram(int diagramId) {
        diagramRepo.delete(diagramId);
        if (currentDiagram != null && currentDiagram.getDiagramId() == diagramId) {
            currentDiagram = null;
        }
    }

    public void exportDiagram(ExportFormat format, String path) {
        if (currentDiagram != null) {
            exportService.export(currentDiagram, format, path);
        }
    }

    // GoF: Observer — registration methods
    public void addListener(DiagramChangeListener l) {
        if (l != null && !listeners.contains(l)) listeners.add(l);
    }

    public void removeListener(DiagramChangeListener l) {
        listeners.remove(l);
    }

    public UMLDiagram getCurrentDiagram() { return currentDiagram; }
    public void setCurrentDiagram(UMLDiagram d) { this.currentDiagram = d; }
}
