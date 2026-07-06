package com.miko.purerender.css;

import java.util.ArrayList;
import java.util.List;

public final class CssParser {
    public StyleSheet parse(String css) {
        StyleSheet styleSheet = new StyleSheet();
        String withoutComments = css.replaceAll("(?s)/\\*.*?\\*/", "");
        int order = 0;
        int index = 0;
        while (index < withoutComments.length()) {
            int blockStart = withoutComments.indexOf('{', index);
            if (blockStart < 0) {
                break;
            }
            int blockEnd = withoutComments.indexOf('}', blockStart + 1);
            if (blockEnd < 0) {
                break;
            }
            String selectorGroup = withoutComments.substring(index, blockStart).trim();
            List<CssDeclaration> declarations = parseDeclarations(withoutComments.substring(blockStart + 1, blockEnd));
            for (String selector : selectorGroup.split(",")) {
                if (!selector.isBlank()) {
                    styleSheet.addRule(new CssRule(CssSelector.parse(selector), declarations, order++));
                }
            }
            index = blockEnd + 1;
        }
        return styleSheet;
    }

    public List<CssDeclaration> parseDeclarations(String source) {
        List<CssDeclaration> declarations = new ArrayList<>();
        for (String raw : splitDeclarations(source)) {
            int colon = topLevelColon(raw);
            if (colon <= 0) {
                continue;
            }
            String value = raw.substring(colon + 1).trim();
            boolean important = value.toLowerCase().endsWith("!important");
            if (important) {
                value = value.substring(0, value.length() - "!important".length()).trim();
            }
            declarations.add(new CssDeclaration(raw.substring(0, colon), value, important));
        }
        return declarations;
    }

    private static List<String> splitDeclarations(String source) {
        List<String> declarations = new ArrayList<>();
        int start = 0;
        int depth = 0;
        char quote = 0;
        boolean escaped = false;
        for (int i = 0; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (quote != 0) {
                if (escaped) {
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (ch == quote) {
                    quote = 0;
                }
                continue;
            }
            if (ch == '"' || ch == '\'') {
                quote = ch;
            } else if (ch == '(') {
                depth++;
            } else if (ch == ')' && depth > 0) {
                depth--;
            } else if (ch == ';' && depth == 0) {
                declarations.add(source.substring(start, i));
                start = i + 1;
            }
        }
        if (start < source.length()) {
            declarations.add(source.substring(start));
        }
        return declarations;
    }

    private static int topLevelColon(String source) {
        int depth = 0;
        char quote = 0;
        boolean escaped = false;
        for (int i = 0; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (quote != 0) {
                if (escaped) {
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (ch == quote) {
                    quote = 0;
                }
                continue;
            }
            if (ch == '"' || ch == '\'') {
                quote = ch;
            } else if (ch == '(') {
                depth++;
            } else if (ch == ')' && depth > 0) {
                depth--;
            } else if (ch == ':' && depth == 0) {
                return i;
            }
        }
        return -1;
    }
}
