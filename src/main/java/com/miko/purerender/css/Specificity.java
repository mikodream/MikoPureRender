package com.miko.purerender.css;

public record Specificity(int ids, int classes, int tags) implements Comparable<Specificity> {
    public static final Specificity INLINE = new Specificity(1_000, 0, 0);

    @Override
    public int compareTo(Specificity other) {
        int byId = Integer.compare(ids, other.ids);
        if (byId != 0) {
            return byId;
        }
        int byClass = Integer.compare(classes, other.classes);
        if (byClass != 0) {
            return byClass;
        }
        return Integer.compare(tags, other.tags);
    }
}
