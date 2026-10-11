package com.example;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.vaadin.flow.server.streams.ClassDownloadHandler;
import com.vaadin.flow.server.streams.DownloadHandler;

/**
 * Loads the private sample media bundled under
 * {@code src/main/resources/media}. Those files are not reachable through a
 * static URL; the views serve them through {@code DownloadHandler}s, the way an
 * application would serve recordings that belong to one user.
 * <p>
 * The samples are small, so they are read into memory once and cached. A real
 * application would stream from a file store or object storage instead.
 * {@link #seekable(String)} serves a file with byte-range support, which a
 * player needs to seek.
 */
public final class MediaLibrary {

    private static final Map<String, byte[]> CACHE = new ConcurrentHashMap<>();

    private MediaLibrary() {
    }

    /**
     * Returns the content of a bundled media file.
     *
     * @param path
     *            the path below {@code media/}, for example
     *            {@code "quarterly-review.mp4"} or {@code "hls/index.m3u8"}
     * @return the file content
     * @throws UncheckedIOException
     *             if there is no such file
     */
    public static byte[] bytes(String path) {
        return CACHE.computeIfAbsent(path, MediaLibrary::load);
    }

    /**
     * Opens a bundled media file as a stream.
     *
     * @param path
     *            the path below {@code media/}
     * @return a stream over the file content
     */
    public static InputStream open(String path) {
        return new ByteArrayInputStream(bytes(path));
    }

    /**
     * Serves a bundled media file inline and answers byte-range requests, so
     * that a player can seek in it.
     * <p>
     * Range requests are opt-in for class resources: inside the packaged jar
     * every seek reads the entry from its start, which is fine for the short
     * samples here. Large media is better served from disk with
     * {@code DownloadHandler.forFile}.
     *
     * @param path
     *            the path below {@code media/}
     * @return a download handler for the file
     */
    public static ClassDownloadHandler seekable(String path) {
        return DownloadHandler
                .forClassResource(MediaLibrary.class, "/media/" + path)
                .enableRangeRequests().inline();
    }

    /**
     * Checks whether a bundled media file exists.
     *
     * @param path
     *            the path below {@code media/}
     * @return {@code true} if the file exists
     */
    public static boolean exists(String path) {
        return CACHE.containsKey(path)
                || MediaLibrary.class.getResource("/media/" + path) != null;
    }

    private static byte[] load(String path) {
        try (InputStream in = MediaLibrary.class
                .getResourceAsStream("/media/" + path)) {
            if (in == null) {
                throw new UncheckedIOException(
                        new IOException("No media file media/" + path));
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
