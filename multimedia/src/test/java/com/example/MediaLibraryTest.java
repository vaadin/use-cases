package com.example;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MediaLibraryTest {

    @Test
    void fileKeepsTheNameAndHoldsTheContent() throws IOException {
        File file = MediaLibrary.file("subtitles/en.vtt");

        assertEquals("en.vtt", file.getName());
        assertArrayEquals(MediaLibrary.bytes("subtitles/en.vtt"),
                Files.readAllBytes(file.toPath()));
    }

    @Test
    void fileOfAMissingPathFails() {
        assertThrows(UncheckedIOException.class,
                () -> MediaLibrary.file("missing.mp4"));
    }
}
