package com.umlytics.ui;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Objects;

/**
 * Shared bundled artwork (logo WebP for splash, window icon, etc.).
 */
public final class AppAssets {

    public static final String LOGO_RESOURCE = "/assets/uml.webp";

    private AppAssets() {
    }

    /**
     * Loads {@value #LOGO_RESOURCE} for {@link javafx.scene.image.ImageView} or {@link javafx.stage.Stage} icons.
     */
    public static Image loadBundledLogo() {
        try (InputStream in = AppAssets.class.getResourceAsStream(LOGO_RESOURCE)) {
            if (in == null) {
                return tryFxImageFromClasspath(LOGO_RESOURCE);
            }
            BufferedImage bi = ImageIO.read(in);
            if (bi != null) {
                return SwingFXUtils.toFXImage(bi, null);
            }
        } catch (Exception ignored) {
            // fall through
        }
        return tryFxImageFromClasspath(LOGO_RESOURCE);
    }

    private static Image tryFxImageFromClasspath(String classpath) {
        try {
            var url = Objects.requireNonNull(AppAssets.class.getResource(classpath), "missing " + classpath);
            return new Image(url.toExternalForm(), true);
        } catch (Exception e) {
            return null;
        }
    }
}
