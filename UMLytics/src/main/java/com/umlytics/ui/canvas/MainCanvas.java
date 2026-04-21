package com.umlytics.ui.canvas;

import com.umlytics.domain.UMLClass;
import com.umlytics.domain.UMLDiagram;
import com.umlytics.domain.relationships.Relationship;
import com.umlytics.enums.RelationshipType;
import com.umlytics.interfaces.DiagramChangeListener;
import com.umlytics.ui.MainWindow;
import com.umlytics.util.RelationshipFactory;

import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.transform.Scale;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * Main drawing area for the UML Diagram.
 * GoF: Observer (implements DiagramChangeListener)
 * GRASP: Controller (Facade / Boundary)
 */
public class MainCanvas extends Pane implements DiagramChangeListener {

    private static final double GRID_SIZE   = 20.0;
    private static final double CANVAS_W    = 3000;
    private static final double CANVAS_H    = 3000;
    private static final int    MAX_UNDO    = 30;

    private final MainWindow facade;
    /** Background canvas — grid + relationship lines + arrowheads */
    private final Canvas bgCanvas;
    /** Content pane — holds ClassNodeBox nodes, supports zoom/pan transforms */
    private final Pane contentPane;
    /** Zoom label overlay */
    private final Label zoomLabel;
    /** Empty-state placeholder */
    private final javafx.scene.layout.VBox placeholder;

    private UMLDiagram currentModel;
    private double zoomLevel = 1.0;

    // Pan state
    private boolean spacePanActive = false;
    private double panStartX, panStartY, panTranslateX, panTranslateY;

    // Rubber-band selection
    private double rbStartX, rbStartY;
    private Rectangle rubberBand;
    private boolean isRubberBanding = false;

    // Relationship drag
    private UMLClass dragSourceClass = null;
    private ClassNodeBox dragSourceBox = null;

    // Undo / Redo
    private final Deque<String> undoStack = new ArrayDeque<>();
    private final Deque<String> redoStack = new ArrayDeque<>();

    // Clipboard
    private UMLClass clipboardClass = null;

    // Context menu reference position
    private double ctxX = 0, ctxY = 0;

    public MainCanvas(MainWindow facade) {
        this.facade = facade;
        getStyleClass().add("main-canvas");

        bgCanvas     = new Canvas(CANVAS_W, CANVAS_H);
        contentPane  = new Pane();
        contentPane.setPrefSize(CANVAS_W, CANVAS_H);

        // Apply zoom via Scale transform on contentPane
        Scale scale = new Scale(1, 1, 0, 0);
        contentPane.getTransforms().add(scale);

        // Zoom label in bottom-right corner
        zoomLabel = new Label("100%");
        zoomLabel.setStyle("-fx-background-color: rgba(30,30,30,0.7); -fx-text-fill: #aaaaaa;"
                + "-fx-padding: 2 8; -fx-background-radius: 4; -fx-font-size: 11px;");
        StackPane.setAlignment(zoomLabel, javafx.geometry.Pos.BOTTOM_RIGHT);

        // Empty canvas placeholder
        placeholder = new javafx.scene.layout.VBox(8);
        placeholder.setAlignment(javafx.geometry.Pos.CENTER);
        Label ph1 = new Label("Your canvas is empty");
        ph1.getStyleClass().add("canvas-placeholder");
        Label ph2 = new Label("Drag a shape from the palette, or type a prompt in the AI chat to generate a diagram automatically.");
        ph2.getStyleClass().add("canvas-placeholder-sub");
        placeholder.getChildren().addAll(ph1, ph2);
        placeholder.setMouseTransparent(true);

        // Layout: bgCanvas + contentPane stacked
        getChildren().addAll(bgCanvas, contentPane, placeholder);

        // Bind canvas size to pane size for background
        bgCanvas.widthProperty().bind(widthProperty());
        bgCanvas.heightProperty().bind(heightProperty());
        bgCanvas.widthProperty().addListener((_obs, _o, _n) -> redrawGrid());
        bgCanvas.heightProperty().addListener((_obs, _o, _n) -> redrawGrid());

        getChildren().add(zoomLabel);

        setupInteraction(scale);
        setupDragDrop();
        setupContextMenu();
        redrawGrid();
    }

    // =====================================================================
    // GoF: Observer
    // =====================================================================
    @Override
    public void onDiagramChanged(UMLDiagram updated) {
        renderDiagram(updated);
    }

    // =====================================================================
    // Rendering
    // =====================================================================

    public void renderDiagram(UMLDiagram diagram) {
        this.currentModel = diagram;

        // Clear content pane (keep only nodes we're about to re-add)
        contentPane.getChildren().clear();

        clearBgCanvas();
        redrawGrid();

        boolean empty = (diagram == null || diagram.getClasses().isEmpty());
        placeholder.setVisible(empty);
        if (empty) return;

        // 1. Render class nodes
        if (diagram != null) {
            for (UMLClass cls : diagram.getClasses()) {
                ClassNodeBox box = new ClassNodeBox(cls, facade);
                box.setLayoutX(cls.getPositionX());
                box.setLayoutY(cls.getPositionY());

                // Apply selected style
                if (cls.isSelected()) box.getStyleClass().add("class-node-selected");

                attachNodeInteraction(box, cls);
                contentPane.getChildren().add(box);
                
                // Force CSS layout computation so bounds are immediately available for relationships!
                box.applyCss();
                box.layout();
            }
        }

        // 2. Draw relationships tightly around the instantiated nodes 
        if (diagram != null) drawRelationships(diagram);

        // Sync status bar
        facade.onCanvasUpdated(diagram);
    }

    // =====================================================================
    // Grid
    // =====================================================================

    private void redrawGrid() {
        GraphicsContext gc = bgCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, bgCanvas.getWidth(), bgCanvas.getHeight());
        gc.setStroke(Color.web("#2e2e2e", 0.6));
        gc.setLineWidth(0.5);

        double w = bgCanvas.getWidth();
        double h = bgCanvas.getHeight();
        double step = GRID_SIZE * zoomLevel;

        double ox = contentPane.getTranslateX() % step;
        double oy = contentPane.getTranslateY() % step;

        for (double x = ox; x < w; x += step) gc.strokeLine(x, 0, x, h);
        for (double y = oy; y < h; y += step) gc.strokeLine(0, y, w, y);
    }

    private void clearBgCanvas() {
        GraphicsContext gc = bgCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, bgCanvas.getWidth(), bgCanvas.getHeight());
    }

    // =====================================================================
    // Relationship drawing
    // =====================================================================

    private void drawRelationships(UMLDiagram diagram) {
        contentPane.getChildren().removeIf(node -> node.getStyleClass().contains("rel-label"));

        GraphicsContext gc = bgCanvas.getGraphicsContext2D();

        for (Relationship r : diagram.getRelationships()) {
            if (r.getSourceClass() == null || r.getTargetClass() == null) continue;
            UMLClass src = r.getSourceClass();
            UMLClass tgt = r.getTargetClass();

            ClassNodeBox srcBox = findBox(src);
            ClassNodeBox tgtBox = findBox(tgt);

            double[][] anchors = getBestAnchors(src, tgt, srcBox, tgtBox);
            double sx = anchors[0][0], sy = anchors[0][1];
            double tx = anchors[1][0], ty = anchors[1][1];

            // Scale to screen coords
            sx = sx * zoomLevel + contentPane.getTranslateX();
            sy = sy * zoomLevel + contentPane.getTranslateY();
            tx = tx * zoomLevel + contentPane.getTranslateX();
            ty = ty * zoomLevel + contentPane.getTranslateY();

            gc.save();
            gc.setStroke(Color.web("#a9b7c6"));
            gc.setLineWidth(1.5);

            RelationshipType type = r.getType();
            boolean dashed = (type == RelationshipType.REALIZATION || type == RelationshipType.DEPENDENCY);
            if (dashed) gc.setLineDashes(8, 4);

            Path path = buildOrthogonalPath(sx, sy, tx, ty);
            gc.beginPath();
            for (PathElement e : path.getElements()) {
                if (e instanceof MoveTo m) gc.moveTo(m.getX(), m.getY());
                else if (e instanceof LineTo l) gc.lineTo(l.getX(), l.getY());
            }
            gc.stroke();

            if (dashed) gc.setLineDashes(0);

            double midX = (sx + tx) / 2.0;
            drawArrowhead(gc, tx, ty, midX, ty, type);

            if (type == RelationshipType.COMPOSITION || type == RelationshipType.AGGREGATION) {
                drawDiamond(gc, sx, sy, midX, sy, type == RelationshipType.COMPOSITION);
            }

            // Relationship label at midpoint
            Label lbl = new Label(type.getDisplayName());
            lbl.getStyleClass().add("rel-label");
            lbl.setStyle("-fx-background-color: rgba(30,30,30,0.7); -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 12px; -fx-font-size: 11px;");
            lbl.setLayoutX(midX - 10);
            lbl.setLayoutY((sy + ty) / 2.0 - 10);
            contentPane.getChildren().add(lbl);

            // Multiplicities
            if (r.getSourceMultiplicity() != null && !r.getSourceMultiplicity().isEmpty()) {
                Label sLbl = new Label(r.getSourceMultiplicity());
                sLbl.getStyleClass().add("rel-label");
                sLbl.setStyle("-fx-text-fill: #999999; -fx-font-size: 10px;");
                sLbl.setLayoutX(sx + 8);
                sLbl.setLayoutY(sy - 15);
                contentPane.getChildren().add(sLbl);
            }
            if (r.getTargetMultiplicity() != null && !r.getTargetMultiplicity().isEmpty()) {
                Label tLbl = new Label(r.getTargetMultiplicity());
                tLbl.getStyleClass().add("rel-label");
                tLbl.setStyle("-fx-text-fill: #999999; -fx-font-size: 10px;");
                tLbl.setLayoutX(tx - 20);
                tLbl.setLayoutY(ty - 15);
                contentPane.getChildren().add(tLbl);
            }

            gc.restore();
        }
    }

    private Path buildOrthogonalPath(double sx, double sy, double tx, double ty) {
        Path path = new Path();
        double midX = (sx + tx) / 2.0;
        path.getElements().addAll(
            new MoveTo(sx, sy),
            new LineTo(midX, sy),
            new LineTo(midX, ty),
            new LineTo(tx, ty)
        );
        return path;
    }

    private ClassNodeBox findBox(UMLClass cls) {
        for (var node : contentPane.getChildren()) {
            if (node instanceof ClassNodeBox b && b.getUMLClass().getClassId() == cls.getClassId()) {
                return b;
            }
        }
        return null;
    }

    private double[][] getBestAnchors(UMLClass src, UMLClass tgt, ClassNodeBox srcBox, ClassNodeBox tgtBox) {
        double sw = srcBox != null ? srcBox.getWidth() : 170;
        double sh = srcBox != null ? srcBox.getHeight() : 60;
        double sX = srcBox != null ? srcBox.getLayoutX() : src.getPositionX();
        double sY = srcBox != null ? srcBox.getLayoutY() : src.getPositionY();
        if (sw == 0) sw = 170; if (sh == 0) sh = 60;

        double tw = tgtBox != null ? tgtBox.getWidth() : 170;
        double th = tgtBox != null ? tgtBox.getHeight() : 60;
        double tX = tgtBox != null ? tgtBox.getLayoutX() : tgt.getPositionX();
        double tY = tgtBox != null ? tgtBox.getLayoutY() : tgt.getPositionY();
        if (tw == 0) tw = 170; if (th == 0) th = 60;

        double scx = sX + sw / 2.0; double scy = sY + sh / 2.0;
        double[][] srcAnchors = { {scx, sY}, {scx, sY + sh}, {sX, scy}, {sX + sw, scy} };

        double tcx = tX + tw / 2.0; double tcy = tY + th / 2.0;
        double[][] tgtAnchors = { {tcx, tY}, {tcx, tY + th}, {tX, tcy}, {tX + tw, tcy} };

        double minDist = Double.MAX_VALUE;
        double[] bestSrc = srcAnchors[0];
        double[] bestTgt = tgtAnchors[0];

        for (double[] s : srcAnchors) {
            for (double[] t : tgtAnchors) {
                double dist = Math.hypot(s[0] - t[0], s[1] - t[1]);
                if (dist < minDist) {
                    minDist = dist;
                    bestSrc = s;
                    bestTgt = t;
                }
            }
        }
        return new double[][]{bestSrc, bestTgt};
    }

    private void drawArrowhead(GraphicsContext gc, double tipX, double tipY,
                                double fromX, double fromY, RelationshipType type) {
        double angle = Math.atan2(tipY - fromY, tipX - fromX);
        double len = 12;
        double spread = Math.toRadians(25);

        switch (type) {
            case INHERITANCE, REALIZATION -> {
                double lx1 = tipX - len * Math.cos(angle - spread);
                double ly1 = tipY - len * Math.sin(angle - spread);
                double lx2 = tipX - len * Math.cos(angle + spread);
                double ly2 = tipY - len * Math.sin(angle + spread);
                gc.setFill(Color.web("#1e1e2e"));
                gc.fillPolygon(new double[]{tipX, lx1, lx2}, new double[]{tipY, ly1, ly2}, 3);
                gc.setStroke(Color.web("#a9b7c6"));
                gc.setLineDashes(0);
                gc.strokePolygon(new double[]{tipX, lx1, lx2}, new double[]{tipY, ly1, ly2}, 3);
            }
            case ASSOCIATION, DEPENDENCY -> {
                double lx1 = tipX - len * Math.cos(angle - spread);
                double ly1 = tipY - len * Math.sin(angle - spread);
                double lx2 = tipX - len * Math.cos(angle + spread);
                double ly2 = tipY - len * Math.sin(angle + spread);
                gc.setStroke(Color.web("#a9b7c6"));
                gc.setLineDashes(0);
                gc.strokeLine(tipX, tipY, lx1, ly1);
                gc.strokeLine(tipX, tipY, lx2, ly2);
            }
            default -> {}
        }
    }

    private void drawDiamond(GraphicsContext gc, double baseX, double baseY,
                              double dirX, double dirY, boolean filled) {
        double angle = Math.atan2(dirY - baseY, dirX - baseX);
        double hw = 4, hl = 16;
        double perpAngle = angle + Math.PI / 2;

        double x0 = baseX;
        double y0 = baseY;
        double x1 = baseX + (hw * Math.cos(perpAngle));
        double y1 = baseY + (hw * Math.sin(perpAngle));
        double x2 = baseX + (hl * Math.cos(angle));
        double y2 = baseY + (hl * Math.sin(angle));
        double x3 = baseX - (hw * Math.cos(perpAngle));
        double y3 = baseY - (hw * Math.sin(perpAngle));

        if (filled) {
            gc.setFill(Color.web("#cc7832"));
            gc.fillPolygon(new double[]{x0, x1, x2, x3}, new double[]{y0, y1, y2, y3}, 4);
        } else {
            gc.setFill(Color.web("#1e1e2e"));
            gc.fillPolygon(new double[]{x0, x1, x2, x3}, new double[]{y0, y1, y2, y3}, 4);
        }
        gc.setStroke(Color.web("#a9b7c6"));
        gc.setLineDashes(0);
        gc.strokePolygon(new double[]{x0, x1, x2, x3}, new double[]{y0, y1, y2, y3}, 4);
    }

    // =====================================================================
    // Node interaction
    // =====================================================================

    private void attachNodeInteraction(ClassNodeBox box, UMLClass cls) {
        final double[] offset = {0, 0};
        final double[] startPos = {0, 0};

        box.setOnMousePressed(e -> {
            if (e.isShiftDown()) {
                dragSourceClass = cls;
                dragSourceBox   = box;
                e.consume();
            } else if (e.getButton() == MouseButton.PRIMARY) {
                offset[0] = box.getLayoutX() - e.getSceneX();
                offset[1] = box.getLayoutY() - e.getSceneY();
                startPos[0] = box.getLayoutX();
                startPos[1] = box.getLayoutY();
                // Deselect all others
                deselectAll();
                cls.setSelected(true);
                box.getStyleClass().add("class-node-selected");
            }
        });

        box.setOnMouseDragged(e -> {
            if (dragSourceClass != null && e.isShiftDown()) {
                // Draw preview line
                double bx = box.getLayoutX() + e.getX();
                double by = box.getLayoutY() + e.getY();
                drawPreviewLine(cls.getPositionX() + box.getWidth() / 2,
                        cls.getPositionY() + box.getHeight() / 2, bx, by);
                e.consume();
            } else if (e.getButton() == MouseButton.PRIMARY && !e.isShiftDown()) {
                double newX = e.getSceneX() + offset[0];
                double newY = e.getSceneY() + offset[1];
                box.setLayoutX(newX);
                box.setLayoutY(newY);
            }
        });

        box.setOnMouseReleased(e -> {
            if (dragSourceClass != null) {
                // Find target box under cursor
                UMLClass targetCls = findClassAt(e.getSceneX(), e.getSceneY(), box);
                if (targetCls != null && targetCls != cls) {
                    showRelationshipDialog(dragSourceClass, targetCls);
                } else {
                    renderDiagram(currentModel); // cancel preview
                }
                dragSourceClass = null;
                dragSourceBox   = null;
            } else if (e.getButton() == MouseButton.PRIMARY) {
                // Snap to grid
                double snappedX = snap(box.getLayoutX());
                double snappedY = snap(box.getLayoutY());
                box.setLayoutX(snappedX);
                box.setLayoutY(snappedY);
                cls.setPositionX(snappedX);
                cls.setPositionY(snappedY);
                pushUndo();
                facade.getDiagramController().saveCurrentDiagram();
                renderDiagram(currentModel);
            }
        });
    }

    private void drawPreviewLine(double sx, double sy, double tx, double ty) {
        clearBgCanvas();
        redrawGrid();
        if (currentModel != null) drawRelationships(currentModel);

        GraphicsContext gc = bgCanvas.getGraphicsContext2D();
        double[] sScreen = toScreen(sx, sy);
        double[] tScreen = toScreen(tx, ty);
        gc.save();
        gc.setStroke(Color.web("#06b6d4"));
        gc.setLineWidth(2);
        gc.setLineDashes(6, 3);
        gc.strokeLine(sScreen[0], sScreen[1], tScreen[0], tScreen[1]);
        gc.setLineDashes(0);
        gc.restore();
    }

    private double[] toScreen(double worldX, double worldY) {
        return new double[]{
            worldX * zoomLevel + contentPane.getTranslateX(),
            worldY * zoomLevel + contentPane.getTranslateY()
        };
    }

    private UMLClass findClassAt(double sceneX, double sceneY, ClassNodeBox exclude) {
        for (var node : contentPane.getChildren()) {
            if (node instanceof ClassNodeBox b && b != exclude) {
                Bounds bounds = b.localToScene(b.getBoundsInLocal());
                if (bounds.contains(sceneX, sceneY)) {
                    return b.getUMLClass();
                }
            }
        }
        return null;
    }

    private void showRelationshipDialog(UMLClass src, UMLClass tgt) {
        ChoiceDialog<RelationshipType> dlg = new ChoiceDialog<>(
                RelationshipType.ASSOCIATION,
                RelationshipType.values());
        dlg.setTitle("Select Relationship Type");
        dlg.setHeaderText("How is " + src.getName() + " related to " + tgt.getName() + "?");
        dlg.setContentText("Relationship:");
        Optional<RelationshipType> result = dlg.showAndWait();
        if (result.isPresent()) {
            Relationship rel = RelationshipFactory.createRelationship(result.get(), src, tgt);
            if (rel != null && currentModel != null) {
                pushUndo();
                currentModel.addRelationship(rel);
                facade.getDiagramController().saveCurrentDiagram();
                renderDiagram(currentModel);
            }
        } else {
            renderDiagram(currentModel);
        }
    }

    // =====================================================================
    // Canvas interaction (zoom, pan, rubber-band, keyboard)
    // =====================================================================

    private void setupInteraction(Scale scale) {
        // Zoom with Ctrl+Scroll
        setOnScroll(e -> {
            if (e.isControlDown()) {
                double delta = e.getDeltaY() > 0 ? 0.1 : -0.1;
                zoomLevel = Math.max(0.2, Math.min(3.0, zoomLevel + delta));
                scale.setX(zoomLevel);
                scale.setY(zoomLevel);
                updateZoomLabel();
                redrawGrid();
                if (currentModel != null) drawRelationships(currentModel);
                e.consume();
            }
        });

        // Space key for pan mode
        setFocusTraversable(true);
        setOnKeyPressed(e -> handleKeyPress(e));

        // Middle mouse drag pan
        final double[] mmStart = {0, 0, 0, 0};
        setOnMousePressed(e -> {
            requestFocus();
            if (e.getButton() == MouseButton.MIDDLE) {
                mmStart[0] = e.getSceneX();
                mmStart[1] = e.getSceneY();
                mmStart[2] = contentPane.getTranslateX();
                mmStart[3] = contentPane.getTranslateY();
                setCursor(Cursor.CLOSED_HAND);
            } else if (e.getButton() == MouseButton.PRIMARY && !spacePanActive) {
                // Start rubber-band
                rbStartX = e.getX();
                rbStartY = e.getY();
                isRubberBanding = false;
                deselectAll();
            } else if (spacePanActive && e.getButton() == MouseButton.PRIMARY) {
                panStartX = e.getSceneX();
                panStartY = e.getSceneY();
                panTranslateX = contentPane.getTranslateX();
                panTranslateY = contentPane.getTranslateY();
                setCursor(Cursor.CLOSED_HAND);
            }
        });

        setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.MIDDLE) {
                double dx = e.getSceneX() - mmStart[0];
                double dy = e.getSceneY() - mmStart[1];
                contentPane.setTranslateX(mmStart[2] + dx);
                contentPane.setTranslateY(mmStart[3] + dy);
                redrawGrid();
                if (currentModel != null) drawRelationships(currentModel);
            } else if (spacePanActive && e.getButton() == MouseButton.PRIMARY) {
                double dx = e.getSceneX() - panStartX;
                double dy = e.getSceneY() - panStartY;
                contentPane.setTranslateX(panTranslateX + dx);
                contentPane.setTranslateY(panTranslateY + dy);
                redrawGrid();
                if (currentModel != null) drawRelationships(currentModel);
            } else if (e.getButton() == MouseButton.PRIMARY && !spacePanActive && dragSourceClass == null) {
                // Rubber-band selection
                if (!isRubberBanding) {
                    isRubberBanding = true;
                    rubberBand = new Rectangle();
                    rubberBand.setFill(Color.web("#4fc3f7", 0.08));
                    rubberBand.setStroke(Color.web("#4fc3f7"));
                    rubberBand.setStrokeWidth(1);
                    rubberBand.getStrokeDashArray().addAll(5.0, 3.0);
                    rubberBand.setMouseTransparent(true);
                    getChildren().add(rubberBand);
                }
                double x = Math.min(e.getX(), rbStartX);
                double y = Math.min(e.getY(), rbStartY);
                double w = Math.abs(e.getX() - rbStartX);
                double h = Math.abs(e.getY() - rbStartY);
                rubberBand.setX(x); rubberBand.setY(y);
                rubberBand.setWidth(w); rubberBand.setHeight(h);
            }
        });

        setOnMouseReleased(e -> {
            setCursor(Cursor.DEFAULT);
            if (isRubberBanding && rubberBand != null) {
                applyRubberBandSelection(rubberBand.getX(), rubberBand.getY(),
                        rubberBand.getWidth(), rubberBand.getHeight());
                getChildren().remove(rubberBand);
                rubberBand = null;
                isRubberBanding = false;
            } else if (dragSourceClass != null) {
                dragSourceClass = null; dragSourceBox = null;
                renderDiagram(currentModel);
            }
        });
    }

    private void handleKeyPress(KeyEvent e) {
        if (e.getCode() == KeyCode.SPACE) {
            spacePanActive = true;
            setCursor(Cursor.OPEN_HAND);
        }
        if (e.getCode() == KeyCode.ESCAPE) {
            deselectAll();
            spacePanActive = false;
            setCursor(Cursor.DEFAULT);
        }
        if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) {
            deleteSelected();
        }
        if (e.isControlDown() && e.getCode() == KeyCode.A) {
            selectAll();
        }
        if (e.isControlDown() && e.getCode() == KeyCode.Z) {
            undo();
        }
        if (e.isControlDown() && e.getCode() == KeyCode.Y) {
            redo();
        }
        if (e.isControlDown() && e.getCode() == KeyCode.C) {
            copySelected();
        }
        if (e.isControlDown() && e.getCode() == KeyCode.V) {
            paste();
        }
        if (e.getCode() == KeyCode.SPACE && !e.isControlDown()) {
            // Prevent scroll on spacebar
            e.consume();
        }
    }

    // Space key release
    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        // Keep zoomLabel in bottom-right
        double lw = zoomLabel.prefWidth(-1);
        double lh = zoomLabel.prefHeight(-1);
        zoomLabel.relocate(getWidth() - lw - 8, getHeight() - lh - 8);

        // Centre placeholder
        placeholder.setPrefWidth(getWidth());
        placeholder.setLayoutX(0);
        placeholder.setLayoutY((getHeight() - 80) / 2);
    }

    // =====================================================================
    // Context menu on empty canvas
    // =====================================================================

    private void setupContextMenu() {
        ContextMenu ctx = new ContextMenu();

        MenuItem addClass = new MenuItem("Add Class");
        addClass.setOnAction(e -> addClassAt(ctxX, ctxY, false, false));

        MenuItem addInterface = new MenuItem("Add Interface");
        addInterface.setOnAction(e -> {
            UMLClass c = addClassAt(ctxX, ctxY, false, true);
            c.setName("NewInterface");
        });

        MenuItem addAbstract = new MenuItem("Add Abstract Class");
        addAbstract.setOnAction(e -> {
            UMLClass c = addClassAt(ctxX, ctxY, true, false);
            c.setName("NewAbstract");
        });

        MenuItem paste = new MenuItem("Paste");
        paste.setOnAction(e -> paste());

        ctx.getItems().addAll(addClass, addInterface, addAbstract, new SeparatorMenuItem(), paste);

        setOnContextMenuRequested(e -> {
            ctxX = toWorldX(e.getX());
            ctxY = toWorldY(e.getY());
            ctx.show(this, e.getScreenX(), e.getScreenY());
            e.consume();
        });
    }

    private double toWorldX(double screenX) {
        return (screenX - contentPane.getTranslateX()) / zoomLevel;
    }

    private double toWorldY(double screenY) {
        return (screenY - contentPane.getTranslateY()) / zoomLevel;
    }

    private UMLClass addClassAt(double x, double y, boolean isAbstract, boolean isInterface) {
        UMLClass c = new UMLClass();
        c.setName("NewClass");
        c.setPositionX(snap(x));
        c.setPositionY(snap(y));
        c.setAbstract(isAbstract);
        c.setInterface(isInterface);
        if (currentModel != null) {
            pushUndo();
            currentModel.addUMLClass(c);
            facade.getDiagramController().saveCurrentDiagram();
            renderDiagram(currentModel);
        }
        return c;
    }

    // =====================================================================
    // Drag & Drop from ShapePalettePanel
    // =====================================================================

    private void setupDragDrop() {
        setOnDragOver(e -> {
            if (e.getGestureSource() != this
                    && e.getDragboard().hasString()
                    && e.getDragboard().getString().startsWith("SHAPE:")) {
                e.acceptTransferModes(TransferMode.COPY);
            }
            e.consume();
        });

        setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            boolean success = false;
            if (db.hasString() && db.getString().startsWith("SHAPE:")) {
                String shapeType = db.getString().replace("SHAPE:", "");
                double x = snap(toWorldX(e.getX()));
                double y = snap(toWorldY(e.getY()));
                UMLClass newC = new UMLClass();
                newC.setPositionX(x);
                newC.setPositionY(y);

                switch (shapeType) {
                    case "CLASS"     -> newC.setName("NewClass");
                    case "ABSTRACT"  -> { newC.setName("NewAbstract"); newC.setAbstract(true); }
                    case "INTERFACE" -> { newC.setName("NewInterface"); newC.setInterface(true); }
                    case "ENUM"      -> { newC.setName("NewEnum"); newC.setStereotype("Enumeration"); }
                    case "NOTE"      -> { newC.setName("Write note here..."); newC.setStereotype("Note"); }
                    default          -> newC.setName("New" + shapeType);
                }

                if (currentModel != null) {
                    pushUndo();
                    currentModel.addUMLClass(newC);
                    facade.getDiagramController().saveCurrentDiagram();
                    renderDiagram(currentModel);
                    MainWindow.showToast("Shape added");
                } else {
                    MainWindow.showToast("Create or load a diagram first");
                }
                success = true;
            }
            e.setDropCompleted(success);
            e.consume();
        });
    }

    // =====================================================================
    // Selection
    // =====================================================================

    private void selectAll() {
        if (currentModel == null) return;
        currentModel.getClasses().forEach(c -> c.setSelected(true));
        renderDiagram(currentModel);
    }

    private void deselectAll() {
        if (currentModel == null) return;
        currentModel.getClasses().forEach(c -> c.setSelected(false));
        // Update visual without full re-render for performance
        contentPane.getChildren().forEach(n -> {
            if (n instanceof ClassNodeBox b) {
                b.getStyleClass().remove("class-node-selected");
            }
        });
    }

    private void applyRubberBandSelection(double rx, double ry, double rw, double rh) {
        if (currentModel == null) return;
        for (var node : contentPane.getChildren()) {
            if (node instanceof ClassNodeBox box) {
                Bounds b = box.getBoundsInParent();
                // Convert rubber-band to content-pane coords
                double rbx = (rx - contentPane.getTranslateX()) / zoomLevel;
                double rby = (ry - contentPane.getTranslateY()) / zoomLevel;
                double rbw = rw / zoomLevel;
                double rbh = rh / zoomLevel;
                if (b.getMinX() < rbx + rbw && b.getMaxX() > rbx
                        && b.getMinY() < rby + rbh && b.getMaxY() > rby) {
                    box.getUMLClass().setSelected(true);
                    if (!box.getStyleClass().contains("class-node-selected"))
                        box.getStyleClass().add("class-node-selected");
                }
            }
        }
    }

    private void deleteSelected() {
        if (currentModel == null) return;
        List<UMLClass> toDelete = currentModel.getClasses().stream()
                .filter(UMLClass::isSelected).toList();
        if (toDelete.isEmpty()) return;

        pushUndo();
        for (UMLClass cls : toDelete) {
            // Remove relationships referencing cls
            currentModel.getRelationships().stream()
                .filter(r -> (r.getSourceClass() != null && r.getSourceClass().getClassId() == cls.getClassId())
                          || (r.getTargetClass() != null && r.getTargetClass().getClassId() == cls.getClassId()))
                .toList()
                .forEach(r -> currentModel.removeRelationship(r.getRelationshipId()));
            currentModel.removeUMLClass(cls.getClassId());
        }
        facade.getDiagramController().saveCurrentDiagram();
        renderDiagram(currentModel);
        MainWindow.showToast("Deleted " + toDelete.size() + " node(s)");
    }

    private void copySelected() {
        if (currentModel == null) return;
        currentModel.getClasses().stream().filter(UMLClass::isSelected).findFirst()
                .ifPresent(c -> clipboardClass = c);
    }

    private void paste() {
        if (clipboardClass == null || currentModel == null) return;
        UMLClass copy = deepCopy(clipboardClass);
        copy.setName(copy.getName() + " Copy");
        copy.setPositionX(snap(copy.getPositionX() + 40));
        copy.setPositionY(snap(copy.getPositionY() + 40));
        pushUndo();
        currentModel.addUMLClass(copy);
        facade.getDiagramController().saveCurrentDiagram();
        renderDiagram(currentModel);
    }

    /** Deep-copy a UMLClass (attributes + methods). */
    private UMLClass deepCopy(UMLClass src) {
        UMLClass copy = new UMLClass(0, src.getName());
        copy.setAbstract(src.isAbstract());
        copy.setInterface(src.isInterface());
        copy.setStereotype(src.getStereotype());
        copy.setPositionX(src.getPositionX());
        copy.setPositionY(src.getPositionY());
        src.getAttributes().forEach(a -> {
            com.umlytics.domain.Attribute ac = new com.umlytics.domain.Attribute(
                    0, a.getName(), a.getType(), a.getVisibility(), a.isStatic());
            copy.addAttribute(ac);
        });
        src.getMethods().forEach(m -> {
            com.umlytics.domain.Method mc = new com.umlytics.domain.Method(
                    0, m.getName(), m.getReturnType(), m.getVisibility(), m.isAbstract());
            mc.setParameters(m.getParameters());
            copy.addMethod(mc);
        });
        return copy;
    }

    // =====================================================================
    // Undo / Redo
    // =====================================================================

    private void pushUndo() {
        if (currentModel == null) return;
        if (undoStack.size() >= MAX_UNDO) undoStack.pollFirst();
        undoStack.push(currentModel.serialize());
        redoStack.clear();
    }

    private void undo() {
        if (undoStack.isEmpty() || currentModel == null) return;
        redoStack.push(currentModel.serialize());
        String prev = undoStack.pop();
        restoreFromJson(prev);
        MainWindow.showToast("Undo");
    }

    private void redo() {
        if (redoStack.isEmpty() || currentModel == null) return;
        undoStack.push(currentModel.serialize());
        String next = redoStack.pop();
        restoreFromJson(next);
        MainWindow.showToast("Redo");
    }

    private void restoreFromJson(String json) {
        // Reload from database (most reliable since serialization loses type info)
        if (currentModel != null) {
            facade.getDiagramController().loadDiagram(currentModel.getDiagramId());
            renderDiagram(facade.getDiagramController().getCurrentDiagram());
        }
    }

    // =====================================================================
    // Fit to screen
    // =====================================================================

    public void fitToScreen() {
        if (currentModel == null || currentModel.getClasses().isEmpty()) return;

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;

        for (UMLClass c : currentModel.getClasses()) {
            minX = Math.min(minX, c.getPositionX());
            minY = Math.min(minY, c.getPositionY());
            maxX = Math.max(maxX, c.getPositionX() + 180);
            maxY = Math.max(maxY, c.getPositionY() + 60);
        }

        double diagramW = maxX - minX;
        double diagramH = maxY - minY;
        double availW   = getWidth()  * 0.9;
        double availH   = getHeight() * 0.9;

        zoomLevel = Math.min(availW / diagramW, availH / diagramH);
        zoomLevel = Math.max(0.2, Math.min(3.0, zoomLevel));

        Scale scale = (Scale) contentPane.getTransforms().get(0);
        scale.setX(zoomLevel);
        scale.setY(zoomLevel);

        // Centre it
        double centreX = (minX + maxX) / 2.0 * zoomLevel;
        double centreY = (minY + maxY) / 2.0 * zoomLevel;
        contentPane.setTranslateX(getWidth() / 2 - centreX);
        contentPane.setTranslateY(getHeight() / 2 - centreY);

        updateZoomLabel();
        redrawGrid();
        if (currentModel != null) drawRelationships(currentModel);
    }

    public void zoomIn() {
        zoomLevel = Math.min(3.0, zoomLevel + 0.1);
        Scale s = (Scale) contentPane.getTransforms().get(0);
        s.setX(zoomLevel); s.setY(zoomLevel);
        updateZoomLabel();
        redrawGrid();
    }

    public void zoomOut() {
        zoomLevel = Math.max(0.2, zoomLevel - 0.1);
        Scale s = (Scale) contentPane.getTransforms().get(0);
        s.setX(zoomLevel); s.setY(zoomLevel);
        updateZoomLabel();
        redrawGrid();
    }

    private void updateZoomLabel() {
        int pct = (int) Math.round(zoomLevel * 100);
        zoomLabel.setText(pct + "%");
        facade.onZoomChanged(zoomLevel);
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private double snap(double v) {
        return Math.round(v / GRID_SIZE) * GRID_SIZE;
    }

    /** Called by ClassNodeBox "Copy Class" menu item. */
    public void setClipboardClass(UMLClass cls) {
        this.clipboardClass = cls;
    }

    public double getZoomLevel() { return zoomLevel; }
}
