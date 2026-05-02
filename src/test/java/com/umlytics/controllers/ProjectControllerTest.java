package com.umlytics.controllers;

import com.umlytics.domain.Project;
import com.umlytics.domain.UMLDiagram;
import com.umlytics.exceptions.ValidationException;
import com.umlytics.interfaces.IDiagramRepository;
import com.umlytics.interfaces.IProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProjectControllerTest {
    private InMemoryProjectRepository projectRepository;
    private ProjectController controller;

    @BeforeEach
    void setUp() {
        projectRepository = new InMemoryProjectRepository();
        controller = new ProjectController(projectRepository, new InMemoryDiagramRepository());
    }

    @Test
    void createProjectValidInput() {
        Project project = controller.createProject("UML Tool", "test");
        assertEquals("UML Tool", project.getName());
        assertEquals(1, projectRepository.findAll().size());
    }

    @Test
    void createProjectDuplicateNameThrows() {
        controller.createProject("SameName", "first");
        assertThrows(ValidationException.class, () -> controller.createProject("SameName", "second"));
    }

    @Test
    void createProjectEmptyNameThrows() {
        assertThrows(ValidationException.class, () -> controller.createProject(" ", "desc"));
    }

    @Test
    void getAllProjectsEmpty() {
        assertEquals(0, controller.getAllProjects().size());
    }

    @Test
    void updateProjectMetadataMissingProjectThrows() {
        assertThrows(ValidationException.class, () -> controller.updateProjectMetadata(UUID.randomUUID(), "name", "desc"));
    }

    @Test
    void openProjectLoadsDiagrams() {
        Project project = controller.createProject("Trace", "desc");
        Project loaded = controller.openProject(project.getProjectId());
        assertEquals(1, loaded.getDiagrams().size());
    }

    private static class InMemoryProjectRepository implements IProjectRepository {
        private final List<Project> projects = new ArrayList<>();

        @Override
        public void save(Project p) {
            p.setProjectId(UUID.randomUUID());
            projects.add(p);
        }

        @Override
        public Project findById(UUID id) {
            return projects.stream().filter(p -> id.equals(p.getProjectId())).findFirst().orElse(null);
        }

        @Override
        public List<Project> findAll() {
            return new ArrayList<>(projects);
        }

        @Override
        public void delete(UUID id) {
            projects.removeIf(p -> id.equals(p.getProjectId()));
        }

        @Override
        public void update(Project p) {
            // no-op for test.
        }
    }

    private static class InMemoryDiagramRepository implements IDiagramRepository {
        @Override
        public void save(UMLDiagram d) {
        }

        @Override
        public UMLDiagram findById(UUID id) {
            return null;
        }

        @Override
        public List<UMLDiagram> findByProject(UUID pid) {
            UMLDiagram diagram = new UMLDiagram();
            diagram.setDiagramId(UUID.randomUUID());
            diagram.setProjectId(pid);
            diagram.setTitle("Project Diagram");
            return List.of(diagram);
        }

        @Override
        public void delete(UUID id) {
        }

        @Override
        public void update(UMLDiagram d) {
        }
    }
}
