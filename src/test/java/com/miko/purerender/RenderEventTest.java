package com.miko.purerender;

import com.miko.purerender.dom.ElementNode;
import com.miko.purerender.event.RenderEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderEventTest {
    @Test
    void carriesTargetCurrentTargetCoordinatesAndPropagationState() {
        ElementNode target = new ElementNode("button");
        ElementNode currentTarget = new ElementNode("section");
        RenderEvent event = new RenderEvent("click", target, currentTarget, 12, 24);

        assertEquals("click", event.type());
        assertEquals(target, event.target());
        assertEquals(currentTarget, event.currentTarget());
        assertEquals(12, event.x());
        assertEquals(24, event.y());
        assertFalse(event.isPropagationStopped());

        event.stopPropagation();

        assertTrue(event.isPropagationStopped());
    }
}
