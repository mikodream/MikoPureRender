package com.miko.purerender.dom;

import com.miko.purerender.css.CssSelector;
import com.miko.purerender.css.ElementState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class DomQuery {
    private DomQuery() {
    }

    public static Optional<ElementNode> querySelector(DomNode root, String selector) {
        return querySelectorAll(root, selector).stream().findFirst();
    }

    public static List<ElementNode> querySelectorAll(DomNode root, String selector) {
        CssSelector cssSelector = CssSelector.parse(selector);
        List<ElementNode> matches = new ArrayList<>();
        collect(root, cssSelector, matches);
        return matches;
    }

    private static void collect(DomNode node, CssSelector selector, List<ElementNode> matches) {
        if (node instanceof ElementNode element && selector.matches(element, ElementState.empty())) {
            matches.add(element);
        }
        for (DomNode child : node.children()) {
            collect(child, selector, matches);
        }
    }
}
