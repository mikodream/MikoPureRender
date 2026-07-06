package com.miko.purerender.css;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class StyleSheet {
    private final List<CssRule> rules = new ArrayList<>();

    public void addRule(CssRule rule) {
        rules.add(rule);
    }

    public List<CssRule> rules() {
        return Collections.unmodifiableList(rules);
    }
}
