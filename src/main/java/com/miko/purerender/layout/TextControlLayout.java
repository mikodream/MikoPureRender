package com.miko.purerender.layout;

import com.miko.purerender.dom.ElementNode;
import com.miko.purerender.style.ComputedStyle;
import com.miko.purerender.style.StyleValues;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TextControlLayout {
    private TextControlLayout() {
    }

    public static Metrics compute(ElementNode control, ComputedStyle style, double boxWidth, double boxHeight, String text) {
        Measurer measurer = new Measurer(style);
        double fontSize = fontSize(style);
        double lineHeight = lineHeight(style);
        double border = StyleValues.length(style, "border-width", 0);
        StyleValues.Edges padding = StyleValues.edges(style, "padding", boxWidth);
        double viewportWidth = Math.max(0, boxWidth - border * 2 - padding.horizontal());
        double viewportHeight = Math.max(0, boxHeight - border * 2 - padding.vertical());
        List<Line> lines = "input".equals(control.tagName())
                ? inputLines(measurer, text == null ? "" : text)
                : textareaLines(control, style, measurer, text == null ? "" : text, viewportWidth);
        double contentWidth = lines.stream().mapToDouble(Line::width).max().orElse(0);
        double contentHeight = Math.max(1, lines.size()) * lineHeight;
        return new Metrics(lines, contentWidth, contentHeight, viewportWidth, viewportHeight, fontSize, lineHeight, border, padding, style);
    }

    public static double fontSize(ComputedStyle style) {
        return StyleValues.length(style, "font-size", 16);
    }

    public static double lineHeight(ComputedStyle style) {
        return StyleValues.length(style, "line-height", fontSize(style) * 1.35);
    }

    public static double measureText(String text, ComputedStyle style) {
        return new Measurer(style).measure(text);
    }

    public static boolean wraps(ElementNode control, ComputedStyle style) {
        if ("input".equals(control.tagName())) {
            return false;
        }
        String wrap = control.attribute("wrap").orElse("").trim().toLowerCase();
        if ("off".equals(wrap)) {
            return false;
        }
        String whiteSpace = style.get("white-space", "pre-wrap").trim().toLowerCase();
        return !"pre".equals(whiteSpace) && !"nowrap".equals(whiteSpace);
    }

    private static List<Line> inputLines(Measurer measurer, String text) {
        String singleLine = text.replace("\r", "").replace("\n", "");
        return List.of(measurer.line(singleLine, 0, singleLine.length(), 0));
    }

    private static List<Line> textareaLines(ElementNode control, ComputedStyle style, Measurer measurer, String text, double viewportWidth) {
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        boolean wrap = wraps(control, style) && viewportWidth > 0;
        List<Line> lines = new ArrayList<>();
        String[] paragraphs = normalized.split("\n", -1);
        int offset = 0;
        for (String paragraph : paragraphs) {
            appendParagraphLines(lines, paragraph, offset, measurer, wrap, viewportWidth);
            offset += paragraph.length() + 1;
        }
        return lines;
    }

    private static void appendParagraphLines(
            List<Line> lines,
            String paragraph,
            int paragraphStart,
            Measurer measurer,
            boolean wrap,
            double viewportWidth
    ) {
        if (paragraph.isEmpty()) {
            lines.add(measurer.line("", paragraphStart, paragraphStart, lines.size()));
            return;
        }
        if (!wrap || measurer.measure(paragraph) <= viewportWidth) {
            lines.add(measurer.line(paragraph, paragraphStart, paragraphStart + paragraph.length(), lines.size()));
            return;
        }

        int lineStart = 0;
        while (lineStart < paragraph.length()) {
            int lineEnd = fittingEnd(paragraph, lineStart, measurer, viewportWidth);
            String line = paragraph.substring(lineStart, lineEnd);
            lines.add(measurer.line(line, paragraphStart + lineStart, paragraphStart + lineEnd, lines.size()));
            lineStart = lineEnd;
        }
    }

    private static int fittingEnd(String paragraph, int start, Measurer measurer, double viewportWidth) {
        int best = start + 1;
        int lastBreak = -1;
        for (int i = start + 1; i <= paragraph.length(); i++) {
            char previous = paragraph.charAt(i - 1);
            if (Character.isWhitespace(previous)) {
                lastBreak = i;
            }
            if (measurer.measure(paragraph.substring(start, i)) > viewportWidth) {
                if (lastBreak > start) {
                    return lastBreak;
                }
                return best;
            }
            best = i;
        }
        return paragraph.length();
    }

    private static Font font(ComputedStyle style) {
        FontWeight weight = "bold".equalsIgnoreCase(style.get("font-weight", "normal"))
                ? FontWeight.BOLD
                : FontWeight.NORMAL;
        return Font.font(style.get("font-family", "System"), weight, fontSize(style));
    }

    private static final class Measurer {
        private final Text textNode = new Text();
        private final Map<String, Double> widthCache = new HashMap<>();

        private Measurer(ComputedStyle style) {
            textNode.setFont(font(style));
        }

        private double measure(String text) {
            if (text == null || text.isEmpty()) {
                return 0;
            }
            return widthCache.computeIfAbsent(text, value -> {
                textNode.setText(value);
                return textNode.getLayoutBounds().getWidth();
            });
        }

        private Line line(String text, int start, int end, int index) {
            double[] prefixWidths = new double[text.length() + 1];
            for (int i = 1; i <= text.length(); i++) {
                prefixWidths[i] = measure(text.substring(0, i));
            }
            return new Line(text, start, end, index, prefixWidths[text.length()], prefixWidths);
        }
    }

    public record Metrics(
            List<Line> lines,
            double contentWidth,
            double contentHeight,
            double viewportWidth,
            double viewportHeight,
            double fontSize,
            double lineHeight,
            double border,
            StyleValues.Edges padding,
            ComputedStyle style
    ) {
        public CaretPoint caretLocation(int caret) {
            Line line = lineForCaret(caret);
            int offset = Math.max(line.start(), Math.min(caret, line.end()));
            return new CaretPoint(xForOffset(line, offset), line.index() * lineHeight);
        }

        public int offsetAt(double x, double y) {
            if (lines.isEmpty()) {
                return 0;
            }
            int lineIndex = Math.max(0, Math.min(lines.size() - 1, (int) Math.floor(y / lineHeight)));
            Line line = lines.get(lineIndex);
            return offsetInLine(line, x);
        }

        public double xForOffset(Line line, int offset) {
            int local = Math.max(0, Math.min(offset - line.start(), line.text().length()));
            return line.prefixWidths()[local];
        }

        private int offsetInLine(Line line, double x) {
            if (x <= 0) {
                return line.start();
            }
            for (int i = 1; i <= line.text().length(); i++) {
                double previous = line.prefixWidths()[i - 1];
                double current = line.prefixWidths()[i];
                if (x < (previous + current) / 2.0) {
                    return line.start() + i - 1;
                }
            }
            return line.end();
        }

        private Line lineForCaret(int caret) {
            if (lines.isEmpty()) {
                return new Line("", 0, 0, 0, 0, new double[]{0});
            }
            int clamped = Math.max(0, caret);
            for (int i = 0; i < lines.size(); i++) {
                Line line = lines.get(i);
                boolean isLastLine = i == lines.size() - 1;
                if (clamped >= line.start() && clamped < line.end()) {
                    return line;
                }
                if (clamped == line.end() && (isLastLine || lines.get(i + 1).start() != line.end())) {
                    return line;
                }
            }
            return lines.getLast();
        }
    }

    public record Line(String text, int start, int end, int index, double width, double[] prefixWidths) {
    }

    public record CaretPoint(double x, double y) {
    }
}
