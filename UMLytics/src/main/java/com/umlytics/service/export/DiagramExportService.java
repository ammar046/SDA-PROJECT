package com.umlytics.service.export;

import com.umlytics.domain.UMLDiagram;
import com.umlytics.enums.ExportFormat;
import com.umlytics.interfaces.IExportService;
import com.umlytics.ui.canvas.MainCanvas;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Context for the export Strategy pattern.
 * Holds a map of format → strategy and dispatches accordingly.
 * GoF: Strategy (context)
 * GRASP: Pure Fabrication, Low Coupling
 */
public class DiagramExportService implements IExportService {

    // GoF: Strategy — map of export formats to strategies
    private final Map<ExportFormat, IExportStrategy> strategies = new EnumMap<>(ExportFormat.class);
    private MainCanvas mainCanvas;

    public DiagramExportService() {
        strategies.put(ExportFormat.PNG, new PngExportStrategy());
        strategies.put(ExportFormat.SVG, new SvgExportStrategy());
        strategies.put(ExportFormat.PDF, new PdfExportStrategy());
    }

    /** Inject the canvas reference for snapshot-based exports (PNG, PDF). */
    public void setMainCanvas(MainCanvas canvas) {
        this.mainCanvas = canvas;
    }

    @Override
    public List<ExportFormat> getSupportedFormats() {
        return Arrays.asList(ExportFormat.values());
    }

    @Override
    public void export(UMLDiagram d, ExportFormat format, String path) {
        if (d == null) throw new IllegalArgumentException("Diagram cannot be null");
        IExportStrategy strategy = strategies.get(format);
        if (strategy == null) throw new UnsupportedOperationException("No strategy for: " + format);
        try {
            strategy.export(d, path, mainCanvas);
        } catch (Exception e) {
            System.err.println("[DiagramExportService] Export failed: " + e.getMessage());
            throw new RuntimeException("Export failed: " + e.getMessage(), e);
        }
    }
}
