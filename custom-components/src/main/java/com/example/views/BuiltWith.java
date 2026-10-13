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
 * Explains in the page how the use case's component is built: the technique, a
 * few points that make it work, and each of its files, collapsed, opening to
 * the real source packaged with the application.
 */
public class BuiltWith extends Div {

    static final String SOURCE_ROOT = "https://github.com/vaadin/use-cases/"
            + "blob/main/custom-components/src/main/";

    private final UnorderedList points = new UnorderedList();
    private final Div files = new Div();

    /**
     * @param title
     *            what is built, such as "AddressForm"
     * @param technique
     *            how, such as "Composite of existing components · Java only"
     */
    public BuiltWith(String title, String technique) {
        addClassName("built-with");
        Span tag = new Span(technique);
        tag.addClassName("technique");
        points.addClassName("points");
        files.addClassName("files");
        add(new H2("How it is built: " + title), tag, points, files);
    }

    /** A point that makes the component work, in one sentence. */
    public BuiltWith point(String text) {
        points.add(new ListItem(text));
        return this;
    }

    /**
     * A Java source file of the component, such as
     * {@code components/AddressForm}.
     */
    public BuiltWith java(String path) {
        return file("java/com/example/" + path + ".java",
                "component-sources/com/example/" + path + ".java");
    }

    /**
     * A test-side file that ships with the component, such as
     * {@code components/RatingStarsTester}.
     */
    public BuiltWith testJava(String path) {
        return file("../test/java/com/example/" + path + ".java",
                "component-sources/com/example/" + path + ".java");
    }

    /**
     * A resource of the component, such as
     * {@code META-INF/resources/frontend/signature-pad.ts}.
     */
    public BuiltWith resource(String path) {
        return file("resources/" + path, "component-sources/web/"
                + path.substring("META-INF/resources/".length()));
    }

    private BuiltWith file(String repositoryPath, String classpathPath) {
        String source = read(classpathPath);
        String name = classpathPath
                .substring(classpathPath.lastIndexOf('/') + 1);
        Pre code = new Pre(source);
        code.addClassName("source");
        Anchor github = new Anchor(SOURCE_ROOT + repositoryPath,
                "Open on GitHub", AnchorTarget.BLANK);
        Details details = new Details(name + " · " + lines(source),
                new Div(code, github));
        details.addClassName("file");
        files.add(details);
        return this;
    }

    private static String lines(String source) {
        long count = source.lines().count();
        return count + (count == 1 ? " line" : " lines");
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
