package com.miko.purerender.layout;

public final class TextNavigation {
    private TextNavigation() {
    }

    public static int previousWordBoundary(String text, int caret) {
        String value = text == null ? "" : text;
        int index = Math.max(0, Math.min(caret, value.length()));
        while (index > 0 && Character.isWhitespace(value.charAt(index - 1))) {
            index--;
        }
        while (index > 0 && !Character.isWhitespace(value.charAt(index - 1))) {
            index--;
        }
        return index;
    }

    public static int nextWordBoundary(String text, int caret) {
        String value = text == null ? "" : text;
        int index = Math.max(0, Math.min(caret, value.length()));
        while (index < value.length() && !Character.isWhitespace(value.charAt(index))) {
            index++;
        }
        while (index < value.length() && Character.isWhitespace(value.charAt(index))) {
            index++;
        }
        return index;
    }

    public static int lineStart(String text, int caret) {
        String value = text == null ? "" : text;
        int index = Math.max(0, Math.min(caret, value.length()));
        int newline = value.lastIndexOf('\n', Math.max(0, index - 1));
        return newline < 0 ? 0 : newline + 1;
    }

    public static int lineEnd(String text, int caret) {
        String value = text == null ? "" : text;
        int index = Math.max(0, Math.min(caret, value.length()));
        int newline = value.indexOf('\n', index);
        return newline < 0 ? value.length() : newline;
    }
}
