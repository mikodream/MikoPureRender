package com.miko.purerender;

import com.miko.purerender.css.CssParser;
import com.miko.purerender.dom.DocumentNode;
import com.miko.purerender.dom.ElementNode;
import com.miko.purerender.html.HtmlParser;
import com.miko.purerender.layout.TextControlLayout;
import com.miko.purerender.style.StyleResolver;
import com.miko.purerender.style.StyledNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextControlLayoutTest {
    @Test
    void caretAtSoftWrapBoundaryBelongsToNextVisualLine() {
        StyledNode styledTextarea = styledTextarea("<textarea>ab</textarea>", "textarea { font-size: 16px; }");
        ElementNode textarea = assertInstanceOf(ElementNode.class, styledTextarea.node());

        TextControlLayout.Metrics metrics = TextControlLayout.compute(textarea, styledTextarea.style(), 19, 120, "ab");

        assertTrue(metrics.lines().size() > 1);
        assertEquals(metrics.lineHeight(), metrics.caretLocation(1).y());
    }

    @Test
    void caretAtHardLineBreakBoundaryStaysOnPreviousLine() {
        StyledNode styledTextarea = styledTextarea("<textarea>a\nb</textarea>", "textarea { font-size: 16px; }");
        ElementNode textarea = assertInstanceOf(ElementNode.class, styledTextarea.node());

        TextControlLayout.Metrics metrics = TextControlLayout.compute(textarea, styledTextarea.style(), 120, 120, "a\nb");

        assertEquals(0, metrics.caretLocation(1).y());
        assertEquals(metrics.lineHeight(), metrics.caretLocation(2).y());
    }

    private static StyledNode styledTextarea(String html, String css) {
        DocumentNode document = new HtmlParser().parse(html);
        return new StyleResolver().resolve(document, new CssParser().parse(css)).children().getFirst();
    }
}
