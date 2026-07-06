package com.miko.purerender.style;

import com.miko.purerender.dom.DomNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class StyledNode {
    private final DomNode node;
    private final ComputedStyle style;
    private final List<StyledNode> children = new ArrayList<>();

    public StyledNode(DomNode node, ComputedStyle style) {
        this.node = node;
        this.style = style;
    }

    public DomNode node() {
        return node;
    }

    public ComputedStyle style() {
        return style;
    }

    public List<StyledNode> children() {
        return Collections.unmodifiableList(children);
    }

    public void appendChild(StyledNode child) {
        children.add(child);
    }
}
