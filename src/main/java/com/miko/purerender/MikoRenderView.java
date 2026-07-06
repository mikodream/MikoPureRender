package com.miko.purerender;

import com.miko.purerender.css.CssParser;
import com.miko.purerender.css.CssSelector;
import com.miko.purerender.css.ElementState;
import com.miko.purerender.css.StyleSheet;
import com.miko.purerender.dom.DocumentNode;
import com.miko.purerender.dom.DomNode;
import com.miko.purerender.dom.DomQuery;
import com.miko.purerender.dom.ElementNode;
import com.miko.purerender.dom.TextNode;
import com.miko.purerender.event.ElementEventListener;
import com.miko.purerender.event.RenderEvent;
import com.miko.purerender.html.HtmlParser;
import com.miko.purerender.layout.HitTester;
import com.miko.purerender.layout.LayoutBox;
import com.miko.purerender.layout.LayoutEngine;
import com.miko.purerender.layout.TextControlLayout;
import com.miko.purerender.layout.TextLineMetrics;
import com.miko.purerender.paint.JavaFxRenderer;
import com.miko.purerender.style.StyleResolver;
import com.miko.purerender.style.StyleValues;
import com.miko.purerender.style.StyledNode;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.Cursor;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public final class MikoRenderView extends Region {
    private final Canvas canvas = new Canvas();
    private final HtmlParser htmlParser = new HtmlParser();
    private final CssParser cssParser = new CssParser();
    private final StyleResolver styleResolver = new StyleResolver();
    private final LayoutEngine layoutEngine = new LayoutEngine();
    private final JavaFxRenderer renderer;
    private final HitTester hitTester = new HitTester();
    private final Map<String, List<ElementEventListener>> eventListeners = new HashMap<>();
    private final List<BoundEventListener> boundEventListeners = new ArrayList<>();
    private final Map<ElementNode, Double> scrollTops = new HashMap<>();
    private final Map<ElementNode, Double> scrollLefts = new HashMap<>();
    private String html = "";
    private String css = "";
    private DocumentNode document;
    private StyleSheet styleSheet;
    private LayoutBox layoutRoot;
    private Set<ElementNode> hoveredElements = Set.of();
    private ElementNode activeElement;
    private ElementNode focusedElement;
    private double lastMouseX = -1;
    private double lastMouseY = -1;
    private final List<TextRun> textRuns = new ArrayList<>();
    private String selectableText = "";
    private Integer selectionAnchor;
    private Integer selectionFocus;
    private boolean selectingText;
    private ScrollbarDrag scrollbarDrag;
    private final Map<ElementNode, Integer> caretPositions = new HashMap<>();
    private Integer controlSelectionAnchor;
    private Integer controlSelectionFocus;

    public MikoRenderView() {
        renderer = new JavaFxRenderer(this::layoutAndPaint);
        getChildren().add(canvas);
        canvas.setFocusTraversable(true);
        canvas.setOnMouseMoved(event -> updateHover(event.getX(), event.getY()));
        canvas.setOnMouseExited(event -> {
            if (!hoveredElements.isEmpty() || activeElement != null) {
                hoveredElements = Set.of();
                activeElement = null;
                canvas.setCursor(Cursor.DEFAULT);
                layoutAndPaint();
            }
        });
        canvas.setOnMousePressed(event -> {
            canvas.requestFocus();
            Optional<ScrollbarGeometry> scrollbar = scrollbarAt(event.getX(), event.getY());
            if (scrollbar.isPresent()) {
                startScrollbarInteraction(scrollbar.get(), event.getX(), event.getY());
                return;
            }
            activeElement = elementAt(event.getX(), event.getY());
            focusedElement = activeElement;
            if (isTextControl(focusedElement)) {
                int offset = caretOffsetAt(focusedElement, event.getX(), event.getY());
                caretPositions.put(focusedElement, offset);
                clearSelection();
                controlSelectionAnchor = offset;
                controlSelectionFocus = offset;
                selectingText = true;
                layoutAndPaint();
                return;
            }
            Optional<Integer> textOffset = textOffsetAt(event.getX(), event.getY());
            if (textOffset.isPresent()) {
                selectionAnchor = textOffset.get();
                selectionFocus = textOffset.get();
                selectingText = true;
            } else {
                clearSelection();
            }
            layoutAndPaint();
        });
        canvas.setOnMouseDragged(event -> {
            if (scrollbarDrag != null) {
                dragScrollbar(event.getX(), event.getY());
            } else if (selectingText && isTextControl(focusedElement)) {
                int offset = caretOffsetAt(focusedElement, event.getX(), event.getY());
                caretPositions.put(focusedElement, offset);
                controlSelectionFocus = offset;
                layoutAndPaint();
            } else if (selectingText) {
                textOffsetAt(event.getX(), event.getY()).ifPresent(offset -> {
                    selectionFocus = offset;
                    layoutAndPaint();
                });
            }
        });
        canvas.setOnMouseReleased(event -> {
            activeElement = null;
            selectingText = false;
            scrollbarDrag = null;
            updateHover(event.getX(), event.getY());
        });
        canvas.setOnMouseClicked(event -> dispatchEvent("click", event.getX(), event.getY()));
        canvas.setOnScroll(event -> {
            double deltaX = -event.getDeltaX();
            double deltaY = -event.getDeltaY();
            if (event.isShiftDown() && Math.abs(deltaX) < 0.1) {
                deltaX = deltaY;
                deltaY = 0;
            }
            if (scrollAt(event.getX(), event.getY(), deltaX, deltaY)) {
                event.consume();
            }
        });
        canvas.setOnKeyPressed(event -> {
            if (event.isShortcutDown() && event.getCode() == KeyCode.A && isTextControl(focusedElement)) {
                selectFocusedControlText();
                event.consume();
            } else if (event.isShortcutDown() && event.getCode() == KeyCode.C) {
                copySelectionToClipboard();
                event.consume();
            } else if (event.isShortcutDown() && event.getCode() == KeyCode.X && isTextControl(focusedElement)) {
                cutControlSelectionToClipboard();
                event.consume();
            } else if (event.isShortcutDown() && event.getCode() == KeyCode.V && isTextControl(focusedElement)) {
                pasteClipboardIntoControl();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                clearSelection();
                clearControlSelection();
                layoutAndPaint();
            } else if (isTextControl(focusedElement) && handleTextControlKeyPressed(event.getCode(), event.isShiftDown())) {
                event.consume();
            }
        });
        canvas.setOnKeyTyped(event -> {
            if (!event.isShortcutDown() && isTextControl(focusedElement) && insertTypedText(event.getCharacter())) {
                event.consume();
            }
        });
        canvas.focusedProperty().addListener((ignored, wasFocused, isFocused) -> {
            if (!isFocused && focusedElement != null) {
                focusedElement = null;
                clearControlSelection();
                layoutAndPaint();
            }
        });
        widthProperty().addListener((ignored, oldValue, newValue) -> layoutAndPaint());
        heightProperty().addListener((ignored, oldValue, newValue) -> layoutAndPaint());
    }

    public void load(String html, String css) {
        this.html = html == null ? "" : html;
        this.css = css == null ? "" : css;
        this.document = htmlParser.parse(this.html);
        this.styleSheet = cssParser.parse(this.css);
        this.hoveredElements = Set.of();
        this.activeElement = null;
        this.focusedElement = null;
        this.scrollTops.clear();
        this.scrollLefts.clear();
        clearControlSelection();
        clearSelection();
        layoutAndPaint();
    }

    public void addEventListener(String type, ElementEventListener listener) {
        eventListeners.computeIfAbsent(type, ignored -> new ArrayList<>()).add(listener);
    }

    public void bindClick(String selector, Runnable action) {
        bindEvent("click", selector, ignored -> action.run());
    }

    public void bindClick(String selector, ElementEventListener listener) {
        bindEvent("click", selector, listener);
    }

    public void bindEvent(String type, String selector, ElementEventListener listener) {
        boundEventListeners.add(new BoundEventListener(type, CssSelector.parse(selector), listener));
    }

    public void clearEventBindings() {
        boundEventListeners.clear();
    }

    public Optional<ElementNode> querySelector(String selector) {
        if (document == null) {
            return Optional.empty();
        }
        return DomQuery.querySelector(document, selector);
    }

    public List<ElementNode> querySelectorAll(String selector) {
        if (document == null) {
            return List.of();
        }
        return DomQuery.querySelectorAll(document, selector);
    }

    public int setText(String selector, String text) {
        return mutate(selector, element -> {
            element.clearChildren();
            element.appendChild(new TextNode(text == null ? "" : text));
        });
    }

    public int setAttribute(String selector, String name, String value) {
        return mutate(selector, element -> element.setAttribute(name, value));
    }

    public int removeAttribute(String selector, String name) {
        return mutate(selector, element -> element.removeAttribute(name));
    }

    public int setStyle(String selector, String style) {
        return setAttribute(selector, "style", style);
    }

    public int addClass(String selector, String className) {
        return mutate(selector, element -> {
            LinkedHashSet<String> classes = classSet(element);
            classes.add(className);
            element.setAttribute("class", String.join(" ", classes));
        });
    }

    public int removeClass(String selector, String className) {
        return mutate(selector, element -> {
            LinkedHashSet<String> classes = classSet(element);
            classes.remove(className);
            if (classes.isEmpty()) {
                element.removeAttribute("class");
            } else {
                element.setAttribute("class", String.join(" ", classes));
            }
        });
    }

    public void refresh() {
        layoutAndPaint();
    }

    public void removeEventListener(String type, ElementEventListener listener) {
        List<ElementEventListener> listeners = eventListeners.get(type);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    @Override
    protected void layoutChildren() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        layoutAndPaint();
    }

    private void layoutAndPaint() {
        if (document == null || styleSheet == null || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        StyledNode styledNode = styleResolver.resolve(document, styleSheet, currentState());
        layoutRoot = layoutEngine.layout(styledNode, getWidth());
        applyScrollOffsets(layoutRoot);
        rebuildTextRuns();
        clampSelection();
        renderer.render(canvas, layoutRoot);
        paintSelection();
        paintControlSelection();
        paintCaret();
        applyCursor(lastMouseX, lastMouseY);
    }

    private ElementState currentState() {
        return ElementState.of(
                hoveredElements,
                activeElement == null ? Set.of() : Set.of(activeElement),
                focusedElement == null ? Set.of() : Set.of(focusedElement)
        );
    }

    private int mutate(String selector, Consumer<ElementNode> mutation) {
        List<ElementNode> elements = querySelectorAll(selector);
        for (ElementNode element : elements) {
            mutation.accept(element);
        }
        if (!elements.isEmpty()) {
            layoutAndPaint();
        }
        return elements.size();
    }

    private static LinkedHashSet<String> classSet(ElementNode element) {
        LinkedHashSet<String> classes = new LinkedHashSet<>();
        element.attribute("class").ifPresent(value -> {
            for (String className : value.split("\\s+")) {
                if (!className.isBlank()) {
                    classes.add(className);
                }
            }
        });
        return classes;
    }

    private void updateHover(double x, double y) {
        lastMouseX = x;
        lastMouseY = y;
        Set<ElementNode> nextHover = hoverChainAt(x, y);
        if (hoveredElements.equals(nextHover)) {
            applyCursor(x, y);
            return;
        }
        hoveredElements = nextHover;
        layoutAndPaint();
    }

    private ElementNode elementAt(double x, double y) {
        if (layoutRoot == null) {
            return null;
        }
        return hitTester.hitElement(layoutRoot, x, y).orElse(null);
    }

    private Set<ElementNode> hoverChainAt(double x, double y) {
        if (layoutRoot == null) {
            return Set.of();
        }
        return hitTester.hoverChain(layoutRoot, x, y);
    }

    private void applyCursor(double x, double y) {
        if (layoutRoot == null || x < 0 || y < 0) {
            canvas.setCursor(Cursor.DEFAULT);
            return;
        }
        String cursor = hitTester.hitTest(layoutRoot, x, y)
                .map(box -> box.styledNode().style().get("cursor", "auto"))
                .orElse("auto");
        canvas.setCursor(toJavaFxCursor(cursor));
    }

    private Cursor toJavaFxCursor(String cursor) {
        return switch (cursor) {
            case "pointer" -> Cursor.HAND;
            case "text" -> Cursor.TEXT;
            case "move" -> Cursor.MOVE;
            case "crosshair" -> Cursor.CROSSHAIR;
            case "wait" -> Cursor.WAIT;
            default -> Cursor.DEFAULT;
        };
    }

    private Optional<Integer> textOffsetAt(double x, double y) {
        for (TextRun run : textRuns) {
            double lineHeight = lineHeight(run.box());
            double lineTop = visualY(run.box()) + run.lineIndex() * lineHeight;
            if (y >= lineTop && y <= lineTop + lineHeight) {
                int offsetInLine = TextLineMetrics.offsetAt(run.prefixWidths(), x - visualX(run.box()));
                return Optional.of(run.start() + offsetInLine);
            }
        }
        return Optional.empty();
    }

    private void rebuildTextRuns() {
        textRuns.clear();
        StringBuilder text = new StringBuilder();
        if (layoutRoot != null) {
            collectTextRuns(layoutRoot, text);
        }
        selectableText = text.toString();
    }

    private void collectTextRuns(LayoutBox box, StringBuilder text) {
        if (box.styledNode().node() instanceof TextNode && !"none".equals(box.styledNode().style().get("user-select", "auto"))) {
            List<String> lines = box.textLines().isEmpty()
                    ? List.of(((TextNode) box.styledNode().node()).text())
                    : box.textLines();
            for (int i = 0; i < lines.size(); i++) {
                if (text.length() > 0) {
                    text.append('\n');
                }
                int start = text.length();
                text.append(lines.get(i));
                textRuns.add(new TextRun(
                        box,
                        i,
                        lines.get(i),
                        start,
                        text.length(),
                        TextLineMetrics.prefixWidths(lines.get(i), box.styledNode().style())
                ));
            }
        }
        for (LayoutBox child : box.children()) {
            collectTextRuns(child, text);
        }
    }

    private void paintSelection() {
        if (selectionAnchor == null || selectionFocus == null || selectionAnchor.equals(selectionFocus)) {
            return;
        }
        int start = Math.min(selectionAnchor, selectionFocus);
        int end = Math.max(selectionAnchor, selectionFocus);
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        graphics.save();
        graphics.setFill(Color.color(0.16, 0.43, 0.85, 0.35));
        for (TextRun run : textRuns) {
            int overlapStart = Math.max(start, run.start());
            int overlapEnd = Math.min(end, run.end());
            if (overlapStart >= overlapEnd) {
                continue;
            }
            double lineHeight = lineHeight(run.box());
            double x = visualX(run.box()) + TextLineMetrics.xForOffset(run.prefixWidths(), overlapStart - run.start());
            double y = visualY(run.box()) + run.lineIndex() * lineHeight;
            double width = Math.max(1,
                    TextLineMetrics.xForOffset(run.prefixWidths(), overlapEnd - run.start())
                            - TextLineMetrics.xForOffset(run.prefixWidths(), overlapStart - run.start()));
            graphics.fillRect(x, y, width, lineHeight);
        }
        graphics.restore();
    }

    private void copySelectionToClipboard() {
        if (isTextControl(focusedElement) && hasControlSelection()) {
            int start = controlSelectionStart();
            int end = controlSelectionEnd();
            ClipboardContent content = new ClipboardContent();
            content.putString(controlText(focusedElement).substring(start, end));
            Clipboard.getSystemClipboard().setContent(content);
            return;
        }
        if (selectionAnchor == null || selectionFocus == null || selectionAnchor.equals(selectionFocus)) {
            return;
        }
        int start = Math.min(selectionAnchor, selectionFocus);
        int end = Math.max(selectionAnchor, selectionFocus);
        ClipboardContent content = new ClipboardContent();
        content.putString(selectableText.substring(start, end));
        Clipboard.getSystemClipboard().setContent(content);
    }

    private void clearSelection() {
        selectionAnchor = null;
        selectionFocus = null;
        selectingText = false;
    }

    private void clearControlSelection() {
        controlSelectionAnchor = null;
        controlSelectionFocus = null;
    }

    private boolean hasControlSelection() {
        return controlSelectionAnchor != null
                && controlSelectionFocus != null
                && !controlSelectionAnchor.equals(controlSelectionFocus);
    }

    private int controlSelectionStart() {
        String text = controlText(focusedElement);
        return Math.max(0, Math.min(Math.min(controlSelectionAnchor, controlSelectionFocus), text.length()));
    }

    private int controlSelectionEnd() {
        String text = controlText(focusedElement);
        return Math.max(0, Math.min(Math.max(controlSelectionAnchor, controlSelectionFocus), text.length()));
    }

    private void selectFocusedControlText() {
        String text = controlText(focusedElement);
        controlSelectionAnchor = 0;
        controlSelectionFocus = text.length();
        caretPositions.put(focusedElement, text.length());
        layoutAndPaint();
    }

    private boolean handleTextControlKeyPressed(KeyCode code, boolean shiftDown) {
        ElementNode control = focusedElement;
        int caret = caretPositions.getOrDefault(control, controlText(control).length());
        String text = controlText(control);
        switch (code) {
            case BACK_SPACE -> {
                if (!isEditableTextControl(control)) {
                    return true;
                }
                if (deleteControlSelection()) {
                    layoutAndPaint();
                    return true;
                }
                if (caret > 0) {
                    setControlText(control, text.substring(0, caret - 1) + text.substring(caret));
                    caretPositions.put(control, caret - 1);
                    layoutAndPaint();
                }
                return true;
            }
            case DELETE -> {
                if (!isEditableTextControl(control)) {
                    return true;
                }
                if (deleteControlSelection()) {
                    layoutAndPaint();
                    return true;
                }
                if (caret < text.length()) {
                    setControlText(control, text.substring(0, caret) + text.substring(caret + 1));
                    caretPositions.put(control, caret);
                    layoutAndPaint();
                }
                return true;
            }
            case LEFT -> {
                moveControlCaret(control, Math.max(0, caret - 1), shiftDown);
                layoutAndPaint();
                return true;
            }
            case RIGHT -> {
                moveControlCaret(control, Math.min(text.length(), caret + 1), shiftDown);
                layoutAndPaint();
                return true;
            }
            case HOME -> {
                moveControlCaret(control, 0, shiftDown);
                layoutAndPaint();
                return true;
            }
            case END -> {
                moveControlCaret(control, text.length(), shiftDown);
                layoutAndPaint();
                return true;
            }
            case ENTER -> {
                if ("textarea".equals(control.tagName()) && isEditableTextControl(control)) {
                    insertTextAtCaret("\n");
                    return true;
                }
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    private void moveControlCaret(ElementNode control, int nextCaret, boolean extendingSelection) {
        String text = controlText(control);
        int clamped = Math.max(0, Math.min(nextCaret, text.length()));
        int previous = Math.max(0, Math.min(caretPositions.getOrDefault(control, text.length()), text.length()));
        if (extendingSelection) {
            if (controlSelectionAnchor == null) {
                controlSelectionAnchor = previous;
            }
            controlSelectionFocus = clamped;
        } else {
            clearControlSelection();
        }
        caretPositions.put(control, clamped);
    }

    private boolean insertTypedText(String text) {
        if (text == null || text.isEmpty() || text.charAt(0) < 0x20) {
            return false;
        }
        if (!isEditableTextControl(focusedElement)) {
            return true;
        }
        if ("input".equals(focusedElement.tagName())) {
            text = text.replace("\r", "").replace("\n", "");
        }
        if (text.isEmpty()) {
            return false;
        }
        insertTextAtCaret(text);
        return true;
    }

    private void insertTextAtCaret(String inserted) {
        ElementNode control = focusedElement;
        if (!isEditableTextControl(control)) {
            return;
        }
        inserted = clampInsertedText(control, inserted);
        if (inserted.isEmpty() && !hasControlSelection()) {
            return;
        }
        String text = controlText(control);
        int caret = Math.max(0, Math.min(caretPositions.getOrDefault(control, text.length()), text.length()));
        if (hasControlSelection()) {
            int start = controlSelectionStart();
            int end = controlSelectionEnd();
            setControlText(control, text.substring(0, start) + inserted + text.substring(end));
            caretPositions.put(control, start + inserted.length());
            clearControlSelection();
        } else {
            setControlText(control, text.substring(0, caret) + inserted + text.substring(caret));
            caretPositions.put(control, caret + inserted.length());
        }
        layoutAndPaint();
    }

    private boolean deleteControlSelection() {
        if (!isEditableTextControl(focusedElement)) {
            return false;
        }
        if (!hasControlSelection()) {
            return false;
        }
        ElementNode control = focusedElement;
        String text = controlText(control);
        int start = controlSelectionStart();
        int end = controlSelectionEnd();
        setControlText(control, text.substring(0, start) + text.substring(end));
        caretPositions.put(control, start);
        clearControlSelection();
        return true;
    }

    private void cutControlSelectionToClipboard() {
        if (!hasControlSelection()) {
            return;
        }
        int start = controlSelectionStart();
        int end = controlSelectionEnd();
        ClipboardContent content = new ClipboardContent();
        content.putString(controlText(focusedElement).substring(start, end));
        Clipboard.getSystemClipboard().setContent(content);
        if (isEditableTextControl(focusedElement)) {
            deleteControlSelection();
            layoutAndPaint();
        }
    }

    private void pasteClipboardIntoControl() {
        if (!isEditableTextControl(focusedElement)) {
            return;
        }
        String text = Clipboard.getSystemClipboard().getString();
        if (text == null || text.isEmpty()) {
            return;
        }
        if ("input".equals(focusedElement.tagName())) {
            text = text.replace("\r", "").replace("\n", "");
        }
        insertTextAtCaret(text);
    }

    private void paintControlSelection() {
        if (!isTextControl(focusedElement) || !hasControlSelection()) {
            return;
        }
        LayoutBox box = findBoxByElement(layoutRoot, focusedElement).orElse(null);
        if (box == null) {
            return;
        }
        TextControlLayout.Metrics metrics = textControlMetrics(box, focusedElement);
        int start = controlSelectionStart();
        int end = controlSelectionEnd();
        double contentX = visualX(box) + metrics.border() + metrics.padding().left();
        double contentY = visualY(box) + metrics.border() + metrics.padding().top();

        GraphicsContext graphics = canvas.getGraphicsContext2D();
        graphics.save();
        graphics.beginPath();
        graphics.rect(visualX(box), visualY(box), box.width(), box.height());
        graphics.clip();
        graphics.setFill(Color.color(0.16, 0.43, 0.85, 0.35));
        for (TextControlLayout.Line line : metrics.lines()) {
            int overlapStart = Math.max(start, line.start());
            int overlapEnd = Math.min(end, line.end());
            if (overlapStart >= overlapEnd) {
                continue;
            }
            double x = contentX - box.scrollX() + metrics.xForOffset(line, overlapStart);
            double y = contentY - box.scrollY() + line.index() * metrics.lineHeight();
            double width = Math.max(1, metrics.xForOffset(line, overlapEnd) - metrics.xForOffset(line, overlapStart));
            graphics.fillRect(x, y, width, metrics.lineHeight());
        }
        graphics.restore();
    }

    private void paintCaret() {
        if (!isTextControl(focusedElement)) {
            return;
        }
        LayoutBox box = findBoxByElement(layoutRoot, focusedElement).orElse(null);
        if (box == null) {
            return;
        }
        String text = controlText(focusedElement);
        int caret = Math.max(0, Math.min(caretPositions.getOrDefault(focusedElement, text.length()), text.length()));
        TextControlLayout.Metrics metrics = textControlMetrics(box, focusedElement);
        TextControlLayout.CaretPoint point = metrics.caretLocation(caret);
        double x = visualX(box) + metrics.border() + metrics.padding().left() + point.x() - box.scrollX();
        double y = visualY(box) + metrics.border() + metrics.padding().top() + point.y() - box.scrollY();

        GraphicsContext graphics = canvas.getGraphicsContext2D();
        graphics.save();
        graphics.beginPath();
        graphics.rect(visualX(box), visualY(box), box.width(), box.height());
        graphics.clip();
        graphics.setStroke(Color.web("#102a43"));
        graphics.setLineWidth(1);
        graphics.strokeLine(x, y + 2, x, y + metrics.fontSize() + 2);
        graphics.restore();
    }

    private int caretOffsetAt(ElementNode control, double x, double y) {
        LayoutBox box = findBoxByElement(layoutRoot, control).orElse(null);
        if (box == null) {
            return controlText(control).length();
        }
        TextControlLayout.Metrics metrics = textControlMetrics(box, control);
        double contentX = x - visualX(box) - metrics.border() - metrics.padding().left() + box.scrollX();
        double contentY = y - visualY(box) - metrics.border() - metrics.padding().top() + box.scrollY();
        return Math.min(controlText(control).length(), metrics.offsetAt(contentX, contentY));
    }

    private static boolean isTextControl(ElementNode element) {
        return element != null && ("input".equals(element.tagName()) || "textarea".equals(element.tagName()));
    }

    private static boolean isEditableTextControl(ElementNode element) {
        return isTextControl(element)
                && element.attribute("disabled").isEmpty()
                && element.attribute("readonly").isEmpty();
    }

    private static String controlText(ElementNode element) {
        if ("input".equals(element.tagName())) {
            return element.attribute("value").orElse("");
        }
        StringBuilder text = new StringBuilder();
        collectText(element, text);
        return text.toString();
    }

    private static String displayControlText(ElementNode element, String text) {
        if ("input".equals(element.tagName())
                && element.attribute("type").map(value -> value.equalsIgnoreCase("password")).orElse(false)) {
            return "*".repeat(text.length());
        }
        return text;
    }

    private static int maxLength(ElementNode element) {
        return element.attribute("maxlength")
                .map(value -> {
                    try {
                        return Integer.parseInt(value.trim());
                    } catch (NumberFormatException ignored) {
                        return -1;
                    }
                })
                .orElse(-1);
    }

    private String clampInsertedText(ElementNode control, String inserted) {
        if (inserted == null) {
            return "";
        }
        if ("input".equals(control.tagName())) {
            inserted = inserted.replace("\r", "").replace("\n", "");
        } else {
            inserted = inserted.replace("\r\n", "\n").replace('\r', '\n');
        }

        int maxLength = maxLength(control);
        if (maxLength < 0) {
            return inserted;
        }
        String current = controlText(control);
        int selectedLength = hasControlSelection() ? controlSelectionEnd() - controlSelectionStart() : 0;
        int available = maxLength - (current.length() - selectedLength);
        if (available <= 0) {
            return "";
        }
        return inserted.length() <= available ? inserted : inserted.substring(0, available);
    }

    private static void setControlText(ElementNode element, String text) {
        if ("input".equals(element.tagName())) {
            element.setAttribute("value", text);
        } else {
            element.clearChildren();
            element.appendChild(new TextNode(text));
        }
    }

    private static void collectText(DomNode node, StringBuilder text) {
        if (node instanceof TextNode textNode) {
            text.append(textNode.text());
        }
        for (DomNode child : node.children()) {
            collectText(child, text);
        }
    }

    private void clampSelection() {
        if (selectionAnchor != null) {
            selectionAnchor = Math.max(0, Math.min(selectionAnchor, selectableText.length()));
        }
        if (selectionFocus != null) {
            selectionFocus = Math.max(0, Math.min(selectionFocus, selectableText.length()));
        }
    }

    private static double lineHeight(LayoutBox box) {
        double fontSize = StyleValues.length(box.styledNode().style(), "font-size", 16);
        return StyleValues.length(box.styledNode().style(), "line-height", fontSize * 1.35);
    }

    private TextControlLayout.Metrics textControlMetrics(LayoutBox box, ElementNode element) {
        String text = controlText(element);
        return TextControlLayout.compute(element, box.styledNode().style(), box.width(), box.height(), displayControlText(element, text));
    }

    private void applyScrollOffsets(LayoutBox box) {
        ElementNode element = box.styledNode().node() instanceof ElementNode node ? node : null;
        if (element != null && isTextControl(element)) {
            applyTextControlScrollOffset(box, element);
        } else if (element != null && isScrollable(box)) {
            double maxLeft = maxScrollLeft(box);
            double maxTop = maxScrollTop(box);
            double scrollLeft = Math.max(0, Math.min(scrollLefts.getOrDefault(element, 0.0), maxLeft));
            double scrollTop = Math.max(0, Math.min(scrollTops.getOrDefault(element, 0.0), maxTop));
            box.setScroll(scrollLeft, scrollTop);
            scrollLefts.put(element, scrollLeft);
            scrollTops.put(element, scrollTop);
        } else {
            box.setScroll(0, 0);
        }
        for (LayoutBox child : box.children()) {
            applyScrollOffsets(child);
        }
    }

    private void applyTextControlScrollOffset(LayoutBox box, ElementNode element) {
        TextControlLayout.Metrics metrics = textControlMetrics(box, element);
        double scrollContentWidth = metrics.border() * 2 + metrics.padding().horizontal() + metrics.contentWidth();
        double scrollContentHeight = metrics.border() * 2 + metrics.padding().vertical() + metrics.contentHeight();
        box.setScrollContentSize(scrollContentWidth, scrollContentHeight);

        double maxLeft = maxScrollLeft(box);
        double maxTop = maxScrollTop(box);
        double scrollLeft = Math.max(0, Math.min(scrollLefts.getOrDefault(element, 0.0), maxLeft));
        double scrollTop = Math.max(0, Math.min(scrollTops.getOrDefault(element, 0.0), maxTop));

        if (element == focusedElement) {
            String text = controlText(element);
            int caret = Math.max(0, Math.min(caretPositions.getOrDefault(element, text.length()), text.length()));
            TextControlLayout.CaretPoint point = metrics.caretLocation(caret);
            scrollLeft = scrollLeftForCaret(metrics, point, scrollLeft);
            scrollTop = scrollTopForCaret(metrics, point, scrollTop);
        }

        scrollLeft = Math.max(0, Math.min(scrollLeft, maxLeft));
        scrollTop = Math.max(0, Math.min(scrollTop, maxTop));
        box.setScroll(scrollLeft, scrollTop);
        scrollLefts.put(element, scrollLeft);
        scrollTops.put(element, scrollTop);
    }

    private static double scrollLeftForCaret(TextControlLayout.Metrics metrics, TextControlLayout.CaretPoint point, double current) {
        double viewportWidth = metrics.viewportWidth();
        if (viewportWidth <= 0) {
            return current;
        }
        if (point.x() < current) {
            return point.x();
        }
        if (point.x() > current + viewportWidth - 1) {
            return point.x() - viewportWidth + 1;
        }
        return current;
    }

    private static double scrollTopForCaret(TextControlLayout.Metrics metrics, TextControlLayout.CaretPoint point, double current) {
        double viewportHeight = metrics.viewportHeight();
        if (viewportHeight <= 0) {
            return current;
        }
        if (point.y() < current) {
            return point.y();
        }
        if (point.y() + metrics.lineHeight() > current + viewportHeight) {
            return point.y() + metrics.lineHeight() - viewportHeight;
        }
        return current;
    }

    private boolean scrollAt(double x, double y, double deltaX, double deltaY) {
        if (layoutRoot == null || (deltaX == 0 && deltaY == 0)) {
            return false;
        }
        Optional<LayoutBox> hit = hitTester.hitTest(layoutRoot, x, y);
        LayoutBox box = hit.orElse(null);
        while (box != null) {
            if (isScrollable(box)) {
                ElementNode element = box.styledNode().node() instanceof ElementNode node ? node : null;
                if (element == null) {
                    return false;
                }
                double maxLeft = maxScrollLeft(box);
                double maxTop = maxScrollTop(box);
                double currentLeft = scrollLefts.getOrDefault(element, box.scrollX());
                double currentTop = scrollTops.getOrDefault(element, box.scrollY());
                double nextLeft = canScrollX(box) ? Math.max(0, Math.min(currentLeft + deltaX, maxLeft)) : currentLeft;
                double nextTop = canScrollY(box) ? Math.max(0, Math.min(currentTop + deltaY, maxTop)) : currentTop;
                if (Math.abs(nextLeft - currentLeft) > 0.1 || Math.abs(nextTop - currentTop) > 0.1) {
                    scrollLefts.put(element, nextLeft);
                    scrollTops.put(element, nextTop);
                    layoutAndPaint();
                    return true;
                }
                return false;
            }
            box = box.parent();
        }
        return false;
    }

    private Optional<ScrollbarGeometry> scrollbarAt(double x, double y) {
        if (layoutRoot == null) {
            return Optional.empty();
        }
        return findScrollbarAt(layoutRoot, x, y);
    }

    private Optional<ScrollbarGeometry> findScrollbarAt(LayoutBox box, double x, double y) {
        if (!containsVisual(box, x, y)) {
            return Optional.empty();
        }
        for (int i = box.children().size() - 1; i >= 0; i--) {
            Optional<ScrollbarGeometry> childHit = findScrollbarAt(box.children().get(i), x, y);
            if (childHit.isPresent()) {
                return childHit;
            }
        }
        Optional<ScrollbarGeometry> vertical = scrollbarGeometry(box, ScrollbarAxis.VERTICAL);
        if (vertical.filter(value -> value.contains(x, y)).isPresent()) {
            return vertical;
        }
        Optional<ScrollbarGeometry> horizontal = scrollbarGeometry(box, ScrollbarAxis.HORIZONTAL);
        return horizontal.filter(value -> value.contains(x, y));
    }

    private Optional<ScrollbarGeometry> scrollbarGeometry(LayoutBox box, ScrollbarAxis axis) {
        String overflow = axis == ScrollbarAxis.VERTICAL ? overflowY(box) : overflowX(box);
        double max = axis == ScrollbarAxis.VERTICAL ? maxScrollTop(box) : maxScrollLeft(box);
        if (!showsScrollbar(overflow, max)) {
            return Optional.empty();
        }
        double width = scrollbarWidth(box);
        if (width <= 0) {
            return Optional.empty();
        }
        boolean hasVertical = showsScrollbar(overflowY(box), maxScrollTop(box));
        boolean hasHorizontal = showsScrollbar(overflowX(box), maxScrollLeft(box));
        if (axis == ScrollbarAxis.VERTICAL) {
            double trackX = visualX(box) + box.width() - width;
            double trackY = visualY(box);
            double trackLength = Math.max(0, box.height() - (hasHorizontal ? width : 0));
            double contentLength = box.height() + max;
            double thumbLength = max <= 0 ? trackLength : Math.max(24, trackLength * box.height() / contentLength);
            thumbLength = Math.min(trackLength, thumbLength);
            double thumbStart = max <= 0 ? trackY : trackY + (trackLength - thumbLength) * (box.scrollY() / max);
            return Optional.of(new ScrollbarGeometry(box, axis, trackX, trackY, width, trackLength, thumbStart, thumbLength, max));
        }
        double trackX = visualX(box);
        double trackY = visualY(box) + box.height() - width;
        double trackLength = Math.max(0, box.width() - (hasVertical ? width : 0));
        double contentLength = box.width() + max;
        double thumbLength = max <= 0 ? trackLength : Math.max(24, trackLength * box.width() / contentLength);
        thumbLength = Math.min(trackLength, thumbLength);
        double thumbStart = max <= 0 ? trackX : trackX + (trackLength - thumbLength) * (box.scrollX() / max);
        return Optional.of(new ScrollbarGeometry(box, axis, trackX, trackY, trackLength, width, thumbStart, thumbLength, max));
    }

    private void startScrollbarInteraction(ScrollbarGeometry geometry, double x, double y) {
        ElementNode element = geometry.box().styledNode().node() instanceof ElementNode node ? node : null;
        if (element == null) {
            return;
        }
        double pointer = geometry.axis() == ScrollbarAxis.VERTICAL ? y : x;
        if (geometry.thumbContains(x, y)) {
            double startScroll = geometry.axis() == ScrollbarAxis.VERTICAL
                    ? scrollTops.getOrDefault(element, geometry.box().scrollY())
                    : scrollLefts.getOrDefault(element, geometry.box().scrollX());
            scrollbarDrag = new ScrollbarDrag(element, geometry.axis(), pointer, startScroll);
        } else {
            double direction = pointer < geometry.thumbStart() ? -1 : 1;
            double page = geometry.axis() == ScrollbarAxis.VERTICAL ? geometry.box().height() : geometry.box().width();
            if (geometry.axis() == ScrollbarAxis.VERTICAL) {
                scrollTops.put(element, Math.max(0, Math.min(
                        scrollTops.getOrDefault(element, geometry.box().scrollY()) + direction * page,
                        geometry.maxScroll()
                )));
            } else {
                scrollLefts.put(element, Math.max(0, Math.min(
                        scrollLefts.getOrDefault(element, geometry.box().scrollX()) + direction * page,
                        geometry.maxScroll()
                )));
            }
            layoutAndPaint();
        }
    }

    private void dragScrollbar(double x, double y) {
        LayoutBox box = findBoxByElement(layoutRoot, scrollbarDrag.element()).orElse(null);
        if (box == null) {
            return;
        }
        ScrollbarGeometry geometry = scrollbarGeometry(box, scrollbarDrag.axis()).orElse(null);
        if (geometry == null || geometry.trackLength() <= geometry.thumbLength()) {
            return;
        }
        double pointer = geometry.axis() == ScrollbarAxis.VERTICAL ? y : x;
        double ratio = geometry.maxScroll() / (geometry.trackLength() - geometry.thumbLength());
        double next = scrollbarDrag.startScroll() + (pointer - scrollbarDrag.startPointer()) * ratio;
        if (geometry.axis() == ScrollbarAxis.VERTICAL) {
            scrollTops.put(scrollbarDrag.element(), Math.max(0, Math.min(next, geometry.maxScroll())));
        } else {
            scrollLefts.put(scrollbarDrag.element(), Math.max(0, Math.min(next, geometry.maxScroll())));
        }
        layoutAndPaint();
    }

    private Optional<LayoutBox> findBoxByElement(LayoutBox box, ElementNode element) {
        if (box.styledNode().node() == element) {
            return Optional.of(box);
        }
        for (LayoutBox child : box.children()) {
            Optional<LayoutBox> found = findBoxByElement(child, element);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    private static boolean isScrollable(LayoutBox box) {
        return (canScrollX(box) && maxScrollLeft(box) > 0) || (canScrollY(box) && maxScrollTop(box) > 0);
    }

    private static boolean canScrollX(LayoutBox box) {
        String overflow = overflowX(box);
        return "auto".equals(overflow) || "scroll".equals(overflow);
    }

    private static boolean canScrollY(LayoutBox box) {
        String overflow = overflowY(box);
        return "auto".equals(overflow) || "scroll".equals(overflow);
    }

    private static double maxScrollLeft(LayoutBox box) {
        if (box.scrollContentWidth() > box.width()) {
            return Math.max(0, box.scrollContentWidth() - box.width());
        }
        double contentRight = box.x() + box.width();
        for (LayoutBox child : box.children()) {
            contentRight = Math.max(contentRight, child.x() + child.width());
        }
        return Math.max(0, contentRight - (box.x() + box.width()));
    }

    private static double maxScrollTop(LayoutBox box) {
        if (box.scrollContentHeight() > box.height()) {
            return Math.max(0, box.scrollContentHeight() - box.height());
        }
        double contentBottom = box.y() + box.height();
        for (LayoutBox child : box.children()) {
            contentBottom = Math.max(contentBottom, child.y() + child.height());
        }
        return Math.max(0, contentBottom - (box.y() + box.height()));
    }

    private static boolean showsScrollbar(String overflow, double maxScroll) {
        return "scroll".equals(overflow) || ("auto".equals(overflow) && maxScroll > 0);
    }

    private static String overflowX(LayoutBox box) {
        return box.styledNode().style().get("overflow-x", box.styledNode().style().get("overflow", "visible"));
    }

    private static String overflowY(LayoutBox box) {
        return box.styledNode().style().get("overflow-y", box.styledNode().style().get("overflow", "visible"));
    }

    private static double visualX(LayoutBox box) {
        double x = box.x();
        LayoutBox parent = box.parent();
        while (parent != null) {
            x -= parent.scrollX();
            parent = parent.parent();
        }
        return x;
    }

    private static double visualY(LayoutBox box) {
        double y = box.y();
        LayoutBox parent = box.parent();
        while (parent != null) {
            y -= parent.scrollY();
            parent = parent.parent();
        }
        return y;
    }

    private static boolean containsVisual(LayoutBox box, double x, double y) {
        double visualX = visualX(box);
        double visualY = visualY(box);
        return x >= visualX
                && y >= visualY
                && x <= visualX + box.width()
                && y <= visualY + box.height();
    }

    private static double scrollbarWidth(LayoutBox box) {
        String raw = box.styledNode().style().get("scrollbar-width", "auto");
        return switch (raw) {
            case "none" -> 0;
            case "thin" -> 6;
            case "auto" -> 10;
            default -> StyleValues.parseLength(raw, 10);
        };
    }

    private void dispatchEvent(String type, double x, double y) {
        if (layoutRoot == null) {
            return;
        }
        List<ElementEventListener> globalListeners = eventListeners.getOrDefault(type, List.of());
        boolean hasBoundListeners = boundEventListeners.stream().anyMatch(listener -> listener.type().equals(type));
        if (globalListeners.isEmpty() && !hasBoundListeners) {
            return;
        }
        List<ElementNode> chain = new ArrayList<>(hitTester.hoverChain(layoutRoot, x, y));
        if (chain.isEmpty()) {
            return;
        }
        ElementNode target = chain.getFirst();
        for (ElementNode current : chain) {
            RenderEvent renderEvent = new RenderEvent(type, target, current, x, y);
            for (ElementEventListener listener : List.copyOf(globalListeners)) {
                listener.handle(renderEvent);
                if (renderEvent.isPropagationStopped()) {
                    return;
                }
            }
            for (BoundEventListener listener : List.copyOf(boundEventListeners)) {
                if (listener.matches(type, current, currentState())) {
                    listener.listener().handle(renderEvent);
                    if (renderEvent.isPropagationStopped()) {
                        return;
                    }
                }
            }
        }
    }

    private record BoundEventListener(String type, CssSelector selector, ElementEventListener listener) {
        boolean matches(String eventType, ElementNode element, ElementState state) {
            return type.equals(eventType) && selector.matches(element, state);
        }
    }

    private record TextRun(LayoutBox box, int lineIndex, String text, int start, int end, double[] prefixWidths) {
    }

    private enum ScrollbarAxis {
        HORIZONTAL,
        VERTICAL
    }

    private record ScrollbarDrag(ElementNode element, ScrollbarAxis axis, double startPointer, double startScroll) {
    }

    private record ScrollbarGeometry(
            LayoutBox box,
            ScrollbarAxis axis,
            double trackX,
            double trackY,
            double trackWidth,
            double trackHeight,
            double thumbStart,
            double thumbLength,
            double maxScroll
    ) {
        boolean contains(double x, double y) {
            return x >= trackX && x <= trackX + trackWidth && y >= trackY && y <= trackY + trackHeight;
        }

        boolean thumbContains(double x, double y) {
            if (axis == ScrollbarAxis.VERTICAL) {
                return y >= thumbStart && y <= thumbStart + thumbLength;
            }
            return x >= thumbStart && x <= thumbStart + thumbLength;
        }

        double height() {
            return trackHeight;
        }

        double trackLength() {
            return axis == ScrollbarAxis.VERTICAL ? trackHeight : trackWidth;
        }
    }
}
