package com.umlytics.interfaces;

import com.umlytics.domain.EvaluationReport;
import com.umlytics.domain.valueobjects.ProjectContext;
import com.umlytics.domain.valueobjects.UMLModel;

public interface IAIEngine {
    UMLModel generateFromText(String description);
    EvaluationReport evaluateDesign(UMLModel model);
    String consultDesign(String query, ProjectContext context);
    String generateStructure(UMLModel model);
    UMLModel analyzeImage(byte[] imageData);
}
