package com.miko.purerender;

import com.miko.purerender.layout.TextEditModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextEditModelTest {
    @Test
    void stripsLineBreaksForSingleLineInput() {
        TextEditModel model = new TextEditModel("ab", 2, false, -1, true);

        assertTrue(model.insert("c\r\nd"));

        assertEquals("abcd", model.text());
        assertEquals(4, model.caret());
    }

    @Test
    void normalizesLineBreaksForMultilineText() {
        TextEditModel model = new TextEditModel("a", 1, true, -1, true);

        assertTrue(model.insert("\r\nb\rc"));

        assertEquals("a\nb\nc", model.text());
    }

    @Test
    void enforcesMaxLengthAfterReplacingSelection() {
        TextEditModel model = new TextEditModel("hello", 5, false, 6, true);
        model.select(1, 4);

        assertTrue(model.insert("ABCDE"));

        assertEquals("hABCDo", model.text());
        assertEquals(5, model.caret());
    }

    @Test
    void preventsEditingWhenReadonlyOrDisabled() {
        TextEditModel model = new TextEditModel("hello", 5, false, 10, false);
        model.select(0, 5);

        assertFalse(model.insert("x"));
        assertFalse(model.backspace());
        assertFalse(model.deleteForward());
        assertEquals("hello", model.text());
        assertEquals(0, model.selectionStart());
        assertEquals(5, model.selectionEnd());
    }

    @Test
    void deletesSelectionAndAdjacentCharacters() {
        TextEditModel model = new TextEditModel("abcd", 2, false, -1, true);

        assertTrue(model.backspace());
        assertEquals("acd", model.text());
        assertEquals(1, model.caret());

        assertTrue(model.deleteForward());
        assertEquals("ad", model.text());

        model.select(0, 2);
        assertTrue(model.deleteSelection());
        assertEquals("", model.text());
        assertEquals(0, model.caret());
    }
}
