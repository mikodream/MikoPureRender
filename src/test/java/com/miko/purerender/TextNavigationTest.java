package com.miko.purerender;

import com.miko.purerender.layout.TextNavigation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextNavigationTest {
    @Test
    void movesToPreviousWordBoundary() {
        String text = "alpha beta  gamma";

        assertEquals(12, TextNavigation.previousWordBoundary(text, text.length()));
        assertEquals(6, TextNavigation.previousWordBoundary(text, 11));
        assertEquals(0, TextNavigation.previousWordBoundary(text, 5));
    }

    @Test
    void movesToNextWordBoundary() {
        String text = "alpha beta  gamma";

        assertEquals(6, TextNavigation.nextWordBoundary(text, 0));
        assertEquals(12, TextNavigation.nextWordBoundary(text, 6));
        assertEquals(text.length(), TextNavigation.nextWordBoundary(text, 12));
    }

    @Test
    void resolvesCurrentLineBoundaries() {
        String text = "one\ntwo three\nfour";

        assertEquals(4, TextNavigation.lineStart(text, 8));
        assertEquals(13, TextNavigation.lineEnd(text, 8));
        assertEquals(0, TextNavigation.lineStart(text, 0));
        assertEquals(3, TextNavigation.lineEnd(text, 0));
        assertEquals(14, TextNavigation.lineStart(text, text.length()));
        assertEquals(text.length(), TextNavigation.lineEnd(text, text.length()));
    }
}
