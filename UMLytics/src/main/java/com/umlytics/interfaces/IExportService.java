package com.umlytics.interfaces;

import com.umlytics.domain.UMLDiagram;
import com.umlytics.enums.ExportFormat;
import java.util.List;

public interface IExportService {
    void export(UMLDiagram d, ExportFormat format, String path);
    List<ExportFormat> getSupportedFormats();
}
