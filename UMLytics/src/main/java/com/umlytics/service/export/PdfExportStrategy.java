package com.umlytics.service.export;

import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfWriter;
import com.umlytics.domain.UMLDiagram;
import com.umlytics.ui.canvas.MainCanvas;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;

/**
 * GoF: Strategy — PDF export via iTextPDF 5.x.
 * GRASP: Pure Fabrication
 */
public class PdfExportStrategy implements IExportStrategy {

    @Override
    public void export(UMLDiagram diagram, String path, MainCanvas canvas) throws Exception {
        // 1. Take PNG snapshot of canvas on FX thread
        byte[] pngBytes = takePngSnapshot(canvas);

        // 2. Write PDF with embedded image
        Document doc = new Document();
        PdfWriter.getInstance(doc, new FileOutputStream(path));
        doc.open();

        Image img = Image.getInstance(pngBytes);
        img.scaleToFit(doc.getPageSize().getWidth()  - doc.leftMargin() - doc.rightMargin(),
                       doc.getPageSize().getHeight() - doc.topMargin()  - doc.bottomMargin());
        img.setAlignment(Image.MIDDLE);
        doc.add(img);

        doc.close();
        System.out.println("[PdfExportStrategy] Exported PDF to: " + path);
    }

    private byte[] takePngSnapshot(MainCanvas canvas) throws Exception {
        final ByteArrayOutputStream[] out = {null};
        final Exception[] err = {null};

        if (Platform.isFxApplicationThread()) {
            out[0] = doSnapshot(canvas);
        } else {
            Platform.runLater(() -> {
                try { out[0] = doSnapshot(canvas); }
                catch (Exception e) { err[0] = e; }
            });
            // Wait for FX thread
            long deadline = System.currentTimeMillis() + 5000;
            while (out[0] == null && err[0] == null && System.currentTimeMillis() < deadline) {
                Thread.sleep(50);
            }
        }
        if (err[0] != null) throw err[0];
        if (out[0] == null) throw new RuntimeException("PDF snapshot timed out");
        return out[0].toByteArray();
    }

    private ByteArrayOutputStream doSnapshot(MainCanvas canvas) throws Exception {
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(javafx.scene.paint.Color.web("#121212"));
        WritableImage img = canvas.snapshot(sp, null);
        BufferedImage bi = SwingFXUtils.fromFXImage(img, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bi, "PNG", baos);
        return baos;
    }
}
