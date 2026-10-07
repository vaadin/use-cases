package com.example;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads the private sample media bundled under
 * {@code src/main/resources/media}. Those files are not reachable through a
 * static URL; the views serve them through {@code DownloadHandler}s, the way an
 * application would serve recordings that belong to one user.
 * <p>
 * The samples are small, so they are read into memory once and cached. A real
 * application would stream from a file store or object storage instead.
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
