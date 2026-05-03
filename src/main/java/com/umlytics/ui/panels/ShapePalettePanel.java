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

import javafx.scene.control.ScrollPane;

// GRASP: Pure Fabrication (UI-only)
public class ShapePalettePanel extends ScrollPane {

    public ShapePalettePanel() {
        VBox inner = new VBox(6);
        inner.setPadding(new Insets(10));
        inner.setFillWidth(true);

        Label title = new Label("Shapes");
        title.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 14px; -fx-font-weight: bold; "
            + "-fx-padding: 0 0 8 0; -fx-border-color: #374151; -fx-border-width: 0 0 1 0;");
        title.setMaxWidth(Double.MAX_VALUE);

        Label hint = new Label("💡 Hint: Drag shapes to canvas\nShift+drag between classes to connect");
        hint.setStyle("-fx-text-fill: #888899; -fx-font-size: 11px; -fx-padding: 6 0;");

        Label sectionUML = sectionSep("─── UML Classes ───");
        Label sectionRel = sectionSep("─── Relationships ───");

        inner.getChildren().addAll(
            title, hint, sectionUML,
            classNode("SHAPE:CLASS",
                "#2d5a8e", "<< Class >>", "NewClass",
                new String[]{"- attribute : Type"},
                new String[]{"+ method() : void"}),
            classNode("SHAPE:ABSTRACT",
                "#5a2d82", "<< Abstract >>", "NewAbstract",
                new String[]{"- attribute : Type"},
                new String[]{"+ method() : void"}),
            classNode("SHAPE:INTERFACE",
                "#1a6040", "<<Interface>>", "NewInterface",
                new String[]{},
                new String[]{"+ operation() : void"}),
            classNode("SHAPE:ENUM",
                "#4a3d00", "<<Enumeration>>", "NewEnum",
                new String[]{"CONSTANT_ONE", "CONSTANT_TWO"},
                new String[]{}),
            classNode("SHAPE:NOTE",
                "#4d3000", "<< Note >>", "...",
                new String[]{},
                new String[]{}),
            sectionRel,
            relLine("REL:ASSOCIATION",  "Association  ──→"),
            relLine("REL:INHERITANCE",  "Inheritance  ──→◁"),
            relLine("REL:REALIZATION",  "Realization  ╌╌→◁"),
            relLine("REL:DEPENDENCY",   "Dependency   ╌╌→"),
            relLine("REL:AGGREGATION",  "Aggregation  ──◇"),
            relLine("REL:COMPOSITION",  "Composition  ──◆")
        );

        setContent(inner);
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        getStyleClass().add("project-explorer");
        setPrefWidth(220);
        setMinWidth(170);
    }

    // ── Full class node preview ───────────────────────────────────────────────
    private VBox classNode(String dragKey,
                           String headerColor,
                           String stereotype,
                           String className,
                           String[] attrs,
                           String[] methods) {
        VBox root = new VBox(0);
        root.setStyle("-fx-border-color: #374151; -fx-border-radius: 4; "
            + "-fx-background-radius: 4; -fx-cursor: hand;");
        root.setMaxWidth(Double.MAX_VALUE);

        // Header
        VBox header = new VBox(1);
        header.setAlignment(Pos.CENTER);
        header.setStyle("-fx-background-color: " + headerColor + "; "
            + "-fx-padding: 5 8; -fx-background-radius: 4 4 0 0;");
        Label stereo = new Label(stereotype);
        stereo.setStyle("-fx-text-fill: rgba(255,255,255,0.8); -fx-font-size: 9px;");
        Label name = new Label(className);
        name.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");
        header.getChildren().addAll(stereo, name);

        root.getChildren().add(header);

        // Attribute rows
        if (attrs.length > 0) {
            root.getChildren().add(new Separator());
            for (String a : attrs) {
                Label l = new Label(a);
                l.setStyle("-fx-font-family: monospace; -fx-font-size: 10px; "
                    + "-fx-text-fill: #bbbbbb; -fx-padding: 2 8; "
                    + "-fx-background-color: #252530;");
                l.setMaxWidth(Double.MAX_VALUE);
                root.getChildren().add(l);
            }
        }

        // Method rows
        if (methods.length > 0) {
            root.getChildren().add(new Separator());
            for (String m : methods) {
                Label l = new Label(m);
                l.setStyle("-fx-font-family: monospace; -fx-font-size: 10px; "
                    + "-fx-text-fill: #bbbbbb; -fx-padding: 2 8; "
                    + "-fx-background-color: #252530;");
                l.setMaxWidth(Double.MAX_VALUE);
                root.getChildren().add(l);
            }
        }

        // Hover effect
        root.setOnMouseEntered(e -> root.setStyle(root.getStyle()
            .replace("#374151", "#06b6d4")));
        root.setOnMouseExited(e -> root.setStyle(root.getStyle()
            .replace("#06b6d4", "#374151")));

        // Drag
        root.setOnDragDetected(e -> {
            Dragboard db = root.startDragAndDrop(TransferMode.COPY);
            ClipboardContent cc = new ClipboardContent();
            cc.putString(dragKey);
            db.setContent(cc);
            e.consume();
        });

        return root;
    }

    // ── Relationship line item ────────────────────────────────────────────────
    private Label relLine(String dragKey, String text) {
        Label l = new Label(text);
        l.setMaxWidth(Double.MAX_VALUE);
        l.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 11px; -fx-padding: 5 8; "
            + "-fx-background-color: rgba(26,26,36,0.5); -fx-background-radius: 3; "
            + "-fx-cursor: hand; -fx-font-family: monospace;");
        l.setOnMouseEntered(e -> l.setStyle(l.getStyle()
            .replace("rgba(26,26,36,0.5)", "rgba(42,42,55,0.9)")));
        l.setOnMouseExited(e -> l.setStyle(l.getStyle()
            .replace("rgba(42,42,55,0.9)", "rgba(26,26,36,0.5)")));
        l.setOnDragDetected(e -> {
            Dragboard db = l.startDragAndDrop(TransferMode.COPY);
            ClipboardContent cc = new ClipboardContent();
            cc.putString(dragKey);
            db.setContent(cc);
            e.consume();
        });
        return l;
    }

    // ── Section separator ─────────────────────────────────────────────────────
    private Label sectionSep(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #555566; -fx-font-size: 10px; "
            + "-fx-padding: 8 0 3 0; -fx-alignment: center;");
        l.setMaxWidth(Double.MAX_VALUE);
        l.setAlignment(Pos.CENTER);
        return l;
    }
}
