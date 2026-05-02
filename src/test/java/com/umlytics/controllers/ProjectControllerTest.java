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
    void listAllProjectsEmpty() {
        assertEquals(0, controller.listAllProjects().size());
    }

    @Test
    void maintainProjectMissingProjectThrows() {
        assertThrows(ValidationException.class, () -> controller.maintainProject(999, "name", "desc"));
    }

    @Test
    void retrieveProjectLoadsDiagrams() {
        Project project = controller.createProject("Trace", "desc");
        Project loaded = controller.retrieveProject(project.getProjectId());
        assertEquals(1, loaded.getDiagrams().size());
    }

    private static class InMemoryProjectRepository implements IProjectRepository {
        private final List<Project> projects = new ArrayList<>();
        private int id = 1;

        @Override
        public void save(Project p) {
            p.setProjectId(id++);
            projects.add(p);
        }

        @Override
        public Project findById(int id) {
            return projects.stream().filter(p -> p.getProjectId() == id).findFirst().orElse(null);
        }

        @Override
        public List<Project> findAll() {
            return new ArrayList<>(projects);
        }

        @Override
        public void delete(int id) {
            projects.removeIf(p -> p.getProjectId() == id);
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
        public UMLDiagram findById(int id) {
            return null;
        }

        @Override
        public List<UMLDiagram> findByProject(int pid) {
            UMLDiagram diagram = new UMLDiagram();
            diagram.setDiagramId(100 + pid);
            diagram.setProjectId(pid);
            diagram.setTitle("Project " + pid + " Diagram");
            return List.of(diagram);
        }

        @Override
        public void delete(int id) {
        }

        @Override
        public void update(UMLDiagram d) {
        }
    }
}
