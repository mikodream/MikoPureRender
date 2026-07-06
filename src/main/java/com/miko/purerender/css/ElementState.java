package com.miko.purerender.css;

import com.miko.purerender.dom.ElementNode;

import java.util.Set;

public record ElementState(Set<ElementNode> hovered, Set<ElementNode> active, Set<ElementNode> focused) {
    private static final ElementState EMPTY = new ElementState(Set.of(), Set.of(), Set.of());

    public static ElementState empty() {
        return EMPTY;
    }

    public static ElementState of(Set<ElementNode> hovered, Set<ElementNode> active, Set<ElementNode> focused) {
        return new ElementState(Set.copyOf(hovered), Set.copyOf(active), Set.copyOf(focused));
    }

    public boolean matches(ElementNode element, String pseudoClass) {
        return switch (pseudoClass) {
            case "hover" -> hovered.contains(element);
            case "active" -> active.contains(element);
            case "focus" -> focused.contains(element);
            default -> false;
        };
    }
}
