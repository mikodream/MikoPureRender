package com.miko.purerender.style;

public final class StyleValues {
    private StyleValues() {
    }

    public static double length(ComputedStyle style, String property, double fallback) {
        return parseLength(style.get(property, ""), fallback);
    }

    public static double length(ComputedStyle style, String property, double percentBase, double fallback) {
        return parseLength(style.get(property, ""), percentBase, fallback);
    }

    public static double parseLength(String raw, double fallback) {
        return parseLength(raw, Double.NaN, fallback);
    }

    public static double parseLength(String raw, double percentBase, double fallback) {
        if (raw == null || raw.isBlank() || raw.equals("auto") || raw.equals("transparent")) {
            return fallback;
        }
        String value = raw.trim().toLowerCase();
        if (value.endsWith("%") && !Double.isNaN(percentBase)) {
            try {
                return percentBase * Double.parseDouble(value.substring(0, value.length() - 1)) / 100.0;
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        if (value.endsWith("px")) {
            value = value.substring(0, value.length() - 2);
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static Edges edges(ComputedStyle style, String property) {
        return edges(style, property, Double.NaN);
    }

    public static Edges edges(ComputedStyle style, String property, double percentBase) {
        String raw = style.get(property, "0");
        String[] parts = raw.trim().split("\\s+");
        double top;
        double right;
        double bottom;
        double left;
        if (parts.length == 1) {
            top = right = bottom = left = parseLength(parts[0], percentBase, 0);
        } else if (parts.length == 2) {
            top = bottom = parseLength(parts[0], percentBase, 0);
            right = left = parseLength(parts[1], percentBase, 0);
        } else if (parts.length == 3) {
            top = parseLength(parts[0], percentBase, 0);
            right = left = parseLength(parts[1], percentBase, 0);
            bottom = parseLength(parts[2], percentBase, 0);
        } else {
            top = parseLength(parts[0], percentBase, 0);
            right = parseLength(parts[1], percentBase, 0);
            bottom = parseLength(parts[2], percentBase, 0);
            left = parseLength(parts[3], percentBase, 0);
        }
        top = length(style, property + "-top", percentBase, top);
        right = length(style, property + "-right", percentBase, right);
        bottom = length(style, property + "-bottom", percentBase, bottom);
        left = length(style, property + "-left", percentBase, left);
        return new Edges(top, right, bottom, left);
    }

    public record Edges(double top, double right, double bottom, double left) {
        public double horizontal() {
            return left + right;
        }

        public double vertical() {
            return top + bottom;
        }
    }
}
