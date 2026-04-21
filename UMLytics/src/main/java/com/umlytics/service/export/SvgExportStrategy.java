package com.umlytics.service.export;

import com.umlytics.domain.UMLClass;
import com.umlytics.domain.UMLDiagram;
import com.umlytics.domain.relationships.Relationship;
import com.umlytics.ui.canvas.MainCanvas;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * GoF: Strategy — SVG export implementation.
 * GRASP: Pure Fabrication
 */
public class SvgExportStrategy implements IExportStrategy {

    @Override
    public void export(UMLDiagram diagram, String path, MainCanvas canvas) throws Exception {
        StringBuilder svg = new StringBuilder();
        svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"2000\" height=\"2000\" ")
           .append("style=\"background:#121212;\">\n");

        // Render each class as a rect + text group
        for (UMLClass cls : diagram.getClasses()) {
            double x  = cls.getPositionX();
            double y  = cls.getPositionY();
            double w  = 180;
            int headerH = 48;
            int attrH   = Math.max(24, cls.getAttributes().size() * 18 + 8);
            int methH   = Math.max(24, cls.getMethods().size() * 18 + 8);
            double totalH = headerH + attrH + methH;

            String headerColour = cls.isInterface() ? "#1a6040"
                    : cls.isAbstract()              ? "#5a2d82"
                    : "Enumeration".equalsIgnoreCase(cls.getStereotype()) ? "#3d3d00"
                    : "#2d5a8e";

            // Box background
            svg.append(String.format(
                "<rect x=\"%.0f\" y=\"%.0f\" width=\"%.0f\" height=\"%.0f\" "
                + "rx=\"6\" fill=\"#252526\" stroke=\"#4a4a4a\" stroke-width=\"1\"/>\n",
                x, y, w, totalH));

            // Header background
            svg.append(String.format(
                "<rect x=\"%.0f\" y=\"%.0f\" width=\"%.0f\" height=\"%d\" "
                + "rx=\"6\" fill=\"%s\"/>\n",
                x, y, w, headerH, headerColour));
            // Smooth the bottom of header
            svg.append(String.format(
                "<rect x=\"%.0f\" y=\"%.0f\" width=\"%.0f\" height=\"8\" fill=\"%s\"/>\n",
                x, y + headerH - 8, w, headerColour));

            // Stereotype text
            String stereo = cls.isInterface() ? "<<Interface>>"
                    : cls.isAbstract() ? "<< Abstract >>"
                    : "Enumeration".equalsIgnoreCase(cls.getStereotype()) ? "<<Enumeration>>"
                    : "<< Class >>";
            svg.append(String.format(
                "<text x=\"%.0f\" y=\"%.0f\" text-anchor=\"middle\" "
                + "font-family=\"Segoe UI,sans-serif\" font-size=\"9\" fill=\"rgba(255,255,255,0.7)\">%s</text>\n",
                x + w / 2, y + 16, escXml(stereo)));

            // Class name
            String nameStyle = cls.isAbstract() || cls.isInterface()
                    ? "font-style=\"italic\"" : "";
            svg.append(String.format(
                "<text x=\"%.0f\" y=\"%.0f\" text-anchor=\"middle\" "
                + "font-family=\"Segoe UI,sans-serif\" font-size=\"13\" font-weight=\"bold\" "
                + "fill=\"white\" %s>%s</text>\n",
                x + w / 2, y + 36, nameStyle, escXml(cls.getName())));

            // Divider line
            svg.append(String.format(
                "<line x1=\"%.0f\" y1=\"%.0f\" x2=\"%.0f\" y2=\"%.0f\" stroke=\"#4a4a4a\"/>\n",
                x, y + headerH, x + w, y + headerH));

            // Attributes
            int ay = headerH + 16;
            for (var a : cls.getAttributes()) {
                svg.append(String.format(
                    "<text x=\"%.0f\" y=\"%.0f\" font-family=\"Consolas,monospace\" "
                    + "font-size=\"10\" fill=\"#d4d4d4\">%s</text>\n",
                    x + 8, y + ay, escXml(a.toUMLString())));
                ay += 18;
            }

            // Divider
            svg.append(String.format(
                "<line x1=\"%.0f\" y1=\"%.0f\" x2=\"%.0f\" y2=\"%.0f\" stroke=\"#4a4a4a\"/>\n",
                x, y + headerH + attrH, x + w, y + headerH + attrH));

            // Methods
            int my = headerH + attrH + 16;
            for (var m : cls.getMethods()) {
                svg.append(String.format(
                    "<text x=\"%.0f\" y=\"%.0f\" font-family=\"Consolas,monospace\" "
                    + "font-size=\"10\" fill=\"#d4d4d4\">%s</text>\n",
                    x + 8, y + my, escXml(m.getSignature())));
                my += 18;
            }
        }

        // Render relationships as lines
        for (Relationship r : diagram.getRelationships()) {
            if (r.getSourceClass() == null || r.getTargetClass() == null) continue;
            double sx = r.getSourceClass().getPositionX() + 90;
            double sy = r.getSourceClass().getPositionY() + 30;
            double tx = r.getTargetClass().getPositionX() + 90;
            double ty = r.getTargetClass().getPositionY() + 30;

            boolean dashed = r.getType().name().equals("REALIZATION")
                          || r.getType().name().equals("DEPENDENCY");
            String dash = dashed ? "stroke-dasharray=\"8 4\"" : "";

            svg.append(String.format(
                "<line x1=\"%.0f\" y1=\"%.0f\" x2=\"%.0f\" y2=\"%.0f\" "
                + "stroke=\"#a9b7c6\" stroke-width=\"1.5\" %s/>\n",
                sx, sy, tx, ty, dash));

            // Label
            svg.append(String.format(
                "<text x=\"%.0f\" y=\"%.0f\" font-family=\"Segoe UI\" font-size=\"10\" "
                + "fill=\"#888888\">%s</text>\n",
                (sx + tx) / 2 + 4, (sy + ty) / 2 - 4,
                escXml(r.getType().getDisplayName())));
        }

        svg.append("</svg>\n");
        Files.writeString(Path.of(path), svg.toString());
        System.out.println("[SvgExportStrategy] Exported SVG to: " + path);
    }

    private String escXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
