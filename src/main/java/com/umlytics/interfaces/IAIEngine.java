package com.umlytics.interfaces;

import com.umlytics.domain.EvaluationReport;
import com.umlytics.domain.ProjectContext;
import com.umlytics.domain.UMLModel;

public interface IAIEngine {
    UMLModel generateFromText(String desc);

    EvaluationReport evaluateDesign(UMLModel m);

    String consultDesign(String q, ProjectContext ctx);

    String generateStructure(UMLModel m);

    UMLModel analyzeImage(byte[] data);
}
