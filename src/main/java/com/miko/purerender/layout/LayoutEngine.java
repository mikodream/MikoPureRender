package com.miko.purerender.layout;

import com.miko.purerender.dom.ElementNode;
import com.miko.purerender.dom.TextNode;
import com.miko.purerender.style.ComputedStyle;
import com.miko.purerender.style.StyleValues;
import com.miko.purerender.style.StyledNode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LayoutEngine {
    private static final Pattern REPEAT_COLUMNS = Pattern.compile("repeat\\((\\d+)\\s*,\\s*[^)]+\\)");

    public LayoutBox layout(StyledNode root, double viewportWidth) {
        LayoutBox rootBox = buildLayoutTree(root);
        layoutBlock(rootBox, 0, 0, viewportWidth);
        return rootBox;
    }

    private LayoutBox buildLayoutTree(StyledNode styledNode) {
        LayoutBox box = new LayoutBox(styledNode);
        if (isFormControl(styledNode)) {
            return box;
        }
        for (StyledNode child : styledNode.children()) {
            if (!"none".equals(child.style().get("display", "block"))) {
                box.appendChild(buildLayoutTree(child));
            }
        }
        return box;
    }

    private double layoutBlock(LayoutBox box, double x, double y, double availableWidth) {
        ComputedStyle style = box.styledNode().style();
        StyleValues.Edges margin = StyleValues.edges(style, "margin", availableWidth);
        StyleValues.Edges padding = StyleValues.edges(style, "padding", availableWidth);
        double border = StyleValues.length(style, "border-width", availableWidth, 0);
        double explicitWidth = StyleValues.length(style, "width", availableWidth, Double.NaN);
        boolean hasExplicitWidth = !Double.isNaN(explicitWidth);
        boolean borderBox = "border-box".equals(style.get("box-sizing", "content-box"));
        double contentWidth = !hasExplicitWidth
                ? Math.max(0, availableWidth - margin.horizontal() - padding.horizontal() - border * 2)
                : Math.max(0, explicitWidth - (borderBox ? padding.horizontal() + border * 2 : 0));

        double outerX = x + margin.left();
        double outerY = y + margin.top();
        double contentX = outerX + border + padding.left();
        double contentY = outerY + border + padding.top();

        double contentHeight;
        String display = style.get("display", "block");
        if ("text".equals(display) && box.styledNode().node() instanceof TextNode textNode) {
            List<String> lines = wrapText(textNode.text(), style, contentWidth);
            box.setTextLines(lines);
            double lineHeight = lineHeight(style);
            contentHeight = lines.size() * lineHeight;
            if (!hasExplicitWidth) {
                contentWidth = Math.min(contentWidth, maxLineWidth(lines, style));
            }
        } else if (isImage(box)) {
            if (!hasExplicitWidth) {
                contentWidth = imageWidth(box, contentWidth);
            }
            contentHeight = imageHeight(box, contentWidth);
        } else if ("flex".equals(display)) {
            contentHeight = layoutFlex(box, contentX, contentY, contentWidth);
        } else if ("grid".equals(display)) {
            contentHeight = layoutGrid(box, contentX, contentY, contentWidth);
        } else {
            contentHeight = layoutBlockChildren(box, contentX, contentY, contentWidth);
            if (!hasExplicitWidth && "inline".equals(display)) {
                contentWidth = Math.min(contentWidth, childrenContentWidth(box, contentX));
            }
        }

        double explicitHeight = StyleValues.length(style, "height", availableWidth, Double.NaN);
        if (!Double.isNaN(explicitHeight)) {
            contentHeight = Math.max(0, explicitHeight - (borderBox ? padding.vertical() + border * 2 : 0));
        }

        double width = hasExplicitWidth && borderBox ? explicitWidth : contentWidth + padding.horizontal() + border * 2;
        double height = !Double.isNaN(explicitHeight) && borderBox ? explicitHeight : contentHeight + padding.vertical() + border * 2;
        box.setFrame(outerX, outerY, width, height);
        return height + margin.vertical();
    }

    private double layoutBlockChildren(LayoutBox box, double contentX, double contentY, double contentWidth) {
        double cursorY = contentY;
        List<LayoutBox> inlineRun = new ArrayList<>();
        for (LayoutBox child : box.children()) {
            if (isInlineLevel(child)) {
                inlineRun.add(child);
                continue;
            }
            if (!inlineRun.isEmpty()) {
                cursorY += layoutInlineRun(inlineRun, contentX, cursorY, contentWidth);
                inlineRun.clear();
            }
            cursorY += layoutBlock(child, contentX, cursorY, contentWidth);
        }
        if (!inlineRun.isEmpty()) {
            cursorY += layoutInlineRun(inlineRun, contentX, cursorY, contentWidth);
        }
        return cursorY - contentY;
    }

    private double layoutInlineRun(List<LayoutBox> run, double contentX, double contentY, double contentWidth) {
        double cursorX = contentX;
        double cursorY = contentY;
        double lineHeight = 0;
        double maxBottom = contentY;
        double lineRight = contentX + Math.max(0, contentWidth);
        for (LayoutBox child : run) {
            layoutBlock(child, 0, cursorY, contentWidth);
            if (cursorX > contentX && cursorX + child.width() > lineRight) {
                cursorX = contentX;
                cursorY += lineHeight;
                lineHeight = 0;
                layoutBlock(child, 0, cursorY, contentWidth);
            }
            child.translate(cursorX - child.x(), cursorY - child.y());
            maxBottom = Math.max(maxBottom, child.y() + child.height());
            InlineAdvance advance = inlineAdvance(child, contentX);
            cursorX = advance.cursorX();
            cursorY = advance.cursorY();
            lineHeight = Math.max(lineHeight, advance.lineHeight());
        }
        return Math.max(0, maxBottom - contentY);
    }

    private double layoutFlex(LayoutBox box, double contentX, double contentY, double contentWidth) {
        String direction = box.styledNode().style().get("flex-direction", "row");
        double gap = StyleValues.length(box.styledNode().style(), "gap", contentWidth, 0);
        if ("column".equals(direction)) {
            return layoutFlexColumn(box, contentX, contentY, contentWidth, gap);
        }
        return layoutFlexRow(box, contentX, contentY, contentWidth, gap);
    }

    private double layoutFlexRow(LayoutBox box, double contentX, double contentY, double contentWidth, double gap) {
        double totalWidth = 0;
        double maxHeight = 0;
        for (LayoutBox child : box.children()) {
            double childHeight = layoutBlock(child, 0, contentY, contentWidth);
            totalWidth += child.width();
            maxHeight = Math.max(maxHeight, childHeight);
        }
        totalWidth += gap * Math.max(0, box.children().size() - 1);

        double cursorX = switch (box.styledNode().style().get("justify-content", "flex-start")) {
            case "center" -> contentX + Math.max(0, contentWidth - totalWidth) / 2.0;
            case "flex-end", "end" -> contentX + Math.max(0, contentWidth - totalWidth);
            default -> contentX;
        };
        for (int i = 0; i < box.children().size(); i++) {
            LayoutBox child = box.children().get(i);
            child.translate(cursorX - child.x(), 0);
            cursorX += child.width() + gap;
        }
        return maxHeight;
    }

    private double layoutFlexColumn(LayoutBox box, double contentX, double contentY, double contentWidth, double gap) {
        double cursorY = contentY;
        for (LayoutBox child : box.children()) {
            cursorY += layoutBlock(child, contentX, cursorY, contentWidth) + gap;
        }
        if (!box.children().isEmpty()) {
            cursorY -= gap;
        }
        return cursorY - contentY;
    }

    private double layoutGrid(LayoutBox box, double contentX, double contentY, double contentWidth) {
        int columns = gridColumnCount(box.styledNode().style().get("grid-template-columns", "1fr"));
        double gap = StyleValues.length(box.styledNode().style(), "gap", contentWidth, 0);
        double columnWidth = columns <= 1 ? contentWidth : Math.max(0, (contentWidth - gap * (columns - 1)) / columns);
        double cursorY = contentY;

        for (int index = 0; index < box.children().size(); index += columns) {
            double rowHeight = 0;
            for (int column = 0; column < columns && index + column < box.children().size(); column++) {
                LayoutBox child = box.children().get(index + column);
                double x = contentX + column * (columnWidth + gap);
                double childHeight = layoutBlock(child, x, cursorY, columnWidth);
                rowHeight = Math.max(rowHeight, childHeight);
            }
            cursorY += rowHeight + gap;
        }

        if (!box.children().isEmpty()) {
            cursorY -= gap;
        }
        return cursorY - contentY;
    }

    private static int gridColumnCount(String template) {
        Matcher matcher = REPEAT_COLUMNS.matcher(template);
        if (matcher.find()) {
            return Math.max(1, Integer.parseInt(matcher.group(1)));
        }
        String trimmed = template.trim();
        if (trimmed.isEmpty() || "none".equals(trimmed)) {
            return 1;
        }
        return Math.max(1, trimmed.split("\\s+").length);
    }

    private static double measureText(String text, ComputedStyle style) {
        return TextControlLayout.measureText(text, style);
    }

    private static double maxLineWidth(List<String> lines, ComputedStyle style) {
        return lines.stream().mapToDouble(line -> measureText(line, style)).max().orElse(0);
    }

    private static double lineHeight(ComputedStyle style) {
        return StyleValues.length(style, "line-height", StyleValues.length(style, "font-size", 16) * 1.35);
    }

    private static List<String> wrapText(String text, ComputedStyle style, double maxWidth) {
        String whiteSpace = style.get("white-space", "normal").trim().toLowerCase();
        String prepared = prepareText(text, whiteSpace);
        if (prepared.isEmpty()) {
            return List.of();
        }
        boolean preserveNewlines = "pre".equals(whiteSpace) || "pre-wrap".equals(whiteSpace);
        boolean wrap = !"pre".equals(whiteSpace) && !"nowrap".equals(whiteSpace);
        if (!wrap || maxWidth <= 0 || measureText(prepared, style) <= maxWidth) {
            return preserveNewlines ? List.of(prepared.split("\n", -1)) : List.of(prepared);
        }

        List<String> lines = new ArrayList<>();
        if (preserveNewlines) {
            for (String paragraph : prepared.split("\n", -1)) {
                appendPreservedWrappedLines(lines, paragraph, style, maxWidth);
            }
            return lines;
        }

        StringBuilder current = new StringBuilder();
        for (String word : prepared.trim().split("\\s+")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (measureText(candidate, style) <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
            } else {
                if (!current.isEmpty()) {
                    lines.add(current.toString());
                    current.setLength(0);
                }
                if (measureText(word, style) <= maxWidth) {
                    current.append(word);
                } else {
                    lines.addAll(splitLongWord(word, style, maxWidth));
                }
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    private static String prepareText(String text, String whiteSpace) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        if ("pre".equals(whiteSpace) || "pre-wrap".equals(whiteSpace)) {
            return normalized;
        }
        String collapsed = normalized.replaceAll("\\s+", " ");
        return collapsed.isBlank() ? "" : collapsed;
    }

    private static void appendPreservedWrappedLines(List<String> lines, String paragraph, ComputedStyle style, double maxWidth) {
        if (paragraph.isEmpty()) {
            lines.add("");
            return;
        }
        int lineStart = 0;
        while (lineStart < paragraph.length()) {
            int lineEnd = fittingEnd(paragraph, lineStart, style, maxWidth);
            lines.add(paragraph.substring(lineStart, lineEnd));
            lineStart = lineEnd;
        }
    }

    private static int fittingEnd(String text, int start, ComputedStyle style, double maxWidth) {
        int best = start + 1;
        int lastBreak = -1;
        for (int i = start + 1; i <= text.length(); i++) {
            if (Character.isWhitespace(text.charAt(i - 1))) {
                lastBreak = i;
            }
            if (measureText(text.substring(start, i), style) > maxWidth) {
                if (lastBreak > start) {
                    return lastBreak;
                }
                return best;
            }
            best = i;
        }
        return text.length();
    }

    private static List<String> splitLongWord(String word, ComputedStyle style, double maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            String candidate = current.toString() + word.charAt(i);
            if (current.isEmpty() || measureText(candidate, style) <= maxWidth) {
                current.append(word.charAt(i));
            } else {
                lines.add(current.toString());
                current.setLength(0);
                current.append(word.charAt(i));
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    private static boolean isImage(LayoutBox box) {
        return box.styledNode().node() instanceof ElementNode element && "img".equals(element.tagName());
    }

    private static boolean isFormControl(StyledNode styledNode) {
        return styledNode.node() instanceof ElementNode element
                && ("input".equals(element.tagName()) || "textarea".equals(element.tagName()));
    }

    private static boolean isInlineLevel(LayoutBox box) {
        String display = box.styledNode().style().get("display", "block");
        return "inline".equals(display) || "text".equals(display);
    }

    private static double childrenContentWidth(LayoutBox box, double contentX) {
        double right = contentX;
        for (LayoutBox child : box.children()) {
            right = Math.max(right, child.x() + child.width());
        }
        return Math.max(0, right - contentX);
    }

    private static InlineAdvance inlineAdvance(LayoutBox child, double lineStartX) {
        if (child.styledNode().node() instanceof TextNode && !child.textLines().isEmpty()) {
            double lineHeight = lineHeight(child.styledNode().style());
            List<String> lines = child.textLines();
            String lastLine = lines.getLast();
            return new InlineAdvance(
                    lineStartX + TextControlLayout.measureText(lastLine, child.styledNode().style()),
                    child.y() + (lines.size() - 1) * lineHeight,
                    lineHeight
            );
        }
        return new InlineAdvance(child.x() + child.width(), child.y(), child.height());
    }

    private record InlineAdvance(double cursorX, double cursorY, double lineHeight) {
    }

    private static double imageWidth(LayoutBox box, double fallback) {
        ComputedStyle style = box.styledNode().style();
        double cssWidth = StyleValues.length(style, "width", fallback, Double.NaN);
        if (!Double.isNaN(cssWidth)) {
            return cssWidth;
        }
        return ((ElementNode) box.styledNode().node()).attribute("width")
                .map(value -> StyleValues.parseLength(value, fallback, Double.NaN))
                .filter(value -> !Double.isNaN(value))
                .orElse(Math.min(fallback, 120));
    }

    private static double imageHeight(LayoutBox box, double width) {
        ComputedStyle style = box.styledNode().style();
        double cssHeight = StyleValues.length(style, "height", width, Double.NaN);
        if (!Double.isNaN(cssHeight)) {
            return cssHeight;
        }
        return ((ElementNode) box.styledNode().node()).attribute("height")
                .map(value -> StyleValues.parseLength(value, width, Double.NaN))
                .filter(value -> !Double.isNaN(value))
                .orElse(width * 0.66);
    }
}
