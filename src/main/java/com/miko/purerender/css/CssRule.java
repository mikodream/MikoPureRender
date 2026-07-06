package com.miko.purerender.css;

import java.util.List;

public record CssRule(CssSelector selector, List<CssDeclaration> declarations, int order) {
}
