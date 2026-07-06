package com.miko.purerender.event;

@FunctionalInterface
public interface ElementEventListener {
    void handle(RenderEvent event);
}
