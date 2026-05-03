package com.umlytics.services;

import com.umlytics.domain.UMLDiagram;
import com.umlytics.enums.ExportFormat;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagramExportServiceTest {
    @Test
    void exportEachFormatProducesFile() throws Exception {
        DiagramExportService service = new DiagramExportService();
        UMLDiagram diagram = new UMLDiagram();
        diagram.setTitle("ExportTest");

        for (ExportFormat format : service.getSupportedFormats()) {
            File out = File.createTempFile("diagram-", "." + format.name().toLowerCase());
            service.export(diagram, format, out.getAbsolutePath());
            assertTrue(out.length() > 0);
            out.delete();
        }
    }
}
