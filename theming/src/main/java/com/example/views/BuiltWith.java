package com.example.views;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;

/**
 * Explains in the page how a use case is built: the technique in one line, a
 * few points that make it work, and the files involved, collapsed, each opening
 * to the source packaged with the application.
 */
public class BuiltWith extends Div {

    static final String SOURCE_ROOT = "https://github.com/vaadin/use-cases/"
            + "blob/main/theming/src/main/";

    private final UnorderedList points = new UnorderedList();
    private final Div files = new Div();

    public BuiltWith(String technique) {
        addClassName("built-with");
        Span tag = new Span(technique);
        tag.addClassName("technique");
        points.addClassName("points");
        add(new H2("How it is built"), tag, points, files);
    }

    /** A point that makes it work, in one sentence. */
    public BuiltWith point(String text) {
        points.add(new ListItem(text));
        return this;
    }

    /** A Java source, such as {@code uc1/BrandTokensView}. */
    public BuiltWith java(String path) {
        return file("java/com/example/" + path + ".java",
                "shown-sources/com/example/" + path + ".java");
    }

    /** A stylesheet, such as {@code brand.css}, as written. */
    public BuiltWith css(String name) {
        return file("resources/META-INF/resources/" + name,
                "shown-sources/web/" + name);
    }

    private BuiltWith file(String repositoryPath, String classpathPath) {
        String source = read(classpathPath);
        String name = classpathPath
                .substring(classpathPath.lastIndexOf('/') + 1);
        Pre code = new Pre(source);
        code.addClassName("source");
        Anchor github = new Anchor(SOURCE_ROOT + repositoryPath,
                "Open on GitHub", AnchorTarget.BLANK);
        long lines = source.lines().count();
        Details details = new Details(
                name + " · " + lines + (lines == 1 ? " line" : " lines"),
                new Div(code, github));
        details.addClassName("file");
        files.add(details);
        return this;
    }

    static String read(String classpathPath) {
        try (InputStream in = BuiltWith.class.getClassLoader()
                .getResourceAsStream(classpathPath)) {
            if (in == null) {
                throw new IllegalArgumentException(
                        "Not packaged: " + classpathPath);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
