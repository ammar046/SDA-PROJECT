package com.umlytics.service.export;

import com.umlytics.domain.UMLDiagram;
import com.umlytics.ui.canvas.MainCanvas;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * GoF: Strategy — PNG export implementation.
 * GRASP: Pure Fabrication
 */
public class PngExportStrategy implements IExportStrategy {

    @Override
    public void export(UMLDiagram diagram, String path, MainCanvas canvas) throws Exception {
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(javafx.scene.paint.Color.web("#121212"));

        // Must run on FX thread
        final WritableImage[] imgHolder = new WritableImage[1];
        if (javafx.application.Platform.isFxApplicationThread()) {
            imgHolder[0] = canvas.snapshot(params, null);
        } else {
            javafx.application.Platform.runLater(() ->
                imgHolder[0] = canvas.snapshot(params, null));
            Thread.sleep(300); // give FX time to snapshot
        }

        WritableImage img = imgHolder[0];
        if (img == null) throw new RuntimeException("Snapshot failed — no canvas content.");

        BufferedImage bi = SwingFXUtils.fromFXImage(img, null);
        File outFile = new File(path);
        if (!ImageIO.write(bi, "PNG", outFile)) {
            throw new RuntimeException("PNG write failed for: " + path);
        }
        System.out.println("[PngExportStrategy] Exported PNG to: " + path);
    }
}
