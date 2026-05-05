package com.umlytics.ui.canvas;

import com.umlytics.enums.RelationshipType;

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;

import java.util.function.DoubleConsumer;

/**
 * Smooth curved connectors between class anchors (cleaner than orthogonal elbows).
 * {@code bendX} is a small user tweak to curvature; large legacy orthogonal values are ignored.
 */
public class RelationshipEdge extends Group {
    public static final Color DEFAULT_EDGE_STROKE = Color.web("#c4b5fd");
    private static final double TIP_INSET = 11;
    /** Association end when no arrowhead (composition / aggregation). */
    private static final double PLAIN_END_INSET = 5;
    private static final double START_INSET = 12;
    private static final double DIAMOND_BACK = 17;
    private static final double LEGACY_BEND_THRESHOLD = 450;
    private static final double MAX_USER_CURVE_TWEAK = 95;

    private final Path route;
    private final Polygon arrowHead;
    private final Polygon startMarker;
    private final Rectangle bendWaypoint;
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
    private Color strokeColor = DEFAULT_EDGE_STROKE;
    private double bendDragSceneX;
    private double bendDragSceneY;
    private double lastApproachX;
    private double lastApproachY;
    private double curveControl2X;
    private double curveControl2Y;

    public RelationshipEdge(double startX, double startY, double endX, double endY) {
        this.route = new Path();
        this.route.setStrokeType(StrokeType.CENTERED);
        this.route.setStrokeLineCap(StrokeLineCap.ROUND);
        this.route.setStrokeLineJoin(StrokeLineJoin.ROUND);
        this.route.setStrokeWidth(1.45);
        this.route.setFill(null);
        this.route.setStyle("-fx-effect: dropshadow(gaussian, rgba(139,92,246,0.22), 4, 0.2, 0, 0);");
        this.arrowHead = new Polygon();
        this.startMarker = new Polygon();
        this.bendWaypoint = new Rectangle(7, 7);
        this.middleLabel = new Label("");
        this.sourceMultiplicityLabel = new Label("1");
        this.targetMultiplicityLabel = new Label("*");

        middleLabel.setStyle("-fx-text-fill: #e8e4ff; -fx-background-color: rgba(30,27,45,0.92); -fx-padding: 2 6; -fx-background-radius: 6;");
        String cardStyle = "-fx-text-fill: #faf5ff; -fx-font-weight: bold; -fx-font-size: 10px; "
                + "-fx-background-color: rgba(49,46,74,0.95); -fx-padding: 2 6; -fx-background-radius: 6;";
        sourceMultiplicityLabel.setStyle(cardStyle);
        targetMultiplicityLabel.setStyle(cardStyle);
        sourceMultiplicityLabel.setMouseTransparent(true);
        targetMultiplicityLabel.setMouseTransparent(true);

        arrowHead.setFill(Color.TRANSPARENT);
        arrowHead.setStroke(DEFAULT_EDGE_STROKE);
        arrowHead.setStrokeLineCap(StrokeLineCap.ROUND);
        arrowHead.setStrokeLineJoin(StrokeLineJoin.ROUND);
        startMarker.setFill(Color.TRANSPARENT);
        startMarker.setStroke(DEFAULT_EDGE_STROKE);
        startMarker.setStrokeLineCap(StrokeLineCap.ROUND);
        startMarker.setStrokeLineJoin(StrokeLineJoin.ROUND);
        bendWaypoint.setFill(Color.web("#818cf8"));
        bendWaypoint.setArcWidth(4);
        bendWaypoint.setArcHeight(4);
        bendWaypoint.setVisible(false);
        bendWaypoint.setManaged(false);

        getChildren().addAll(route, arrowHead, startMarker, bendWaypoint);
        getChildren().addAll(middleLabel, sourceMultiplicityLabel, targetMultiplicityLabel);
        initializeWaypointDragging();
        update(startX, startY, endX, endY);
    }

    public void update(double startX, double startY, double endX, double endY) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;

        if (bendX == 0) {
            bendX = 0;
        } else if (Math.abs(bendX) > LEGACY_BEND_THRESHOLD) {
            bendX = 0;
        }

        setRelationshipType(relationshipType);
        layoutLabelsAndHandle();
    }

    private double userCurveTweak() {
        return Math.max(-MAX_USER_CURVE_TWEAK, Math.min(MAX_USER_CURVE_TWEAK, bendX));
    }

    private void layoutSmoothCurve() {
        double dx = endX - startX;
        double dy = endY - startY;
        double len = Math.hypot(dx, dy);
        if (len < 1e-3) {
            route.getElements().clear();
            route.getElements().addAll(new MoveTo(startX, startY), new LineTo(endX, endY));
            lastApproachX = startX;
            lastApproachY = startY;
            curveControl2X = startX;
            curveControl2Y = startY;
            return;
        }
        double ux = dx / len;
        double uy = dy / len;

        boolean hasDiamond = relationshipType == RelationshipType.COMPOSITION || relationshipType == RelationshipType.AGGREGATION;
        boolean arrowAtTarget = relationshipType != RelationshipType.COMPOSITION
                && relationshipType != RelationshipType.AGGREGATION;
        double endInset = arrowAtTarget ? TIP_INSET : PLAIN_END_INSET;
        double startPull = START_INSET + (hasDiamond ? DIAMOND_BACK + 4 : 0);
        double lsX = startX + ux * startPull;
        double lsY = startY + uy * startPull;
        double leiX = endX - ux * endInset;
        double leiY = endY - uy * endInset;

        double chordDx = leiX - lsX;
        double chordDy = leiY - lsY;
        double chordLen = Math.hypot(chordDx, chordDy);
        if (chordLen < 1e-3) {
            route.getElements().clear();
            route.getElements().addAll(new MoveTo(lsX, lsY), new LineTo(leiX, leiY));
            lastApproachX = lsX;
            lastApproachY = lsY;
            curveControl2X = lsX;
            curveControl2Y = lsY;
            return;
        }

        double nx = -chordDy / chordLen;
        double ny = chordDx / chordLen;
        double bulge = Math.min(96, chordLen * 0.32) + userCurveTweak();
        bulge = Math.max(-chordLen * 0.42, Math.min(chordLen * 0.42, bulge));

        double c1x = lsX + chordDx * 0.28 + nx * bulge * 0.55;
        double c1y = lsY + chordDy * 0.28 + ny * bulge * 0.55;
        double c2x = lsX + chordDx * 0.72 + nx * bulge * 0.55;
        double c2y = lsY + chordDy * 0.72 + ny * bulge * 0.55;

        route.getElements().clear();
        route.getElements().addAll(
                new MoveTo(lsX, lsY),
                new CubicCurveTo(c1x, c1y, c2x, c2y, leiX, leiY)
        );
        curveControl2X = c2x;
        curveControl2Y = c2y;
        lastApproachX = c2x;
        lastApproachY = c2y;

        if (hasDiamond) {
            layoutStartMarkerAlongChord(ux, uy);
        }
    }

    private void layoutStartMarkerAlongChord(double ux, double uy) {
        double wx = -uy * 5;
        double wy = ux * 5;
        double bx = ux * DIAMOND_BACK;
        double by = uy * DIAMOND_BACK;
        startMarker.getPoints().setAll(
                startX, startY,
                startX + wx, startY + wy,
                startX + bx, startY + by,
                startX - wx, startY - wy
        );
    }

    private void layoutLabelsAndHandle() {
        double mx = (startX + endX) / 2.0;
        double my = (startY + endY) / 2.0;
        double dx = endX - startX;
        double dy = endY - startY;
        double len = Math.hypot(dx, dy);
        double nx = len > 1e-3 ? -dy / len : 0;
        double ny = len > 1e-3 ? dx / len : -1;
        double bulgeN = userCurveTweak();
        double off = Math.min(28, 12 + Math.abs(bulgeN) * 0.15);

        middleLabel.setLayoutX(mx + nx * off + 4);
        middleLabel.setLayoutY(my + ny * off - 8);

        sourceMultiplicityLabel.setLayoutX(startX + nx * 18 - (nx < 0 ? 40 : 0));
        sourceMultiplicityLabel.setLayoutY(startY + ny * 18 - 12);
        targetMultiplicityLabel.setLayoutX(endX + nx * 18 - (nx < 0 ? 48 : 4));
        targetMultiplicityLabel.setLayoutY(endY + ny * 18 - 12);

        bendWaypoint.setLayoutX(mx + nx * (off + 8) - 3.5);
        bendWaypoint.setLayoutY(my + ny * (off + 8) - 3.5);
    }

    private Point2D arrowApproachUnit() {
        double ddx = endX - lastApproachX;
        double ddy = endY - lastApproachY;
        double l = Math.hypot(ddx, ddy);
        if (l < 1e-4) {
            ddx = endX - curveControl2X;
            ddy = endY - curveControl2Y;
            l = Math.hypot(ddx, ddy);
        }
        if (l < 1e-4) {
            ddx = endX - startX;
            ddy = endY - startY;
            l = Math.hypot(ddx, ddy);
            if (l < 1e-4) {
                return new Point2D(1, 0);
            }
        }
        return new Point2D(ddx / l, ddy / l);
    }

    public void setRelationshipType(RelationshipType type) {
        this.relationshipType = type;
        layoutSmoothCurve();
        route.getStrokeDashArray().clear();

        startMarker.setVisible(false);
        arrowHead.setVisible(true);
        Point2D u = arrowApproachUnit();
        double ux = u.getX();
        double uy = u.getY();
        double px = -uy;
        double py = ux;

        arrowHead.setFill(Color.TRANSPARENT);
        arrowHead.getPoints().setAll(
                endX, endY,
                endX - 8 * ux + 3.5 * px, endY - 8 * uy + 3.5 * py,
                endX - 8 * ux - 3.5 * px, endY - 8 * uy - 3.5 * py
        );

        switch (type) {
            case DEPENDENCY -> route.getStrokeDashArray().setAll(7.0, 5.0);
            case REALIZATION -> {
                route.getStrokeDashArray().setAll(7.0, 5.0);
                arrowHead.setFill(Color.WHITE);
                arrowHead.getPoints().setAll(
                        endX, endY,
                        endX - 11 * ux + 5.5 * px, endY - 11 * uy + 5.5 * py,
                        endX - 11 * ux - 5.5 * px, endY - 11 * uy - 5.5 * py
                );
            }
            case INHERITANCE -> {
                arrowHead.setFill(Color.WHITE);
                arrowHead.getPoints().setAll(
                        endX, endY,
                        endX - 11 * ux + 5.5 * px, endY - 11 * uy + 5.5 * py,
                        endX - 11 * ux - 5.5 * px, endY - 11 * uy - 5.5 * py
                );
            }
            case COMPOSITION -> {
                startMarker.setVisible(true);
                startMarker.setFill(strokeColor);
                arrowHead.setVisible(false);
                arrowHead.getPoints().clear();
            }
            case AGGREGATION -> {
                startMarker.setVisible(true);
                startMarker.setFill(Color.WHITE);
                arrowHead.setVisible(false);
                arrowHead.getPoints().clear();
            }
            default -> {
                // ASSOCIATION
            }
        }
        applyStrokeColor();
        if (dashed) {
            route.getStrokeDashArray().setAll(7.0, 5.0);
        } else if (type != RelationshipType.DEPENDENCY && type != RelationshipType.REALIZATION) {
            route.getStrokeDashArray().clear();
        }
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
        Color color = selected ? Color.web("#818cf8") : strokeColor;
        if (selected) {
            route.setStroke(color);
            arrowHead.setStroke(color);
            startMarker.setStroke(color);
        } else {
            applyStrokeColor();
        }
        bendWaypoint.setVisible(selected);
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
        this.strokeColor = color == null ? DEFAULT_EDGE_STROKE : color;
        applyStrokeColor();
    }

    public Color getStrokeColor() {
        return strokeColor;
    }

    public void setLineStyleDashed(boolean dashed) {
        this.dashed = dashed;
        if (dashed) {
            route.getStrokeDashArray().setAll(7.0, 5.0);
        } else if (relationshipType == RelationshipType.DEPENDENCY || relationshipType == RelationshipType.REALIZATION) {
            route.getStrokeDashArray().setAll(7.0, 5.0);
        } else {
            route.getStrokeDashArray().clear();
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
        bendWaypoint.setOnMousePressed(event -> {
            bendDragSceneX = event.getSceneX();
            bendDragSceneY = event.getSceneY();
            event.consume();
        });
        bendWaypoint.setOnMouseDragged(event -> {
            double sx = event.getSceneX();
            double sy = event.getSceneY();
            bendX += (sx - bendDragSceneX + sy - bendDragSceneY) * 0.12;
            bendDragSceneX = sx;
            bendDragSceneY = sy;
            bendX = Math.max(-MAX_USER_CURVE_TWEAK, Math.min(MAX_USER_CURVE_TWEAK, bendX));
            update(startX, startY, endX, endY);
            if (bendChangedHandler != null) {
                bendChangedHandler.accept(bendX);
            }
            event.consume();
        });
    }

    private void applyStrokeColor() {
        route.setStroke(strokeColor);
        arrowHead.setStroke(strokeColor);
        startMarker.setStroke(strokeColor);
    }
}
