package com.miko.purerender.css;

public record CssDeclaration(String property, String value, boolean important) {
    public CssDeclaration {
        property = property.trim().toLowerCase();
        value = value.trim();
    }

    public CssDeclaration(String property, String value) {
        this(property, value, false);
    }
}
