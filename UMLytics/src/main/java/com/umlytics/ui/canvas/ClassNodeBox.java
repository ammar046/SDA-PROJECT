package com.umlytics.ui.canvas;

import com.umlytics.domain.Attribute;
import com.umlytics.domain.Method;
import com.umlytics.domain.UMLClass;
import com.umlytics.enums.Visibility;
import com.umlytics.ui.MainWindow;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;

/**
 * Visual representation of a single UML Class node.
 * Polished 3-section design: header / attributes / methods.
 * GRASP: Information Expert (knows how to display UMLClass)
 */
public class ClassNodeBox extends VBox {

    private final UMLClass cls;
    private final MainWindow facade;

    // Header label reference so others can update it
    private Label lblName;

    public ClassNodeBox(UMLClass cls, MainWindow facade) {
        this.cls    = cls;
        this.facade = facade;

        getStyleClass().add("class-node");
        setPrefWidth(180);
        setMinWidth(170);
        setPadding(Insets.EMPTY);

        buildHeader();
        buildAttributeSection();
        buildMethodSection();

        // Apply selection border if selected
        if (cls.isSelected()) getStyleClass().add("class-node-selected");

        // Node-level context menu
        setupNodeContextMenu();
    }

    // ---- Section builders ----

    private void buildHeader() {
        VBox header = new VBox();
        header.getStyleClass().add("class-node-header");
        header.setPadding(new Insets(6, 10, 6, 10));

        // Determine stereotype string
        String stereoText = resolveStereotype();
        // Apply header CSS colour class
        applyHeaderStyle(header);

        // Stereotype label (small, above name)
        if (!stereoText.isEmpty()) {
            Label stereoLabel = new Label(stereoText);
            stereoLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: rgba(255,255,255,0.75); -fx-alignment: center;");
            stereoLabel.setMaxWidth(Double.MAX_VALUE);
            header.getChildren().add(stereoLabel);
        }

        // Class name label
        lblName = new Label(cls.getName());
        lblName.setStyle(buildNameStyle());
        lblName.setMaxWidth(Double.MAX_VALUE);
        lblName.setWrapText(false);

        // Double-click to rename inline
        makeEditable(lblName, header, newValue -> {
            String clean = newValue.trim();
            if (!clean.isEmpty()) {
                cls.setName(clean);
                lblName.setText(clean);
                facade.getDiagramController().saveCurrentDiagram();
            }
        });

        header.getChildren().add(lblName);

        // Right-click on exact name label for convenience
        setupNameLabelContextMenu();

        getChildren().addAll(header, new Separator());
    }

    private void buildAttributeSection() {
        VBox attrBox = new VBox();
        attrBox.getStyleClass().add("class-node-attr-section");
        attrBox.setPadding(new Insets(4, 8, 4, 8));

        for (Attribute a : cls.getAttributes()) {
            Label al = new Label(a.toUMLString());
            al.setStyle("-fx-font-family: monospace; -fx-font-size: 11px; -fx-text-fill: #d4d4d4;");
            al.setMaxWidth(Double.MAX_VALUE);

            // Double-click inline edit
            makeEditable(al, attrBox, newVal -> {
                parseAttributeString(a, newVal);
                al.setText(a.toUMLString());
                facade.getDiagramController().saveCurrentDiagram();
            });

            // Right-click context menu per attribute
            setupAttributeContextMenu(al, a, attrBox);
            attrBox.getChildren().add(al);
        }

        if (cls.getAttributes().isEmpty()) {
            Label empty = new Label("  (no attributes)");
            empty.setStyle("-fx-font-size: 10px; -fx-text-fill: #555555; -fx-font-style: italic;");
            attrBox.getChildren().add(empty);
        }

        getChildren().addAll(attrBox, new Separator());
    }

    private void buildMethodSection() {
        VBox methodBox = new VBox();
        methodBox.getStyleClass().add("class-node-method-section");
        methodBox.setPadding(new Insets(4, 8, 4, 8));

        for (Method m : cls.getMethods()) {
            Label ml = new Label(m.getSignature());
            ml.setStyle("-fx-font-family: monospace; -fx-font-size: 11px; -fx-text-fill: #d4d4d4;"
                    + (m.isAbstract() ? " -fx-font-style: italic;" : ""));
            ml.setMaxWidth(Double.MAX_VALUE);

            makeEditable(ml, methodBox, newVal -> {
                parseMethodString(m, newVal);
                ml.setText(m.getSignature());
                facade.getDiagramController().saveCurrentDiagram();
            });

            setupMethodContextMenu(ml, m, methodBox);
            methodBox.getChildren().add(ml);
        }

        if (cls.getMethods().isEmpty()) {
            Label empty = new Label("  (no methods)");
            empty.setStyle("-fx-font-size: 10px; -fx-text-fill: #555555; -fx-font-style: italic;");
            methodBox.getChildren().add(empty);
        }

        getChildren().add(methodBox);
    }

    // ---- Style helpers ----

    private String resolveStereotype() {
        if (cls.isInterface())                                return "<<Interface>>";
        if (cls.isAbstract())                                 return "<< Abstract >>";
        String st = cls.getStereotype();
        if ("Enumeration".equalsIgnoreCase(st))               return "<<Enumeration>>";
        if ("Note".equalsIgnoreCase(st))                      return "<<Note>>";
        return "<< Class >>";
    }

    private void applyHeaderStyle(VBox header) {
        if (cls.isInterface())                                          header.getStyleClass().add("class-header-interface");
        else if (cls.isAbstract())                                      header.getStyleClass().add("class-header-abstract");
        else if ("Enumeration".equalsIgnoreCase(cls.getStereotype()))  header.getStyleClass().add("class-header-enum");
        else if ("Note".equalsIgnoreCase(cls.getStereotype()))         header.getStyleClass().add("class-header-note");
        else                                                            header.getStyleClass().add("class-header-class");
    }

    private String buildNameStyle() {
        String base = "-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: white; -fx-alignment: center;";
        if (cls.isAbstract() || cls.isInterface()) base += " -fx-font-style: italic;";
        return base;
    }

    // ---- Context menus ----

    private void setupNodeContextMenu() {
        ContextMenu cm = new ContextMenu();

        MenuItem addAttr = new MenuItem("Add Attribute");
        addAttr.setOnAction(e -> {
            Attribute a = new Attribute(0, "newAttr", "String", Visibility.PRIVATE, false);
            cls.addAttribute(a);
            facade.getDiagramController().saveCurrentDiagram();
            rerenderCanvas();
        });

        MenuItem addMethod = new MenuItem("Add Method");
        addMethod.setOnAction(e -> {
            Method m = new Method(0, "newMethod", "void", Visibility.PUBLIC, false);
            cls.addMethod(m);
            facade.getDiagramController().saveCurrentDiagram();
            rerenderCanvas();
        });

        MenuItem rename = new MenuItem("Rename Class");
        rename.setOnAction(e -> {
            TextInputDialog dlg = new TextInputDialog(cls.getName());
            dlg.setTitle("Rename Class");
            dlg.setHeaderText("Enter new class name:");
            dlg.showAndWait().ifPresent(name -> {
                if (!name.trim().isEmpty()) {
                    cls.setName(name.trim());
                    facade.getDiagramController().saveCurrentDiagram();
                    rerenderCanvas();
                }
            });
        });

        MenuItem toggleAbstract = new MenuItem("Toggle Abstract");
        toggleAbstract.setOnAction(e -> {
            cls.setAbstract(!cls.isAbstract());
            if (cls.isAbstract()) cls.setInterface(false);
            facade.getDiagramController().saveCurrentDiagram();
            rerenderCanvas();
        });

        MenuItem deleteClass = new MenuItem("Delete Class");
        deleteClass.setOnAction(e -> {
            facade.getDiagramController().getCurrentDiagram().removeUMLClass(cls.getClassId());
            // Also remove relationships referencing this class
            facade.getDiagramController().getCurrentDiagram().getRelationships().stream()
                .filter(r -> (r.getSourceClass() != null && r.getSourceClass().getClassId() == cls.getClassId())
                          || (r.getTargetClass() != null && r.getTargetClass().getClassId() == cls.getClassId()))
                .toList()
                .forEach(r -> facade.getDiagramController().getCurrentDiagram().removeRelationship(r.getRelationshipId()));
            facade.getDiagramController().saveCurrentDiagram();
            rerenderCanvas();
        });

        MenuItem copyClass = new MenuItem("Copy Class");
        copyClass.setOnAction(e -> facade.getMainCanvas().setClipboardClass(cls));

        cm.getItems().addAll(addAttr, addMethod, new SeparatorMenuItem(),
                rename, toggleAbstract, new SeparatorMenuItem(),
                copyClass, deleteClass);

        setOnContextMenuRequested(e -> {
            cm.show(this, e.getScreenX(), e.getScreenY());
            e.consume();
        });

        // Double-click on the node body → focus AI chat
        setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY) {
                facade.getAiChatPanel().focusWithText("Tell me about `" + cls.getName() + "` design.");
                e.consume();
            }
        });
    }

    private void setupNameLabelContextMenu() {
        // Double-click is already handled by makeEditable on the label
        // No extra context needed on name label — node context menu covers it
    }

    private void setupAttributeContextMenu(Label al, Attribute a, VBox parent) {
        ContextMenu cm = new ContextMenu();

        MenuItem edit = new MenuItem("Edit");
        edit.setOnAction(e -> triggerEdit(al, parent, newVal -> {
            parseAttributeString(a, newVal);
            al.setText(a.toUMLString());
            facade.getDiagramController().saveCurrentDiagram();
        }));

        Menu changeVis = new Menu("Change Visibility");
        for (Visibility v : Visibility.values()) {
            MenuItem vi = new MenuItem(v.name());
            vi.setOnAction(ee -> {
                a.setVisibility(v);
                al.setText(a.toUMLString());
                facade.getDiagramController().saveCurrentDiagram();
            });
            changeVis.getItems().add(vi);
        }

        MenuItem toggleStatic = new MenuItem("Toggle Static");
        toggleStatic.setOnAction(e -> {
            a.setStatic(!a.isStatic());
            al.setText(a.toUMLString());
            facade.getDiagramController().saveCurrentDiagram();
        });

        MenuItem delete = new MenuItem("Delete");
        delete.setOnAction(e -> {
            cls.removeAttribute(a.getAttributeId());
            facade.getDiagramController().saveCurrentDiagram();
            rerenderCanvas();
        });

        cm.getItems().addAll(edit, changeVis, toggleStatic, new SeparatorMenuItem(), delete);
        al.setOnContextMenuRequested(e -> {
            cm.show(al, e.getScreenX(), e.getScreenY());
            e.consume();
        });
    }

    private void setupMethodContextMenu(Label ml, Method m, VBox parent) {
        ContextMenu cm = new ContextMenu();

        MenuItem edit = new MenuItem("Edit");
        edit.setOnAction(e -> triggerEdit(ml, parent, newVal -> {
            parseMethodString(m, newVal);
            ml.setText(m.getSignature());
            facade.getDiagramController().saveCurrentDiagram();
        }));

        Menu changeVis = new Menu("Change Visibility");
        for (Visibility v : Visibility.values()) {
            MenuItem vi = new MenuItem(v.name());
            vi.setOnAction(ee -> {
                m.setVisibility(v);
                ml.setText(m.getSignature());
                facade.getDiagramController().saveCurrentDiagram();
            });
            changeVis.getItems().add(vi);
        }

        MenuItem toggleAbstract = new MenuItem("Toggle Abstract");
        toggleAbstract.setOnAction(e -> {
            m.setAbstract(!m.isAbstract());
            ml.setText(m.getSignature());
            ml.setStyle("-fx-font-family: monospace; -fx-font-size: 11px; -fx-text-fill: #d4d4d4;"
                    + (m.isAbstract() ? " -fx-font-style: italic;" : ""));
            facade.getDiagramController().saveCurrentDiagram();
        });

        MenuItem delete = new MenuItem("Delete");
        delete.setOnAction(e -> {
            cls.removeMethod(m.getMethodId());
            facade.getDiagramController().saveCurrentDiagram();
            rerenderCanvas();
        });

        cm.getItems().addAll(edit, changeVis, toggleAbstract, new SeparatorMenuItem(), delete);
        ml.setOnContextMenuRequested(e -> {
            cm.show(ml, e.getScreenX(), e.getScreenY());
            e.consume();
        });
    }

    // ---- Inline editing helpers ----

    private void makeEditable(Label label, VBox parent, java.util.function.Consumer<String> onSave) {
        label.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY) {
                e.consume();
                triggerEdit(label, parent, onSave);
            }
        });
    }

    private void triggerEdit(Label label, VBox parent, java.util.function.Consumer<String> onSave) {
        TextField field = new TextField(label.getText());
        field.getStyleClass().add("text-field");
        field.setStyle("-fx-font-size: 12px;");

        field.setOnKeyPressed(ke -> {
            if (ke.getCode() == KeyCode.ENTER) {
                onSave.accept(field.getText());
                int idx = parent.getChildren().indexOf(field);
                if (idx != -1) parent.getChildren().set(idx, label);
            } else if (ke.getCode() == KeyCode.ESCAPE) {
                int idx = parent.getChildren().indexOf(field);
                if (idx != -1) parent.getChildren().set(idx, label);
            }
        });

        // Prevent mouse clicks inside the field from bubbling up to ClassNodeBox and stealing focus!
        field.setOnMousePressed(e -> e.consume());

        field.focusedProperty().addListener((obs, was, isNow) -> {
            if (!isNow) {
                int idx = parent.getChildren().indexOf(field);
                if (idx != -1) {
                    onSave.accept(field.getText());
                    parent.getChildren().set(idx, label);
                }
            }
        });

        int idx = parent.getChildren().indexOf(label);
        if (idx != -1) {
            parent.getChildren().set(idx, field);
            field.requestFocus();
            field.selectAll();
        }
    }

    // ---- Parsing helpers ----

    /** Parses "- name : Type" or "name: Type" or just "name" into the Attribute */
    private void parseAttributeString(Attribute a, String raw) {
        String s = raw.trim();
        // Strip visibility prefix if present
        if (s.startsWith("-") || s.startsWith("+") || s.startsWith("#") || s.startsWith("~")) {
            s = s.substring(1).trim();
        }
        // Remove {static} marker
        s = s.replace("{static}", "").trim();
        if (s.contains(":")) {
            String[] parts = s.split(":", 2);
            a.setName(parts[0].trim());
            a.setType(parts[1].trim());
        } else {
            a.setName(s.trim());
        }
    }

    /** Parses "+ methodName(params) : ReturnType" into the Method */
    private void parseMethodString(Method m, String raw) {
        String s = raw.trim();
        // Strip visibility prefix
        if (s.startsWith("-") || s.startsWith("+") || s.startsWith("#") || s.startsWith("~")) {
            s = s.substring(1).trim();
        }
        // Strip {abstract} marker
        s = s.replace("{abstract}", "").trim();
        // Return type
        String returnType = m.getReturnType() != null ? m.getReturnType() : "void";
        if (s.contains(":")) {
            returnType = s.substring(s.lastIndexOf(":") + 1).trim();
            s = s.substring(0, s.lastIndexOf(":")).trim();
        }
        // Method name (strip params)
        if (s.contains("(")) {
            m.setName(s.substring(0, s.indexOf("(")).trim());
        } else {
            m.setName(s.trim());
        }
        m.setReturnType(returnType);
    }

    // ---- Utility ----

    private void rerenderCanvas() {
        facade.getMainCanvas().renderDiagram(facade.getDiagramController().getCurrentDiagram());
    }

    /** Expose the reference for canvas to update the name label text after external rename. */
    public UMLClass getUMLClass() { return cls; }
}
