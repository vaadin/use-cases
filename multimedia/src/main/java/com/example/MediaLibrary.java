package com.example;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
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
 * {@link #file(String)} gives the files on disk, which is what
 * {@code DownloadHandler.forFile} needs to answer byte-range requests.
 */
public final class MediaLibrary {

    private static final Map<String, byte[]> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, File> FILES = new ConcurrentHashMap<>();

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
     * Returns a bundled media file as a file on disk.
     * <p>
     * When the application runs from exploded classes the file is used where it
     * is; inside a packaged jar it is copied to a temporary directory once. The
     * file keeps its name, so its MIME type can be derived from it.
     *
     * @param path
     *            the path below {@code media/}
     * @return the file
     * @throws UncheckedIOException
     *             if there is no such file or it cannot be copied
     */
    public static File file(String path) {
        return FILES.computeIfAbsent(path, MediaLibrary::extract);
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

    private static File extract(String path) {
        URL url = MediaLibrary.class.getResource("/media/" + path);
        if (url == null) {
            throw new UncheckedIOException(
                    new IOException("No media file media/" + path));
        }
        try {
            if ("file".equals(url.getProtocol())) {
                return Path.of(url.toURI()).toFile();
            }
            Path directory = Files.createTempDirectory("media");
            directory.toFile().deleteOnExit();
            File copy = directory.resolve(new File(path).getName()).toFile();
            Files.write(copy.toPath(), bytes(path));
            copy.deleteOnExit();
            return copy;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
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
