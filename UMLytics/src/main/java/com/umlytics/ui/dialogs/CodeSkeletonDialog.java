package com.umlytics.ui.dialogs;

import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.DirectoryChooser;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * UC7 — Code Skeleton dialog: tabbed view per class with syntax highlighting.
 */
public class CodeSkeletonDialog extends Dialog<Void> {

    private static final Set<String> KEYWORDS = Set.of(
        "class", "interface", "extends", "implements", "public", "private",
        "protected", "void", "return", "new", "this", "static", "final",
        "abstract", "import", "package", "if", "else", "for", "while",
        "try", "catch", "throws", "null", "boolean", "int", "double",
        "String", "List", "Map", "Override"
    );

    private final Map<String, String> classCodes;

    public CodeSkeletonDialog(String rawCode) {
        setTitle("Generated Code Skeletons");
        setHeaderText(null);
        getDialogPane().setPrefSize(780, 560);

        this.classCodes = parseClassBlocks(rawCode);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        if (classCodes.isEmpty()) {
            // Show raw code if parsing yields no blocks
            classCodes.put("Generated", rawCode);
        }

        classCodes.forEach((className, code) -> {
            CodeArea area = new CodeArea();
            area.setEditable(false);
            area.replaceText(code);
            area.setStyleSpans(0, computeHighlighting(code));

            Tab tab = new Tab(className, area);
            tabPane.getTabs().add(tab);
        });

        // Buttons
        ButtonType closeBtn   = ButtonType.CLOSE;
        ButtonType copyAllBtn = new ButtonType("Copy All",    ButtonBar.ButtonData.LEFT);
        ButtonType saveAllBtn = new ButtonType("Save All…",   ButtonBar.ButtonData.LEFT);
        getDialogPane().getButtonTypes().addAll(copyAllBtn, saveAllBtn, closeBtn);

        getDialogPane().setContent(tabPane);
        getDialogPane().setStyle("-fx-background-color: #1e1e2e;");

        // Copy All
        getDialogPane().lookupButton(copyAllBtn).addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            e.consume();
            String all = classCodes.values().stream().reduce("", (a, b) -> a + "\n\n" + b);
            ClipboardContent cc = new ClipboardContent();
            cc.putString(all);
            Clipboard.getSystemClipboard().setContent(cc);
        });

        // Save All
        getDialogPane().lookupButton(saveAllBtn).addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            e.consume();
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select Directory to Save Files");
            File dir = dc.showDialog(null);
            if (dir != null) {
                classCodes.forEach((name, code) -> {
                    try {
                        Files.writeString(Path.of(dir.getAbsolutePath(), name + ".java"), code);
                    } catch (IOException ex) {
                        System.err.println("[CodeSkeletonDialog] Save failed for " + name + ": " + ex.getMessage());
                    }
                });
            }
        });
    }

    /** Split on "// === ClassName ===" or "public class ClassName" markers */
    private Map<String, String> parseClassBlocks(String raw) {
        Map<String, String> result = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) return result;

        // Try splitting on // === ClassName === markers first
        Pattern markerName = Pattern.compile("// === (.+?) ===");
        Matcher m = markerName.matcher(raw);

        List<String> names   = new ArrayList<>();
        List<Integer> starts = new ArrayList<>();
        while (m.find()) { names.add(m.group(1).trim()); starts.add(m.end()); }

        if (!names.isEmpty()) {
            for (int i = 0; i < names.size(); i++) {
                int end = (i + 1 < starts.size()) ? starts.get(i + 1) - names.get(i + 1).length() - 12 : raw.length();
                result.put(names.get(i), raw.substring(starts.get(i), end).strip());
            }
            return result;
        }

        // Fallback: split on "public class ClassName" or "public interface ClassName"
        Pattern classDecl = Pattern.compile("(?m)^(public\\s+(?:class|interface|abstract class|enum)\\s+(\\w+))");
        Matcher cm = classDecl.matcher(raw);
        List<String> classNames = new ArrayList<>();
        List<Integer> classStarts = new ArrayList<>();
        while (cm.find()) { classNames.add(cm.group(2)); classStarts.add(cm.start()); }

        if (!classNames.isEmpty()) {
            for (int i = 0; i < classNames.size(); i++) {
                int end = (i + 1 < classStarts.size()) ? classStarts.get(i + 1) : raw.length();
                result.put(classNames.get(i), raw.substring(classStarts.get(i), end).strip());
            }
            return result;
        }

        // Last resort — show everything under "Generated"
        result.put("Generated", raw);
        return result;
    }

    /** Simple keyword-based syntax highlighting for Java */
    private StyleSpans<Collection<String>> computeHighlighting(String text) {
        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
        Pattern pattern = Pattern.compile(
            "(?<KEYWORD>\\b(?:" + String.join("|", KEYWORDS) + ")\\b)"
            + "|(?<STRING>\"([^\"\\\\]|\\\\.)*\")"
            + "|(?<COMMENT>//[^\n]*|/\\*.*?\\*/)"
            + "|(?<ANNOTATION>@\\w+)",
            Pattern.DOTALL
        );
        Matcher matcher = pattern.matcher(text);
        int last = 0;
        while (matcher.find()) {
            builder.add(Collections.emptyList(), matcher.start() - last);
            String styleClass = matcher.group("KEYWORD") != null ? "keyword"
                    : matcher.group("STRING")  != null ? "string"
                    : matcher.group("COMMENT") != null ? "comment"
                    : "annotation";
            builder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            last = matcher.end();
        }
        builder.add(Collections.emptyList(), text.length() - last);
        return builder.create();
    }
}
