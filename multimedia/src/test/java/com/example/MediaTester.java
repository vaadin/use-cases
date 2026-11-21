package com.example;

import java.util.ArrayList;
import java.util.List;

import tools.jackson.databind.node.ObjectNode;

import com.vaadin.browserless.BrowserlessTestConfig;
import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.ExecutedJavaScript;
import com.vaadin.browserless.JavaScriptHandler;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Media;
import com.vaadin.flow.internal.JacksonUtils;

/**
 * Stands in for the browser side of a {@code <video>} / {@code <audio>} in
 * browserless tests: fires the media events the browser would send and exposes
 * the JavaScript calls ({@code play()}, {@code load()}, seeking, ...) the
 * server sent for the element. There is no media simulator in the test kit; see
 * {@code API-GAPS.md #10}.
 * <p>
 * {@link #takeJavaScript()} needs the {@link Browser} recorder, registered with
 * {@code @BrowserlessTestConfig(javaScriptHandlers = MediaTester.Browser.class)},
 * so that it also sees the calls sent while the view was navigated to.
 */
public class MediaTester extends ComponentTester<Media> {

    /**
     * Records the JavaScript sent to the browser of each UI, so that
     * {@link #takeJavaScript()} can return it. Register it with
     * {@link BrowserlessTestConfig}.
     */
    public static final class Browser implements JavaScriptHandler {

        @Override
        public void handle(ExecutedJavaScript.Execution execution) {
            received(execution.getUI()).add(execution);
        }
    }

    public MediaTester(Media media) {
        super(media);
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
        UI ui = UI.getCurrent();
        // Starting a capture sends whatever is still pending to the browser
        ExecutedJavaScript.capture(ui).stop();
        List<ExecutedJavaScript.Execution> received = received(ui);
        List<ExecutedJavaScript.Execution> taken = received.stream()
                .filter(execution -> execution.getElement()
                        .equals(getComponent().getElement()))
                .toList();
        received.removeAll(taken);
        return taken.stream().map(execution -> execution.getExpression() + " "
                + execution.getParameters()).toList();
    }

    @SuppressWarnings("unchecked")
    private static List<ExecutedJavaScript.Execution> received(UI ui) {
        List<ExecutedJavaScript.Execution> received = (List<ExecutedJavaScript.Execution>) ComponentUtil
                .getData(ui, Browser.class.getName());
        if (received == null) {
            received = new ArrayList<>();
            ComponentUtil.setData(ui, Browser.class.getName(), received);
        }
        return received;
    }
}
