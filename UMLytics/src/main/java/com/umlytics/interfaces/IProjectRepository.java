package com.umlytics.interfaces;

import com.umlytics.domain.Project;
import java.util.List;

public interface IProjectRepository {
    void save(Project p);
    Project findById(int id);
    List<Project> findAll();
    void delete(int id);
    void update(Project p);
}
