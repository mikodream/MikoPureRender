package com.miko.purerender.style;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class ComputedStyle {
    private final Map<String, String> values = new LinkedHashMap<>();

    public void set(String property, String value) {
        values.put(property.toLowerCase(), value);
    }

    public String get(String property, String defaultValue) {
        return values.getOrDefault(property.toLowerCase(), defaultValue);
    }

    public Optional<String> get(String property) {
        return Optional.ofNullable(values.get(property.toLowerCase()));
    }

    public Map<String, String> values() {
        return Collections.unmodifiableMap(values);
    }
}
