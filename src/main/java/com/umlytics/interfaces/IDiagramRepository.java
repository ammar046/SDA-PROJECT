package com.umlytics.interfaces;

import com.umlytics.domain.UMLDiagram;

import java.util.List;
import java.util.UUID;

public interface IDiagramRepository {
    void save(UMLDiagram d);

    UMLDiagram findById(UUID id);

    List<UMLDiagram> findByProject(UUID projectId);

    void delete(UUID id);

    void update(UMLDiagram d);
}
