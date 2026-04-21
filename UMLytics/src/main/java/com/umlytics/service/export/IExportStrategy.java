package com.umlytics.service.export;

import com.umlytics.domain.UMLDiagram;
import com.umlytics.ui.canvas.MainCanvas;

/**
 * Strategy interface for diagram export operations.
 * GoF: Strategy
 */
public interface IExportStrategy {
    /** Export the diagram to the given file path. */
    void export(UMLDiagram diagram, String path, MainCanvas canvas) throws Exception;
}
