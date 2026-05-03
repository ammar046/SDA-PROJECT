package com.umlytics.ui.canvas;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;

public class DiagramCanvas extends Canvas {
    private static final double GRID_SIZE = 20.0;
    private double zoom = 1.0;
    private double panX;
    private double panY;
    private double dragStartX;
    private double dragStartY;

    public DiagramCanvas() {
        super(1600, 1000);
        redraw();
        initializeInteractions();
    }

    public void redraw() {
        GraphicsContext gc = getGraphicsContext2D();

        // Dark background
        gc.setFill(Color.web("#1e1e2a"));
        gc.fillRect(0, 0, getWidth(), getHeight());

        // Dark subtle grid lines
        gc.setStroke(Color.web("#2e2e3e", 0.7));
        gc.setLineWidth(0.5);
        double step = GRID_SIZE * zoom;
        for (double x = panX % step; x < getWidth();  x += step) gc.strokeLine(x, 0, x, getHeight());
        for (double y = panY % step; y < getHeight(); y += step) gc.strokeLine(0, y, getWidth(), y);
    }

    private void initializeInteractions() {
        setOnMousePressed(event -> {
            dragStartX = event.getX();
            dragStartY = event.getY();
        });
        setOnMouseDragged(event -> {
            panX += event.getX() - dragStartX;
            panY += event.getY() - dragStartY;
            dragStartX = event.getX();
            dragStartY = event.getY();
            redraw();
        });
        setOnScroll(event -> {
            if (event.isControlDown()) {
                // Let DiagramEditorPanel handle Ctrl+scroll for unified zoom
                return;
            }
            onScrollZoom(event);
        });
    }

    private void onScrollZoom(ScrollEvent event) {
        double factor = event.getDeltaY() > 0 ? 1.1 : 0.9;
        zoom = Math.max(0.1, Math.min(4.0, zoom * factor));
        redraw();
    }

    public double getZoom() {
        return zoom;
    }

    public void resetPan() {
        this.panX = 0;
        this.panY = 0;
        redraw();
    }

    public void setZoom(double zoom) {
        this.zoom = Math.max(0.1, Math.min(4.0, zoom));
        redraw();
    }

    public void fitToWindow() {
        this.panX = 0;
        this.panY = 0;
        this.zoom = 1.0;
        redraw();
    }
}
