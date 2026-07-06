package com.miko.purerender;

import com.miko.purerender.css.CssParser;
import com.miko.purerender.css.StyleSheet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CssParserTest {
    @Test
    void parsesSelectorGroupsAndDeclarations() {
        StyleSheet sheet = new CssParser().parse("div, .card { color: #333; padding: 8px; }");

        assertEquals(2, sheet.rules().size());
        assertEquals(2, sheet.rules().getFirst().declarations().size());
        assertEquals("color", sheet.rules().getFirst().declarations().getFirst().property());
    }

    @Test
    void parsesImportantDeclarations() {
        StyleSheet sheet = new CssParser().parse(".card { color: red !important; }");

        assertEquals("red", sheet.rules().getFirst().declarations().getFirst().value());
        assertEquals(true, sheet.rules().getFirst().declarations().getFirst().important());
    }
}
