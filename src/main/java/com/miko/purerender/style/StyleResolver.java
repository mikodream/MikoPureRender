package com.miko.purerender.style;

import com.miko.purerender.css.CssDeclaration;
import com.miko.purerender.css.ElementState;
import com.miko.purerender.css.CssParser;
import com.miko.purerender.css.CssRule;
import com.miko.purerender.css.Specificity;
import com.miko.purerender.css.StyleSheet;
import com.miko.purerender.dom.DomNode;
import com.miko.purerender.dom.ElementNode;
import com.miko.purerender.dom.TextNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class StyleResolver {
    private static final Set<String> INHERITED = Set.of(
            "color",
            "font-size",
            "font-family",
            "font-weight",
            "white-space",
            "cursor",
            "user-select"
    );
    private final CssParser cssParser = new CssParser();

    public StyledNode resolve(DomNode root, StyleSheet styleSheet) {
        return resolve(root, styleSheet, ElementState.empty());
    }

    public StyledNode resolve(DomNode root, StyleSheet styleSheet, ElementState state) {
        return resolveNode(root, styleSheet, null, state);
    }

    private StyledNode resolveNode(DomNode node, StyleSheet styleSheet, ComputedStyle parentStyle, ElementState state) {
        ComputedStyle style = new ComputedStyle();
        applyDefaults(node, style);
        inherit(parentStyle, style);
        applyPostInheritanceDefaults(node, style);

        if (node instanceof ElementNode element) {
            Map<String, AppliedValue> applied = new HashMap<>();
            for (CssRule rule : styleSheet.rules()) {
                if (rule.selector().matches(element, state)) {
                    applyDeclarations(applied, rule.declarations(), rule.selector().specificity(), rule.order());
                }
            }
            element.attribute("style").ifPresent(inline -> applyDeclarations(
                    applied,
                    cssParser.parseDeclarations(inline),
                    Specificity.INLINE,
                    Integer.MAX_VALUE
            ));
            applied.forEach((property, value) -> style.set(property, value.value()));
        }

        StyledNode styled = new StyledNode(node, style);
        for (DomNode child : node.children()) {
            styled.appendChild(resolveNode(child, styleSheet, style, state));
        }
        return styled;
    }

    private static void applyDefaults(DomNode node, ComputedStyle style) {
        style.set("display", "block");
        style.set("box-sizing", "content-box");
        style.set("width", "auto");
        style.set("height", "auto");
        style.set("margin", "0");
        style.set("padding", "0");
        style.set("border-width", "0");
        style.set("border-color", "#000000");
        style.set("border-radius", "0");
        style.set("box-shadow", "none");
        style.set("overflow", "visible");
        style.set("overflow-x", "visible");
        style.set("overflow-y", "visible");
        style.set("cursor", "auto");
        style.set("user-select", "auto");
        style.set("scrollbar-width", "auto");
        style.set("scrollbar-color", "#9aa5b1 #e5e7eb");
        style.set("background-color", "transparent");
        style.set("color", "#222222");
        style.set("font-size", "16px");
        style.set("font-family", "System");
        style.set("font-weight", "normal");
        style.set("white-space", "normal");
        style.set("gap", "0");
        style.set("flex-direction", "row");
        style.set("justify-content", "flex-start");
        style.set("align-items", "stretch");
        style.set("grid-template-columns", "1fr");

        if (node instanceof TextNode) {
            style.set("display", "text");
        }
        if (node instanceof ElementNode element) {
            String tag = element.tagName();
            if (Set.of("span", "strong", "em").contains(tag)) {
                style.set("display", "inline");
            }
            if (tag.equals("img")) {
                style.set("display", "block");
            }
            if (tag.equals("input")) {
                style.set("display", "block");
                style.set("width", "180px");
                style.set("height", "32px");
                style.set("padding", "6px 8px");
                style.set("border-width", "1px");
                style.set("border-color", "#bcccdc");
                style.set("border-radius", "4px");
                style.set("background-color", "#ffffff");
                style.set("cursor", "text");
                style.set("user-select", "text");
                style.set("white-space", "nowrap");
            }
            if (tag.equals("textarea")) {
                style.set("display", "block");
                style.set("width", "240px");
                style.set("height", "88px");
                style.set("padding", "6px 8px");
                style.set("border-width", "1px");
                style.set("border-color", "#bcccdc");
                style.set("border-radius", "4px");
                style.set("background-color", "#ffffff");
                style.set("cursor", "text");
                style.set("user-select", "text");
                style.set("overflow", "auto");
                style.set("overflow-x", "auto");
                style.set("overflow-y", "auto");
                style.set("white-space", "pre-wrap");
            }
            if (tag.equals("pre")) {
                style.set("white-space", "pre");
            }
            if (tag.matches("h[1-6]")) {
                style.set("font-weight", "bold");
                style.set("font-size", tag.equals("h1") ? "32px" : "24px");
                style.set("margin", "0 0 12px 0");
            }
            if (tag.equals("body")) {
                style.set("margin", "0");
            }
        }
    }

    private static void inherit(ComputedStyle parentStyle, ComputedStyle style) {
        if (parentStyle == null) {
            return;
        }
        for (String property : INHERITED) {
            parentStyle.get(property).ifPresent(value -> style.set(property, value));
        }
    }

    private static void applyPostInheritanceDefaults(DomNode node, ComputedStyle style) {
        if (node instanceof ElementNode element) {
            if ("input".equals(element.tagName())) {
                style.set("cursor", "text");
                style.set("user-select", "text");
                style.set("white-space", "nowrap");
            }
            if ("textarea".equals(element.tagName())) {
                style.set("cursor", "text");
                style.set("user-select", "text");
                style.set("white-space", "pre-wrap");
            }
            if ("pre".equals(element.tagName())) {
                style.set("white-space", "pre");
            }
        }
    }

    private static void applyDeclarations(
            Map<String, AppliedValue> applied,
            List<CssDeclaration> declarations,
            Specificity specificity,
            int order
    ) {
        for (CssDeclaration declaration : declarations) {
            for (CssDeclaration expanded : expand(declaration)) {
                AppliedValue current = applied.get(expanded.property());
                AppliedValue candidate = new AppliedValue(expanded.value(), specificity, order, expanded.important());
                if (current == null || candidate.compareTo(current) >= 0) {
                    applied.put(expanded.property(), candidate);
                }
            }
        }
    }

    private static List<CssDeclaration> expand(CssDeclaration declaration) {
        if ("border".equals(declaration.property())) {
            String width = null;
            String color = null;
            for (String token : declaration.value().split("\\s+")) {
                if (StyleValues.parseLength(token, Double.NaN) == StyleValues.parseLength(token, Double.NaN)
                        || token.endsWith("px")) {
                    width = token;
                } else if (!Set.of("none", "solid", "dashed", "dotted", "double").contains(token.toLowerCase())) {
                    color = token;
                }
            }
            java.util.ArrayList<CssDeclaration> expanded = new java.util.ArrayList<>();
            if (width != null) {
                expanded.add(new CssDeclaration("border-width", width, declaration.important()));
            }
            if (color != null) {
                expanded.add(new CssDeclaration("border-color", color, declaration.important()));
            }
            return expanded.isEmpty() ? List.of(declaration) : expanded;
        }
        if ("background".equals(declaration.property()) && !declaration.value().contains("url(")) {
            return List.of(declaration, new CssDeclaration("background-color", declaration.value(), declaration.important()));
        }
        if ("overflow".equals(declaration.property())) {
            return List.of(
                    declaration,
                    new CssDeclaration("overflow-x", declaration.value(), declaration.important()),
                    new CssDeclaration("overflow-y", declaration.value(), declaration.important())
            );
        }
        return List.of(declaration);
    }

    private record AppliedValue(String value, Specificity specificity, int order, boolean important)
            implements Comparable<AppliedValue> {
        @Override
        public int compareTo(AppliedValue other) {
            int byImportance = Boolean.compare(important, other.important);
            if (byImportance != 0) {
                return byImportance;
            }
            int bySpecificity = specificity.compareTo(other.specificity);
            if (bySpecificity != 0) {
                return bySpecificity;
            }
            return Integer.compare(order, other.order);
        }
    }
}
