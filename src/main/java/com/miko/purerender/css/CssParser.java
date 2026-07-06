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
        for (String raw : source.split(";")) {
            int colon = raw.indexOf(':');
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
}
