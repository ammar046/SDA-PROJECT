package com.umlytics.ui.canvas;

import com.umlytics.domain.Attribute;
import com.umlytics.domain.Method;
import com.umlytics.enums.RelationshipType;

import javafx.geometry.Insets;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;

import java.util.List;
import java.util.stream.Collectors;
import java.util.function.Consumer;

public class ClassNode extends Region {
    private final VBox root;
    private final Label titleLabel;
    private final Label attributesLabel;
    private final Label methodsLabel;
    private Color borderColor = Color.web("#6c8ebf");
    private Color headerColor = Color.web("#dae8fc");
    private double memberFontSize = 12;
    private Consumer<String> renameHandler;
    private Runnable addAttributeHandler;
    private Runnable addMethodHandler;
    private Runnable editClassHandler;
    private Consumer<RelationshipType> addRelationshipHandler;
    private Runnable deleteHandler;
    private Consumer<Boolean> toggleAbstractHandler;
    private Consumer<Boolean> toggleInterfaceHandler;
    private Runnable copyHandler;
    private Runnable pasteHandler;
    private Consumer<String> editAttributesTextHandler;
    private Consumer<String> editMethodsTextHandler;
    private boolean abstractKind;
    private boolean interfaceKind;

    public ClassNode(String className) {
        root = new VBox();
        root.setBorder(new Border(new BorderStroke(Color.web("#6c8ebf"), BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
        root.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
        root.setPrefWidth(200);
        root.setMinWidth(140);
        root.setPrefHeight(140);
        root.setMinHeight(90);

        titleLabel = new Label(className);
        titleLabel.getStyleClass().add("class-node-header");
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setPadding(new Insets(6));
        titleLabel.setFont(Font.font(14));

        attributesLabel = new Label("");
        attributesLabel.setPadding(new Insets(4));
        attributesLabel.setFont(Font.font(memberFontSize));
        methodsLabel = new Label("");
        methodsLabel.setPadding(new Insets(4));
        methodsLabel.setFont(Font.font(memberFontSize));

        root.getChildren().addAll(titleLabel, attributesLabel, methodsLabel);
        getChildren().add(root);
        initializeInteractions();
        applyVisualStyle();
    }

    public void setClassName(String name) {
        titleLabel.setText(name);
    }

    public String getClassName() {
        return titleLabel.getText();
    }

    /** Starts inline rename of the class title (same as double-click). */
    public void requestRenameEditor() {
        beginRename();
    }

    public void setSelected(boolean selected) {
        if (selected) {
            root.getStyleClass().add("class-node-selected");
        } else {
            root.getStyleClass().remove("class-node-selected");
        }
    }

    public void setRenameHandler(Consumer<String> renameHandler) {
        this.renameHandler = renameHandler;
    }

    public void setAddAttributeHandler(Runnable addAttributeHandler) {
        this.addAttributeHandler = addAttributeHandler;
    }

    public void setAddMethodHandler(Runnable addMethodHandler) {
        this.addMethodHandler = addMethodHandler;
    }

    public void setEditClassHandler(Runnable editClassHandler) {
        this.editClassHandler = editClassHandler;
    }

    public void setAddRelationshipHandler(Consumer<RelationshipType> addRelationshipHandler) {
        this.addRelationshipHandler = addRelationshipHandler;
    }

    public void setDeleteHandler(Runnable deleteHandler) {
        this.deleteHandler = deleteHandler;
    }

    public void setToggleAbstractHandler(Consumer<Boolean> toggleAbstractHandler) {
        this.toggleAbstractHandler = toggleAbstractHandler;
    }

    public void setToggleInterfaceHandler(Consumer<Boolean> toggleInterfaceHandler) {
        this.toggleInterfaceHandler = toggleInterfaceHandler;
    }

    public void setCopyHandler(Runnable copyHandler) {
        this.copyHandler = copyHandler;
    }

    public void setPasteHandler(Runnable pasteHandler) {
        this.pasteHandler = pasteHandler;
    }

    public void setKindFlags(boolean abstractKind, boolean interfaceKind) {
        this.abstractKind = abstractKind;
        this.interfaceKind = interfaceKind;
    }

    public void setMembers(List<Attribute> attributes, List<Method> methods) {
        String attrText = attributes.stream()
                .map(a -> visibilitySymbol(a.getVisibility() == null ? "PACKAGE" : a.getVisibility().name()) + " " + a.getName() + ": " + a.getType())
                .collect(Collectors.joining("\n"));
        String methodText = methods.stream()
                .map(m -> visibilitySymbol(m.getVisibility() == null ? "PACKAGE" : m.getVisibility().name()) + " " + m.getSignature())
                .collect(Collectors.joining("\n"));
        attributesLabel.setText(attrText);
        methodsLabel.setText(methodText);
    }

    public void setHeaderColor(Color color) {
        this.headerColor = color == null ? Color.web("#dae8fc") : color;
        applyVisualStyle();
    }

    public void setBorderColor(Color color) {
        this.borderColor = color == null ? Color.web("#6c8ebf") : color;
        applyVisualStyle();
    }

    public void setMemberFontSize(double size) {
        this.memberFontSize = Math.max(9, Math.min(24, size));
        attributesLabel.setFont(Font.font(memberFontSize));
        methodsLabel.setFont(Font.font(memberFontSize));
    }

    public Color getHeaderColor() {
        return headerColor;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public double getMemberFontSize() {
        return memberFontSize;
    }

    public void setEditAttributesTextHandler(Consumer<String> editAttributesTextHandler) {
        this.editAttributesTextHandler = editAttributesTextHandler;
    }

    public void setEditMethodsTextHandler(Consumer<String> editMethodsTextHandler) {
        this.editMethodsTextHandler = editMethodsTextHandler;
    }

    private void initializeInteractions() {
        titleLabel.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                beginRename();
            }
        });

        ContextMenu menu = new ContextMenu();
        MenuItem rename = new MenuItem("Rename");
        rename.setOnAction(event -> beginRename());
        MenuItem addAttribute = new MenuItem("Add Attribute");
        addAttribute.setOnAction(event -> {
            if (addAttributeHandler != null) {
                addAttributeHandler.run();
            }
        });
        MenuItem addMethod = new MenuItem("Add Method");
        addMethod.setOnAction(event -> {
            if (addMethodHandler != null) {
                addMethodHandler.run();
            }
        });
        Menu addRelationship = new Menu("Add Relationship ->");
        for (RelationshipType relationshipType : RelationshipType.values()) {
            MenuItem relType = new MenuItem(relationshipType.name());
            relType.setOnAction(event -> {
                if (addRelationshipHandler != null) {
                    addRelationshipHandler.accept(relationshipType);
                }
            });
            addRelationship.getItems().add(relType);
        }
        MenuItem editAttributes = new MenuItem("Edit Attributes");
        editAttributes.setOnAction(event -> beginEditAttributes());
        MenuItem editMethods = new MenuItem("Edit Methods");
        editMethods.setOnAction(event -> beginEditMethods());
        MenuItem editClass = new MenuItem("Edit Class");
        editClass.setOnAction(event -> {
            if (editClassHandler != null) {
                editClassHandler.run();
            }
        });
        MenuItem deleteClass = new MenuItem("Delete Class");
        deleteClass.setOnAction(event -> {
            if (deleteHandler != null) {
                deleteHandler.run();
            }
        });
        CheckMenuItem setAbstract = new CheckMenuItem("Set Abstract");
        setAbstract.setSelected(abstractKind);
        setAbstract.setOnAction(event -> {
            abstractKind = setAbstract.isSelected();
            if (setAbstract.isSelected()) {
                interfaceKind = false;
            }
            if (toggleAbstractHandler != null) {
                toggleAbstractHandler.accept(setAbstract.isSelected());
            }
            if (toggleInterfaceHandler != null && !setAbstract.isSelected()) {
                toggleInterfaceHandler.accept(interfaceKind);
            }
        });
        CheckMenuItem setInterface = new CheckMenuItem("Set Interface");
        setInterface.setSelected(interfaceKind);
        setInterface.setOnAction(event -> {
            interfaceKind = setInterface.isSelected();
            if (setInterface.isSelected()) {
                abstractKind = false;
            }
            if (toggleInterfaceHandler != null) {
                toggleInterfaceHandler.accept(setInterface.isSelected());
            }
            if (toggleAbstractHandler != null && !setInterface.isSelected()) {
                toggleAbstractHandler.accept(abstractKind);
            }
        });
        MenuItem copy = new MenuItem("Copy");
        copy.setOnAction(event -> {
            if (copyHandler != null) {
                copyHandler.run();
            }
        });
        MenuItem paste = new MenuItem("Paste");
        paste.setOnAction(event -> {
            if (pasteHandler != null) {
                pasteHandler.run();
            }
        });
        menu.getItems().addAll(rename, addAttribute, addMethod, addRelationship, editClass, editAttributes, editMethods,
                deleteClass, setAbstract, setInterface, copy, paste);
        setOnContextMenuRequested(event -> menu.show(this, event.getScreenX(), event.getScreenY()));

        attributesLabel.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                beginEditAttributes();
            }
        });
        methodsLabel.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                beginEditMethods();
            }
        });
    }

    private void beginRename() {
        TextField editor = new TextField(titleLabel.getText());
        editor.setOnAction(event -> finishRename(editor));
        editor.focusedProperty().addListener((obs, oldVal, focused) -> {
            if (!focused) {
                finishRename(editor);
            }
        });
        root.getChildren().set(0, editor);
        editor.requestFocus();
        editor.selectAll();
    }

    private void finishRename(TextField editor) {
        String updated = editor.getText() == null || editor.getText().isBlank() ? "NewClass" : editor.getText().trim();
        titleLabel.setText(updated);
        root.getChildren().set(0, titleLabel);
        if (renameHandler != null) {
            renameHandler.accept(updated);
        }
    }

    private String visibilitySymbol(String visibility) {
        return switch (visibility) {
            case "PUBLIC" -> "+";
            case "PROTECTED" -> "#";
            case "PRIVATE" -> "-";
            default -> "~";
        };
    }

    private void applyVisualStyle() {
        root.setBorder(new Border(new BorderStroke(borderColor, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
        root.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
        titleLabel.setStyle("-fx-background-color: " + toHex(headerColor) + "; -fx-font-weight: bold;");
    }

    private void beginEditAttributes() {
        TextArea editor = new TextArea(attributesLabel.getText());
        editor.setWrapText(false);
        editor.setPrefRowCount(4);
        editor.focusedProperty().addListener((obs, oldVal, focused) -> {
            if (!focused) {
                finishEditAttributes(editor);
            }
        });
        root.getChildren().set(1, editor);
        editor.requestFocus();
        editor.selectAll();
    }

    private void finishEditAttributes(TextArea editor) {
        String updated = editor.getText() == null ? "" : editor.getText();
        attributesLabel.setText(updated);
        root.getChildren().set(1, attributesLabel);
        if (editAttributesTextHandler != null) {
            editAttributesTextHandler.accept(updated);
        }
    }

    private void beginEditMethods() {
        TextArea editor = new TextArea(methodsLabel.getText());
        editor.setWrapText(false);
        editor.setPrefRowCount(4);
        editor.focusedProperty().addListener((obs, oldVal, focused) -> {
            if (!focused) {
                finishEditMethods(editor);
            }
        });
        root.getChildren().set(2, editor);
        editor.requestFocus();
        editor.selectAll();
    }

    private void finishEditMethods(TextArea editor) {
        String updated = editor.getText() == null ? "" : editor.getText();
        methodsLabel.setText(updated);
        root.getChildren().set(2, methodsLabel);
        if (editMethodsTextHandler != null) {
            editMethodsTextHandler.accept(updated);
        }
    }

    private String toHex(Color color) {
        int r = (int) Math.round(color.getRed() * 255);
        int g = (int) Math.round(color.getGreen() * 255);
        int b = (int) Math.round(color.getBlue() * 255);
        return String.format("#%02x%02x%02x", r, g, b);
    }

    @Override
    protected void layoutChildren() {
        root.resizeRelocate(0, 0, getWidth(), getHeight());
    }
}
