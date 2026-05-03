package com.umlytics.interfaces;

import com.umlytics.domain.Project;

import java.util.List;
import java.util.UUID;

public interface IProjectRepository {
    void save(Project p);

    Project findById(UUID id);

    List<Project> findAll();

    void delete(UUID id);

    void update(Project p);
}
