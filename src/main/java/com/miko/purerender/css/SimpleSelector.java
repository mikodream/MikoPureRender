package com.miko.purerender.css;

import com.miko.purerender.dom.ElementNode;

import java.util.ArrayList;
import java.util.List;

public record SimpleSelector(String tag, String id, List<String> classes, List<String> pseudoClasses) {
    static SimpleSelector parse(String source) {
        String tag = null;
        String id = null;
        List<String> classes = new ArrayList<>();
        List<String> pseudoClasses = new ArrayList<>();
        int index = 0;

        if (index < source.length() && source.charAt(index) == '*') {
            index++;
        } else if (index < source.length() && Character.isLetter(source.charAt(index))) {
            int start = index;
            while (index < source.length() && isNameChar(source.charAt(index))) {
                index++;
            }
            tag = source.substring(start, index).toLowerCase();
        }

        while (index < source.length()) {
            char marker = source.charAt(index++);
            int start = index;
            while (index < source.length() && isNameChar(source.charAt(index))) {
                index++;
            }
            String value = source.substring(start, index);
            if (marker == '#') {
                id = value;
            } else if (marker == '.') {
                classes.add(value);
            } else if (marker == ':') {
                pseudoClasses.add(value.toLowerCase());
            }
        }

        return new SimpleSelector(tag, id, List.copyOf(classes), List.copyOf(pseudoClasses));
    }

    public boolean matches(ElementNode element) {
        return matches(element, ElementState.empty());
    }

    public boolean matches(ElementNode element, ElementState state) {
        if (tag != null && !tag.equals(element.tagName())) {
            return false;
        }
        if (id != null && element.attribute("id").filter(id::equals).isEmpty()) {
            return false;
        }
        for (String className : classes) {
            if (!element.hasClass(className)) {
                return false;
            }
        }
        for (String pseudoClass : pseudoClasses) {
            if (!state.matches(element, pseudoClass)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isNameChar(char c) {
        return Character.isLetterOrDigit(c) || c == '-' || c == '_';
    }
}
