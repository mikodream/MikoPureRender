package com.miko.purerender.dom;

import java.util.Optional;

public final class DocumentNode extends DomNode {
    public DocumentNode() {
        super(NodeType.DOCUMENT);
    }

    public Optional<ElementNode> firstElement() {
        return children().stream()
                .filter(ElementNode.class::isInstance)
                .map(ElementNode.class::cast)
                .findFirst();
    }
}
