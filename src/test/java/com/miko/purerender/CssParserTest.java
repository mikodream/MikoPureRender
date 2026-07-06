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

    @Test
    void keepsSemicolonsAndColonsInsideDeclarationValues() {
        StyleSheet sheet = new CssParser().parse("""
                .icon {
                  background-image: url("data:image/svg+xml;utf8,<svg></svg>");
                  font-family: "A;B:C";
                  color: red;
                }
                """);

        assertEquals(3, sheet.rules().getFirst().declarations().size());
        assertEquals("url(\"data:image/svg+xml;utf8,<svg></svg>\")", sheet.rules().getFirst().declarations().getFirst().value());
        assertEquals("\"A;B:C\"", sheet.rules().getFirst().declarations().get(1).value());
        assertEquals("red", sheet.rules().getFirst().declarations().get(2).value());
    }
}
