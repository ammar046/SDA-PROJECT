package com.umlytics.interfaces;

import com.umlytics.domain.UMLDiagram;
import java.util.List;

public interface IDiagramRepository {
    void save(UMLDiagram d);
    UMLDiagram findById(int id);
    List<UMLDiagram> findByProject(int projectId);
    void delete(int id);
    void update(UMLDiagram d);
}
