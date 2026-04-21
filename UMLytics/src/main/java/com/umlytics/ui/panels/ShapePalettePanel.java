package com.umlytics.ui.panels;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;

/**
 * Shape palette panel with 5 draggable UML shape previews.
 * GRASP: Pure Fabrication (UI-only component)
 */
public class ShapePalettePanel extends VBox {

    public ShapePalettePanel() {
        getStyleClass().add("project-explorer");
        setPadding(new Insets(10));
        setSpacing(8);
        setPrefWidth(200);

        Label title = new Label("Shapes");
        title.getStyleClass().add("panel-header");
        title.setMaxWidth(Double.MAX_VALUE);

        Label hint = new Label("💡 Hint:\nShift + drag between classes\nto create relationships.");
        hint.setStyle("-fx-text-fill: #a9b7c6; -fx-font-size: 11px; -fx-padding: 8px; -fx-text-alignment: center; -fx-background-color: rgba(6, 182, 212, 0.1); -fx-background-radius: 6px;");
        hint.setMaxWidth(Double.MAX_VALUE);
        hint.setAlignment(Pos.CENTER);

        getChildren().addAll(
            title,
            hint,
            createClassShape(),
            createAbstractShape(),
            createInterfaceShape(),
            createEnumShape(),
            createNoteShape()
        );
    }

    // ---- Palette item builders ----

    private VBox createClassShape() {
        VBox box = buildShapeContainer("SHAPE:CLASS", "Standard 3-section UML Class node");
        box.getChildren().addAll(
            buildHeader("<< Class >>", "NewClass", "#2d5a8e", false),
            new Separator(),
            buildSection("- attribute : Type"),
            new Separator(),
            buildSection("+ method() : void")
        );
        return box;
    }

    private VBox createAbstractShape() {
        VBox box = buildShapeContainer("SHAPE:ABSTRACT", "Abstract class — italic name, no instantiation");
        box.getChildren().addAll(
            buildHeader("<< Abstract >>", "NewAbstract", "#5a2d82", true),
            new Separator(),
            buildSection("- attribute : Type"),
            new Separator(),
            buildSection("+ method() : void")
        );
        return box;
    }

    private VBox createInterfaceShape() {
        VBox box = buildShapeContainer("SHAPE:INTERFACE", "Interface — only method signatures");
        box.getChildren().addAll(
            buildHeader("<<Interface>>", "NewInterface", "#1a6040", true),
            new Separator(),
            buildSection("+ operation() : void")
        );
        return box;
    }

    private VBox createEnumShape() {
        VBox box = buildShapeContainer("SHAPE:ENUM", "Enumeration — constants list");
        box.getChildren().addAll(
            buildHeader("<<Enumeration>>", "NewEnum", "#3d3d00", false),
            new Separator(),
            buildSection("CONSTANT_ONE"),
            buildSection("CONSTANT_TWO")
        );
        return box;
    }

    private VBox createNoteShape() {
        VBox box = buildShapeContainer("SHAPE:NOTE", "Annotation note — free text comment");
        // Single section, no dividers, warm background
        VBox noteBody = new VBox();
        noteBody.setStyle("-fx-background-color: #4d3000; -fx-padding: 6 8; -fx-background-radius: 4;");
        Label noteLabel = new Label("<<Note>>\nWrite note here...");
        noteLabel.setStyle("-fx-text-fill: #e0c080; -fx-font-size: 10px; -fx-wrap-text: true;");
        noteBody.getChildren().add(noteLabel);
        box.getChildren().add(noteBody);
        return box;
    }

    // ---- Building helpers ----

    private VBox buildShapeContainer(String dragPayload, String tooltipText) {
        VBox box = new VBox();
        box.getStyleClass().add("class-node");
        box.setPrefWidth(160);
        box.setMaxWidth(160);
        box.setStyle("-fx-cursor: grab;");
        Tooltip.install(box, new Tooltip(tooltipText));

        box.setOnDragDetected(e -> {
            Dragboard db = box.startDragAndDrop(TransferMode.COPY);
            ClipboardContent cc = new ClipboardContent();
            cc.putString(dragPayload);
            db.setContent(cc);
            e.consume();
        });

        return box;
    }

    private VBox buildHeader(String stereotype, String name, String bgColor, boolean italic) {
        VBox header = new VBox(2);
        header.setStyle("-fx-background-color: " + bgColor + "; -fx-padding: 4 8; -fx-alignment: center;");
        header.setAlignment(Pos.CENTER);

        Label stereoLabel = new Label(stereotype);
        stereoLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.75); -fx-font-size: 9px;");

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: 500;"
                + (italic ? " -fx-font-style: italic;" : ""));

        header.getChildren().addAll(stereoLabel, nameLabel);
        return header;
    }

    private Label buildSection(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-family: monospace; -fx-font-size: 9px; -fx-text-fill: #b0b0b0;"
                + "-fx-padding: 2 8; -fx-background-color: #252526;");
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }
}
