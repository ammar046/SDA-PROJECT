package com.umlytics.controllers;

import com.umlytics.domain.Project;
import com.umlytics.exceptions.ValidationException;
import com.umlytics.interfaces.IDiagramRepository;
import com.umlytics.interfaces.IProjectRepository;

import java.util.List;

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

    public Project retrieveProject(int id) {
        Project project = projectRepo.findById(id);
        if (project == null) {
            return null;
        }
        project.getDiagrams().clear();
        project.getDiagrams().addAll(diagramRepo.findByProject(id));
        return project;
    }

    public void maintainProject(int id, String name, String desc) {
        Project project = projectRepo.findById(id);
        if (project == null) {
            throw new ValidationException("Project not found.");
        }
        if (name == null || name.isBlank()) {
            throw new ValidationException("Project name cannot be empty.");
        }
        boolean duplicate = projectRepo.findAll().stream()
                .anyMatch(p -> p.getProjectId() != id && p.getName() != null && p.getName().equalsIgnoreCase(name));
        if (duplicate) {
            throw new ValidationException("Project name already exists.");
        }
        project.updateProject(name, desc);
        projectRepo.update(project);
    }

    public void deleteProject(int id) {
        projectRepo.delete(id);
    }

    public List<Project> listAllProjects() {
        return projectRepo.findAll();
    }
}
