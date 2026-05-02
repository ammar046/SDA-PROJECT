package com.umlytics.interfaces;

import com.umlytics.domain.EvaluationReport;

import java.util.List;

public interface IEvaluationRepository {
    void save(EvaluationReport r);

    List<EvaluationReport> findByProject(int pid);

    EvaluationReport findByDiagram(int did);

    void delete(int id);
}
