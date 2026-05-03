package com.umlytics.ui;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lightweight Java highlighting for structure suggestion previews (draw.io-style readability). */
public final class JavaSyntaxHighlighter {

    private static final Set<String> KEYWORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "continue",
            "default", "do", "double", "else", "enum", "extends", "final", "finally", "float", "for",
            "if", "implements", "import", "instanceof", "int", "interface", "long", "native", "new",
            "package", "private", "protected", "public", "return", "short", "static", "super", "switch",
            "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while",
            "true", "false", "null", "var", "record", "sealed", "permits", "yield");

    private static final Pattern TOKEN = Pattern.compile(
            "(\"(?:[^\"\\\\]|\\\\.)*\")|('(?:[^'\\\\]|\\\\.)*')|(//[^\r\n]*)|(/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/)|(@\\w+)|(\\d+[lLdDfF]?)|(\\w+)|(\\s+)|(.)");

    private JavaSyntaxHighlighter() {
    }

    public static void fillTextFlow(TextFlow flow, String code) {
        flow.getChildren().clear();
        if (code == null || code.isEmpty()) {
            return;
        }
        Font mono = Font.font("monospaced", 13);
        Matcher m = TOKEN.matcher(code);
        while (m.find()) {
            Text t = new Text(m.group());
            t.setFont(mono);
            if (m.group(1) != null || m.group(2) != null) {
                t.setFill(Color.web("#067d17"));
            } else if (m.group(3) != null || m.group(4) != null) {
                t.setFill(Color.web("#707070"));
            } else if (m.group(5) != null) {
                t.setFill(Color.web("#808000"));
            } else if (m.group(6) != null) {
                t.setFill(Color.web("#098658"));
            } else if (m.group(7) != null) {
                String w = m.group(7);
                if (KEYWORDS.contains(w)) {
                    t.setFill(Color.web("#0000cc"));
                } else {
                    t.setFill(Color.web("#1a1a1a"));
                }
            } else {
                t.setFill(Color.web("#1a1a1a"));
            }
            flow.getChildren().add(t);
        }
    }
}
