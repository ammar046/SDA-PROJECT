package com.umlytics.ui.canvas;

import com.umlytics.enums.RelationshipType;

import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

import java.util.function.DoubleConsumer;

public class RelationshipEdge extends Group {
    private final Line segment1;
    private final Line segment2;
    private final Line segment3;
    private final Polygon arrowHead;
    private final Polygon startMarker;
    private final Rectangle topWaypoint;
    private final Rectangle bottomWaypoint;
    private final Label middleLabel;
    private final Label sourceMultiplicityLabel;
    private final Label targetMultiplicityLabel;
    private RelationshipType relationshipType = RelationshipType.ASSOCIATION;
    private boolean dashed;
    private String sourceMultiplicity = "1";
    private String targetMultiplicity = "*";
    private double startX;
    private double startY;
    private double endX;
    private double endY;
    private double bendX;
    private DoubleConsumer bendChangedHandler;
    private Color strokeColor = Color.BLACK;

    public RelationshipEdge(double startX, double startY, double endX, double endY) {
        this.segment1 = new Line();
        this.segment2 = new Line();
        this.segment3 = new Line();
        this.arrowHead = new Polygon();
        this.startMarker = new Polygon();
        this.topWaypoint = new Rectangle(8, 8);
        this.bottomWaypoint = new Rectangle(8, 8);
        this.middleLabel = new Label("");
        this.sourceMultiplicityLabel = new Label("1");
        this.targetMultiplicityLabel = new Label("*");

        segment1.setStrokeWidth(1.4);
        segment2.setStrokeWidth(1.4);
        segment3.setStrokeWidth(1.4);
        arrowHead.setFill(Color.TRANSPARENT);
        arrowHead.setStroke(Color.BLACK);
        startMarker.setFill(Color.TRANSPARENT);
        startMarker.setStroke(Color.BLACK);
        topWaypoint.setFill(Color.web("#0066ff"));
        bottomWaypoint.setFill(Color.web("#0066ff"));
        topWaypoint.setVisible(false);
        bottomWaypoint.setVisible(false);
        topWaypoint.setManaged(false);
        bottomWaypoint.setManaged(false);

        getChildren().addAll(segment1, segment2, segment3, arrowHead, startMarker, topWaypoint, bottomWaypoint,
                middleLabel, sourceMultiplicityLabel, targetMultiplicityLabel);
        initializeWaypointDragging();
        update(startX, startY, endX, endY);
        setRelationshipType(RelationshipType.ASSOCIATION);
    }

    public void update(double startX, double startY, double endX, double endY) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;

        if (bendX == 0) {
            bendX = (startX + endX) / 2.0;
        }
        double midX = bendX;
        segment1.setStartX(startX);
        segment1.setStartY(startY);
        segment1.setEndX(midX);
        segment1.setEndY(startY);

        segment2.setStartX(midX);
        segment2.setStartY(startY);
        segment2.setEndX(midX);
        segment2.setEndY(endY);

        segment3.setStartX(midX);
        segment3.setStartY(endY);
        segment3.setEndX(endX);
        segment3.setEndY(endY);

        arrowHead.getPoints().setAll(
                endX, endY,
                endX - 10, endY - 5,
                endX - 10, endY + 5
        );
        startMarker.getPoints().setAll(
                startX, startY,
                startX + 10, startY - 5,
                startX + 10, startY + 5
        );

        middleLabel.setLayoutX(midX + 6);
        middleLabel.setLayoutY((startY + endY) / 2.0 - 10);
        sourceMultiplicityLabel.setLayoutX(startX + 2);
        sourceMultiplicityLabel.setLayoutY(startY - 14);
        targetMultiplicityLabel.setLayoutX(endX - 10);
        targetMultiplicityLabel.setLayoutY(endY - 14);
        topWaypoint.setLayoutX(midX - 4);
        topWaypoint.setLayoutY(startY - 4);
        bottomWaypoint.setLayoutX(midX - 4);
        bottomWaypoint.setLayoutY(endY - 4);
        setRelationshipType(relationshipType);
    }

    public void setRelationshipType(RelationshipType type) {
        this.relationshipType = type;
        segment1.getStrokeDashArray().clear();
        segment2.getStrokeDashArray().clear();
        segment3.getStrokeDashArray().clear();
        startMarker.setVisible(false);
        arrowHead.setFill(Color.TRANSPARENT);
        arrowHead.getPoints().setAll(
                endX, endY,
                endX - 10, endY - 5,
                endX - 10, endY + 5
        );

        switch (type) {
            case DEPENDENCY -> {
                segment1.getStrokeDashArray().setAll(7.0, 5.0);
                segment2.getStrokeDashArray().setAll(7.0, 5.0);
                segment3.getStrokeDashArray().setAll(7.0, 5.0);
            }
            case REALIZATION -> {
                segment1.getStrokeDashArray().setAll(7.0, 5.0);
                segment2.getStrokeDashArray().setAll(7.0, 5.0);
                segment3.getStrokeDashArray().setAll(7.0, 5.0);
                arrowHead.getPoints().setAll(
                        endX, endY,
                        endX - 14, endY - 8,
                        endX - 14, endY + 8
                );
            }
            case INHERITANCE -> arrowHead.getPoints().setAll(
                    endX, endY,
                    endX - 14, endY - 8,
                    endX - 14, endY + 8
            );
            case COMPOSITION -> {
                startMarker.setVisible(true);
                startMarker.setFill(Color.BLACK);
                startMarker.getPoints().setAll(
                        startX, startY,
                        startX + 10, startY - 6,
                        startX + 20, startY,
                        startX + 10, startY + 6
                );
            }
            case AGGREGATION -> {
                startMarker.setVisible(true);
                startMarker.setFill(Color.WHITE);
                startMarker.getPoints().setAll(
                        startX, startY,
                        startX + 10, startY - 6,
                        startX + 20, startY,
                        startX + 10, startY + 6
                );
            }
            default -> {
                // ASSOCIATION defaults.
            }
        }
        applyStrokeColor();
    }

    public RelationshipType getRelationshipType() {
        return relationshipType;
    }

    public void setMiddleLabel(String text) {
        middleLabel.setText(text == null ? "" : text);
    }

    public String getMiddleLabel() {
        return middleLabel.getText();
    }

    public void setSourceMultiplicity(String text) {
        sourceMultiplicity = text == null ? "" : text;
        sourceMultiplicityLabel.setText(sourceMultiplicity);
    }

    public void setTargetMultiplicity(String text) {
        targetMultiplicity = text == null ? "" : text;
        targetMultiplicityLabel.setText(targetMultiplicity);
    }

    public String getSourceMultiplicity() {
        return sourceMultiplicity;
    }

    public String getTargetMultiplicity() {
        return targetMultiplicity;
    }

    public void setSelected(boolean selected) {
        Color color = selected ? Color.web("#0066ff") : Color.BLACK;
        if (selected) {
            segment1.setStroke(color);
            segment2.setStroke(color);
            segment3.setStroke(color);
            arrowHead.setStroke(color);
            startMarker.setStroke(color);
        } else {
            applyStrokeColor();
        }
        topWaypoint.setVisible(selected);
        bottomWaypoint.setVisible(selected);
    }

    public void setBendChangedHandler(DoubleConsumer bendChangedHandler) {
        this.bendChangedHandler = bendChangedHandler;
    }

    public double getBendX() {
        return bendX;
    }

    public void setBendX(double bendX) {
        this.bendX = bendX;
        update(startX, startY, endX, endY);
    }

    public void setStrokeColor(Color color) {
        this.strokeColor = color == null ? Color.BLACK : color;
        applyStrokeColor();
    }

    public Color getStrokeColor() {
        return strokeColor;
    }

    public void setLineStyleDashed(boolean dashed) {
        this.dashed = dashed;
        if (dashed) {
            segment1.getStrokeDashArray().setAll(7.0, 5.0);
            segment2.getStrokeDashArray().setAll(7.0, 5.0);
            segment3.getStrokeDashArray().setAll(7.0, 5.0);
        } else {
            segment1.getStrokeDashArray().clear();
            segment2.getStrokeDashArray().clear();
            segment3.getStrokeDashArray().clear();
        }
    }

    public boolean isDashed() {
        return dashed;
    }

    public double getStartCenterX() {
        return startX;
    }

    public double getStartCenterY() {
        return startY;
    }

    public double getEndCenterX() {
        return endX;
    }

    public double getEndCenterY() {
        return endY;
    }

    private void initializeWaypointDragging() {
        topWaypoint.setOnMouseDragged(event -> {
            bendX = sceneToLocal(event.getSceneX(), event.getSceneY()).getX();
            update(startX, startY, endX, endY);
            if (bendChangedHandler != null) {
                bendChangedHandler.accept(bendX);
            }
            event.consume();
        });
        bottomWaypoint.setOnMouseDragged(event -> {
            bendX = sceneToLocal(event.getSceneX(), event.getSceneY()).getX();
            update(startX, startY, endX, endY);
            if (bendChangedHandler != null) {
                bendChangedHandler.accept(bendX);
            }
            event.consume();
        });
    }

    private void applyStrokeColor() {
        segment1.setStroke(strokeColor);
        segment2.setStroke(strokeColor);
        segment3.setStroke(strokeColor);
        arrowHead.setStroke(strokeColor);
        startMarker.setStroke(strokeColor);
    }
}
