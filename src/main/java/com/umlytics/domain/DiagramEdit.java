package com.umlytics.domain;

import com.umlytics.enums.EditType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DiagramEdit {
    private EditType editType;
    private UUID targetClassId;
    private Map<String, Object> payload = new HashMap<>();

    public void apply(UMLDiagram diagram) {
        if (diagram == null || editType == null) {
            return;
        }
        switch (editType) {
            case ADD_CLASS -> {
                ConceptualClass conceptualClass = new ConceptualClass();
                Object className = payload.get("name");
                conceptualClass.setName(className == null ? "NewClass" : className.toString());
                conceptualClass.setClassId(UUID.randomUUID());
                diagram.addConceptualClass(conceptualClass);
            }
            case REMOVE_CLASS -> diagram.removeConceptualClass(targetClassId);
            case RENAME_CLASS -> {
                Object name = payload.get("name");
                if (name != null) {
                    for (ConceptualClass conceptualClass : diagram.getClasses()) {
                        if (targetClassId != null && targetClassId.equals(conceptualClass.getClassId())) {
                            conceptualClass.setName(name.toString());
                            break;
                        }
                    }
                }
            }
            default -> {
                // Additional edit logic will be added in the editor module.
            }
        }
    }

    public EditType getEditType() {
        return editType;
    }

    public void setEditType(EditType editType) {
        this.editType = editType;
    }

    public UUID getTargetClassId() {
        return targetClassId;
    }

    public void setTargetClassId(UUID targetClassId) {
        this.targetClassId = targetClassId;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
}
