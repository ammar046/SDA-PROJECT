package com.umlytics.ui.panels;

import com.umlytics.domain.Project;
import com.umlytics.domain.UMLDiagram;
import com.umlytics.ui.MainWindow;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.UUID;

// GRASP: Information Expert
public class ProjectExplorerPanel extends VBox {

    private static final String WORKSPACE_ROOT = "__WORKSPACE__";

    /** Double-click target: create a blank diagram under this project. */
    public static final class NewDiagramSlot {
        public final UUID projectId;

        public NewDiagramSlot(UUID projectId) {
            this.projectId = projectId;
        }
    }

    private final MainWindow facade;
    private final TreeView<Object> tree;

    public ProjectExplorerPanel(MainWindow facade) {
        this.facade = facade;
        getStyleClass().add("project-explorer");
        setPadding(new Insets(8));
        setSpacing(4);

        Label title = new Label("Explorer");
        title.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 14px; -fx-font-weight: bold; "
            + "-fx-padding: 0 0 8 0; -fx-border-color: #374151; -fx-border-width: 0 0 1 0;");
        title.setMaxWidth(Double.MAX_VALUE);

        tree = new TreeView<>();
        tree.setShowRoot(true);
        tree.getStyleClass().add("tree-view");
        tree.setCellFactory(tv -> new ExplorerCell());
        VBox.setVgrow(tree, Priority.ALWAYS);
        setMaxHeight(Double.MAX_VALUE);
        setMinHeight(100);

        tree.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                var sel = tree.getSelectionModel().getSelectedItem();
                if (sel == null) return;
                Object v = sel.getValue();
                if (v instanceof UMLDiagram d) {
                    facade.openDiagram(d);
                } else if (v instanceof NewDiagramSlot slot) {
                    facade.newDiagramForProject(slot.projectId);
                } else if (v instanceof Project p) {
                    facade.setActiveProjectId(p.getProjectId());
                }
            }
        });

        getChildren().addAll(title, tree);
    }

    /** Builds a workspace tree: all projects, each with its diagrams and a “new diagram” slot. */
    public void loadWorkspace(List<Project> projects) {
        TreeItem<Object> root = new TreeItem<>(WORKSPACE_ROOT);
        root.setExpanded(true);

        for (Project p : projects) {
            TreeItem<Object> pNode = new TreeItem<>(p);
            pNode.setExpanded(true);
            for (UMLDiagram d : facade.getDiagramController().listProjectDiagrams(p.getProjectId())) {
                TreeItem<Object> dNode = new TreeItem<>(d);
                pNode.getChildren().add(dNode);
            }
            pNode.getChildren().add(new TreeItem<>(new NewDiagramSlot(p.getProjectId())));
            root.getChildren().add(pNode);
        }
        tree.setRoot(root);
    }

    private class ExplorerCell extends TreeCell<Object> {
        @Override
        protected void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                setContextMenu(null);
                return;
            }
            if (WORKSPACE_ROOT.equals(item)) {
                setText("📂  Workspace");
                setStyle(null);
                setContextMenu(null);
            } else if (item instanceof Project p) {
                setText("📁  " + p.getName());
                setStyle(null);
                setContextMenu(null);
            } else if (item instanceof UMLDiagram d) {
                setText("📄  " + d.getTitle());
                setStyle(null);
                MenuItem open = new MenuItem("Open");
                MenuItem delete = new MenuItem("Delete");
                open.setOnAction(e -> facade.openDiagram(d));
                delete.setOnAction(e -> {
                    facade.getEditorPanel().deleteDiagram(d.getDiagramId());
                    facade.refreshProjectExplorer();
                    MainWindow.showToast("Deleted: " + d.getTitle());
                });
                setContextMenu(new ContextMenu(open, new SeparatorMenuItem(), delete));
            } else if (item instanceof NewDiagramSlot) {
                setText("＋  New Diagram");
                setStyle("-fx-text-fill: #06b6d4; -fx-font-style: italic;");
                setContextMenu(null);
            }
        }
    }
}
