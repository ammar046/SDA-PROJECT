package com.umlytics.controllers;

import com.umlytics.domain.Project;
import com.umlytics.exceptions.ValidationException;
import com.umlytics.interfaces.IDiagramRepository;
import com.umlytics.interfaces.IProjectRepository;

import java.util.List;
import java.util.UUID;

// GRASP: Controller (Use Case)
public class ProjectController {
    private final IProjectRepository projectRepo;
    private final IDiagramRepository diagramRepo;

    public ProjectController(IProjectRepository projectRepo, IDiagramRepository diagramRepo) {
        this.projectRepo = projectRepo;
        this.diagramRepo = diagramRepo;
    }

    public Project createProject(String name, String desc) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Project name cannot be empty.");
        }
        boolean duplicate = projectRepo.findAll().stream()
                .anyMatch(project -> project.getName() != null && project.getName().equalsIgnoreCase(name));
        if (duplicate) {
            throw new ValidationException("Project name already exists.");
        }
        Project project = new Project();
        project.createProject(name, desc);
        projectRepo.save(project);
        return project;
    }

    public Project openProject(UUID projectId) {
        Project project = projectRepo.findById(projectId);
        if (project == null) {
            return null;
        }
        project.getDiagrams().clear();
        project.getDiagrams().addAll(diagramRepo.findByProject(projectId));
        return project;
    }

    public Project openProject(int projectId) {
        return openProject(UUID.nameUUIDFromBytes(("legacy-project-" + projectId).getBytes()));
    }

    public void updateProjectMetadata(UUID id, String name, String desc) {
        Project project = projectRepo.findById(id);
        if (project == null) {
            throw new ValidationException("Project not found.");
        }
        if (name == null || name.isBlank()) {
            throw new ValidationException("Project name cannot be empty.");
        }
        boolean duplicate = projectRepo.findAll().stream()
                .anyMatch(p -> !id.equals(p.getProjectId()) && p.getName() != null && p.getName().equalsIgnoreCase(name));
        if (duplicate) {
            throw new ValidationException("Project name already exists.");
        }
        project.updateProject(name, desc);
        projectRepo.update(project);
    }

    public void updateProjectMetadata(int id, String name, String desc) {
        updateProjectMetadata(UUID.nameUUIDFromBytes(("legacy-project-" + id).getBytes()), name, desc);
    }

    public void deleteProject(UUID id) {
        projectRepo.delete(id);
    }

    public void deleteProject(int id) {
        deleteProject(UUID.nameUUIDFromBytes(("legacy-project-" + id).getBytes()));
    }

    public List<Project> getAllProjects() {
        return projectRepo.findAll();
    }
}
