package com.miko.purerender.dom;

public final class TextNode extends DomNode {
    private String text;

    public TextNode(String text) {
        super(NodeType.TEXT);
        this.text = text;
    }

    public String text() {
        return text;
    }

    public void setText(String text) {
        this.text = text == null ? "" : text;
    }
}
