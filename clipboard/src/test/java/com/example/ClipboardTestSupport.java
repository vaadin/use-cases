package com.example;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.clipboard.Clipboard;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.internal.nodefeature.ReturnChannelMap;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;
import com.vaadin.flow.server.StreamResourceRegistry;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.streams.UploadEvent;
import com.vaadin.flow.server.streams.UploadHandler;

/**
 * Plays the browser's part of the Clipboard API in browserless tests, which
 * have no clipboard simulator yet (API-GAPS.md #4,
 * https://github.com/vaadin/browserless-test/issues/64). Everything here relies
 * on Flow internals; replace it with the simulator once it exists.
 */
public final class ClipboardTestSupport {

    // Copied from Clipboard: the paste listener reads the event data under
    // these exact JavaScript expressions.
    private static final String PASTE_TEXT_EXPR = "event.clipboardData?.getData('text/plain') || null";
    private static final String PASTE_HTML_EXPR = "event.clipboardData?.getData('text/html') || null";

    private static final String FILE_PASTE_ATTRIBUTE_PREFIX = "_vaadin-paste-upload-";

    private static final AtomicLong PASTE_IDS = new AtomicLong();

    /** A file on the clipboard. */
    public record ClipboardFile(String name, String contentType, byte[] bytes) {
    }

    private ClipboardTestSupport() {
    }

    /**
     * Reports to the server that the browser wrote {@code copied} to the
     * clipboard, as if the write bound with {@code Clipboard.onClick(trigger)}
     * had resolved.
     */
    public static void reportCopied(Component trigger,
            @Nullable String copied) {
        ObjectNode outcome = JacksonUtils.createObjectNode();
        outcome.put("ok", true);
        outcome.put("value", copied);
        invokeLatestChannel(trigger.getElement().getNode(), outcome);
    }

    /**
     * Reports to the server that the browser rejected the clipboard write bound
     * with {@code Clipboard.onClick(trigger)}.
     */
    public static void reportCopyFailed(Component trigger, String name,
            String message) {
        ObjectNode error = JacksonUtils.createObjectNode();
        error.put("name", name);
        error.put("message", message);
        ObjectNode outcome = JacksonUtils.createObjectNode();
        outcome.put("ok", false);
        outcome.set("error", error);
        invokeLatestChannel(trigger.getElement().getNode(), outcome);
    }

    /**
     * Runs the task of the latest {@link UI#triggerAfter} timer, as the browser
     * does once the delay has elapsed.
     */
    public static void elapseLatestTimer(UI ui) {
        invokeLatestChannel(ui.getElement().getNode(), null);
    }

    /** Pastes text and HTML into {@code target}, as the browser does. */
    public static void paste(Component target, @Nullable String text,
            @Nullable String html) {
        ObjectNode data = JacksonUtils.createObjectNode();
        data.put(PASTE_TEXT_EXPR, text);
        data.put(PASTE_HTML_EXPR, html);
        Element element = target.getElement();
        element.getNode().getFeature(ElementListenerMap.class)
                .fireEvent(new DomEvent(element, "paste", data));
    }

    /**
     * Pastes files into {@code target}: uploads each one to the handler that
     * {@link Clipboard#onFilePaste} registered, with the headers the browser
     * sends, then runs the UI tasks the handler queued.
     */
    public static void pasteFiles(Component target, List<ClipboardFile> files) {
        Element element = target.getElement();
        UploadHandler handler = filePasteHandler(element);
        VaadinSession session = VaadinSession.getCurrent();
        String pasteId = Long.toString(PASTE_IDS.incrementAndGet());
        VaadinRequest request = withHeaders(VaadinRequest.getCurrent(),
                Map.of(Clipboard.PASTE_ID_HEADER, pasteId,
                        Clipboard.PASTE_FILE_COUNT_HEADER,
                        Integer.toString(files.size())));
        for (ClipboardFile file : files) {
            UploadEvent event = new UploadEvent(request,
                    VaadinResponse.getCurrent(), session, file.name(),
                    file.bytes().length, file.contentType(), element, null) {
                @Override
                public InputStream getInputStream() {
                    return new ByteArrayInputStream(file.bytes());
                }
            };
            try {
                handler.handleUploadRequest(event);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        session.getService().runPendingAccessTasks(session);
    }

    private static UploadHandler filePasteHandler(Element element) {
        String url = element.getAttributeNames()
                .filter(name -> name.startsWith(FILE_PASTE_ATTRIBUTE_PREFIX))
                .map(element::getAttribute).findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No Clipboard.onFilePaste on " + element));
        return (UploadHandler) VaadinSession.getCurrent().getResourceRegistry()
                .getResource(StreamResourceRegistry.ElementStreamResource.class,
                        java.net.URI.create(url))
                .orElseThrow().getElementRequestHandler();
    }

    private static VaadinRequest withHeaders(VaadinRequest request,
            Map<String, String> headers) {
        return (VaadinRequest) Proxy.newProxyInstance(
                VaadinRequest.class.getClassLoader(),
                new Class<?>[] { VaadinRequest.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("getHeader")
                            && headers.containsKey(args[0])) {
                        return headers.get(args[0]);
                    }
                    return method.invoke(request, args);
                });
    }

    /**
     * Invokes the most recently registered return channel of a node. A
     * clipboard write with callbacks and a {@code triggerAfter} timer each
     * register one; their ids count up from 0.
     */
    private static void invokeLatestChannel(StateNode node,
            @Nullable ObjectNode argument) {
        ReturnChannelMap channels = node.getFeature(ReturnChannelMap.class);
        ReturnChannelRegistration channel = IntStream
                .iterate(1000, id -> id - 1).limit(1001).mapToObj(channels::get)
                .filter(c -> c != null).findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No return channel on the node"));
        ArrayNode arguments = JacksonUtils.createArrayNode();
        if (argument != null) {
            arguments.add(argument);
        }
        channel.invoke(arguments);
    }
}
