package com.example.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.StreamResourceRegistry.ElementStreamResource;
import com.vaadin.flow.server.streams.DownloadEvent;
import com.vaadin.flow.server.streams.DownloadHandler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Runs the download behind an {@link Anchor} the way a download request does:
 * without a current UI, which a handler only gets back through
 * {@code UI#access}.
 */
public final class Downloads {

    private Downloads() {
    }

    /** The bytes the browser would receive when the user clicks the link. */
    public static byte[] download(Anchor anchor) {
        UI ui = anchor.getUI().orElseThrow();
        DownloadHandler handler = (DownloadHandler) ui.getSession()
                .getResourceRegistry()
                .getResource(ElementStreamResource.class,
                        URI.create(anchor.getHref()))
                .orElseThrow().getElementRequestHandler();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(out, StandardCharsets.UTF_8));
        DownloadEvent event = mock(DownloadEvent.class);
        when(event.getUI()).thenReturn(ui);
        when(event.getOutputStream()).thenReturn(out);
        when(event.getWriter()).thenReturn(writer);

        Map<Class<?>, CurrentInstance> instances = CurrentInstance
                .getInstances();
        UI.setCurrent(null);
        try {
            handler.handleDownloadRequest(event);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            CurrentInstance.restoreInstances(instances);
        }
        writer.flush();
        return out.toByteArray();
    }
}
