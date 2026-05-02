package com.umlytics.domain;

import com.umlytics.enums.EditType;

import java.util.HashMap;
import java.util.Map;

public class DiagramEdit {
    private EditType editType;
    private int targetClassId;
    private Map<String, Object> payload = new HashMap<>();

    public void apply(UMLDiagram diagram) {
        if (diagram == null || editType == null) {
            return;
        }
        switch (editType) {
            case ADD_CLASS -> {
                UMLClass umlClass = new UMLClass();
                Object className = payload.get("name");
                umlClass.setName(className == null ? "NewClass" : className.toString());
                diagram.addUMLClass(umlClass);
            }
            case REMOVE_CLASS -> diagram.removeUMLClass(targetClassId);
            case RENAME -> {
                Object name = payload.get("name");
                if (name != null) {
                    for (UMLClass umlClass : diagram.getClasses()) {
                        if (umlClass.getClassId() == targetClassId) {
                            umlClass.setName(name.toString());
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

    public int getTargetClassId() {
        return targetClassId;
    }

    public void setTargetClassId(int targetClassId) {
        this.targetClassId = targetClassId;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
}
