package com.umlytics.services;

import com.umlytics.domain.AssociationRelationship;
import com.umlytics.domain.Relationship;
import com.umlytics.domain.ConceptualClass;
import com.umlytics.domain.UMLDiagram;
import com.umlytics.enums.ExportFormat;
import com.umlytics.enums.RelationshipType;
import com.umlytics.enums.Visibility;
import com.umlytics.exceptions.UnsupportedFormatException;
import com.umlytics.interfaces.IExportService;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.w3c.dom.Document;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.List;

// GRASP: Pure Fabrication
public class DiagramExportService implements IExportService {
    private static final int DEFAULT_CLASS_WIDTH = 200;
    private static final int DEFAULT_CLASS_HEIGHT = 140;
    private static final int HEADER_HEIGHT = 30;
    private static final int PADDING = 40;

    public DiagramExportService() {
    }

    @Override
    public void export(UMLDiagram d, ExportFormat fmt, String path) {
        if (!getSupportedFormats().contains(fmt)) {
            throw new UnsupportedFormatException("Unsupported export format: " + fmt);
        }
        ensureParentDirectory(path);
        byte[] bytes;
        switch (fmt) {
            case PNG -> bytes = renderToPNG(d);
            case PDF -> bytes = renderToPDF(d);
            case SVG -> bytes = renderToSVG(d);
            default -> throw new UnsupportedFormatException("Unsupported export format: " + fmt);
        }
        try (FileOutputStream outputStream = new FileOutputStream(path)) {
            outputStream.write(bytes);
        } catch (Exception e) {
            throw new UnsupportedFormatException("Failed to write export output: " + e.getMessage());
        }
    }

    @Override
    public List<ExportFormat> getSupportedFormats() {
        return List.of(ExportFormat.PNG, ExportFormat.PDF, ExportFormat.SVG);
    }

    private byte[] renderToPNG(UMLDiagram d) {
        try {
            BufferedImage image = renderDiagramImage(d);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new UnsupportedFormatException("Failed to render PNG output: " + e.getMessage());
        }
    }

    private byte[] renderToPDF(UMLDiagram d) {
        try (PDDocument document = new PDDocument()) {
            BufferedImage image = renderDiagramImage(d);
            float pageWidth = image.getWidth();
            float pageHeight = image.getHeight();
            PDPage page = new PDPage(new PDRectangle(pageWidth, pageHeight));
            document.addPage(page);
            PDImageXObject pdImage = LosslessFactory.createFromImage(document, image);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.drawImage(pdImage, 0, 0, pageWidth, pageHeight);
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new UnsupportedFormatException("Failed to render PDF output: " + e.getMessage());
        }
    }

    private byte[] renderToSVG(UMLDiagram d) {
        try {
            DiagramBounds bounds = calculateBounds(d);
            Document document = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .getDOMImplementation()
                    .createDocument("http://www.w3.org/2000/svg", "svg", null);
            SVGGraphics2D g2 = new SVGGraphics2D(document);
            g2.setSVGCanvasSize(new java.awt.Dimension((int) bounds.width, (int) bounds.height));
            paintDiagram(g2, d, bounds);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            g2.stream(new java.io.OutputStreamWriter(outputStream), true);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new UnsupportedFormatException("Failed to render SVG output: " + e.getMessage());
        }
    }

    private BufferedImage renderDiagramImage(UMLDiagram diagram) {
        DiagramBounds bounds = calculateBounds(diagram);
        BufferedImage image = new BufferedImage((int) bounds.width, (int) bounds.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        paintDiagram(g2, diagram, bounds);
        g2.dispose();
        return image;
    }

    private void paintDiagram(Graphics2D g2, UMLDiagram diagram, DiagramBounds bounds) {
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, (int) bounds.width, (int) bounds.height);

        if (diagram == null) {
            return;
        }
        for (Relationship relationship : diagram.getRelationships()) {
            drawRelationship(g2, relationship, bounds);
        }
        for (ConceptualClass umlClass : diagram.getClasses()) {
            drawClass(g2, umlClass, bounds);
        }
    }

    private void drawClass(Graphics2D g2, ConceptualClass umlClass, DiagramBounds bounds) {
        int x = (int) Math.round(umlClass.getPositionX() - bounds.minX + PADDING);
        int y = (int) Math.round(umlClass.getPositionY() - bounds.minY + PADDING);
        int width = classWidth(umlClass);
        int height = classHeight(umlClass);
        int compartmentCut = Math.min(height - 20, HEADER_HEIGHT + 44);

        Color border = toClassBorderColor(umlClass.getBorderColor());
        Color header = toClassHeaderColor(umlClass.getHeaderColor());
        g2.setColor(Color.WHITE);
        g2.fillRect(x, y, width, height);
        g2.setColor(header);
        g2.fillRect(x, y, width, HEADER_HEIGHT);
        g2.setColor(border);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(x, y, width, height);
        g2.drawLine(x, y + HEADER_HEIGHT, x + width, y + HEADER_HEIGHT);
        g2.drawLine(x, y + compartmentCut, x + width, y + compartmentCut);

        g2.setColor(Color.BLACK);
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        g2.drawString(safe(umlClass.getName(), "Class"), x + 8, y + 20);

        int fontSize = (int) Math.max(10, Math.min(24, umlClass.getMemberFontSize()));
        g2.setFont(new Font("SansSerif", Font.PLAIN, fontSize));
        int lineY = y + HEADER_HEIGHT + 17;
        for (com.umlytics.domain.Attribute attribute : umlClass.getAttributes()) {
            g2.drawString(formatAttribute(attribute), x + 8, lineY);
            lineY += fontSize + 2;
            if (lineY > y + compartmentCut - 4) {
                break;
            }
        }
        lineY = y + compartmentCut + 18;
        for (com.umlytics.domain.Method method : umlClass.getMethods()) {
            g2.drawString(formatMethod(method), x + 8, lineY);
            lineY += fontSize + 2;
            if (lineY > y + height - 8) {
                break;
            }
        }
    }

    private void drawRelationship(Graphics2D g2, Relationship relationship, DiagramBounds bounds) {
        ConceptualClass source = relationship.getSource();
        ConceptualClass target = relationship.getTarget();
        if (source == null || target == null) {
            return;
        }

        double sx = source.getPositionX() - bounds.minX + PADDING + classWidth(source) / 2.0;
        double sy = source.getPositionY() - bounds.minY + PADDING + classHeight(source) / 2.0;
        double tx = target.getPositionX() - bounds.minX + PADDING + classWidth(target) / 2.0;
        double ty = target.getPositionY() - bounds.minY + PADDING + classHeight(target) / 2.0;
        double bendX = relationship.getBendX() == null ? (sx + tx) / 2.0 : relationship.getBendX() - bounds.minX + PADDING;
        double bendY = (sy + ty) / 2.0;

        g2.setColor(toEdgeColor(relationship.getEdgeColor()));
        float dash = relationship.isDashed() || relationship.getType() == RelationshipType.DEPENDENCY || relationship.getType() == RelationshipType.REALIZATION ? 6f : 0f;
        if (dash > 0) {
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10f, new float[]{dash, dash}, 0f));
        } else {
            g2.setStroke(new BasicStroke(2f));
        }
        Path2D path = new Path2D.Double();
        path.moveTo(sx, sy);
        path.lineTo(bendX, bendY);
        path.lineTo(tx, ty);
        g2.draw(path);

        drawArrowMarker(g2, relationship, bendX, bendY, tx, ty);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2.setColor(Color.DARK_GRAY);
        if (relationship.getLabel() != null && !relationship.getLabel().isBlank()) {
            g2.drawString(relationship.getLabel(), (int) bendX + 5, (int) bendY - 4);
        }
        if (relationship.getSourceMultiplicity() != null && !relationship.getSourceMultiplicity().isBlank()) {
            g2.drawString(relationship.getSourceMultiplicity(), (int) sx - 10, (int) sy - 5);
        }
        if (relationship.getTargetMultiplicity() != null && !relationship.getTargetMultiplicity().isBlank()) {
            g2.drawString(relationship.getTargetMultiplicity(), (int) tx + 4, (int) ty - 5);
        }
    }

    private void drawArrowMarker(Graphics2D g2, Relationship relationship, double fromX, double fromY, double toX, double toY) {
        double angle = Math.atan2(toY - fromY, toX - fromX);
        double len = 14;
        double wing = 6;
        double x1 = toX - len * Math.cos(angle - Math.PI / 6);
        double y1 = toY - len * Math.sin(angle - Math.PI / 6);
        double x2 = toX - len * Math.cos(angle + Math.PI / 6);
        double y2 = toY - len * Math.sin(angle + Math.PI / 6);
        RelationshipType type = relationship.getType();

        if (type == RelationshipType.INHERITANCE || type == RelationshipType.REALIZATION) {
            Path2D triangle = new Path2D.Double();
            triangle.moveTo(toX, toY);
            triangle.lineTo(x1, y1);
            triangle.lineTo(x2, y2);
            triangle.closePath();
            g2.setColor(Color.WHITE);
            g2.fill(triangle);
            g2.setColor(toEdgeColor(relationship.getEdgeColor()));
            g2.draw(triangle);
            return;
        }

        if (type == RelationshipType.COMPOSITION || type == RelationshipType.AGGREGATION) {
            double backX = toX - 10 * Math.cos(angle);
            double backY = toY - 10 * Math.sin(angle);
            double side1X = backX + wing * Math.cos(angle + Math.PI / 2);
            double side1Y = backY + wing * Math.sin(angle + Math.PI / 2);
            double side2X = backX + wing * Math.cos(angle - Math.PI / 2);
            double side2Y = backY + wing * Math.sin(angle - Math.PI / 2);
            Path2D diamond = new Path2D.Double();
            diamond.moveTo(toX, toY);
            diamond.lineTo(side1X, side1Y);
            diamond.lineTo(backX - 10 * Math.cos(angle), backY - 10 * Math.sin(angle));
            diamond.lineTo(side2X, side2Y);
            diamond.closePath();
            if (type == RelationshipType.COMPOSITION) {
                g2.fill(diamond);
            } else {
                g2.setColor(Color.WHITE);
                g2.fill(diamond);
                g2.setColor(toEdgeColor(relationship.getEdgeColor()));
            }
            g2.draw(diamond);
            return;
        }

        if (type == RelationshipType.ASSOCIATION) {
            AssociationRelationship association = relationship instanceof AssociationRelationship a ? a : null;
            if (association != null && association.getNavigability() != null && association.getNavigability().name().contains("UNI")) {
                g2.draw(new Line2D.Double(toX, toY, x1, y1));
                g2.draw(new Line2D.Double(toX, toY, x2, y2));
            }
            return;
        }

        g2.draw(new Line2D.Double(toX, toY, x1, y1));
        g2.draw(new Line2D.Double(toX, toY, x2, y2));
    }

    private DiagramBounds calculateBounds(UMLDiagram diagram) {
        if (diagram == null || diagram.getClasses().isEmpty()) {
            return new DiagramBounds(0, 0, 960, 640);
        }
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;
        for (ConceptualClass umlClass : diagram.getClasses()) {
            minX = Math.min(minX, umlClass.getPositionX());
            minY = Math.min(minY, umlClass.getPositionY());
            maxX = Math.max(maxX, umlClass.getPositionX() + classWidth(umlClass));
            maxY = Math.max(maxY, umlClass.getPositionY() + classHeight(umlClass));
        }
        return new DiagramBounds(minX, minY, (maxX - minX) + PADDING * 2, (maxY - minY) + PADDING * 2);
    }

    private int classWidth(ConceptualClass umlClass) {
        return (int) Math.max(140, umlClass == null ? DEFAULT_CLASS_WIDTH : umlClass.getClassWidth());
    }

    private int classHeight(ConceptualClass umlClass) {
        return (int) Math.max(90, umlClass == null ? DEFAULT_CLASS_HEIGHT : umlClass.getClassHeight());
    }

    private String formatAttribute(com.umlytics.domain.Attribute attribute) {
        String vis = visibilitySymbol(attribute.getVisibility());
        return vis + " " + safe(attribute.getName(), "attr") + ": " + safe(attribute.getType(), "String");
    }

    private String formatMethod(com.umlytics.domain.Method method) {
        String vis = visibilitySymbol(method.getVisibility());
        String returnType = safe(method.getReturnType(), "void");
        return vis + " " + safe(method.getName(), "method") + "(): " + returnType;
    }

    private String visibilitySymbol(Visibility visibility) {
        if (visibility == null) {
            return "~";
        }
        return switch (visibility) {
            case PUBLIC -> "+";
            case PRIVATE -> "-";
            case PROTECTED -> "#";
            case PACKAGE -> "~";
        };
    }

    private Color toClassHeaderColor(String value) {
        return switch (safe(value, "Blue")) {
            case "Green" -> new Color(0xD5, 0xE8, 0xD4);
            case "Orange" -> new Color(0xFF, 0xE6, 0xCC);
            case "White" -> Color.WHITE;
            default -> new Color(0xDA, 0xE8, 0xFC);
        };
    }

    private Color toClassBorderColor(String value) {
        return switch (safe(value, "Blue")) {
            case "Black" -> Color.BLACK;
            case "Gray" -> new Color(0x99, 0x99, 0x99);
            default -> new Color(0x6C, 0x8E, 0xBF);
        };
    }

    private Color toEdgeColor(String value) {
        return switch (safe(value, "Black")) {
            case "Blue" -> new Color(0x1A, 0x73, 0xE8);
            case "Red" -> new Color(0xD9, 0x30, 0x25);
            case "Green" -> new Color(0x18, 0x80, 0x38);
            default -> Color.BLACK;
        };
    }

    private String safe(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private void ensureParentDirectory(String path) {
        File file = new File(path);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new UnsupportedFormatException("Failed to create export directory: " + parent.getAbsolutePath());
        }
    }

    private record DiagramBounds(double minX, double minY, double width, double height) {
    }
}
