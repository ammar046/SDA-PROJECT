package com.umlytics.interfaces;

import com.umlytics.domain.UMLDiagram;

/**
 * Observer interface for diagram change notifications.
 * GoF: Observer
 */
public interface DiagramChangeListener {
    /** Called whenever the current diagram is saved/updated. */
    void onDiagramChanged(UMLDiagram updated);
}
