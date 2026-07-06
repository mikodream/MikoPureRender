package com.miko.purerender;

import com.miko.purerender.css.ElementState;
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

import java.util.Set;

class StyleResolverTest {
    @Test
    void resolvesSpecificityAndInlineStyles() {
        DocumentNode document = new HtmlParser().parse("<div id='app' class='card' style='color: #000'>Text</div>");
        StyledNode styled = new StyleResolver().resolve(
                document,
                new CssParser().parse("div { color: red; } .card { color: blue; } #app { background-color: #fff; }")
        );

        StyledNode div = styled.children().getFirst();
        assertInstanceOf(ElementNode.class, div.node());
        assertEquals("#000", div.style().get("color", ""));
        assertEquals("#fff", div.style().get("background-color", ""));
    }

    @Test
    void resolvesImportantAndChildSelectors() {
        DocumentNode document = new HtmlParser().parse("<div class='parent'><p class='child' style='color: blue'>Text</p></div>");
        StyledNode styled = new StyleResolver().resolve(
                document,
                new CssParser().parse(".parent > .child { color: red !important; }")
        );

        StyledNode child = styled.children().getFirst().children().getFirst();
        assertEquals("red", child.style().get("color", ""));
    }

    @Test
    void resolvesHoverActiveAndFocusPseudoClasses() {
        DocumentNode document = new HtmlParser().parse("<button class='primary'>Press</button>");
        ElementNode button = assertInstanceOf(ElementNode.class, document.children().getFirst());

        StyledNode styled = new StyleResolver().resolve(
                document,
                new CssParser().parse("""
                        .primary:hover { background-color: #111; }
                        .primary:active { color: #fff; }
                        .primary:focus { border-color: #38bec9; }
                        .primary { background-color: #eee; color: #222; border-color: #ccc; }
                        """),
                ElementState.of(Set.of(button), Set.of(button), Set.of(button))
        );

        StyledNode styledButton = styled.children().getFirst();
        assertEquals("#111", styledButton.style().get("background-color", ""));
        assertEquals("#fff", styledButton.style().get("color", ""));
        assertEquals("#38bec9", styledButton.style().get("border-color", ""));
    }

    @Test
    void expandsBasicBorderBackgroundAndInteractionProperties() {
        DocumentNode document = new HtmlParser().parse("<div class='panel'></div>");

        StyledNode styled = new StyleResolver().resolve(
                document,
                new CssParser().parse(".panel { border: 2px solid #333; background: #fafafa; overflow: hidden; cursor: pointer; }")
        );

        StyledNode panel = styled.children().getFirst();
        assertEquals("2px", panel.style().get("border-width", ""));
        assertEquals("#333", panel.style().get("border-color", ""));
        assertEquals("#fafafa", panel.style().get("background-color", ""));
        assertEquals("hidden", panel.style().get("overflow", ""));
        assertEquals("pointer", panel.style().get("cursor", ""));
    }

    @Test
    void cursorIsInheritedByDescendantTextLikeCss() {
        DocumentNode document = new HtmlParser().parse("<article class='card'><p>Hover text</p></article>");

        StyledNode styled = new StyleResolver().resolve(
                document,
                new CssParser().parse(".card { cursor: pointer; }")
        );

        StyledNode text = styled.children().getFirst().children().getFirst().children().getFirst();
        assertEquals("pointer", text.style().get("cursor", ""));
    }

    @Test
    void userSelectCanDisableDescendantTextSelection() {
        DocumentNode document = new HtmlParser().parse("<button><span>Copy</span></button>");

        StyledNode styled = new StyleResolver().resolve(
                document,
                new CssParser().parse("button { user-select: none; }")
        );

        StyledNode text = styled.children().getFirst().children().getFirst().children().getFirst();
        assertEquals("none", text.style().get("user-select", ""));
    }

    @Test
    void resolvesConfigurableScrollbarProperties() {
        DocumentNode document = new HtmlParser().parse("<div class='panel'></div>");

        StyledNode styled = new StyleResolver().resolve(
                document,
                new CssParser().parse(".panel { overflow: auto; scrollbar-width: thin; scrollbar-color: #111 #eee; }")
        );

        StyledNode panel = styled.children().getFirst();
        assertEquals("auto", panel.style().get("overflow", ""));
        assertEquals("auto", panel.style().get("overflow-x", ""));
        assertEquals("auto", panel.style().get("overflow-y", ""));
        assertEquals("thin", panel.style().get("scrollbar-width", ""));
        assertEquals("#111 #eee", panel.style().get("scrollbar-color", ""));
    }

    @Test
    void overflowAxesCanOverrideOverflowShorthand() {
        DocumentNode document = new HtmlParser().parse("<div class='panel'></div>");

        StyledNode styled = new StyleResolver().resolve(
                document,
                new CssParser().parse(".panel { overflow: auto; overflow-y: hidden; }")
        );

        StyledNode panel = styled.children().getFirst();
        assertEquals("auto", panel.style().get("overflow", ""));
        assertEquals("auto", panel.style().get("overflow-x", ""));
        assertEquals("hidden", panel.style().get("overflow-y", ""));
    }

    @Test
    void appliesDefaultFormControlStyles() {
        DocumentNode document = new HtmlParser().parse("<input value='A'><textarea>B</textarea>");

        StyledNode styled = new StyleResolver().resolve(document, new CssParser().parse(""));
        StyledNode input = styled.children().getFirst();
        StyledNode textarea = styled.children().get(1);

        assertEquals("32px", input.style().get("height", ""));
        assertEquals("text", input.style().get("cursor", ""));
        assertEquals("nowrap", input.style().get("white-space", ""));
        assertEquals("88px", textarea.style().get("height", ""));
        assertEquals("auto", textarea.style().get("overflow", ""));
        assertEquals("auto", textarea.style().get("overflow-x", ""));
        assertEquals("auto", textarea.style().get("overflow-y", ""));
        assertEquals("pre-wrap", textarea.style().get("white-space", ""));
    }

    @Test
    void textareasWrapByDefaultAndCanDisableWrappingWithCss() {
        DocumentNode document = new HtmlParser().parse("<textarea>abcdefghij</textarea><textarea class='nowrap'>abcdefghij</textarea>");

        StyledNode styled = new StyleResolver().resolve(
                document,
                new CssParser().parse(".nowrap { white-space: pre; }")
        );

        ElementNode wrappingElement = assertInstanceOf(ElementNode.class, styled.children().getFirst().node());
        ElementNode nowrapElement = assertInstanceOf(ElementNode.class, styled.children().get(1).node());

        assertEquals(true, TextControlLayout.wraps(wrappingElement, styled.children().getFirst().style()));
        assertEquals(false, TextControlLayout.wraps(nowrapElement, styled.children().get(1).style()));
    }
}
