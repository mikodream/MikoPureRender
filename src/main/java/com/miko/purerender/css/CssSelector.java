package com.miko.purerender.css;

import com.miko.purerender.dom.ElementNode;

import java.util.ArrayList;
import java.util.List;

public final class CssSelector {
    private final List<SelectorPart> chain;
    private final Specificity specificity;

    private CssSelector(List<SelectorPart> chain) {
        this.chain = List.copyOf(chain);
        this.specificity = calculateSpecificity(chain);
    }

    public static CssSelector parse(String source) {
        List<SelectorPart> chain = new ArrayList<>();
        String normalized = source.trim().replace(">", " > ");
        Combinator combinator = Combinator.DESCENDANT;
        for (String token : normalized.split("\\s+")) {
            if (token.isBlank()) {
                continue;
            }
            if (">".equals(token)) {
                combinator = Combinator.CHILD;
            } else {
                chain.add(new SelectorPart(SimpleSelector.parse(token), chain.isEmpty() ? Combinator.ROOT : combinator));
                combinator = Combinator.DESCENDANT;
            }
        }
        if (chain.isEmpty()) {
            throw new IllegalArgumentException("Empty selector");
        }
        return new CssSelector(chain);
    }

    public boolean matches(ElementNode element) {
        return matches(element, ElementState.empty());
    }

    public boolean matches(ElementNode element, ElementState state) {
        return matchesAt(element, chain.size() - 1, state);
    }

    public Specificity specificity() {
        return specificity;
    }

    private boolean matchesAt(ElementNode element, int index, ElementState state) {
        SelectorPart part = chain.get(index);
        if (!part.selector().matches(element, state)) {
            return false;
        }
        if (index == 0) {
            return true;
        }
        if (part.combinatorToPrevious() == Combinator.CHILD) {
            return element.parent() instanceof ElementNode parent && matchesAt(parent, index - 1, state);
        }
        var current = element.parent();
        while (current != null) {
            if (current instanceof ElementNode parent && matchesAt(parent, index - 1, state)) {
                return true;
            }
            current = current.parent();
        }
        return false;
    }

    private static Specificity calculateSpecificity(List<SelectorPart> chain) {
        int ids = 0;
        int classes = 0;
        int tags = 0;
        for (SelectorPart part : chain) {
            SimpleSelector selector = part.selector();
            ids += selector.id() == null ? 0 : 1;
            classes += selector.classes().size() + selector.pseudoClasses().size();
            tags += selector.tag() == null ? 0 : 1;
        }
        return new Specificity(ids, classes, tags);
    }

    private enum Combinator {
        ROOT,
        DESCENDANT,
        CHILD
    }

    private record SelectorPart(SimpleSelector selector, Combinator combinatorToPrevious) {
    }
}
