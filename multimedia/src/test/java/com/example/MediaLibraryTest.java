package com.example;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.server.streams.FileDownloadHandler;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaLibraryTest {

    @Test
    void fileKeepsTheNameAndHoldsTheContent() throws IOException {
        File file = MediaLibrary.file("subtitles/en.vtt");

        assertEquals("en.vtt", file.getName());
        try (InputStream in = MediaLibraryTest.class
                .getResourceAsStream("/media/subtitles/en.vtt")) {
            assertArrayEquals(in.readAllBytes(),
                    Files.readAllBytes(file.toPath()));
        }
    }

    @Test
    void serveAnswersRangeRequestsInline() {
        FileDownloadHandler handler = MediaLibrary
                .serve("quarterly-review.mp4");

        assertTrue(handler.isRangeRequestsEnabled());
        assertTrue(handler.isInline());
    }

    @Test
    void missingFileFails() {
        assertThrows(UncheckedIOException.class,
                () -> MediaLibrary.serve("missing.mp4"));
    }
}
