package com.miko.purerender;

import com.miko.purerender.dom.DocumentNode;
import com.miko.purerender.dom.DomQuery;
import com.miko.purerender.dom.ElementNode;
import com.miko.purerender.dom.TextNode;
import com.miko.purerender.html.HtmlParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class DomQueryTest {
    @Test
    void queriesElementsWithCssSelectors() {
        DocumentNode document = new HtmlParser().parse("""
                <section id='app'>
                  <article class='card selected'><p>Hello</p></article>
                  <article class='card'><p>World</p></article>
                </section>
                """);

        assertEquals("section", DomQuery.querySelector(document, "#app").orElseThrow().tagName());
        assertEquals(2, DomQuery.querySelectorAll(document, ".card").size());
        assertEquals(1, DomQuery.querySelectorAll(document, ".card.selected").size());
    }

    @Test
    void elementTextContentCanBeReplacedForBackendUpdates() {
        DocumentNode document = new HtmlParser().parse("<p id='message'>Old</p>");
        ElementNode message = DomQuery.querySelector(document, "#message").orElseThrow();

        message.clearChildren();
        message.appendChild(new TextNode("Saved"));

        List<?> children = message.children();
        TextNode text = assertInstanceOf(TextNode.class, children.getFirst());
        assertEquals("Saved", text.text());
    }
}
