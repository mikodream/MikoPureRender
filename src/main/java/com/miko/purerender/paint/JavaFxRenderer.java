package com.miko.purerender.paint;

import com.miko.purerender.dom.ElementNode;
import com.miko.purerender.dom.DomNode;
import com.miko.purerender.dom.TextNode;
import com.miko.purerender.layout.LayoutBox;
import com.miko.purerender.layout.TextControlLayout;
import com.miko.purerender.style.ComputedStyle;
import com.miko.purerender.style.StyleValues;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class JavaFxRenderer {
    private final Map<String, Image> imageCache = new HashMap<>();
    private final Runnable repaintCallback;

    public JavaFxRenderer() {
        this(() -> {
        });
    }

    public JavaFxRenderer(Runnable repaintCallback) {
        this.repaintCallback = repaintCallback == null ? () -> {
        } : repaintCallback;
    }

    public void render(Canvas canvas, LayoutBox root) {
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        graphics.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        paintBox(graphics, root);
    }

    private void paintBox(GraphicsContext graphics, LayoutBox box) {
        ComputedStyle style = box.styledNode().style();
        if (box.styledNode().node() instanceof TextNode textNode) {
            paintText(graphics, box, style, textNode.text());
        } else {
            paintShadow(graphics, box, style);
            paintBackground(graphics, box, style);
            paintImage(graphics, box);
            paintBorder(graphics, box, style);
            paintFormControl(graphics, box, style);
        }

        boolean clipped = isClippedOverflow(style);
        if (clipped) {
            graphics.save();
            graphics.beginPath();
            graphics.rect(box.x(), box.y(), box.width(), box.height());
            graphics.clip();
            graphics.translate(-box.scrollX(), -box.scrollY());
        }
        try {
            for (LayoutBox child : box.children()) {
                paintBox(graphics, child);
            }
        } finally {
            if (clipped) {
                graphics.restore();
            }
        }
        paintScrollbar(graphics, box, style);
    }

    private static boolean isClippedOverflow(ComputedStyle style) {
        return isClippedOverflowValue(overflowX(style)) || isClippedOverflowValue(overflowY(style));
    }

    private static boolean isClippedOverflowValue(String overflow) {
        return "hidden".equals(overflow)
                || "clip".equals(overflow)
                || "auto".equals(overflow)
                || "scroll".equals(overflow);
    }

    private void paintBackground(GraphicsContext graphics, LayoutBox box, ComputedStyle style) {
        String raw = style.get("background-color", "transparent");
        if ("transparent".equals(raw)) {
            raw = style.get("background", "transparent");
        }
        if ("transparent".equals(raw)) {
            return;
        }
        graphics.setFill(parsePaint(raw, box, Color.TRANSPARENT));
        fillBox(graphics, box, style);
    }

    private void paintShadow(GraphicsContext graphics, LayoutBox box, ComputedStyle style) {
        String raw = style.get("box-shadow", "none");
        if ("none".equals(raw) || "transparent".equals(style.get("background-color", "transparent"))) {
            return;
        }
        String[] parts = raw.trim().split("\\s+");
        if (parts.length < 3) {
            return;
        }
        double offsetX = StyleValues.parseLength(parts[0], 0);
        double offsetY = StyleValues.parseLength(parts[1], 0);
        double radius = StyleValues.parseLength(parts[2], 0);
        Color color = parts.length >= 4 ? parseColor(parts[3], Color.color(0, 0, 0, 0.24)) : Color.color(0, 0, 0, 0.24);

        graphics.save();
        graphics.setEffect(new DropShadow(radius, offsetX, offsetY, color));
        graphics.setFill(parsePaint(style.get("background-color", "#ffffff"), box, Color.WHITE));
        fillBox(graphics, box, style);
        graphics.restore();
    }

    private void fillBox(GraphicsContext graphics, LayoutBox box, ComputedStyle style) {
        double radius = StyleValues.length(style, "border-radius", 0);
        if (radius > 0) {
            graphics.fillRoundRect(box.x(), box.y(), box.width(), box.height(), radius * 2, radius * 2);
        } else {
            graphics.fillRect(box.x(), box.y(), box.width(), box.height());
        }
    }

    private void paintImage(GraphicsContext graphics, LayoutBox box) {
        if (!(box.styledNode().node() instanceof ElementNode element) || !"img".equals(element.tagName())) {
            return;
        }
        element.attribute("src").ifPresent(src -> {
            Image image = imageCache.computeIfAbsent(src, this::loadImage);
            if (image != null && !image.isError()) {
                graphics.drawImage(image, box.x(), box.y(), box.width(), box.height());
            }
        });
    }

    private void paintBorder(GraphicsContext graphics, LayoutBox box, ComputedStyle style) {
        double border = StyleValues.length(style, "border-width", 0);
        if (border <= 0) {
            return;
        }
        graphics.setStroke(parseColor(style.get("border-color", "#000000"), Color.BLACK));
        graphics.setLineWidth(border);
        double offset = border / 2.0;
        double radius = StyleValues.length(style, "border-radius", 0);
        if (radius > 0) {
            graphics.strokeRoundRect(
                    box.x() + offset,
                    box.y() + offset,
                    Math.max(0, box.width() - border),
                    Math.max(0, box.height() - border),
                    radius * 2,
                    radius * 2
            );
        } else {
            graphics.strokeRect(
                    box.x() + offset,
                    box.y() + offset,
                    Math.max(0, box.width() - border),
                    Math.max(0, box.height() - border)
            );
        }
    }

    private void paintText(GraphicsContext graphics, LayoutBox box, ComputedStyle style, String text) {
        double fontSize = StyleValues.length(style, "font-size", 16);
        double lineHeight = StyleValues.length(style, "line-height", fontSize * 1.35);
        String family = style.get("font-family", "System");
        FontWeight weight = "bold".equalsIgnoreCase(style.get("font-weight", "normal"))
                ? FontWeight.BOLD
                : FontWeight.NORMAL;
        graphics.setFont(Font.font(family, weight, fontSize));
        graphics.setFill(parseColor(style.get("color", "#222222"), Color.web("#222222")));
        List<String> lines = box.textLines().isEmpty() ? List.of(text) : box.textLines();
        for (int i = 0; i < lines.size(); i++) {
            graphics.fillText(lines.get(i), box.x(), box.y() + fontSize + i * lineHeight);
        }
    }

    private void paintFormControl(GraphicsContext graphics, LayoutBox box, ComputedStyle style) {
        if (!(box.styledNode().node() instanceof ElementNode element) || !isTextControl(element)) {
            return;
        }
        String rawValue = controlText(element);
        String value = displayControlText(element, rawValue);
        boolean placeholder = value.isEmpty();
        if (placeholder) {
            value = element.attribute("placeholder").orElse("");
        }
        if (value.isEmpty()) {
            return;
        }

        TextControlLayout.Metrics metrics = TextControlLayout.compute(element, style, box.width(), box.height(), value);
        double fontSize = metrics.fontSize();
        double lineHeight = metrics.lineHeight();
        double border = metrics.border();
        StyleValues.Edges padding = metrics.padding();
        double x = box.x() + border + padding.left();
        double y = box.y() + border + padding.top();

        graphics.save();
        graphics.beginPath();
        graphics.rect(box.x(), box.y(), box.width(), box.height());
        graphics.clip();
        graphics.setFont(Font.font(style.get("font-family", "System"), FontWeight.NORMAL, fontSize));
        graphics.setFill(placeholder ? Color.web("#9aa5b1") : parseColor(style.get("color", "#222222"), Color.web("#222222")));
        for (TextControlLayout.Line line : metrics.lines()) {
            graphics.fillText(line.text(), x - box.scrollX(), y - box.scrollY() + fontSize + line.index() * lineHeight);
        }
        graphics.restore();
    }

    private void paintScrollbar(GraphicsContext graphics, LayoutBox box, ComputedStyle style) {
        double width = scrollbarWidth(style);
        if (width <= 0) {
            return;
        }
        double maxScrollTop = maxScrollTop(box);
        double maxScrollLeft = maxScrollLeft(box);
        boolean vertical = showsScrollbar(overflowY(style), maxScrollTop);
        boolean horizontal = showsScrollbar(overflowX(style), maxScrollLeft);
        if (!vertical && !horizontal) {
            return;
        }

        ScrollbarColors colors = scrollbarColors(style);
        if (vertical) {
            paintVerticalScrollbar(graphics, box, colors, width, horizontal, maxScrollTop);
        }
        if (horizontal) {
            paintHorizontalScrollbar(graphics, box, colors, width, vertical, maxScrollLeft);
        }
    }

    private static void paintVerticalScrollbar(
            GraphicsContext graphics,
            LayoutBox box,
            ScrollbarColors colors,
            double width,
            boolean hasHorizontalScrollbar,
            double maxScrollTop
    ) {
        double trackX = box.x() + box.width() - width;
        double trackY = box.y();
        double trackHeight = Math.max(0, box.height() - (hasHorizontalScrollbar ? width : 0));
        double contentHeight = box.height() + maxScrollTop;
        double thumbHeight = maxScrollTop <= 0
                ? trackHeight
                : Math.max(24, trackHeight * box.height() / contentHeight);
        thumbHeight = Math.min(trackHeight, thumbHeight);
        double thumbY = maxScrollTop <= 0
                ? trackY
                : trackY + (trackHeight - thumbHeight) * (box.scrollY() / maxScrollTop);

        graphics.save();
        graphics.setFill(colors.track());
        graphics.fillRect(trackX, trackY, width, trackHeight);
        graphics.setFill(colors.thumb());
        graphics.fillRoundRect(trackX + 1, thumbY + 1, Math.max(1, width - 2), Math.max(1, thumbHeight - 2), width, width);
        graphics.restore();
    }

    private static void paintHorizontalScrollbar(
            GraphicsContext graphics,
            LayoutBox box,
            ScrollbarColors colors,
            double width,
            boolean hasVerticalScrollbar,
            double maxScrollLeft
    ) {
        double trackX = box.x();
        double trackY = box.y() + box.height() - width;
        double trackWidth = Math.max(0, box.width() - (hasVerticalScrollbar ? width : 0));
        double contentWidth = box.width() + maxScrollLeft;
        double thumbWidth = maxScrollLeft <= 0
                ? trackWidth
                : Math.max(24, trackWidth * box.width() / contentWidth);
        thumbWidth = Math.min(trackWidth, thumbWidth);
        double thumbX = maxScrollLeft <= 0
                ? trackX
                : trackX + (trackWidth - thumbWidth) * (box.scrollX() / maxScrollLeft);

        graphics.save();
        graphics.setFill(colors.track());
        graphics.fillRect(trackX, trackY, trackWidth, width);
        graphics.setFill(colors.thumb());
        graphics.fillRoundRect(thumbX + 1, trackY + 1, Math.max(1, thumbWidth - 2), Math.max(1, width - 2), width, width);
        graphics.restore();
    }

    private Image loadImage(String src) {
        try {
            String uri = src.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*") ? src : new File(src).toURI().toString();
            Image image = new Image(uri, true);
            image.progressProperty().addListener((ignored, oldValue, newValue) -> {
                if (newValue.doubleValue() >= 1.0) {
                    repaintCallback.run();
                }
            });
            image.errorProperty().addListener((ignored, oldValue, newValue) -> repaintCallback.run());
            return image;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static Paint parsePaint(String raw, LayoutBox box, Color fallback) {
        if (raw.startsWith("linear-gradient(") && raw.endsWith(")")) {
            String body = raw.substring("linear-gradient(".length(), raw.length() - 1).trim();
            String[] parts = body.split(",");
            if (parts.length >= 2) {
                boolean toRight = parts[0].trim().equalsIgnoreCase("to right");
                int colorStart = toRight ? 1 : 0;
                Color first = parseColor(parts[colorStart].trim(), fallback);
                Color second = parseColor(parts[Math.min(colorStart + 1, parts.length - 1)].trim(), first);
                return new LinearGradient(
                        0,
                        0,
                        toRight ? box.width() : 0,
                        toRight ? 0 : box.height(),
                        false,
                        CycleMethod.NO_CYCLE,
                        new Stop(0, first),
                        new Stop(1, second)
                );
            }
        }
        return parseColor(raw, fallback);
    }

    private static Color parseColor(String raw, Color fallback) {
        try {
            return Color.web(raw);
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private static double maxScrollTop(LayoutBox box) {
        if (box.scrollContentHeight() > box.height()) {
            return Math.max(0, box.scrollContentHeight() - box.height());
        }
        double contentBottom = box.y() + box.height();
        for (LayoutBox child : box.children()) {
            contentBottom = Math.max(contentBottom, child.y() + child.height());
        }
        return Math.max(0, contentBottom - (box.y() + box.height()));
    }

    private static double maxScrollLeft(LayoutBox box) {
        if (box.scrollContentWidth() > box.width()) {
            return Math.max(0, box.scrollContentWidth() - box.width());
        }
        double contentRight = box.x() + box.width();
        for (LayoutBox child : box.children()) {
            contentRight = Math.max(contentRight, child.x() + child.width());
        }
        return Math.max(0, contentRight - (box.x() + box.width()));
    }

    private static boolean showsScrollbar(String overflow, double maxScroll) {
        return "scroll".equals(overflow) || ("auto".equals(overflow) && maxScroll > 0);
    }

    private static String overflowX(ComputedStyle style) {
        return style.get("overflow-x", style.get("overflow", "visible"));
    }

    private static String overflowY(ComputedStyle style) {
        return style.get("overflow-y", style.get("overflow", "visible"));
    }

    private static double scrollbarWidth(ComputedStyle style) {
        String raw = style.get("scrollbar-width", "auto");
        return switch (raw) {
            case "none" -> 0;
            case "thin" -> 6;
            case "auto" -> 10;
            default -> StyleValues.parseLength(raw, 10);
        };
    }

    private static ScrollbarColors scrollbarColors(ComputedStyle style) {
        String[] parts = style.get("scrollbar-color", "#9aa5b1 #e5e7eb").trim().split("\\s+");
        Color thumb = parts.length >= 1 ? parseColor(parts[0], Color.web("#9aa5b1")) : Color.web("#9aa5b1");
        Color track = parts.length >= 2 ? parseColor(parts[1], Color.web("#e5e7eb")) : Color.web("#e5e7eb");
        return new ScrollbarColors(thumb, track);
    }

    private record ScrollbarColors(Color thumb, Color track) {
    }

    private static boolean isTextControl(ElementNode element) {
        return "input".equals(element.tagName()) || "textarea".equals(element.tagName());
    }

    private static String controlText(ElementNode element) {
        if ("input".equals(element.tagName())) {
            return element.attribute("value").orElse("");
        }
        StringBuilder text = new StringBuilder();
        collectText(element, text);
        return text.toString();
    }

    private static void collectText(DomNode node, StringBuilder text) {
        if (node instanceof TextNode textNode) {
            text.append(textNode.text());
        }
        for (DomNode child : node.children()) {
            collectText(child, text);
        }
    }

    private static String displayControlText(ElementNode element, String text) {
        if ("input".equals(element.tagName())
                && element.attribute("type").map(value -> value.equalsIgnoreCase("password")).orElse(false)) {
            return "*".repeat(text.length());
        }
        return text;
    }
}
