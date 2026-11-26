package com.example;

import java.util.List;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.ExecutedJavaScript;
import com.vaadin.flow.component.html.Media;
import com.vaadin.flow.internal.JacksonUtils;

/**
 * Stands in for the browser side of a {@code <video>} / {@code <audio>} in
 * browserless tests: fires the media events the browser would send and exposes
 * the JavaScript calls ({@code play()}, {@code load()}, seeking, ...) the
 * server sent for the element. There is no media simulator in the test kit; see
 * {@code API-GAPS.md #10}.
 * <p>
 * {@link #takeJavaScript()} reads the JavaScript from a capture passed to
 * {@link #MediaTester(Media, ExecutedJavaScript)}. A test starts it before it
 * navigates, typically in a {@code @BeforeEach} method, so that it also holds
 * the calls sent while the view was navigated to.
 */
public class MediaTester extends ComponentTester<Media> {

    private final @Nullable ExecutedJavaScript executed;
    private int read;

    /**
     * Creates a tester that only fires media events.
     *
     * @param media
     *            the media component
     */
    public MediaTester(Media media) {
        this(media, null);
    }

    /**
     * Creates a tester that also reads what the server sent for the element.
     *
     * @param media
     *            the media component
     * @param executed
     *            the capture of the JavaScript sent to the UI, or {@code null}
     *            when the test does not read it
     */
    public MediaTester(Media media, @Nullable ExecutedJavaScript executed) {
        super(media);
        this.executed = executed;
    }

    /**
     * Fires a media event without event data, such as {@code play} or
     * {@code ended}.
     *
     * @param event
     *            the event name
     */
    public void fire(String event) {
        fireDomEvent(event);
    }

    /**
     * Fires a media event carrying one numeric property of the element, such as
     * {@code timeupdate} with {@code element.currentTime}.
     *
     * @param event
     *            the event name
     * @param property
     *            the event data key, for example {@code element.currentTime}
     * @param value
     *            the value
     */
    public void fire(String event, String property, double value) {
        ObjectNode data = JacksonUtils.createObjectNode();
        data.put(property, value);
        fireDomEvent(event, data);
    }

    /**
     * Fires a media event carrying one string property of the element, such as
     * {@code loadedmetadata} with {@code element.currentSrc}.
     *
     * @param event
     *            the event name
     * @param property
     *            the event data key
     * @param value
     *            the value
     */
    public void fire(String event, String property, String value) {
        ObjectNode data = JacksonUtils.createObjectNode();
        data.put(property, value);
        fireDomEvent(event, data);
    }

    /**
     * Takes the JavaScript the server sent for this element since the last
     * call, in order. Each entry is the expression followed by its parameters,
     * since {@code callJsFunction} passes the function name as a parameter.
     *
     * @return the sent expressions with their parameters
     */
    public List<String> takeJavaScript() {
        if (executed == null) {
            throw new IllegalStateException(
                    "Create the tester with a capture to read its JavaScript");
        }
        List<ExecutedJavaScript.Execution> all = executed.getExecutions();
        List<ExecutedJavaScript.Execution> sent = all.subList(read, all.size())
                .stream().filter(execution -> execution.getElement()
                        .equals(getComponent().getElement()))
                .toList();
        read = all.size();
        return sent.stream().map(execution -> execution.getExpression() + " "
                + execution.getParameters()).toList();
    }
}
