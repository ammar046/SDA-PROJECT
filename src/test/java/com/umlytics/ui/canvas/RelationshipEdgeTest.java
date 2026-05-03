package com.umlytics.ui.canvas;

import javafx.embed.swing.JFXPanel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RelationshipEdgeTest {
    @Test
    void updateChangesEdgeCenterCoordinates() {
        new JFXPanel();
        RelationshipEdge edge = new RelationshipEdge(10, 20, 100, 120);
        edge.update(30, 40, 140, 160);

        assertEquals(30, edge.getStartCenterX());
        assertEquals(40, edge.getStartCenterY());
        assertEquals(140, edge.getEndCenterX());
        assertEquals(160, edge.getEndCenterY());
    }
}
