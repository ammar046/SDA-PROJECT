package com.umlytics.domain.valueobjects;

import com.umlytics.enums.EditType;
import com.umlytics.domain.UMLDiagram;
import java.util.HashMap;
import java.util.Map;

/**
 * Command object representing a single change to a UML diagram.
 * GoF: Command Pattern
 */
public class DiagramEdit {
    private EditType          editType;
    private int               targetClassId;
    private Map<String,Object> payload = new HashMap<>();

    public DiagramEdit() {}

    public DiagramEdit(EditType editType, int targetClassId, Map<String,Object> payload) {
        this.editType      = editType;
        this.targetClassId = targetClassId;
        this.payload       = payload != null ? payload : new HashMap<>();
    }

    /** Apply this edit to a diagram in-memory. */
    public void apply(UMLDiagram diagram) {
        if (diagram == null) return;
        switch (editType) {
            case ADD_CLASS -> {
                String name = (String) payload.getOrDefault("name", "NewClass");
                com.umlytics.domain.UMLClass cls = new com.umlytics.domain.UMLClass();
                cls.setName(name);
                cls.setPositionX((double) payload.getOrDefault("x", 100.0));
                cls.setPositionY((double) payload.getOrDefault("y", 100.0));
                diagram.addUMLClass(cls);
            }
            case REMOVE_CLASS -> diagram.removeUMLClass(targetClassId);
            case RENAME -> diagram.getClasses().stream()
                    .filter(c -> c.getClassId() == targetClassId)
                    .findFirst().ifPresent(c -> c.setName((String) payload.get("name")));
            default -> { /* Additional cases handled by controller */ }
        }
    }

    public EditType          getEditType()                   { return editType; }
    public void              setEditType(EditType t)         { this.editType = t; }
    public int               getTargetClassId()              { return targetClassId; }
    public void              setTargetClassId(int id)        { this.targetClassId = id; }
    public Map<String,Object> getPayload()                   { return payload; }
    public void              setPayload(Map<String,Object> p) { this.payload = p; }
}
