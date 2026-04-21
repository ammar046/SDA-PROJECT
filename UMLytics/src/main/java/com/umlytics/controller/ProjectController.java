package com.umlytics.controller;

import com.umlytics.domain.Project;
import com.umlytics.interfaces.IProjectRepository;
import com.umlytics.persistence.ProjectRepositoryImpl;
import java.util.List;

/**
 * Manages Project CRUD.
 * GRASP: Controller, High Cohesion
 */
public class ProjectController {
    
    private final IProjectRepository repository;
    private Project currentProject;

    public ProjectController() {
        this.repository = new ProjectRepositoryImpl();
    }

    public ProjectController(IProjectRepository repo) {
        this.repository = repo;
    }

    public Project createProject(String name, String desc) {
        Project p = new Project(name, desc);
        repository.save(p);
        this.currentProject = p;
        return p;
    }

    public void openProject(int id) {
        this.currentProject = repository.findById(id);
    }

    public List<Project> getAllProjects() {
        return repository.findAll();
    }

    public void updateProject(String name, String desc) {
        if (currentProject != null) {
            currentProject.updateProject(name, desc);
            repository.update(currentProject);
        }
    }

    public void deleteProject(int id) {
        repository.delete(id);
        if (currentProject != null && currentProject.getProjectId() == id) {
            currentProject = null;
        }
    }

    public Project getCurrentProject() {
        return currentProject;
    }
}
