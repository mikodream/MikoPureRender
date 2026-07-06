package com.miko.purerender.layout;

import com.miko.purerender.style.ComputedStyle;

public final class TextLineMetrics {
    private TextLineMetrics() {
    }

    public static double[] prefixWidths(String text, ComputedStyle style) {
        String value = text == null ? "" : text;
        double[] widths = new double[value.length() + 1];
        for (int i = 1; i <= value.length(); i++) {
            widths[i] = TextControlLayout.measureText(value.substring(0, i), style);
        }
        return widths;
    }

    public static double xForOffset(double[] prefixWidths, int offset) {
        if (prefixWidths.length == 0) {
            return 0;
        }
        int clamped = Math.max(0, Math.min(offset, prefixWidths.length - 1));
        return prefixWidths[clamped];
    }

    public static int offsetAt(double[] prefixWidths, double x) {
        if (prefixWidths.length <= 1 || x <= 0) {
            return 0;
        }
        for (int i = 1; i < prefixWidths.length; i++) {
            double previous = prefixWidths[i - 1];
            double current = prefixWidths[i];
            if (x < (previous + current) / 2.0) {
                return i - 1;
            }
        }
        return prefixWidths.length - 1;
    }
}
