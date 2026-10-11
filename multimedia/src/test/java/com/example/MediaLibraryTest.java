package com.example;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.server.streams.ClassDownloadHandler;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaLibraryTest {

    @Test
    void seekableAnswersRangeRequestsInline() {
        ClassDownloadHandler handler = MediaLibrary
                .seekable("quarterly-review.mp4");

        assertTrue(handler.isRangeRequestsEnabled());
        assertTrue(handler.isInline());
    }
}
