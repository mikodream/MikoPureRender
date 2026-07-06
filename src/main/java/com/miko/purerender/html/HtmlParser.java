package com.miko.purerender.html;

import com.miko.purerender.dom.DocumentNode;
import com.miko.purerender.dom.DomNode;
import com.miko.purerender.dom.ElementNode;
import com.miko.purerender.dom.TextNode;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class HtmlParser {
    private static final Set<String> VOID_TAGS = Set.of(
            "area", "base", "br", "col", "embed", "hr", "img", "input",
            "link", "meta", "param", "source", "track", "wbr"
    );

    public DocumentNode parse(String html) {
        DocumentNode document = new DocumentNode();
        Deque<DomNode> stack = new ArrayDeque<>();
        stack.push(document);

        int index = 0;
        while (index < html.length()) {
            int tagStart = html.indexOf('<', index);
            if (tagStart < 0) {
                appendText(stack.peek(), html.substring(index));
                break;
            }

            appendText(stack.peek(), html.substring(index, tagStart));

            if (html.startsWith("<!--", tagStart)) {
                int commentEnd = html.indexOf("-->", tagStart + 4);
                index = commentEnd < 0 ? html.length() : commentEnd + 3;
                continue;
            }

            int tagEnd = html.indexOf('>', tagStart + 1);
            if (tagEnd < 0) {
                appendText(stack.peek(), html.substring(tagStart));
                break;
            }

            String rawTag = html.substring(tagStart + 1, tagEnd).trim();
            index = tagEnd + 1;
            if (rawTag.isEmpty() || rawTag.startsWith("!")) {
                continue;
            }

            if (rawTag.startsWith("/")) {
                closeElement(stack, rawTag.substring(1).trim().toLowerCase());
                continue;
            }

            boolean selfClosing = rawTag.endsWith("/");
            if (selfClosing) {
                rawTag = rawTag.substring(0, rawTag.length() - 1).trim();
            }

            ParsedTag parsedTag = parseTag(rawTag);
            ElementNode element = new ElementNode(parsedTag.name());
            parsedTag.attributes().forEach(element::setAttribute);
            stack.peek().appendChild(element);

            if (!selfClosing && !VOID_TAGS.contains(parsedTag.name())) {
                if (Set.of("script", "style").contains(parsedTag.name())) {
                    String closeTag = "</" + parsedTag.name() + ">";
                    int closeStart = html.toLowerCase(Locale.ROOT).indexOf(closeTag, index);
                    if (closeStart >= 0) {
                        String rawText = html.substring(index, closeStart);
                        if (!rawText.isBlank()) {
                            element.appendChild(new TextNode(rawText));
                        }
                        index = closeStart + closeTag.length();
                        continue;
                    }
                }
                stack.push(element);
            }
        }

        return document;
    }

    private static void appendText(DomNode parent, String rawText) {
        if (rawText == null) {
            return;
        }
        if (parent instanceof ElementNode element && "textarea".equals(element.tagName())) {
            String text = decodeEntities(rawText.replace("\r\n", "\n").replace('\r', '\n'));
            if (text.startsWith("\n")) {
                text = text.substring(1);
            }
            parent.appendChild(new TextNode(text));
            return;
        }
        if (rawText.isBlank()) {
            return;
        }
        parent.appendChild(new TextNode(decodeEntities(rawText.replaceAll("\\s+", " ").trim())));
    }

    private static String decodeEntities(String value) {
        return value.replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
    }

    private static void closeElement(Deque<DomNode> stack, String tagName) {
        while (stack.size() > 1) {
            DomNode node = stack.pop();
            if (node instanceof ElementNode element && element.tagName().equals(tagName)) {
                return;
            }
        }
    }

    private static ParsedTag parseTag(String rawTag) {
        int index = 0;
        while (index < rawTag.length() && !Character.isWhitespace(rawTag.charAt(index))) {
            index++;
        }
        String name = rawTag.substring(0, index).toLowerCase();
        AttributeParser parser = new AttributeParser(rawTag.substring(index));
        return new ParsedTag(name, parser.parse());
    }

    private record ParsedTag(String name, Map<String, String> attributes) {
    }
}
