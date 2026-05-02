package com.umlytics.ui;

import com.umlytics.controllers.ProjectController;
import com.umlytics.domain.Project;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class ProjectDashboardPanel extends VBox {
    private ProjectController projectCtrl;
    private final ListView<Project> projectList;
    private final Label detailsLabel;
    private Consumer<Project> openProjectHandler;

    public ProjectDashboardPanel() {
        this.projectList = new ListView<>();
        this.detailsLabel = new Label("Select a project");
        setSpacing(10);
        setPadding(new Insets(10));

        Button newBtn = new Button("New Project");
        newBtn.setOnAction(event -> onCreateProject());
        Button openBtn = new Button("Open");
        openBtn.setOnAction(event -> onOpenProject());
        Button refreshBtn = new Button("Refresh");
        refreshBtn.setOnAction(event -> refreshProjects());
        Button editBtn = new Button("Edit");
        editBtn.setOnAction(event -> onEditProject());
        Button deleteBtn = new Button("Delete");
        deleteBtn.setOnAction(event -> onDeleteProject());
        HBox actions = new HBox(8, newBtn, openBtn, refreshBtn, editBtn, deleteBtn);

        projectList.setPlaceholder(new Label("No projects yet. Create one to get started."));
        projectList.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Project item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " • " + (item.getLastModifiedDate() == null ? "-" : item.getLastModifiedDate()));
                }
            }
        });
        projectList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected == null) {
                detailsLabel.setText("Select a project");
            } else {
                detailsLabel.setText("Name: " + selected.getName() + "\nDescription: " + selected.getDescription());
            }
        });
        VBox.setVgrow(projectList, Priority.ALWAYS);

        getChildren().addAll(actions, projectList, detailsLabel);
    }

    public void onCreateProject() {
        if (projectCtrl == null) {
            return;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("New Project");
        dialog.setHeaderText("Create a new project");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField nameField = new TextField();
        nameField.setPromptText("Project name");
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Project description");
        descriptionArea.setPrefRowCount(4);

        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(8);
        form.addRow(0, new Label("Name:"), nameField);
        form.addRow(1, new Label("Description:"), descriptionArea);
        dialog.getDialogPane().setContent(form);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }
        try {
            projectCtrl.createProject(nameField.getText(), descriptionArea.getText());
            refreshProjects();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    public void onOpenProject() {
        Project selected = projectList.getSelectionModel().getSelectedItem();
        if (selected != null && openProjectHandler != null) {
            openProjectHandler.accept(selected);
        }
    }

    public void displayProjects(List<Project> list) {
        if (projectList != null && list != null) {
            projectList.setItems(FXCollections.observableArrayList(list));
        }
    }

    public void setProjectCtrl(ProjectController projectCtrl) {
        this.projectCtrl = projectCtrl;
    }

    public void setOpenProjectHandler(Consumer<Project> openProjectHandler) {
        this.openProjectHandler = openProjectHandler;
    }

    public void refreshProjects() {
        if (projectCtrl == null) {
            return;
        }
        displayProjects(projectCtrl.listAllProjects());
    }

    private void onEditProject() {
        if (projectCtrl == null) {
            return;
        }
        Project selected = projectList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        TextInputDialog nameDialog = new TextInputDialog(selected.getName());
        nameDialog.setHeaderText("Edit Project Name");
        nameDialog.setContentText("Name:");
        Optional<String> maybeName = nameDialog.showAndWait();
        if (maybeName.isEmpty()) {
            return;
        }
        TextArea descriptionArea = new TextArea(selected.getDescription() == null ? "" : selected.getDescription());
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText("Edit Description");
        alert.getDialogPane().setContent(descriptionArea);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }
        try {
            projectCtrl.maintainProject(selected.getProjectId(), maybeName.get(), descriptionArea.getText());
            refreshProjects();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    private void onDeleteProject() {
        if (projectCtrl == null) {
            return;
        }
        Project selected = projectList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete project " + selected.getName() + "?", ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            projectCtrl.deleteProject(selected.getProjectId());
            refreshProjects();
        }
    }
}
