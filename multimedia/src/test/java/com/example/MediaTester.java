package com.example;

import java.util.List;

import tools.jackson.databind.node.ObjectNode;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Media;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.internal.JacksonUtils;

/**
 * Stands in for the browser side of a {@code <video>} / {@code <audio>} in
 * browserless tests: fires the media events the browser would send and exposes
 * the JavaScript calls ({@code play()}, {@code load()}, seeking, ...) the
 * server queued for the element. There is no media simulator in the test kit;
 * see {@code API-GAPS.md}.
 */
public class MediaTester extends ComponentTester<Media> {

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
     * Takes the JavaScript the server queued for this element since the last
     * call, in order. Each entry is the expression followed by its parameters,
     * since {@code callJsFunction} passes the function name as a parameter.
     *
     * @return the queued expressions with their parameters
     */
    public List<String> takeJavaScript() {
        UI ui = UI.getCurrent();
        // executeJs is queued before the response; run that step without
        // the rest of the round trip, which would discard the calls.
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        return ui.getInternals().dumpPendingJavaScriptInvocations().stream()
                .filter(invocation -> invocation.getOwner() == getComponent()
                        .getElement().getNode())
                .map(PendingJavaScriptInvocation::getInvocation)
                .map(invocation -> invocation.getExpression() + " "
                        + invocation.getParameters())
                .toList();
    }
}
