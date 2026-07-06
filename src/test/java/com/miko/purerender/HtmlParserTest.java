package com.miko.purerender;

import com.miko.purerender.dom.DocumentNode;
import com.miko.purerender.dom.ElementNode;
import com.miko.purerender.dom.TextNode;
import com.miko.purerender.html.HtmlParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class HtmlParserTest {
    @Test
    void parsesElementsAttributesAndText() {
        DocumentNode document = new HtmlParser().parse("<div id=\"app\" class='root'><p>Hello&nbsp;Java</p></div>");

        ElementNode div = assertInstanceOf(ElementNode.class, document.children().getFirst());
        assertEquals("div", div.tagName());
        assertEquals("app", div.attribute("id").orElseThrow());
        assertEquals("root", div.attribute("class").orElseThrow());

        ElementNode p = assertInstanceOf(ElementNode.class, div.children().getFirst());
        TextNode text = assertInstanceOf(TextNode.class, p.children().getFirst());
        assertEquals("Hello Java", text.text());
    }

    @Test
    void preservesRawTextInsideStyleAndScriptTags() {
        DocumentNode document = new HtmlParser().parse("<style>.a > .b { color: red; }</style>");

        ElementNode style = assertInstanceOf(ElementNode.class, document.children().getFirst());
        TextNode text = assertInstanceOf(TextNode.class, style.children().getFirst());
        assertEquals(".a > .b { color: red; }", text.text());
    }

    @Test
    void preservesTextareaWhitespaceAndEntities() {
        DocumentNode document = new HtmlParser().parse("<textarea>\n  line 1\n  line&nbsp;2</textarea>");

        ElementNode textarea = assertInstanceOf(ElementNode.class, document.children().getFirst());
        TextNode text = assertInstanceOf(TextNode.class, textarea.children().getFirst());

        assertEquals("  line 1\n  line 2", text.text());
    }
}
