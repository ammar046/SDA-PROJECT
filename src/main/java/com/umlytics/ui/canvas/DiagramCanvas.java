package com.umlytics.ui.canvas;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;

public class DiagramCanvas extends Canvas {
    private static final double GRID_SIZE = 10.0;
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
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, getWidth(), getHeight());

        gc.setStroke(Color.web("#e0e0e0"));
        gc.setLineWidth(1);
        for (double x = panX % GRID_SIZE; x < getWidth(); x += GRID_SIZE * zoom) {
            gc.strokeLine(x, 0, x, getHeight());
        }
        for (double y = panY % GRID_SIZE; y < getHeight(); y += GRID_SIZE * zoom) {
            gc.strokeLine(0, y, getWidth(), y);
        }
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
        setOnScroll(this::onScrollZoom);
    }

    private void onScrollZoom(ScrollEvent event) {
        double factor = event.getDeltaY() > 0 ? 1.1 : 0.9;
        zoom = Math.max(0.1, Math.min(4.0, zoom * factor));
        redraw();
    }

    public double getZoom() {
        return zoom;
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
