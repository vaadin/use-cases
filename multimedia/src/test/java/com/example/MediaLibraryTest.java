package com.example;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

    @Test
    void extractFromAJarKeepsTheRelativePath(@TempDir Path temp)
            throws IOException {
        Path jar = temp.resolve("app.jar");
        try (JarOutputStream out = new JarOutputStream(
                Files.newOutputStream(jar))) {
            for (String rendition : new String[] { "240p", "360p" }) {
                out.putNextEntry(new JarEntry(
                        "media/hls/" + rendition + "/segment00.m4s"));
                out.write(rendition.getBytes(StandardCharsets.US_ASCII));
                out.closeEntry();
            }
        }
        Path target = Files.createDirectory(temp.resolve("media"));

        File low = MediaLibrary.extract(
                entry(jar, "media/hls/240p/segment00.m4s"), target,
                "hls/240p/segment00.m4s");
        File high = MediaLibrary.extract(
                entry(jar, "media/hls/360p/segment00.m4s"), target,
                "hls/360p/segment00.m4s");

        assertEquals(target.resolve("hls/240p/segment00.m4s").toFile(), low);
        assertEquals(target.resolve("hls/360p/segment00.m4s").toFile(), high);
        assertEquals("240p", Files.readString(low.toPath()));
        assertEquals("360p", Files.readString(high.toPath()));
    }

    private static URL entry(Path jar, String name) throws IOException {
        return URI.create("jar:" + jar.toUri() + "!/" + name).toURL();
    }
}
