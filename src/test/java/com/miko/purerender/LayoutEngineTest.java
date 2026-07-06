package com.miko.purerender;

import com.miko.purerender.css.CssParser;
import com.miko.purerender.dom.DocumentNode;
import com.miko.purerender.dom.ElementNode;
import com.miko.purerender.html.HtmlParser;
import com.miko.purerender.layout.HitTester;
import com.miko.purerender.layout.LayoutBox;
import com.miko.purerender.layout.LayoutEngine;
import com.miko.purerender.style.StyleResolver;
import com.miko.purerender.style.StyledNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayoutEngineTest {
    @Test
    void laysOutBlockChildrenVertically() {
        DocumentNode document = new HtmlParser().parse("<div><p>A</p><p>B</p></div>");
        StyledNode styled = new StyleResolver().resolve(
                document,
                new CssParser().parse("div { padding: 10px; } p { height: 20px; margin: 0 0 5px 0; }")
        );

        LayoutBox root = new LayoutEngine().layout(styled, 300);
        LayoutBox div = root.children().getFirst();
        LayoutBox firstP = div.children().getFirst();
        LayoutBox secondP = div.children().get(1);

        assertEquals(10, firstP.x());
        assertTrue(secondP.y() > firstP.y());
    }

    @Test
    void laysOutFlexChildrenInRows() {
        DocumentNode document = new HtmlParser().parse("<div class='row'><span>A</span><span>B</span></div>");
        StyledNode styled = new StyleResolver().resolve(
                document,
                new CssParser().parse(".row { display: flex; gap: 8px; } span { width: 20px; height: 10px; }")
        );

        LayoutBox root = new LayoutEngine().layout(styled, 300);
        LayoutBox row = root.children().getFirst();
        LayoutBox first = row.children().getFirst();
        LayoutBox second = row.children().get(1);

        assertEquals(first.x() + first.width() + 8, second.x());
    }

    @Test
    void wrapsTextIntoMultipleLines() {
        DocumentNode document = new HtmlParser().parse("<p>Alpha beta gamma delta epsilon zeta</p>");
        StyledNode styled = new StyleResolver().resolve(
                document,
                new CssParser().parse("p { width: 80px; font-size: 16px; }")
        );

        LayoutBox root = new LayoutEngine().layout(styled, 300);
        LayoutBox text = root.children().getFirst().children().getFirst();

        assertTrue(text.textLines().size() > 1);
    }

    @Test
    void laysOutSimpleGridAndSupportsHitTesting() {
        DocumentNode document = new HtmlParser().parse("<div class='grid'><div class='item'></div><div class='item'></div></div>");
        StyledNode styled = new StyleResolver().resolve(
                document,
                new CssParser().parse(".grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; } .item { height: 20px; }")
        );

        LayoutBox root = new LayoutEngine().layout(styled, 210);
        LayoutBox grid = root.children().getFirst();
        LayoutBox first = grid.children().getFirst();
        LayoutBox second = grid.children().get(1);

        assertEquals(first.x() + first.width() + 10, second.x());
        assertEquals(second, new HitTester().hitTest(root, second.x() + 1, second.y() + 1).orElseThrow());
    }

    @Test
    void usesImageAttributesForImageBoxSize() {
        DocumentNode document = new HtmlParser().parse("<img src='missing.png' width='200' height='100'>");
        StyledNode styled = new StyleResolver().resolve(document, new CssParser().parse(""));

        LayoutBox image = new LayoutEngine().layout(styled, 300).children().getFirst();

        assertEquals(200, image.width());
        assertEquals(100, image.height());
    }

    @Test
    void hoverChainIncludesAncestorsWhenTextIsHit() {
        DocumentNode document = new HtmlParser().parse("<article class='card'><p>Hover text</p></article>");
        StyledNode styled = new StyleResolver().resolve(
                document,
                new CssParser().parse(".card { width: 200px; padding: 16px; }")
        );

        LayoutBox root = new LayoutEngine().layout(styled, 300);
        LayoutBox card = root.children().getFirst();
        LayoutBox text = card.children().getFirst().children().getFirst();
        ElementNode cardElement = (ElementNode) card.styledNode().node();

        assertTrue(new HitTester().hoverChain(root, text.x() + 1, text.y() + 1).contains(cardElement));
    }

    @Test
    void hitTestingAccountsForScrollOffsets() {
        DocumentNode document = new HtmlParser().parse("""
                <div class='scroller'>
                  <p id='first'></p>
                  <p id='second'></p>
                </div>
                """);
        StyledNode styled = new StyleResolver().resolve(
                document,
                new CssParser().parse(".scroller { height: 20px; overflow: auto; } p { height: 20px; margin: 0; }")
        );

        LayoutBox root = new LayoutEngine().layout(styled, 100);
        LayoutBox scroller = root.children().getFirst();
        scroller.setScroll(0, 20);

        ElementNode hit = new HitTester().hitElement(root, scroller.x() + 1, scroller.y() + 1).orElseThrow();

        assertEquals("second", hit.attribute("id").orElseThrow());
    }

    @Test
    void treatsFormControlsAsLeafLayoutBoxes() {
        DocumentNode document = new HtmlParser().parse("<textarea>Text child should not create layout child</textarea>");
        StyledNode styled = new StyleResolver().resolve(document, new CssParser().parse(""));

        LayoutBox textarea = new LayoutEngine().layout(styled, 300).children().getFirst();

        assertEquals(0, textarea.children().size());
        assertEquals(102, textarea.height());
    }
}
