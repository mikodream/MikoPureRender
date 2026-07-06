package com.miko.purerender.event;

import com.miko.purerender.dom.ElementNode;

public final class RenderEvent {
    private final String type;
    private final ElementNode target;
    private final ElementNode currentTarget;
    private final double x;
    private final double y;
    private boolean propagationStopped;

    public RenderEvent(String type, ElementNode target, ElementNode currentTarget, double x, double y) {
        this.type = type;
        this.target = target;
        this.currentTarget = currentTarget;
        this.x = x;
        this.y = y;
    }

    public String type() {
        return type;
    }

    public ElementNode target() {
        return target;
    }

    public ElementNode currentTarget() {
        return currentTarget;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public void stopPropagation() {
        propagationStopped = true;
    }

    public boolean isPropagationStopped() {
        return propagationStopped;
    }
}
