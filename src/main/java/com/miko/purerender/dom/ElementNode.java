package com.miko.purerender.dom;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class ElementNode extends DomNode {
    private final String tagName;
    private final Map<String, String> attributes = new LinkedHashMap<>();

    public ElementNode(String tagName) {
        super(NodeType.ELEMENT);
        this.tagName = tagName.toLowerCase();
    }

    public String tagName() {
        return tagName;
    }

    public void setAttribute(String name, String value) {
        attributes.put(name.toLowerCase(), value);
    }

    public void removeAttribute(String name) {
        attributes.remove(name.toLowerCase());
    }

    public Optional<String> attribute(String name) {
        return Optional.ofNullable(attributes.get(name.toLowerCase()));
    }

    public Map<String, String> attributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public boolean hasClass(String className) {
        return attribute("class")
                .map(value -> (" " + value + " ").contains(" " + className + " "))
                .orElse(false);
    }
}
