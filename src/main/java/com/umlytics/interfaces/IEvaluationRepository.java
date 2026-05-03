package com.umlytics.interfaces;

import com.umlytics.domain.DesignEvaluationReport;

import java.util.List;
import java.util.UUID;

public interface IEvaluationRepository {
    void save(DesignEvaluationReport r);

    List<DesignEvaluationReport> findByProject(UUID projectId);

    DesignEvaluationReport findByDiagram(UUID diagramId);

    void delete(UUID id);
}
