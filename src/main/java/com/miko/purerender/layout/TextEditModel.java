package com.miko.purerender.layout;

public final class TextEditModel {
    private final boolean multiline;
    private final int maxLength;
    private final boolean editable;
    private String text;
    private int caret;
    private Integer selectionAnchor;
    private Integer selectionFocus;

    public TextEditModel(String text, int caret, boolean multiline, int maxLength, boolean editable) {
        this.text = text == null ? "" : text;
        this.caret = clamp(caret, 0, this.text.length());
        this.multiline = multiline;
        this.maxLength = maxLength;
        this.editable = editable;
    }

    public String text() {
        return text;
    }

    public int caret() {
        return caret;
    }

    public int selectionStart() {
        if (!hasSelection()) {
            return caret;
        }
        return clamp(Math.min(selectionAnchor, selectionFocus), 0, text.length());
    }

    public int selectionEnd() {
        if (!hasSelection()) {
            return caret;
        }
        return clamp(Math.max(selectionAnchor, selectionFocus), 0, text.length());
    }

    public boolean hasSelection() {
        return selectionAnchor != null && selectionFocus != null && !selectionAnchor.equals(selectionFocus);
    }

    public void select(int anchor, int focus) {
        selectionAnchor = clamp(anchor, 0, text.length());
        selectionFocus = clamp(focus, 0, text.length());
        caret = selectionFocus;
    }

    public boolean insert(String inserted) {
        if (!editable) {
            return false;
        }
        String value = normalize(inserted);
        value = clampInsertedText(value);
        if (value.isEmpty() && !hasSelection()) {
            return false;
        }
        int start = selectionStart();
        int end = selectionEnd();
        text = text.substring(0, start) + value + text.substring(end);
        caret = start + value.length();
        clearSelection();
        return true;
    }

    public boolean backspace() {
        if (!editable) {
            return false;
        }
        if (deleteSelection()) {
            return true;
        }
        if (caret <= 0) {
            return false;
        }
        text = text.substring(0, caret - 1) + text.substring(caret);
        caret--;
        return true;
    }

    public boolean deleteForward() {
        if (!editable) {
            return false;
        }
        if (deleteSelection()) {
            return true;
        }
        if (caret >= text.length()) {
            return false;
        }
        text = text.substring(0, caret) + text.substring(caret + 1);
        return true;
    }

    public boolean deleteSelection() {
        if (!editable || !hasSelection()) {
            return false;
        }
        int start = selectionStart();
        int end = selectionEnd();
        text = text.substring(0, start) + text.substring(end);
        caret = start;
        clearSelection();
        return true;
    }

    private String normalize(String inserted) {
        if (inserted == null) {
            return "";
        }
        if (multiline) {
            return inserted.replace("\r\n", "\n").replace('\r', '\n');
        }
        return inserted.replace("\r", "").replace("\n", "");
    }

    private String clampInsertedText(String inserted) {
        if (maxLength < 0) {
            return inserted;
        }
        int selectedLength = hasSelection() ? selectionEnd() - selectionStart() : 0;
        int available = maxLength - (text.length() - selectedLength);
        if (available <= 0) {
            return "";
        }
        return inserted.length() <= available ? inserted : inserted.substring(0, available);
    }

    private void clearSelection() {
        selectionAnchor = null;
        selectionFocus = null;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }
}
