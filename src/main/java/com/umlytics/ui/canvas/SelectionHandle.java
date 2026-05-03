package com.umlytics.ui.canvas;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class SelectionHandle extends Rectangle {
    public SelectionHandle(double x, double y) {
        super(x, y, 8, 8);
        setFill(Color.web("#0066ff"));
        setStroke(Color.WHITE);
    }
}
