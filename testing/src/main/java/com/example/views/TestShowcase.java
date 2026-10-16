package com.example.views;

import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;

/**
 * Shows the tests of a use case in the page itself: one line per test, written
 * out as a sentence, which opens to show the test's code. Above the list, what
 * kind of test it is and how long it takes; below it, a link to the whole file
 * and how to run it.
 */
public class TestShowcase extends Div {

    static final String SOURCE_ROOT = "https://github.com/vaadin/use-cases/"
            + "blob/main/testing/src/test/java/com/example/";

    static final String FAST = "./mvnw -pl testing -am test";
    static final String BROWSER = "./mvnw -pl testing -am verify -Pit";
    static final String LOAD = "./mvnw -pl testing -am verify -Pload";

    /** A plain JUnit test of logic without any UI. */
    public static TestShowcase unit(String path) {
        return new TestShowcase(path,
                "Unit test · plain JUnit, no Spring, no Vaadin · milliseconds",
                FAST);
    }

    /** A view test without a browser. */
    public static TestShowcase browserless(String path) {
        return new TestShowcase(path,
                "View test without a browser · the "
                        + "view and Spring run in memory · well under a second",
                FAST);
    }

    /** An end-to-end test with TestBench. */
    public static TestShowcase testBench(String path) {
        return new TestShowcase(path,
                "End-to-end test with TestBench · "
                        + "real Chrome against the running app · a few seconds",
                BROWSER);
    }

    /** An end-to-end test with Playwright. */
    public static TestShowcase playwright(String path) {
        return new TestShowcase(path,
                "End-to-end test with Playwright · "
                        + "real Chrome against the running app · a few seconds",
                BROWSER);
    }

    /** The end-to-end test a load test is recorded from. */
    public static TestShowcase loadScenario(String path) {
        return new TestShowcase(path,
                "Recorded once, then replayed by many "
                        + "virtual users with k6 · as long as the run lasts",
                LOAD);
    }

    /**
     * @param path
     *            the test class, such as {@code uc2/NewOrderViewTest}
     * @param kind
     *            what kind of test it is, such as "Browserless view test ·
     *            milliseconds, no browser"
     * @param run
     *            the command that runs it
     */
    public TestShowcase(String path, String kind, String run) {
        addClassName("tests");
        String className = path.substring(path.lastIndexOf('/') + 1);
        Span tag = new Span(kind);
        tag.addClassName("test-kind");
        add(new H2("How it is tested: " + className), tag);

        for (TestSource.TestMethod method : TestSource.methods(path)) {
            Pre code = new Pre(method.code());
            code.addClassName("test-code");
            Details details = new Details(method.sentence(), code);
            details.addClassName("test");
            add(details);
        }

        Anchor source = new Anchor(SOURCE_ROOT + path + ".java",
                "Whole test on GitHub", AnchorTarget.BLANK);
        Span command = new Span(run);
        command.addClassName("test-run");
        Div footer = new Div(source, command);
        footer.addClassName("test-footer");
        add(footer);
    }
}
