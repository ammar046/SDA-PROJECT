package com.umlytics.ui;

import com.umlytics.controllers.DiagramController;
import com.umlytics.domain.AssociationRelationship;
import com.umlytics.domain.Attribute;
import com.umlytics.domain.DependencyRelationship;
import com.umlytics.domain.DiagramEdit;
import com.umlytics.domain.InheritanceRelationship;
import com.umlytics.domain.Method;
import com.umlytics.domain.Relationship;
import com.umlytics.domain.UMLClass;
import com.umlytics.domain.UMLDiagram;
import com.umlytics.enums.Navigability;
import com.umlytics.enums.EditType;
import com.umlytics.enums.ExportFormat;
import com.umlytics.enums.RelationshipType;
import com.umlytics.enums.Visibility;
import com.umlytics.ui.canvas.ClassNode;
import com.umlytics.ui.canvas.DiagramCanvas;
import com.umlytics.ui.canvas.EditToolBar;
import com.umlytics.ui.canvas.RelationshipEdge;
import com.umlytics.ui.canvas.SelectionHandle;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class DiagramEditorPanel extends BorderPane {
    private final DiagramCanvas canvas;
    private final DiagramController diagramCtrl;
    private final EditToolBar toolBar;
    private UMLDiagram currentDiagram;
    private final Pane canvasLayer;
    private final VBox rightFormatPanel;
    private final Label statusZoomLabel;
    private final Label statusCursorLabel;
    private final Rectangle miniViewport;
    private final Rectangle selectionBox;
    private final Deque<DiagramEdit> undoStack = new ArrayDeque<>();
    private final Deque<DiagramEdit> redoStack = new ArrayDeque<>();
    private ToolMode toolMode = ToolMode.SELECT;
    private UMLClass pendingRelationshipSource;
    private final Map<UMLClass, ClassNode> nodeMap = new HashMap<>();
    private final Map<UMLClass, List<SelectionHandle>> handlesMap = new HashMap<>();
    private final Map<RelationshipEdge, RelationshipLink> links = new HashMap<>();
    private final Map<RelationshipEdge, Relationship> relationshipMap = new HashMap<>();
    private final Set<UMLClass> selectedClasses = new HashSet<>();
    private RelationshipEdge activeEdgeSelection;
    private int pageCounter = 1;
    private int tempClassId = -1;
    private double selectionStartX;
    private double selectionStartY;

    public DiagramEditorPanel() {
        this(null);
    }

    public DiagramEditorPanel(DiagramController diagramCtrl) {
        this.canvas = new DiagramCanvas();
        this.diagramCtrl = diagramCtrl;
        this.toolBar = new EditToolBar();
        this.currentDiagram = new UMLDiagram();
        this.canvasLayer = new Pane();
        this.rightFormatPanel = new VBox(8);
        this.statusZoomLabel = new Label("zoom: 100%");
        this.statusCursorLabel = new Label("cursor: 0,0");
        this.miniViewport = new Rectangle(24, 18, Color.TRANSPARENT);
        miniViewport.setStroke(Color.web("#0066ff"));
        this.selectionBox = new Rectangle();
        selectionBox.setStroke(Color.web("#1a73e8"));
        selectionBox.getStrokeDashArray().setAll(4.0, 4.0);
        selectionBox.setFill(Color.web("#1a73e8", 0.15));
        selectionBox.setVisible(false);
        selectionBox.setManaged(false);

        canvasLayer.getChildren().add(canvas);
        canvasLayer.getChildren().add(selectionBox);
        canvasLayer.setPrefSize(1600, 1000);
        canvasLayer.getStyleClass().add("diagram-canvas-container");
        canvasLayer.setOnMouseClicked(event -> requestFocus());

        VBox leftShapePanel = buildLeftShapePanel();
        leftShapePanel.getStyleClass().add("shape-panel");
        rightFormatPanel.getChildren().add(new TitledPane("Style", new VBox(new Label("Select an element to edit styles."))));
        rightFormatPanel.getStyleClass().add("format-panel");

        SplitPane splitPane = new SplitPane(leftShapePanel, buildCanvasCenter(), rightFormatPanel);
        splitPane.setDividerPositions(0.16, 0.82);

        setTop(toolBar);
        setCenter(splitPane);
        setBottom(buildBottomBar());
        wireToolbarActions();
        wireCanvasStatusUpdates();
        wireKeyboardShortcuts();
        wireSelectionBox();
    }

    public void onAddClass() {
        UMLClass umlClass = new UMLClass();
        umlClass.setClassId(tempClassId--);
        umlClass.setName("NewClass");
        umlClass.setPositionX(120 + currentDiagram.getClasses().size() * 30);
        umlClass.setPositionY(120 + currentDiagram.getClasses().size() * 30);
        umlClass.setHeaderColor("Blue");
        umlClass.setBorderColor("Blue");
        umlClass.setMemberFontSize(12);
        currentDiagram.addUMLClass(umlClass);
        DiagramEdit edit = new DiagramEdit();
        edit.setEditType(EditType.ADD_CLASS);
        edit.getPayload().put("name", umlClass.getName());
        undoStack.push(edit);
        redoStack.clear();
        addClassNode(umlClass);
        refreshMiniMap();
    }

    public void onAddRelationship() {
        toolMode = ToolMode.RELATIONSHIP;
        pendingRelationshipSource = null;
    }

    public void onSaveDiagram() {
        if (diagramCtrl != null) {
            diagramCtrl.saveDiagram(currentDiagram);
        }
    }

    public void onExport() {
        String base = currentDiagram.getTitle() == null || currentDiagram.getTitle().isBlank()
                ? "diagram"
                : currentDiagram.getTitle().replaceAll("[^a-zA-Z0-9-_]", "_");
        String path = "exports/" + base + ".png";
        if (diagramCtrl != null && currentDiagram.getDiagramId() > 0) {
            diagramCtrl.exportDiagram(currentDiagram.getDiagramId(), ExportFormat.PNG, path);
        } else {
            new com.umlytics.services.DiagramExportService().export(currentDiagram, ExportFormat.PNG, path);
        }
    }

    public void renderDiagram(UMLDiagram d) {
        this.currentDiagram = d;
        this.tempClassId = Math.min(-1, currentDiagram.getClasses().stream().mapToInt(UMLClass::getClassId).min().orElse(0) - 1);
        nodeMap.clear();
        links.clear();
        relationshipMap.clear();
        handlesMap.clear();
        canvasLayer.getChildren().setAll(canvas);
        for (UMLClass umlClass : currentDiagram.getClasses()) {
            addClassNode(umlClass);
        }
        for (Relationship relationship : currentDiagram.getRelationships()) {
            if (relationship.getSource() != null && relationship.getTarget() != null) {
                addVisualRelationship(relationship.getSource(), relationship.getTarget(), relationship.getType(), relationship);
            }
        }
        canvas.redraw();
        refreshMiniMap();
    }

    private void wireToolbarActions() {
        toolBar.getClassButton().setOnAction(event -> onAddClass());
        toolBar.getRelationshipButton().setOnAction(event -> onAddRelationship());
        toolBar.getUndoButton().setOnAction(event -> undo());
        toolBar.getRedoButton().setOnAction(event -> redo());
        toolBar.getSelectButton().setOnAction(event -> toolMode = ToolMode.SELECT);
        toolBar.getPanButton().setOnAction(event -> {
            toolMode = ToolMode.PAN;
            canvas.fitToWindow();
        });
    }

    private void addClassNode(UMLClass umlClass) {
        ClassNode node = new ClassNode(umlClass.getName());
        node.setMembers(umlClass.getAttributes(), umlClass.getMethods());
        node.setLayoutX(umlClass.getPositionX());
        node.setLayoutY(umlClass.getPositionY());
        node.setPrefSize(200, 140);
        node.setHeaderColor(parseClassHeaderColor(umlClass.getHeaderColor()));
        node.setBorderColor(parseClassBorderColor(umlClass.getBorderColor()));
        node.setMemberFontSize(umlClass.getMemberFontSize() <= 0 ? 12 : umlClass.getMemberFontSize());
        node.setRenameHandler(newName -> {
            String oldName = umlClass.getName();
            umlClass.setName(newName);
            DiagramEdit renameEdit = new DiagramEdit();
            renameEdit.setEditType(EditType.RENAME);
            renameEdit.setTargetClassId(umlClass.getClassId());
            renameEdit.getPayload().put("classRef", umlClass);
            renameEdit.getPayload().put("oldName", oldName);
            renameEdit.getPayload().put("name", newName);
            undoStack.push(renameEdit);
            redoStack.clear();
        });
        node.setAddAttributeHandler(() -> addAttributeToClass(umlClass, node));
        node.setAddMethodHandler(() -> addMethodToClass(umlClass, node));
        node.setEditAttributesTextHandler(text -> {
            String before = attributesToText(umlClass);
            parseAndApplyAttributes(umlClass, text);
            DiagramEdit edit = new DiagramEdit();
            edit.setEditType(EditType.EDIT_ATTR);
            edit.getPayload().put("classRef", umlClass);
            edit.getPayload().put("target", "attributes");
            edit.getPayload().put("before", before);
            edit.getPayload().put("after", text);
            undoStack.push(edit);
            redoStack.clear();
            node.setMembers(umlClass.getAttributes(), umlClass.getMethods());
        });
        node.setEditMethodsTextHandler(text -> {
            String before = methodsToText(umlClass);
            parseAndApplyMethods(umlClass, text);
            DiagramEdit edit = new DiagramEdit();
            edit.setEditType(EditType.EDIT_ATTR);
            edit.getPayload().put("classRef", umlClass);
            edit.getPayload().put("target", "methods");
            edit.getPayload().put("before", before);
            edit.getPayload().put("after", text);
            undoStack.push(edit);
            redoStack.clear();
            node.setMembers(umlClass.getAttributes(), umlClass.getMethods());
        });
        configureDrag(node, umlClass);

        node.setOnMouseClicked(event -> {
            if (toolMode == ToolMode.PAN) {
                return;
            }
            if (!event.isControlDown()) {
                clearSelections();
            }
            toggleClassSelection(umlClass);
            showClassFormatControls(umlClass);

            if (toolMode != ToolMode.RELATIONSHIP) {
                return;
            }
            if (pendingRelationshipSource == null) {
                pendingRelationshipSource = umlClass;
            } else {
                if (pendingRelationshipSource != umlClass) {
                    RelationshipType type = chooseRelationshipType();
                    if (type != null) {
                        addVisualRelationship(pendingRelationshipSource, umlClass, type, null);
                    }
                }
                toolMode = ToolMode.SELECT;
                pendingRelationshipSource = null;
            }
        });
        nodeMap.put(umlClass, node);
        canvasLayer.getChildren().add(node);
        updateSelectionHandles(umlClass);
    }

    private void addVisualRelationship(UMLClass source, UMLClass target, RelationshipType type, Relationship existingRelationship) {
        ClassNode s = nodeMap.get(source);
        ClassNode t = nodeMap.get(target);
        if (s == null || t == null) {
            return;
        }
        RelationshipEdge edge = new RelationshipEdge(
                centerX(s),
                centerY(s),
                centerX(t),
                centerY(t)
        );
        edge.setRelationshipType(type);
        edge.setMiddleLabel(type.name());
        edge.setOnMouseClicked(event -> {
            clearSelections();
            activeEdgeSelection = edge;
            edge.setSelected(true);
            showEdgeFormatControls(edge);
        });
        links.put(edge, new RelationshipLink(source, target, type));
        Relationship relationship = existingRelationship == null ? buildRelationship(type, source, target) : existingRelationship;
        relationship.setSourceClass(source);
        relationship.setTargetClass(target);
        edge.setMiddleLabel(relationship.getLabel() == null ? type.name() : relationship.getLabel());
        edge.setSourceMultiplicity(relationship.getSourceMultiplicity() == null ? "1" : relationship.getSourceMultiplicity());
        edge.setTargetMultiplicity(relationship.getTargetMultiplicity() == null ? "*" : relationship.getTargetMultiplicity());
        edge.setStrokeColor(parseEdgeColor(relationship.getEdgeColor()));
        edge.setLineStyleDashed(relationship.isDashed());
        if (relationship.getBendX() != null) {
            edge.setBendX(relationship.getBendX());
        } else {
            relationship.setBendX(edge.getBendX());
        }
        edge.setBendChangedHandler(value -> {
            Relationship rel = relationshipMap.get(edge);
            if (rel != null) {
                rel.setBendX(value);
            }
        });
        relationshipMap.put(edge, relationship);
        if (existingRelationship == null) {
            currentDiagram.addRelationship(relationship);
            DiagramEdit edit = new DiagramEdit();
            edit.setEditType(EditType.ADD_REL);
            edit.getPayload().put("source", source);
            edit.getPayload().put("target", target);
            edit.getPayload().put("type", type);
            edit.getPayload().put("relationship", relationship);
            undoStack.push(edit);
            redoStack.clear();
        }
        canvasLayer.getChildren().add(1, edge);
        refreshMiniMap();
    }

    private void undo() {
        if (undoStack.isEmpty()) {
            return;
        }
        DiagramEdit previous = undoStack.pop();
        redoStack.push(previous);
        if (previous.getEditType() == EditType.ADD_CLASS && !currentDiagram.getClasses().isEmpty()) {
            UMLClass last = currentDiagram.getClasses().get(currentDiagram.getClasses().size() - 1);
            currentDiagram.getClasses().remove(last);
            ClassNode node = nodeMap.remove(last);
            if (node != null) {
                canvasLayer.getChildren().remove(node);
            }
            List<SelectionHandle> handles = handlesMap.remove(last);
            if (handles != null) {
                canvasLayer.getChildren().removeAll(handles);
            }
            removeRelationshipsForClass(last);
            refreshMiniMap();
        } else if (previous.getEditType() == EditType.REMOVE_CLASS) {
            Object restored = previous.getPayload().get("classRef");
            if (restored instanceof UMLClass restoredClass) {
                currentDiagram.addUMLClass(restoredClass);
                addClassNode(restoredClass);
            }
        } else if (previous.getEditType() == EditType.RENAME) {
            Object classRef = previous.getPayload().get("classRef");
            Object oldName = previous.getPayload().get("oldName");
            if (classRef instanceof UMLClass umlClass && oldName instanceof String name) {
                umlClass.setName(name);
                ClassNode node = nodeMap.get(umlClass);
                if (node != null) {
                    node.setClassName(name);
                }
            }
        } else if (previous.getEditType() == EditType.EDIT_ATTR) {
            Object target = previous.getPayload().get("target");
            Object before = previous.getPayload().get("before");
            if (target instanceof String t && "edge".equals(t)) {
                Object edgeRef = previous.getPayload().get("edgeRef");
                if (edgeRef instanceof RelationshipEdge edge && before instanceof Map<?, ?> state) {
                    applyEdgeState(
                            edge,
                            (RelationshipType) state.get("type"),
                            (String) state.get("label"),
                            (String) state.get("sourceMultiplicity"),
                            (String) state.get("targetMultiplicity"),
                            Boolean.TRUE.equals(state.get("dashed")),
                            (String) state.get("color"),
                            ((Number) state.get("bendX")).doubleValue()
                    );
                }
            } else {
                Object classRef = previous.getPayload().get("classRef");
                if (classRef instanceof UMLClass umlClass && target instanceof String t2 && before instanceof String text) {
                    if ("attributes".equals(t2)) {
                        parseAndApplyAttributes(umlClass, text);
                    } else {
                        parseAndApplyMethods(umlClass, text);
                    }
                    ClassNode node = nodeMap.get(umlClass);
                    if (node != null) {
                        node.setMembers(umlClass.getAttributes(), umlClass.getMethods());
                    }
                }
            }
        } else if (previous.getEditType() == EditType.ADD_REL) {
            Object relationshipObj = previous.getPayload().get("relationship");
            if (relationshipObj instanceof Relationship relationship) {
                RelationshipEdge edge = findEdgeByRelationship(relationship);
                if (edge != null) {
                    removeEdgeInstance(edge, true);
                }
            }
        } else if (previous.getEditType() == EditType.REMOVE_REL) {
            Object source = previous.getPayload().get("source");
            Object target = previous.getPayload().get("target");
            Object type = previous.getPayload().get("type");
            Object relationshipObj = previous.getPayload().get("relationship");
            if (source instanceof UMLClass s && target instanceof UMLClass t && type instanceof RelationshipType rt) {
                addVisualRelationship(s, t, rt, relationshipObj instanceof Relationship r ? r : null);
            }
        }
    }

    private void redo() {
        if (redoStack.isEmpty()) {
            return;
        }
        DiagramEdit edit = redoStack.pop();
        undoStack.push(edit);
        if (edit.getEditType() == EditType.ADD_CLASS) {
            UMLClass umlClass = new UMLClass();
            umlClass.setClassId(tempClassId--);
            Object name = edit.getPayload().get("name");
            umlClass.setName(name == null ? "NewClass" : name.toString());
            umlClass.setPositionX(120 + currentDiagram.getClasses().size() * 30);
            umlClass.setPositionY(120 + currentDiagram.getClasses().size() * 30);
            currentDiagram.addUMLClass(umlClass);
            addClassNode(umlClass);
            refreshMiniMap();
        } else if (edit.getEditType() == EditType.RENAME) {
            Object classRef = edit.getPayload().get("classRef");
            Object newName = edit.getPayload().get("name");
            if (classRef instanceof UMLClass umlClass && newName instanceof String name) {
                umlClass.setName(name);
                ClassNode node = nodeMap.get(umlClass);
                if (node != null) {
                    node.setClassName(name);
                }
            }
        } else if (edit.getEditType() == EditType.EDIT_ATTR) {
            Object target = edit.getPayload().get("target");
            Object after = edit.getPayload().get("after");
            if (target instanceof String t && "edge".equals(t)) {
                Object edgeRef = edit.getPayload().get("edgeRef");
                if (edgeRef instanceof RelationshipEdge edge && after instanceof Map<?, ?> state) {
                    applyEdgeState(
                            edge,
                            (RelationshipType) state.get("type"),
                            (String) state.get("label"),
                            (String) state.get("sourceMultiplicity"),
                            (String) state.get("targetMultiplicity"),
                            Boolean.TRUE.equals(state.get("dashed")),
                            (String) state.get("color"),
                            ((Number) state.get("bendX")).doubleValue()
                    );
                }
            } else {
                Object classRef = edit.getPayload().get("classRef");
                if (classRef instanceof UMLClass umlClass && target instanceof String t2 && after instanceof String text) {
                    if ("attributes".equals(t2)) {
                        parseAndApplyAttributes(umlClass, text);
                    } else {
                        parseAndApplyMethods(umlClass, text);
                    }
                    ClassNode node = nodeMap.get(umlClass);
                    if (node != null) {
                        node.setMembers(umlClass.getAttributes(), umlClass.getMethods());
                    }
                }
            }
        } else if (edit.getEditType() == EditType.ADD_REL) {
            Object source = edit.getPayload().get("source");
            Object target = edit.getPayload().get("target");
            Object type = edit.getPayload().get("type");
            Object relationshipObj = edit.getPayload().get("relationship");
            if (source instanceof UMLClass s && target instanceof UMLClass t && type instanceof RelationshipType rt) {
                addVisualRelationship(s, t, rt, relationshipObj instanceof Relationship r ? r : null);
            }
        } else if (edit.getEditType() == EditType.REMOVE_REL) {
            Object relationshipObj = edit.getPayload().get("relationship");
            if (relationshipObj instanceof Relationship relationship) {
                RelationshipEdge edge = findEdgeByRelationship(relationship);
                if (edge != null) {
                    removeEdgeInstance(edge, true);
                }
            }
        }
    }

    private VBox buildLeftShapePanel() {
        TextField searchField = new TextField();
        searchField.setPromptText("Search shapes");
        Button classShape = new Button("Class");
        classShape.setMaxWidth(Double.MAX_VALUE);
        classShape.setOnAction(event -> onAddClass());
        Button interfaceShape = new Button("Interface");
        interfaceShape.setMaxWidth(Double.MAX_VALUE);
        interfaceShape.setOnAction(event -> onAddClass());
        Button noteShape = new Button("Note");
        noteShape.setMaxWidth(Double.MAX_VALUE);
        VBox umlBox = new VBox(6, classShape, interfaceShape, noteShape);
        TitledPane umlPane = new TitledPane("UML", umlBox);
        umlPane.setCollapsible(false);
        VBox panel = new VBox(8, searchField, umlPane);
        panel.setPadding(new Insets(8));
        return panel;
    }

    private Node buildCanvasCenter() {
        Node miniMap = buildMiniMap();
        StackPane stack = new StackPane(canvasLayer, miniMap);
        StackPane.setAlignment(miniMap, Pos.BOTTOM_RIGHT);
        return stack;
    }

    private Node buildMiniMap() {
        Pane miniMap = new Pane();
        miniMap.setPrefSize(110, 88);
        miniMap.setMaxSize(110, 88);
        miniMap.setBackground(new Background(new BackgroundFill(Color.web("#f8f8f8"), new CornerRadii(4), Insets.EMPTY)));
        miniMap.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1px;");
        miniViewport.setLayoutX(6);
        miniViewport.setLayoutY(6);
        miniMap.getChildren().add(miniViewport);
        miniMap.setOnMouseClicked(event -> {
            miniViewport.setLayoutX(Math.max(0, Math.min(86, event.getX() - miniViewport.getWidth() / 2)));
            miniViewport.setLayoutY(Math.max(0, Math.min(70, event.getY() - miniViewport.getHeight() / 2)));
        });
        StackPane.setAlignment(miniMap, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(miniMap, new Insets(0, 12, 12, 0));
        return miniMap;
    }

    private Node buildBottomBar() {
        Button addPageButton = new Button("+");
        HBox pageTabs = new HBox(6, addPageButton, new Label("1"));
        addPageButton.setOnAction(event -> {
            pageCounter++;
            pageTabs.getChildren().add(new Label(String.valueOf(pageCounter)));
        });

        HBox status = new HBox(18, new Label("Page:"), pageTabs, statusZoomLabel, statusCursorLabel);
        status.setPadding(new Insets(6, 10, 6, 10));
        status.setAlignment(Pos.CENTER_LEFT);
        status.setStyle("-fx-border-color: #d0d0d0; -fx-border-width: 1 0 0 0;");
        return status;
    }

    private void wireCanvasStatusUpdates() {
        canvas.setOnMouseMoved(event -> {
            statusCursorLabel.setText("cursor: " + (int) event.getX() + "," + (int) event.getY());
            statusZoomLabel.setText("zoom: " + (int) Math.round(canvas.getZoom() * 100) + "%");
        });
    }

    private void wireSelectionBox() {
        canvasLayer.setOnMousePressed(event -> {
            if (toolMode != ToolMode.SELECT) {
                return;
            }
            if (!(event.getTarget() == canvas || event.getTarget() == canvasLayer)) {
                return;
            }
            clearSelections();
            selectionStartX = event.getX();
            selectionStartY = event.getY();
            selectionBox.setX(selectionStartX);
            selectionBox.setY(selectionStartY);
            selectionBox.setWidth(0);
            selectionBox.setHeight(0);
            selectionBox.setVisible(true);
        });

        canvasLayer.setOnMouseDragged(event -> {
            if (toolMode != ToolMode.SELECT || !selectionBox.isVisible()) {
                return;
            }
            double x = Math.min(selectionStartX, event.getX());
            double y = Math.min(selectionStartY, event.getY());
            double width = Math.abs(event.getX() - selectionStartX);
            double height = Math.abs(event.getY() - selectionStartY);
            selectionBox.setX(x);
            selectionBox.setY(y);
            selectionBox.setWidth(width);
            selectionBox.setHeight(height);
        });

        canvasLayer.setOnMouseReleased(event -> {
            if (toolMode != ToolMode.SELECT || !selectionBox.isVisible()) {
                return;
            }
            selectClassesInBox(selectionBox.getX(), selectionBox.getY(), selectionBox.getWidth(), selectionBox.getHeight());
            selectionBox.setVisible(false);
        });
    }

    private void wireKeyboardShortcuts() {
        setFocusTraversable(true);
        sceneProperty().addListener((obs, oldScene, scene) -> {
            if (scene == null) {
                return;
            }
            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN), this::undo);
            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN), this::redo);
            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.H, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN), canvas::fitToWindow);
            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.A, KeyCombination.CONTROL_DOWN), this::selectAllClasses);
        });
        setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE) {
                if (activeEdgeSelection != null) {
                    deleteSelectedEdge();
                } else {
                    deleteSelectedClasses();
                }
            }
        });
    }

    private void configureDrag(ClassNode node, UMLClass umlClass) {
        final double[] drag = new double[2];
        final Map<UMLClass, double[]> initialPositions = new HashMap<>();
        node.setOnMousePressed(event -> {
            if (toolMode == ToolMode.PAN) {
                return;
            }
            drag[0] = event.getSceneX() - node.getLayoutX();
            drag[1] = event.getSceneY() - node.getLayoutY();
            initialPositions.clear();
            if (selectedClasses.contains(umlClass) && selectedClasses.size() > 1) {
                for (UMLClass selected : selectedClasses) {
                    ClassNode selectedNode = nodeMap.get(selected);
                    if (selectedNode != null) {
                        initialPositions.put(selected, new double[]{selectedNode.getLayoutX(), selectedNode.getLayoutY()});
                    }
                }
            } else {
                initialPositions.put(umlClass, new double[]{node.getLayoutX(), node.getLayoutY()});
            }
        });
        node.setOnMouseDragged(event -> {
            if (toolMode == ToolMode.PAN) {
                return;
            }
            double primaryX = snap(event.getSceneX() - drag[0]);
            double primaryY = snap(event.getSceneY() - drag[1]);
            double deltaX = primaryX - initialPositions.get(umlClass)[0];
            double deltaY = primaryY - initialPositions.get(umlClass)[1];

            for (Map.Entry<UMLClass, double[]> entry : initialPositions.entrySet()) {
                UMLClass movingClass = entry.getKey();
                double[] original = entry.getValue();
                ClassNode movingNode = nodeMap.get(movingClass);
                if (movingNode == null) {
                    continue;
                }
                double snappedX = snap(original[0] + deltaX);
                double snappedY = snap(original[1] + deltaY);
                movingNode.setLayoutX(snappedX);
                movingNode.setLayoutY(snappedY);
                movingClass.setPositionX(snappedX);
                movingClass.setPositionY(snappedY);
                updateSelectionHandles(movingClass);
                updateRelationshipsForClass(movingClass);
            }
            refreshMiniMap();
        });
    }

    private double snap(double value) {
        return Math.round(value / 10.0) * 10.0;
    }

    private void toggleClassSelection(UMLClass umlClass) {
        ClassNode node = nodeMap.get(umlClass);
        if (node == null) {
            return;
        }
        if (selectedClasses.contains(umlClass)) {
            selectedClasses.remove(umlClass);
            node.setSelected(false);
        } else {
            selectedClasses.add(umlClass);
            node.setSelected(true);
        }
        updateSelectionHandles(umlClass);
    }

    private void clearSelections() {
        for (UMLClass selected : selectedClasses) {
            ClassNode node = nodeMap.get(selected);
            if (node != null) {
                node.setSelected(false);
            }
            updateSelectionHandles(selected);
        }
        selectedClasses.clear();
        if (activeEdgeSelection != null) {
            activeEdgeSelection.setSelected(false);
        }
        activeEdgeSelection = null;
    }

    private void updateSelectionHandles(UMLClass umlClass) {
        List<SelectionHandle> old = handlesMap.remove(umlClass);
        if (old != null) {
            canvasLayer.getChildren().removeAll(old);
        }
        if (!selectedClasses.contains(umlClass)) {
            return;
        }
        ClassNode node = nodeMap.get(umlClass);
        if (node == null) {
            return;
        }
        double x = node.getLayoutX();
        double y = node.getLayoutY();
        double w = nodeWidth(node);
        double h = nodeHeight(node);
        List<SelectionHandle> handles = List.of(
                new SelectionHandle(x - 4, y - 4),
                new SelectionHandle(x + w / 2 - 4, y - 4),
                new SelectionHandle(x + w - 4, y - 4),
                new SelectionHandle(x - 4, y + h / 2 - 4),
                new SelectionHandle(x + w - 4, y + h / 2 - 4),
                new SelectionHandle(x - 4, y + h - 4),
                new SelectionHandle(x + w / 2 - 4, y + h - 4),
                new SelectionHandle(x + w - 4, y + h - 4)
        );
        attachResizeHandlers(umlClass, node, handles, x, y, w, h);
        handlesMap.put(umlClass, handles);
        canvasLayer.getChildren().addAll(handles);
    }

    private void updateRelationshipsForClass(UMLClass umlClass) {
        for (Map.Entry<RelationshipEdge, RelationshipLink> entry : links.entrySet()) {
            RelationshipLink link = entry.getValue();
            if (link.source != umlClass && link.target != umlClass) {
                continue;
            }
            ClassNode sourceNode = nodeMap.get(link.source);
            ClassNode targetNode = nodeMap.get(link.target);
            if (sourceNode == null || targetNode == null) {
                continue;
            }
            entry.getKey().update(
                    centerX(sourceNode),
                    centerY(sourceNode),
                    centerX(targetNode),
                    centerY(targetNode)
            );
        }
    }

    private void removeRelationshipsForClass(UMLClass umlClass) {
        Set<RelationshipEdge> toRemove = new HashSet<>();
        for (Map.Entry<RelationshipEdge, RelationshipLink> entry : links.entrySet()) {
            if (entry.getValue().source == umlClass || entry.getValue().target == umlClass) {
                toRemove.add(entry.getKey());
            }
        }
        toRemove.forEach(edge -> removeEdgeInstance(edge, false));
    }

    private void deleteSelectedClasses() {
        if (selectedClasses.isEmpty()) {
            return;
        }
        Set<UMLClass> toDelete = new HashSet<>(selectedClasses);
        clearSelections();
        for (UMLClass umlClass : toDelete) {
            DiagramEdit deleteEdit = new DiagramEdit();
            deleteEdit.setEditType(EditType.REMOVE_CLASS);
            deleteEdit.getPayload().put("classRef", umlClass);
            undoStack.push(deleteEdit);
            redoStack.clear();

            currentDiagram.getClasses().remove(umlClass);
            ClassNode node = nodeMap.remove(umlClass);
            if (node != null) {
                canvasLayer.getChildren().remove(node);
            }
            List<SelectionHandle> handles = handlesMap.remove(umlClass);
            if (handles != null) {
                canvasLayer.getChildren().removeAll(handles);
            }
            removeRelationshipsForClass(umlClass);
        }
        refreshMiniMap();
    }

    private void deleteSelectedEdge() {
        if (activeEdgeSelection == null) {
            return;
        }
        Relationship relationship = relationshipMap.get(activeEdgeSelection);
        RelationshipLink link = links.get(activeEdgeSelection);
        if (relationship != null && link != null) {
            DiagramEdit edit = new DiagramEdit();
            edit.setEditType(EditType.REMOVE_REL);
            edit.getPayload().put("relationship", relationship);
            edit.getPayload().put("source", link.source);
            edit.getPayload().put("target", link.target);
            edit.getPayload().put("type", link.type);
            undoStack.push(edit);
            redoStack.clear();
        }
        removeEdgeInstance(activeEdgeSelection, true);
        activeEdgeSelection = null;
        refreshMiniMap();
    }

    private void removeEdgeInstance(RelationshipEdge edge, boolean removeFromDiagram) {
        Relationship relationship = relationshipMap.remove(edge);
        if (removeFromDiagram && relationship != null) {
            currentDiagram.getRelationships().remove(relationship);
        }
        links.remove(edge);
        canvasLayer.getChildren().remove(edge);
    }

    private RelationshipEdge findEdgeByRelationship(Relationship relationship) {
        for (Map.Entry<RelationshipEdge, Relationship> entry : relationshipMap.entrySet()) {
            if (entry.getValue() == relationship) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void showClassFormatControls(UMLClass umlClass) {
        TextField xField = new TextField(String.valueOf((int) umlClass.getPositionX()));
        TextField yField = new TextField(String.valueOf((int) umlClass.getPositionY()));
        xField.setPromptText("X");
        yField.setPromptText("Y");
        ClassNode classNode = nodeMap.get(umlClass);
        ComboBox<String> headerColorBox = new ComboBox<>();
        headerColorBox.getItems().addAll("Blue", "Green", "Orange", "White");
        headerColorBox.setValue(classNode == null ? "Blue" : colorNameFromClassColor(classNode.getHeaderColor()));
        ComboBox<String> borderColorBox = new ComboBox<>();
        borderColorBox.getItems().addAll("Blue", "Black", "Gray");
        borderColorBox.setValue(classNode == null ? "Blue" : colorNameFromClassBorder(classNode.getBorderColor()));
        ComboBox<String> fontSizeBox = new ComboBox<>();
        fontSizeBox.getItems().addAll("10", "12", "14", "16");
        fontSizeBox.setValue(classNode == null ? "12" : String.valueOf((int) classNode.getMemberFontSize()));
        Button apply = new Button("Apply Position");
        apply.setOnAction(event -> {
            try {
                umlClass.setPositionX(snap(Double.parseDouble(xField.getText())));
                umlClass.setPositionY(snap(Double.parseDouble(yField.getText())));
                ClassNode node = nodeMap.get(umlClass);
                if (node != null) {
                    node.setLayoutX(umlClass.getPositionX());
                    node.setLayoutY(umlClass.getPositionY());
                    node.setHeaderColor(parseClassHeaderColor(headerColorBox.getValue()));
                    node.setBorderColor(parseClassBorderColor(borderColorBox.getValue()));
                    node.setMemberFontSize(Double.parseDouble(fontSizeBox.getValue()));
                }
                umlClass.setHeaderColor(headerColorBox.getValue());
                umlClass.setBorderColor(borderColorBox.getValue());
                umlClass.setMemberFontSize(Double.parseDouble(fontSizeBox.getValue()));
                updateSelectionHandles(umlClass);
                updateRelationshipsForClass(umlClass);
                refreshMiniMap();
            } catch (NumberFormatException ignored) {
                // Ignore invalid input in quick controls.
            }
        });
        ComboBox<String> kind = new ComboBox<>();
        kind.getItems().addAll("Class", "Interface", "Abstract");
        kind.setValue(umlClass.isInterface() ? "Interface" : umlClass.isAbstract() ? "Abstract" : "Class");
        kind.setOnAction(event -> {
            umlClass.setInterface("Interface".equals(kind.getValue()));
            umlClass.setAbstract("Abstract".equals(kind.getValue()));
        });
        Button toFront = new Button("To Front");
        toFront.setOnAction(event -> {
            ClassNode node = nodeMap.get(umlClass);
            if (node != null) {
                node.toFront();
            }
        });
        Button toBack = new Button("To Back");
        toBack.setOnAction(event -> {
            ClassNode node = nodeMap.get(umlClass);
            if (node != null) {
                node.toBack();
                canvas.toBack();
            }
        });
        HBox orderButtons = new HBox(6, toFront, toBack);
        VBox box = new VBox(6, new Label("Class Style"), kind, headerColorBox, borderColorBox, fontSizeBox, xField, yField, apply, orderButtons);
        rightFormatPanel.getChildren().setAll(new TitledPane("Arrange", box));
    }

    private void showEdgeFormatControls(RelationshipEdge edge) {
        TextField labelField = new TextField(edge.getMiddleLabel());
        TextField sourceMult = new TextField("1");
        TextField targetMult = new TextField("*");
        ComboBox<RelationshipType> relationshipTypeBox = new ComboBox<>();
        relationshipTypeBox.getItems().addAll(RelationshipType.values());
        relationshipTypeBox.setValue(edge.getRelationshipType());
        ComboBox<String> lineStyleBox = new ComboBox<>();
        lineStyleBox.getItems().addAll("Solid", "Dashed");
        lineStyleBox.setValue(edge.isDashed() ? "Dashed" : "Solid");
        ComboBox<String> colorBox = new ComboBox<>();
        colorBox.getItems().addAll("Black", "Blue", "Red", "Green");
        colorBox.setValue(colorNameFromColor(edge.getStrokeColor()));
        Button apply = new Button("Apply Edge Format");
        apply.setOnAction(event -> {
            Map<String, Object> before = captureEdgeState(edge);
            applyEdgeState(edge, relationshipTypeBox.getValue(), labelField.getText(), sourceMult.getText(),
                    targetMult.getText(), "Dashed".equals(lineStyleBox.getValue()), colorBox.getValue(), edge.getBendX());
            Map<String, Object> after = captureEdgeState(edge);
            DiagramEdit edit = new DiagramEdit();
            edit.setEditType(EditType.EDIT_ATTR);
            edit.getPayload().put("target", "edge");
            edit.getPayload().put("edgeRef", edge);
            edit.getPayload().put("before", before);
            edit.getPayload().put("after", after);
            undoStack.push(edit);
            redoStack.clear();
        });
        VBox box = new VBox(6, new Label("Connection"), relationshipTypeBox, labelField, sourceMult, targetMult, lineStyleBox, colorBox, apply);
        rightFormatPanel.getChildren().setAll(new TitledPane("Connection", box));
    }

    private void refreshMiniMap() {
        double coverage = Math.min(1.0, Math.max(0.2, 1.0 / Math.max(1.0, canvas.getZoom())));
        miniViewport.setWidth(100 * coverage * 0.6);
        miniViewport.setHeight(80 * coverage * 0.6);
        statusZoomLabel.setText("zoom: " + (int) Math.round(canvas.getZoom() * 100) + "%");
    }

    private Relationship buildRelationship(RelationshipType type, UMLClass source, UMLClass target) {
        Relationship relationship;
        switch (type) {
            case INHERITANCE -> relationship = new InheritanceRelationship();
            case REALIZATION -> {
                InheritanceRelationship rel = new InheritanceRelationship();
                rel.setInterface(true);
                relationship = rel;
            }
            case DEPENDENCY -> relationship = new DependencyRelationship();
            case COMPOSITION -> {
                AssociationRelationship rel = new AssociationRelationship();
                rel.setComposition(true);
                rel.setNavigability(Navigability.BIDIRECTIONAL);
                relationship = rel;
            }
            case AGGREGATION -> {
                AssociationRelationship rel = new AssociationRelationship();
                rel.setAggregation(true);
                rel.setNavigability(Navigability.BIDIRECTIONAL);
                relationship = rel;
            }
            default -> {
                AssociationRelationship rel = new AssociationRelationship();
                rel.setNavigability(Navigability.BIDIRECTIONAL);
                relationship = rel;
            }
        }
        relationship.setSourceClass(source);
        relationship.setTargetClass(target);
        relationship.setSourceMultiplicity("1");
        relationship.setTargetMultiplicity("*");
        relationship.setLabel(type.name());
        relationship.setEdgeColor("Black");
        relationship.setDashed(false);
        return relationship;
    }

    private RelationshipType chooseRelationshipType() {
        ChoiceDialog<RelationshipType> dialog = new ChoiceDialog<>(RelationshipType.ASSOCIATION, RelationshipType.values());
        dialog.setHeaderText("Choose relationship type");
        dialog.setContentText("Type:");
        Optional<RelationshipType> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private void addAttributeToClass(UMLClass umlClass, ClassNode node) {
        Attribute attribute = new Attribute();
        attribute.setName("attribute" + (umlClass.getAttributes().size() + 1));
        attribute.setType("String");
        attribute.setVisibility(Visibility.PRIVATE);
        umlClass.addAttribute(attribute);
        node.setMembers(umlClass.getAttributes(), umlClass.getMethods());
    }

    private void addMethodToClass(UMLClass umlClass, ClassNode node) {
        Method method = new Method();
        method.setName("method" + (umlClass.getMethods().size() + 1));
        method.setReturnType("void");
        method.setVisibility(Visibility.PUBLIC);
        umlClass.addMethod(method);
        node.setMembers(umlClass.getAttributes(), umlClass.getMethods());
    }

    private void parseAndApplyAttributes(UMLClass umlClass, String text) {
        umlClass.getAttributes().clear();
        if (text == null || text.isBlank()) {
            return;
        }
        String[] lines = text.split("\\n");
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            String visibilityToken = line.substring(0, 1);
            String body = line.length() > 1 ? line.substring(1).trim() : "";
            String[] parts = body.split(":");
            String name = parts[0].trim();
            String type = parts.length > 1 ? parts[1].trim() : "String";
            Attribute attribute = new Attribute();
            attribute.setName(name);
            attribute.setType(type);
            attribute.setVisibility(parseVisibility(visibilityToken));
            umlClass.addAttribute(attribute);
        }
    }

    private void parseAndApplyMethods(UMLClass umlClass, String text) {
        umlClass.getMethods().clear();
        if (text == null || text.isBlank()) {
            return;
        }
        String[] lines = text.split("\\n");
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            String visibilityToken = line.substring(0, 1);
            String body = line.length() > 1 ? line.substring(1).trim() : "";
            String[] nameReturn = body.split(":");
            String signature = nameReturn[0].trim();
            String returnType = nameReturn.length > 1 ? nameReturn[1].trim() : "void";
            String methodName = signature.contains("(") ? signature.substring(0, signature.indexOf("(")).trim() : signature;
            Method method = new Method();
            method.setName(methodName);
            method.setReturnType(returnType);
            method.setVisibility(parseVisibility(visibilityToken));
            umlClass.addMethod(method);
        }
    }

    private String attributesToText(UMLClass umlClass) {
        return umlClass.getAttributes().stream()
                .map(a -> visibilityToToken(a.getVisibility()) + " " + a.getName() + ": " + a.getType())
                .collect(Collectors.joining("\n"));
    }

    private String methodsToText(UMLClass umlClass) {
        return umlClass.getMethods().stream()
                .map(m -> visibilityToToken(m.getVisibility()) + " " + m.getName() + "(): " + m.getReturnType())
                .collect(Collectors.joining("\n"));
    }

    private Visibility parseVisibility(String token) {
        return switch (token) {
            case "+" -> Visibility.PUBLIC;
            case "#" -> Visibility.PROTECTED;
            case "-" -> Visibility.PRIVATE;
            default -> Visibility.PACKAGE;
        };
    }

    private String visibilityToToken(Visibility visibility) {
        if (visibility == null) {
            return "~";
        }
        return switch (visibility) {
            case PUBLIC -> "+";
            case PROTECTED -> "#";
            case PRIVATE -> "-";
            case PACKAGE -> "~";
        };
    }

    private Color parseEdgeColor(String color) {
        if (color == null) {
            return Color.BLACK;
        }
        return switch (color) {
            case "Blue" -> Color.web("#1a73e8");
            case "Red" -> Color.web("#d93025");
            case "Green" -> Color.web("#188038");
            default -> Color.BLACK;
        };
    }

    private String colorNameFromColor(Color color) {
        if (color == null) {
            return "Black";
        }
        if (Color.web("#1a73e8").equals(color)) {
            return "Blue";
        }
        if (Color.web("#d93025").equals(color)) {
            return "Red";
        }
        if (Color.web("#188038").equals(color)) {
            return "Green";
        }
        return "Black";
    }

    private Color parseClassHeaderColor(String color) {
        if (color == null) {
            return Color.web("#dae8fc");
        }
        return switch (color) {
            case "Green" -> Color.web("#d5e8d4");
            case "Orange" -> Color.web("#ffe6cc");
            case "White" -> Color.WHITE;
            default -> Color.web("#dae8fc");
        };
    }

    private Color parseClassBorderColor(String color) {
        if (color == null) {
            return Color.web("#6c8ebf");
        }
        return switch (color) {
            case "Black" -> Color.BLACK;
            case "Gray" -> Color.web("#999999");
            default -> Color.web("#6c8ebf");
        };
    }

    private String colorNameFromClassColor(Color color) {
        if (color == null) {
            return "Blue";
        }
        if (Color.web("#d5e8d4").equals(color)) {
            return "Green";
        }
        if (Color.web("#ffe6cc").equals(color)) {
            return "Orange";
        }
        if (Color.WHITE.equals(color)) {
            return "White";
        }
        return "Blue";
    }

    private String colorNameFromClassBorder(Color color) {
        if (color == null) {
            return "Blue";
        }
        if (Color.BLACK.equals(color)) {
            return "Black";
        }
        if (Color.web("#999999").equals(color)) {
            return "Gray";
        }
        return "Blue";
    }

    private Map<String, Object> captureEdgeState(RelationshipEdge edge) {
        Map<String, Object> state = new HashMap<>();
        state.put("type", edge.getRelationshipType());
        state.put("label", edge.getMiddleLabel());
        state.put("sourceMultiplicity", edge.getSourceMultiplicity());
        state.put("targetMultiplicity", edge.getTargetMultiplicity());
        state.put("dashed", edge.isDashed());
        state.put("color", colorNameFromColor(edge.getStrokeColor()));
        state.put("bendX", edge.getBendX());
        return state;
    }

    private void applyEdgeState(RelationshipEdge edge, RelationshipType type, String label, String sourceMultiplicity,
                                String targetMultiplicity, boolean dashed, String colorName, double bendX) {
        edge.setMiddleLabel(label);
        edge.setSourceMultiplicity(sourceMultiplicity);
        edge.setTargetMultiplicity(targetMultiplicity);
        edge.setRelationshipType(type);
        edge.setLineStyleDashed(dashed);
        edge.setStrokeColor(parseEdgeColor(colorName));
        edge.setBendX(bendX);

        RelationshipLink link = links.get(edge);
        if (link != null) {
            link.type = type;
        }
        Relationship relation = relationshipMap.get(edge);
        if (relation != null && relation.getType() != type) {
            Relationship replacement = buildRelationship(type, relation.getSource(), relation.getTarget());
            replacement.setLabel(label);
            replacement.setSourceMultiplicity(sourceMultiplicity);
            replacement.setTargetMultiplicity(targetMultiplicity);
            replacement.setBendX(bendX);
            replacement.setDashed(dashed);
            replacement.setEdgeColor(colorName);
            currentDiagram.getRelationships().remove(relation);
            currentDiagram.addRelationship(replacement);
            relationshipMap.put(edge, replacement);
        } else if (relation != null) {
            relation.setLabel(label);
            relation.setSourceMultiplicity(sourceMultiplicity);
            relation.setTargetMultiplicity(targetMultiplicity);
            relation.setBendX(bendX);
            relation.setDashed(dashed);
            relation.setEdgeColor(colorName);
        }
    }

    private void selectClassesInBox(double x, double y, double w, double h) {
        for (Map.Entry<UMLClass, ClassNode> entry : nodeMap.entrySet()) {
            ClassNode node = entry.getValue();
            double nx = node.getLayoutX();
            double ny = node.getLayoutY();
            double nw = nodeWidth(node);
            double nh = nodeHeight(node);
            boolean intersects = nx < x + w && nx + nw > x && ny < y + h && ny + nh > y;
            if (intersects) {
                selectedClasses.add(entry.getKey());
                node.setSelected(true);
                updateSelectionHandles(entry.getKey());
            }
        }
    }

    private void selectAllClasses() {
        clearSelections();
        for (Map.Entry<UMLClass, ClassNode> entry : nodeMap.entrySet()) {
            selectedClasses.add(entry.getKey());
            entry.getValue().setSelected(true);
            updateSelectionHandles(entry.getKey());
        }
    }

    private void attachResizeHandlers(UMLClass umlClass, ClassNode node, List<SelectionHandle> handles,
                                      double startX, double startY, double startW, double startH) {
        if (selectedClasses.size() != 1 || !selectedClasses.contains(umlClass)) {
            return;
        }
        for (int i = 0; i < handles.size(); i++) {
            final int index = i;
            SelectionHandle handle = handles.get(i);
            handle.setCursor(cursorForHandle(index));
            final double[] anchor = new double[2];
            handle.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                anchor[0] = event.getSceneX();
                anchor[1] = event.getSceneY();
                event.consume();
            });
            handle.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
                double dx = event.getSceneX() - anchor[0];
                double dy = event.getSceneY() - anchor[1];
                double[] resized = resizeForHandle(index, startX, startY, startW, startH, dx, dy);
                node.setLayoutX(snap(resized[0]));
                node.setLayoutY(snap(resized[1]));
                node.setPrefWidth(snap(resized[2]));
                node.setPrefHeight(snap(resized[3]));
                umlClass.setPositionX(node.getLayoutX());
                umlClass.setPositionY(node.getLayoutY());
                updateRelationshipsForClass(umlClass);
                updateSelectionHandles(umlClass);
                refreshMiniMap();
                event.consume();
            });
        }
    }

    private Cursor cursorForHandle(int index) {
        return switch (index) {
            case 0, 7 -> Cursor.NW_RESIZE;
            case 1, 6 -> Cursor.N_RESIZE;
            case 2, 5 -> Cursor.NE_RESIZE;
            case 3, 4 -> Cursor.E_RESIZE;
            default -> Cursor.DEFAULT;
        };
    }

    private double[] resizeForHandle(int index, double x, double y, double w, double h, double dx, double dy) {
        double minW = 140;
        double minH = 90;
        double newX = x;
        double newY = y;
        double newW = w;
        double newH = h;
        switch (index) {
            case 0 -> {
                newX = x + dx;
                newY = y + dy;
                newW = w - dx;
                newH = h - dy;
            }
            case 1 -> {
                newY = y + dy;
                newH = h - dy;
            }
            case 2 -> {
                newY = y + dy;
                newW = w + dx;
                newH = h - dy;
            }
            case 3 -> {
                newX = x + dx;
                newW = w - dx;
            }
            case 4 -> newW = w + dx;
            case 5 -> {
                newX = x + dx;
                newW = w - dx;
                newH = h + dy;
            }
            case 6 -> newH = h + dy;
            case 7 -> {
                newW = w + dx;
                newH = h + dy;
            }
            default -> {
            }
        }
        if (newW < minW) {
            if (index == 0 || index == 3 || index == 5) {
                newX -= (minW - newW);
            }
            newW = minW;
        }
        if (newH < minH) {
            if (index == 0 || index == 1 || index == 2) {
                newY -= (minH - newH);
            }
            newH = minH;
        }
        return new double[]{newX, newY, newW, newH};
    }

    private double nodeWidth(ClassNode node) {
        double width = node.getWidth();
        return width > 0 ? width : Math.max(140, node.getPrefWidth());
    }

    private double nodeHeight(ClassNode node) {
        double height = node.getHeight();
        return height > 0 ? height : Math.max(90, node.getPrefHeight());
    }

    private double centerX(ClassNode node) {
        return node.getLayoutX() + (nodeWidth(node) / 2);
    }

    private double centerY(ClassNode node) {
        return node.getLayoutY() + (nodeHeight(node) / 2);
    }

    private static class RelationshipLink {
        private final UMLClass source;
        private final UMLClass target;
        private RelationshipType type;

        private RelationshipLink(UMLClass source, UMLClass target, RelationshipType type) {
            this.source = source;
            this.target = target;
            this.type = type;
        }
    }

    private enum ToolMode {
        SELECT,
        PAN,
        RELATIONSHIP
    }
}
