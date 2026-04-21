package com.umlytics.interfaces;

import com.umlytics.domain.EvaluationReport;
import java.util.List;

public interface IEvaluationRepository {
    void save(EvaluationReport r);
    List<EvaluationReport> findByProject(int projectId);
    EvaluationReport findByDiagram(int diagramId);
    void delete(int id);
}
