package com.example.views;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The source of a test class, packaged with the application under
 * {@code test-sources/}, split into its test methods so a view can show them.
 */
public final class TestSource {

    /** One test method: its name as a sentence, and its code. */
    public record TestMethod(String name, String sentence, String code) {
    }

    private static final Pattern TEST_ANNOTATION = Pattern
            .compile("^\\s*@(Test|BrowserTest|ParameterizedTest)\\b");
    private static final Pattern METHOD = Pattern
            .compile("\\bvoid\\s+(\\w+)\\s*\\(");

    private TestSource() {
    }

    /**
     * The test methods of a test class, in source order.
     *
     * @param path
     *            the path below {@code com/example} without {@code .java}, such
     *            as {@code uc2/NewOrderViewTest}
     */
    public static List<TestMethod> methods(String path) {
        return methods(read(path).lines().toList());
    }

    /** The test methods in the lines of a source file. */
    static List<TestMethod> methods(List<String> lines) {
        List<TestMethod> methods = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            if (!TEST_ANNOTATION.matcher(lines.get(i)).find()) {
                continue;
            }
            int signature = i;
            Matcher name = METHOD.matcher(lines.get(signature));
            while (!name.find()) {
                if (++signature == lines.size()) {
                    // No method after the annotation: nothing to show.
                    return methods;
                }
                name = METHOD.matcher(lines.get(signature));
            }
            int start = firstAnnotation(lines, i);
            int end = endOfBlock(lines, signature);
            methods.add(new TestMethod(name.group(1), sentence(name.group(1)),
                    unindent(lines.subList(start, end + 1))));
            i = end;
        }
        return methods;
    }

    /**
     * The first of the annotation lines directly above a test annotation, so
     * that {@code @WithMockUser} above {@code @Test} is shown too.
     */
    private static int firstAnnotation(List<String> lines, int annotation) {
        int start = annotation;
        while (start > 0 && lines.get(start - 1).strip().startsWith("@")) {
            start--;
        }
        return start;
    }

    /** The whole source of a test class. */
    public static String read(String path) {
        String resource = "test-sources/com/example/" + path + ".java";
        try (InputStream in = TestSource.class.getClassLoader()
                .getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalArgumentException("No test source " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * A method name as a sentence: {@code placingAnOrderConfirmsTheOrder}
     * becomes "Placing an order confirms the order".
     */
    static String sentence(String methodName) {
        String words = methodName.replaceAll("(?<=[a-z0-9])(?=[A-Z])", " ")
                .toLowerCase(Locale.ROOT);
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    /**
     * The line of the brace that closes the block opened at or after
     * {@code from}. Braces in strings, characters, text blocks and comments do
     * not count. When the block never closes, the last line.
     */
    private static int endOfBlock(List<String> lines, int from) {
        int depth = 0;
        boolean opened = false;
        boolean blockComment = false;
        boolean textBlock = false;
        for (int i = from; i < lines.size(); i++) {
            String line = lines.get(i);
            for (int c = 0; c < line.length(); c++) {
                char ch = line.charAt(c);
                if (blockComment) {
                    if (line.startsWith("*/", c)) {
                        blockComment = false;
                        c++;
                    }
                } else if (textBlock) {
                    if (line.startsWith("\"\"\"", c)) {
                        textBlock = false;
                        c += 2;
                    } else if (ch == '\\') {
                        c++;
                    }
                } else if (line.startsWith("//", c)) {
                    break;
                } else if (line.startsWith("/*", c)) {
                    blockComment = true;
                    c++;
                } else if (line.startsWith("\"\"\"", c)) {
                    textBlock = true;
                    c += 2;
                } else if (ch == '"' || ch == '\'') {
                    c = endOfLiteral(line, c);
                } else if (ch == '{') {
                    depth++;
                    opened = true;
                } else if (ch == '}') {
                    depth--;
                    if (opened && depth == 0) {
                        return i;
                    }
                }
            }
        }
        return lines.size() - 1;
    }

    /** The index of the quote that closes the literal opened at start. */
    private static int endOfLiteral(String line, int start) {
        char quote = line.charAt(start);
        for (int c = start + 1; c < line.length(); c++) {
            if (line.charAt(c) == '\\') {
                c++;
            } else if (line.charAt(c) == quote) {
                return c;
            }
        }
        return line.length();
    }

    private static String unindent(List<String> lines) {
        int indent = lines.stream().filter(line -> !line.isBlank())
                .mapToInt(line -> line.length() - line.stripLeading().length())
                .min().orElse(0);
        return String.join("\n",
                lines.stream()
                        .map(line -> line.length() >= indent
                                ? line.substring(indent)
                                : line.strip())
                        .toList());
    }
}
