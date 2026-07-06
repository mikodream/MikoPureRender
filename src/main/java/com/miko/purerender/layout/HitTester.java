package com.miko.purerender.layout;

import com.miko.purerender.dom.DomNode;
import com.miko.purerender.dom.ElementNode;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class HitTester {
    public Optional<LayoutBox> hitTest(LayoutBox root, double x, double y) {
        if (!contains(root, x, y)) {
            return Optional.empty();
        }
        List<LayoutBox> children = root.children();
        double childX = x + root.scrollX();
        double childY = y + root.scrollY();
        for (int i = children.size() - 1; i >= 0; i--) {
            Optional<LayoutBox> hit = hitTest(children.get(i), childX, childY);
            if (hit.isPresent()) {
                return hit;
            }
        }
        return Optional.of(root);
    }

    public Optional<ElementNode> hitElement(LayoutBox root, double x, double y) {
        return hitTest(root, x, y).flatMap(this::nearestElement);
    }

    public Set<ElementNode> hoverChain(LayoutBox root, double x, double y) {
        return hitTest(root, x, y)
                .map(this::elementAncestors)
                .orElseGet(Set::of);
    }

    private static boolean contains(LayoutBox box, double x, double y) {
        return x >= box.x()
                && y >= box.y()
                && x <= box.x() + box.width()
                && y <= box.y() + box.height();
    }

    private Optional<ElementNode> nearestElement(LayoutBox box) {
        DomNode node = box.styledNode().node();
        while (node != null) {
            if (node instanceof ElementNode element) {
                return Optional.of(element);
            }
            node = node.parent();
        }
        return Optional.empty();
    }

    private Set<ElementNode> elementAncestors(LayoutBox box) {
        Set<ElementNode> elements = new LinkedHashSet<>();
        DomNode node = box.styledNode().node();
        while (node != null) {
            if (node instanceof ElementNode element) {
                elements.add(element);
            }
            node = node.parent();
        }
        return elements;
    }
}
