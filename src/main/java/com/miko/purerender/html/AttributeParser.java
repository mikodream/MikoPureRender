package com.miko.purerender.html;

import java.util.LinkedHashMap;
import java.util.Map;

final class AttributeParser {
    private final String source;
    private int index;

    AttributeParser(String source) {
        this.source = source;
    }

    Map<String, String> parse() {
        Map<String, String> attributes = new LinkedHashMap<>();
        while (index < source.length()) {
            skipWhitespace();
            String name = readName();
            if (name.isEmpty()) {
                index++;
                continue;
            }
            skipWhitespace();
            String value = "";
            if (peek('=')) {
                index++;
                skipWhitespace();
                value = readValue();
            }
            attributes.put(name.toLowerCase(), value);
        }
        return attributes;
    }

    private String readName() {
        int start = index;
        while (index < source.length()) {
            char c = source.charAt(index);
            if (Character.isWhitespace(c) || c == '=') {
                break;
            }
            index++;
        }
        return source.substring(start, index);
    }

    private String readValue() {
        if (index >= source.length()) {
            return "";
        }
        char quote = source.charAt(index);
        if (quote == '"' || quote == '\'') {
            index++;
            int start = index;
            while (index < source.length() && source.charAt(index) != quote) {
                index++;
            }
            String value = source.substring(start, index);
            if (index < source.length()) {
                index++;
            }
            return value;
        }
        int start = index;
        while (index < source.length() && !Character.isWhitespace(source.charAt(index))) {
            index++;
        }
        return source.substring(start, index);
    }

    private boolean peek(char expected) {
        return index < source.length() && source.charAt(index) == expected;
    }

    private void skipWhitespace() {
        while (index < source.length() && Character.isWhitespace(source.charAt(index))) {
            index++;
        }
    }
}
