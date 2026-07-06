package com.miko.purerender;

import com.miko.purerender.layout.TextLineMetrics;
import com.miko.purerender.style.ComputedStyle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextLineMetricsTest {
    @Test
    void usesMeasuredGlyphWidthsForOffsets() {
        ComputedStyle style = new ComputedStyle();
        style.set("font-family", "System");
        style.set("font-size", "16px");
        style.set("font-weight", "normal");

        double[] widths = TextLineMetrics.prefixWidths("iW", style);

        assertTrue(widths[1] < widths[2] - widths[1]);
        assertEquals(0, TextLineMetrics.offsetAt(widths, widths[1] / 2.0 - 0.1));
        assertEquals(1, TextLineMetrics.offsetAt(widths, widths[1] / 2.0 + 0.1));
        assertEquals(widths[2], TextLineMetrics.xForOffset(widths, 2));
    }

    @Test
    void clampsOffsetsAndCoordinatesAtLineBounds() {
        ComputedStyle style = style("16px", "normal");
        double[] widths = TextLineMetrics.prefixWidths("abc", style);

        assertEquals(0, TextLineMetrics.offsetAt(widths, -10));
        assertEquals(3, TextLineMetrics.offsetAt(widths, widths[3] + 10));
        assertEquals(0, TextLineMetrics.xForOffset(widths, -1));
        assertEquals(widths[3], TextLineMetrics.xForOffset(widths, 99));
    }

    @Test
    void measurementRespondsToFontStyle() {
        double[] normal = TextLineMetrics.prefixWidths("Hello", style("16px", "normal"));
        double[] large = TextLineMetrics.prefixWidths("Hello", style("24px", "normal"));
        double[] bold = TextLineMetrics.prefixWidths("Hello", style("16px", "bold"));

        assertTrue(large[5] > normal[5]);
        assertTrue(bold[5] > 0);
    }

    private static ComputedStyle style(String fontSize, String fontWeight) {
        ComputedStyle style = new ComputedStyle();
        style.set("font-family", "System");
        style.set("font-size", fontSize);
        style.set("font-weight", fontWeight);
        return style;
    }
}
