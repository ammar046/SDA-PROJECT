package com.umlytics.ui;

import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.Transition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Full-window splash: logo (WebP), neon atmosphere, sequential typewriter credits.
 */
public final class SplashScreen {

    private static final TeamMember[] TEAM = {
            new TeamMember("Ammar Bin Omer", "24I-0500"),
            new TeamMember("Haris Zahid", "24I-0643"),
            new TeamMember("Shahmeer Jadoon", "24I-0879"),
    };

    private SplashScreen() {
    }

    private record TeamMember(String name, String roll) {
        String typedLine() {
            return name + "  ·  " + roll;
        }
    }

    /**
     * Covers {@code host} (typically the root {@link StackPane}). Mostly transparent wash so the UI bleeds through;
     * click anywhere to skip. Team names type out sequentially.
     */
    public static void attachOver(StackPane host) {
        StackPane layer = new StackPane();
        layer.setPickOnBounds(true);
        layer.prefWidthProperty().bind(host.widthProperty());
        layer.prefHeightProperty().bind(host.heightProperty());
        layer.setStyle("-fx-background-color: transparent;");

        List<Particle> particles = new ArrayList<>();
        double w = host.getWidth() > 0 ? host.getWidth() : 1440;
        double h = host.getHeight() > 0 ? host.getHeight() : 860;
        Canvas particleCanvas = new Canvas(w, h);
        GraphicsContext pg = particleCanvas.getGraphicsContext2D();
        host.widthProperty().addListener((o, a, nv) -> {
            particleCanvas.setWidth(nv.doubleValue());
            seedParticles(particles, nv.doubleValue(), particleCanvas.getHeight());
        });
        host.heightProperty().addListener((o, a, nv) -> {
            particleCanvas.setHeight(nv.doubleValue());
            seedParticles(particles, particleCanvas.getWidth(), nv.doubleValue());
        });
        seedParticles(particles, w, h);

        AnimationTimer ticker = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double cw = particleCanvas.getWidth();
                double ch = particleCanvas.getHeight();
                pg.clearRect(0, 0, cw, ch);
                for (Particle p : particles) {
                    p.tick(cw, ch);
                    pg.setGlobalAlpha(p.alpha);
                    pg.setFill(Color.color(0.55, 0.95, 1.0));
                    pg.fillOval(p.x, p.y, p.r, p.r);
                }
                pg.setGlobalAlpha(1);
            }
        };

        StackPane wash = new StackPane();
        wash.prefWidthProperty().bind(layer.widthProperty());
        wash.prefHeightProperty().bind(layer.heightProperty());
        wash.setStyle("-fx-background-color: linear-gradient(to bottom,"
                + " rgba(6,4,18,0.42), rgba(10,6,28,0.5), rgba(4,2,14,0.28));");

        Rectangle vignette = new Rectangle();
        vignette.widthProperty().bind(layer.widthProperty());
        vignette.heightProperty().bind(layer.heightProperty());
        vignette.setFill(new RadialGradient(0, 0, 0.5, 0.45, 0.65, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(0.08, 0.02, 0.15, 0.0)),
                new Stop(0.55, Color.color(0.02, 0.01, 0.06, 0.22)),
                new Stop(1, Color.color(0, 0, 0, 0.68))));

        ImageView logo = createLogo();
        logo.setPreserveRatio(true);
        logo.setFitWidth(240);
        logo.setSmooth(true);
        Bloom bloom = new Bloom(0.18);
        DropShadow glow = new DropShadow(48, Color.color(0.2, 0.95, 0.95, 0.9));
        glow.setSpread(0.18);
        logo.setEffect(bloom);
        bloom.setInput(glow);

        ScaleTransition pulse = new ScaleTransition(Duration.seconds(2.2), logo);
        pulse.setFromX(1.0);
        pulse.setToX(1.07);
        pulse.setFromY(1.0);
        pulse.setToY(1.07);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();

        RotateTransition wobble = new RotateTransition(Duration.seconds(5), logo);
        wobble.setFromAngle(-2.5);
        wobble.setToAngle(2.5);
        wobble.setAutoReverse(true);
        wobble.setCycleCount(Animation.INDEFINITE);
        wobble.play();

        Timeline hueShift = new Timeline(new KeyFrame(Duration.ZERO,
                        new KeyValue(glow.colorProperty(), Color.color(0.2, 0.95, 0.95))),
                new KeyFrame(Duration.seconds(2.2),
                        new KeyValue(glow.colorProperty(), Color.color(0.95, 0.25, 0.95))),
                new KeyFrame(Duration.seconds(4.4),
                        new KeyValue(glow.colorProperty(), Color.color(0.55, 0.45, 1.0))),
                new KeyFrame(Duration.seconds(6.6),
                        new KeyValue(glow.colorProperty(), Color.color(0.2, 0.95, 0.95))));
        hueShift.setCycleCount(Animation.INDEFINITE);
        hueShift.play();

        Text title = new Text("UMLytics");
        title.setFont(Font.font("Segoe UI", FontWeight.BLACK, 56));
        title.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#7ef9ff")),
                new Stop(0.45, Color.web("#e879f9")),
                new Stop(1, Color.web("#a78bfa"))));
        title.setEffect(new DropShadow(22, Color.color(0.5, 0.8, 1.0, 0.55)));
        title.setOpacity(0);

        Text subtitle = new Text("AI-Powered Design Intelligence");
        subtitle.setFont(Font.font("Segoe UI Semibold", FontWeight.NORMAL, 15));
        subtitle.setFill(Color.color(0.78, 0.85, 0.96, 0.88));
        subtitle.setOpacity(0);

        VBox teamCol = new VBox(12);
        teamCol.setAlignment(Pos.CENTER);
        teamCol.setPadding(new Insets(10, 0, 0, 0));
        teamCol.setMaxWidth(Double.MAX_VALUE);
        List<Label> memberLabels = new ArrayList<>();
        for (int mi = 0; mi < TEAM.length; mi++) {
            Label row = new Label();
            row.setFont(Font.font("Consolas", FontWeight.BOLD, 18));
            row.setTextFill(Color.color(0.88, 0.96, 1.0));
            row.setAlignment(Pos.CENTER);
            row.setMaxWidth(Double.MAX_VALUE);
            row.setEffect(new DropShadow(10, Color.color(0.0, 0.85, 1.0, 0.4)));
            memberLabels.add(row);
            teamCol.getChildren().add(row);
        }

        Text hint = new Text("click anywhere to skip");
        hint.setFont(Font.font("Segoe UI", 11));
        hint.setFill(Color.color(1, 1, 1, 0.38));

        VBox centerCol = new VBox(22, logo, title, subtitle, teamCol, hint);
        centerCol.setAlignment(Pos.CENTER);

        StackPane card = new StackPane(centerCol);
        card.setPadding(new Insets(40, 52, 48, 52));
        card.setMaxWidth(680);
        card.setStyle("-fx-background-color: linear-gradient(145deg,"
                + " rgba(255,255,255,0.09), rgba(255,255,255,0.025));"
                + "-fx-background-radius: 28;"
                + "-fx-border-color: rgba(167,139,250,0.45);"
                + "-fx-border-width: 1.2;"
                + "-fx-border-radius: 28;"
                + "-fx-effect: dropshadow(gaussian, rgba(120,80,255,0.4), 36, 0.22, 0, 10);");

        layer.getChildren().addAll(wash, vignette, particleCanvas, card);
        StackPane.setAlignment(card, Pos.CENTER);
        host.getChildren().add(layer);

        AtomicBoolean finished = new AtomicBoolean(false);
        SequentialTransition mainSeq = new SequentialTransition();

        Runnable finish = () -> {
            if (!finished.compareAndSet(false, true)) {
                return;
            }
            mainSeq.stop();
            ticker.stop();
            pulse.stop();
            wobble.stop();
            hueShift.stop();
            FadeTransition out = new FadeTransition(Duration.millis(750), layer);
            out.setFromValue(1);
            out.setToValue(0);
            out.setOnFinished(e -> host.getChildren().remove(layer));
            out.play();
        };

        layer.setOnMouseClicked(e -> finish.run());

        FadeTransition titleIn = new FadeTransition(Duration.millis(650), title);
        titleIn.setFromValue(0);
        titleIn.setToValue(1);
        FadeTransition subIn = new FadeTransition(Duration.millis(750), subtitle);
        subIn.setFromValue(0);
        subIn.setToValue(1);

        FadeTransition logoIn = new FadeTransition(Duration.millis(900), logo);
        logoIn.setFromValue(0);
        logoIn.setToValue(1);
        ScaleTransition logoPop = new ScaleTransition(Duration.millis(900), logo);
        logoPop.setFromX(0.32);
        logoPop.setFromY(0.32);
        logoPop.setToX(1);
        logoPop.setToY(1);

        SequentialTransition credits = new SequentialTransition();
        credits.getChildren().add(new PauseTransition(Duration.millis(420)));
        for (int i = 0; i < memberLabels.size(); i++) {
            Label row = memberLabels.get(i);
            String full = TEAM[i].typedLine();
            credits.getChildren().add(typewriterTransition(row, full, Duration.millis(34)));
            credits.getChildren().add(new PauseTransition(Duration.millis(200)));
        }
        credits.getChildren().add(new PauseTransition(Duration.millis(1200)));

        ParallelTransition intro = new ParallelTransition(logoIn, logoPop);
        mainSeq.getChildren().addAll(
                intro,
                new ParallelTransition(titleIn, subIn),
                credits
        );
        mainSeq.setOnFinished(e -> {
            PauseTransition hold = new PauseTransition(Duration.millis(1400));
            hold.setOnFinished(ev -> finish.run());
            hold.play();
        });

        ticker.start();
        mainSeq.play();
    }

    private static Transition typewriterTransition(Labeled target, String full, Duration perChar) {
        return new Transition() {
            {
                setCycleDuration(perChar.multiply(Math.max(1, full.length())));
            }

            @Override
            protected void interpolate(double frac) {
                int n = (int) Math.round(frac * full.length());
                n = Math.min(n, full.length());
                target.setText(full.substring(0, n));
            }
        };
    }

    private static void seedParticles(List<Particle> particles, double w, double h) {
        particles.clear();
        int count = (int) Math.min(160, Math.max(48, w * h / 11000));
        for (int i = 0; i < count; i++) {
            particles.add(new Particle(Math.random() * w, Math.random() * h, w, h));
        }
    }

    private static final class Particle {
        double x;
        double y;
        double vx;
        double vy;
        double r;
        double alpha;

        Particle(double x, double y, double w, double h) {
            this.x = x;
            this.y = y;
            this.vx = (Math.random() - 0.5) * 0.65;
            this.vy = (Math.random() - 0.5) * 0.65;
            this.r = 0.7 + Math.random() * 2.0;
            this.alpha = 0.18 + Math.random() * 0.5;
        }

        void tick(double w, double h) {
            x += vx;
            y += vy;
            if (x < 0 || x > w) {
                vx = -vx;
            }
            if (y < 0 || y > h) {
                vy = -vy;
            }
        }
    }

    private static ImageView createLogo() {
        ImageView iv = new ImageView();
        Image fx = AppAssets.loadBundledLogo();
        if (fx != null) {
            iv.setImage(fx);
        }
        return iv;
    }
}
