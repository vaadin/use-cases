package com.example;

import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.flow.component.upload.Upload;

/**
 * Plays the browser's part of {@link MissingAPI#addQueueSizeListener}: the
 * upload testers queue files on the server side only, so the test reports the
 * queue size the way the browser's {@code files-changed} event would.
 */
public class BrowserQueue extends ComponentTester<Upload> {

    public BrowserQueue(Upload upload) {
        super(upload);
    }

    public void reportQueued(int count) {
        ObjectNode data = JsonNodeFactory.instance.objectNode();
        data.put(MissingAPI.QUEUED_COUNT, count);
        fireDomEvent(MissingAPI.FILES_CHANGED, data);
    }
}
