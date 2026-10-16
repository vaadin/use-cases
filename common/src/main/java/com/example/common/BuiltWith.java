package com.example.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.dependency.StyleSheet;
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
 * <p>
 * A module that uses it packages the sources it shows under
 * {@code shown-sources} on the classpath: {@code src/main/java} (and any
 * {@code src/test/java} files it shows) as they are, and
 * {@code src/main/resources/META-INF/resources} as {@code shown-sources/web},
 * because the production build minifies the copies it serves.
 */
@StyleSheet("built-with.css")
public class BuiltWith extends Div {

    private static final String SHOWN_SOURCES = "shown-sources/";
    private static final String WEB_RESOURCES = "META-INF/resources/";

    private final UnorderedList points = new UnorderedList();
    private final Div files = new Div();
    private final Map<Anchor, String> githubLinks = new LinkedHashMap<>();

    public BuiltWith(String technique) {
        this(new H2("How it is built"), technique);
    }

    /**
     * @param title
     *            what is built, such as "AddressForm"
     * @param technique
     *            how, such as "Composite of existing components · Java only"
     */
    public BuiltWith(String title, String technique) {
        this(new H2("How it is built: " + title), technique);
    }

    private BuiltWith(H2 heading, String technique) {
        addClassName("built-with");
        Span tag = new Span(technique);
        tag.addClassName("technique");
        points.addClassName("points");
        files.addClassName("files");
        add(heading, tag, points, files);
    }

    /** A point that makes it work, in one sentence. */
    public BuiltWith point(String text) {
        points.add(new ListItem(text));
        return this;
    }

    /** A Java source, such as {@code uc1/TranslatedTextView}. */
    public BuiltWith java(String path) {
        return file("src/main/java/com/example/" + path + ".java",
                SHOWN_SOURCES + "com/example/" + path + ".java");
    }

    /**
     * A test-side Java source that is part of what is shown, such as
     * {@code components/RatingStarsTester}.
     */
    public BuiltWith testJava(String path) {
        return file("src/test/java/com/example/" + path + ".java",
                SHOWN_SOURCES + "com/example/" + path + ".java");
    }

    /**
     * A resource, such as {@code vaadin-i18n/translations.properties} or
     * {@code META-INF/resources/frontend/signature-pad.ts}.
     */
    public BuiltWith resource(String path) {
        String classpathPath = path.startsWith(WEB_RESOURCES)
                ? SHOWN_SOURCES + "web/"
                        + path.substring(WEB_RESOURCES.length())
                : path;
        return file("src/main/resources/" + path, classpathPath);
    }

    /** A stylesheet, such as {@code uc6.css}, as written. */
    public BuiltWith css(String name) {
        return resource(WEB_RESOURCES + name);
    }

    private BuiltWith file(String repositoryPath, String classpathPath) {
        String source = read(classpathPath);
        String name = classpathPath
                .substring(classpathPath.lastIndexOf('/') + 1);
        Pre code = new Pre(source);
        code.addClassName("source");
        Anchor github = new Anchor("", "Open on GitHub", AnchorTarget.BLANK);
        githubLinks.put(github, repositoryPath);
        Details details = new Details(name + " · " + lines(source),
                new Div(code, github));
        details.addClassName("file");
        files.add(details);
        return this;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        // The layout knows the app's module and the Git commit it runs.
        BaseMainLayout layout = findAncestor(BaseMainLayout.class);
        if (layout != null) {
            githubLinks.forEach(
                    (link, path) -> link.setHref(layout.fileUrl(path)));
        }
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
