package com.umlytics.domain;

import com.umlytics.enums.EditType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiagramEditTest {
    @Test
    void addClassUsesPayloadName() {
        UMLDiagram diagram = new UMLDiagram();
        DiagramEdit edit = new DiagramEdit();
        edit.setEditType(EditType.ADD_CLASS);
        edit.getPayload().put("name", "Customer");

        edit.apply(diagram);

        assertEquals(1, diagram.getClasses().size());
        assertEquals("Customer", diagram.getClasses().get(0).getName());
    }
}
