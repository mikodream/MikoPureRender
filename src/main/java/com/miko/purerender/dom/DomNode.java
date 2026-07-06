package com.miko.purerender.dom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class DomNode {
    private final NodeType type;
    private DomNode parent;
    private final List<DomNode> children = new ArrayList<>();

    protected DomNode(NodeType type) {
        this.type = type;
    }

    public NodeType type() {
        return type;
    }

    public DomNode parent() {
        return parent;
    }

    public List<DomNode> children() {
        return Collections.unmodifiableList(children);
    }

    public void appendChild(DomNode child) {
        child.parent = this;
        children.add(child);
    }

    public void clearChildren() {
        for (DomNode child : children) {
            child.parent = null;
        }
        children.clear();
    }

    public boolean isElement() {
        return type == NodeType.ELEMENT;
    }

    public boolean isText() {
        return type == NodeType.TEXT;
    }
}
