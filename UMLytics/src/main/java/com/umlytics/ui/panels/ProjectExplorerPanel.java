package com.umlytics.ui.panels;

import com.umlytics.domain.Project;
import com.umlytics.domain.UMLDiagram;
import com.umlytics.enums.SourceType;
import com.umlytics.ui.MainWindow;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Project / Diagram explorer using TreeView.
 * GRASP: Information Expert
 */
public class ProjectExplorerPanel extends VBox {

    private final MainWindow facade;
    private final TreeView<Object> treeView;
    private TreeItem<Object> rootItem;

    public ProjectExplorerPanel(MainWindow facade) {
        this.facade = facade;
        setSpacing(6);
        setPadding(new Insets(8));
        getStyleClass().add("project-explorer");

        Label title = new Label("Explorer");
        title.getStyleClass().add("panel-header");

        treeView = new TreeView<>();
        treeView.setShowRoot(true);
        treeView.getStyleClass().add("tree-view");
        treeView.setCellFactory(tv -> new DiagramTreeCell());
        VBox.setVgrow(treeView, javafx.scene.layout.Priority.ALWAYS);

        // Double-click opens diagram
        treeView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && treeView.getSelectionModel().getSelectedItem() != null) {
                Object val = treeView.getSelectionModel().getSelectedItem().getValue();
                if (val instanceof UMLDiagram d) openDiagram(d);
            }
        });

        getChildren().addAll(title, treeView);
    }

    public void loadProject(Project p, List<UMLDiagram> diagrams) {
        rootItem = new TreeItem<>(p);
        rootItem.setExpanded(true);

        for (UMLDiagram d : diagrams) {
            TreeItem<Object> item = new TreeItem<>(d);
            rootItem.getChildren().add(item);
            attachDiagramContextMenu(item, d);
        }

        // "＋ New Diagram" create item
        TreeItem<Object> createItem = new TreeItem<>("__CREATE__");
        rootItem.getChildren().add(createItem);

        // Project root context menu
        attachProjectContextMenu(rootItem, p);

        treeView.setRoot(rootItem);
    }

    // ---- Context menus ----

    private void attachDiagramContextMenu(TreeItem<Object> item, UMLDiagram d) {
        ContextMenu cm = new ContextMenu();

        MenuItem open = new MenuItem("📂 Open");
        open.setOnAction(e -> openDiagram(d));

        MenuItem rename = new MenuItem("✏ Rename");
        rename.setOnAction(e -> {
            TextInputDialog dlg = new TextInputDialog(d.getTitle());
            dlg.setTitle("Rename Diagram");
            dlg.setHeaderText("New diagram title:");
            dlg.showAndWait().ifPresent(name -> {
                if (!name.trim().isEmpty()) {
                    facade.getDiagramController().renameDiagram(d.getDiagramId(), name.trim());
                    d.setTitle(name.trim());
                    treeView.refresh();
                    MainWindow.showToast("Renamed to \"" + name.trim() + "\"");
                }
            });
        });

        MenuItem duplicate = new MenuItem("⧉ Duplicate");
        duplicate.setOnAction(e -> {
            facade.getDiagramController().loadDiagram(d.getDiagramId());
            UMLDiagram src = facade.getDiagramController().getCurrentDiagram();
            if (src == null) return;
            UMLDiagram copy = facade.getDiagramController().createDiagram(
                    src.getProjectId(), "Copy of " + src.getTitle(), SourceType.MANUAL);
            src.getClasses().forEach(copy::addUMLClass);
            src.getRelationships().forEach(copy::addRelationship);
            facade.getDiagramController().saveCurrentDiagram();
            facade.refreshProjectExplorer();
            MainWindow.showToast("Duplicated diagram");
        });

        MenuItem delete = new MenuItem("🗑 Delete");
        delete.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Delete \"" + d.getTitle() + "\"? This cannot be undone.", ButtonType.YES, ButtonType.CANCEL);
            confirm.setTitle("Delete Diagram");
            confirm.setHeaderText(null);
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.YES) {
                    facade.getDiagramController().deleteDiagram(d.getDiagramId());
                    if (facade.getDiagramController().getCurrentDiagram() == null) {
                        facade.getMainCanvas().renderDiagram(null);
                    }
                    facade.refreshProjectExplorer();
                    MainWindow.showToast("Diagram deleted");
                }
            });
        });

        cm.getItems().addAll(open, rename, duplicate, new SeparatorMenuItem(), delete);

        // Attach to tree cell via lookup — stored on item for cell to use
        item.setValue(d); // ensure value is diagram
    }

    private void attachProjectContextMenu(TreeItem<Object> item, Project p) {
        // Handled in cell factory below
    }

    // ---- Actions ----

    private void openDiagram(UMLDiagram d) {
        facade.getDiagramController().loadDiagram(d.getDiagramId());
        UMLDiagram loaded = facade.getDiagramController().getCurrentDiagram();
        if (loaded != null) {
            facade.getMainCanvas().renderDiagram(loaded);
        }
        // Scroll to item
        treeView.getSelectionModel().select(findItem(d));
        treeView.scrollTo(treeView.getSelectionModel().getSelectedIndex());
    }

    private TreeItem<Object> findItem(UMLDiagram d) {
        if (rootItem == null) return null;
        for (TreeItem<Object> child : rootItem.getChildren()) {
            if (child.getValue() instanceof UMLDiagram dd && dd.getDiagramId() == d.getDiagramId()) {
                return child;
            }
        }
        return null;
    }

    // =====================================================================
    // Custom TreeCell
    // =====================================================================

    private class DiagramTreeCell extends TreeCell<Object> {

        private ContextMenu projectContextMenu;

        DiagramTreeCell() {
            buildProjectContextMenu();
        }

        @Override
        protected void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                setContextMenu(null);
                return;
            }

            if (item instanceof Project p) {
                setText("🗂 " + p.getName());
                setStyle("-fx-font-weight: bold; -fx-text-fill: #d4d4d4;");
                setContextMenu(projectContextMenu);

            } else if (item instanceof UMLDiagram d) {
                String title = d.getTitle() != null ? d.getTitle() : "(Untitled)";
                if (title.length() > 26) title = title.substring(0, 23) + "...";
                setText("📄 " + title);
                setStyle("-fx-text-fill: #c0c0c0;");
                setContextMenu(buildDiagramContextMenu(d));

            } else if ("__CREATE__".equals(item)) {
                setText("＋ New Diagram");
                setStyle("-fx-font-style: italic; -fx-text-fill: #666666;");
                setContextMenu(null);
                setOnMouseClicked(e -> {
                    if (e.getClickCount() == 1 || e.getClickCount() == 2) {
                        Project p = facade.getProjectController().getCurrentProject();
                        if (p != null) {
                            facade.getDiagramController().createDiagram(
                                    p.getProjectId(), "Diagram " + System.currentTimeMillis(), SourceType.MANUAL);
                            facade.refreshProjectExplorer();
                        }
                    }
                });
            }
        }

        private ContextMenu buildDiagramContextMenu(UMLDiagram d) {
            ContextMenu cm = new ContextMenu();
            MenuItem open = new MenuItem("📂 Open");
            open.setOnAction(e -> openDiagram(d));

            MenuItem rename = new MenuItem("✏ Rename");
            rename.setOnAction(e -> {
                TextInputDialog dlg = new TextInputDialog(d.getTitle());
                dlg.setTitle("Rename Diagram"); dlg.setHeaderText("New title:");
                dlg.showAndWait().ifPresent(name -> {
                    if (!name.trim().isEmpty()) {
                        facade.getDiagramController().renameDiagram(d.getDiagramId(), name.trim());
                        d.setTitle(name.trim());
                        treeView.refresh();
                        MainWindow.showToast("Renamed");
                    }
                });
            });

            MenuItem duplicate = new MenuItem("⧉ Duplicate");
            duplicate.setOnAction(e -> {
                facade.getDiagramController().loadDiagram(d.getDiagramId());
                UMLDiagram src = facade.getDiagramController().getCurrentDiagram();
                if (src == null) return;
                UMLDiagram copy = facade.getDiagramController().createDiagram(
                        src.getProjectId(), "Copy of " + src.getTitle(), SourceType.MANUAL);
                src.getClasses().forEach(copy::addUMLClass);
                src.getRelationships().forEach(copy::addRelationship);
                facade.getDiagramController().saveCurrentDiagram();
                facade.refreshProjectExplorer();
                MainWindow.showToast("Duplicated");
            });

            MenuItem delete = new MenuItem("🗑 Delete");
            delete.setOnAction(e -> {
                Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                        "Delete \"" + d.getTitle() + "\"?", ButtonType.YES, ButtonType.CANCEL);
                conf.setTitle("Delete"); conf.setHeaderText(null);
                conf.showAndWait().ifPresent(bt -> {
                    if (bt == ButtonType.YES) {
                        facade.getDiagramController().deleteDiagram(d.getDiagramId());
                        if (facade.getDiagramController().getCurrentDiagram() == null)
                            facade.getMainCanvas().renderDiagram(null);
                        facade.refreshProjectExplorer();
                        MainWindow.showToast("Diagram deleted");
                    }
                });
            });

            cm.getItems().addAll(open, rename, duplicate, new SeparatorMenuItem(), delete);
            return cm;
        }

        private void buildProjectContextMenu() {
            projectContextMenu = new ContextMenu();

            MenuItem renameProj = new MenuItem("✏ Rename Project");
            renameProj.setOnAction(e -> {
                Project p = facade.getProjectController().getCurrentProject();
                if (p == null) return;
                TextInputDialog dlg = new TextInputDialog(p.getName());
                dlg.setTitle("Rename Project"); dlg.setHeaderText("New project name:");
                dlg.showAndWait().ifPresent(name -> {
                    if (!name.trim().isEmpty()) {
                        facade.getProjectController().updateProject(name.trim(), p.getDescription());
                        facade.refreshProjectExplorer();
                        MainWindow.showToast("Project renamed");
                    }
                });
            });

            MenuItem newDiagram = new MenuItem("＋ New Diagram");
            newDiagram.setOnAction(e -> {
                Project p = facade.getProjectController().getCurrentProject();
                if (p != null) {
                    facade.getDiagramController().createDiagram(
                            p.getProjectId(), "Diagram " + System.currentTimeMillis(), SourceType.MANUAL);
                    facade.refreshProjectExplorer();
                }
            });

            projectContextMenu.getItems().addAll(renameProj, newDiagram);
        }
    }
}
