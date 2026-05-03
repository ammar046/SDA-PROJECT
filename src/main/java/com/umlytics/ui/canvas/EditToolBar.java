package com.umlytics.ui.canvas;

import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;

public class EditToolBar extends ToolBar {
    private final Button selectButton;
    private final Button panButton;
    private final Button classButton;
    private final Button relationshipButton;
    private final Button undoButton;
    private final Button redoButton;
    private final Button saveButton;

    public EditToolBar() {
        super();
        selectButton = new Button("Select");
        panButton = new Button("Pan");
        classButton = new Button("Class");
        relationshipButton = new Button("Relationship");
        undoButton = new Button("Undo");
        redoButton = new Button("Redo");
        saveButton = new Button("Save");
        getItems().addAll(selectButton, panButton, classButton, relationshipButton, undoButton, redoButton, saveButton);
    }

    public Button getSelectButton() {
        return selectButton;
    }

    public Button getPanButton() {
        return panButton;
    }

    public Button getClassButton() {
        return classButton;
    }

    public Button getRelationshipButton() {
        return relationshipButton;
    }

    public Button getUndoButton() {
        return undoButton;
    }

    public Button getRedoButton() {
        return redoButton;
    }

    public Button getSaveButton() {
        return saveButton;
    }
}
