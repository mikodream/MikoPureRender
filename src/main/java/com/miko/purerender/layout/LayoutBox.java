package com.miko.purerender.layout;

import com.miko.purerender.style.StyledNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LayoutBox {
    private final StyledNode styledNode;
    private final List<LayoutBox> children = new ArrayList<>();
    private LayoutBox parent;
    private double x;
    private double y;
    private double width;
    private double height;
    private double scrollX;
    private double scrollY;
    private double scrollContentWidth;
    private double scrollContentHeight;
    private List<String> textLines = List.of();

    public LayoutBox(StyledNode styledNode) {
        this.styledNode = styledNode;
    }

    public StyledNode styledNode() {
        return styledNode;
    }

    public List<LayoutBox> children() {
        return Collections.unmodifiableList(children);
    }

    public LayoutBox parent() {
        return parent;
    }

    public void appendChild(LayoutBox child) {
        child.parent = this;
        children.add(child);
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double width() {
        return width;
    }

    public double height() {
        return height;
    }

    public double scrollX() {
        return scrollX;
    }

    public double scrollY() {
        return scrollY;
    }

    public double scrollContentWidth() {
        return scrollContentWidth;
    }

    public double scrollContentHeight() {
        return scrollContentHeight;
    }

    public void setScroll(double scrollX, double scrollY) {
        this.scrollX = Math.max(0, scrollX);
        this.scrollY = Math.max(0, scrollY);
    }

    public void setScrollContentSize(double scrollContentWidth, double scrollContentHeight) {
        this.scrollContentWidth = Math.max(width, scrollContentWidth);
        this.scrollContentHeight = Math.max(height, scrollContentHeight);
    }

    public List<String> textLines() {
        return textLines;
    }

    void setTextLines(List<String> textLines) {
        this.textLines = List.copyOf(textLines);
    }

    void setFrame(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.scrollContentWidth = width;
        this.scrollContentHeight = height;
    }

    void translate(double dx, double dy) {
        this.x += dx;
        this.y += dy;
        for (LayoutBox child : children) {
            child.translate(dx, dy);
        }
    }
}
