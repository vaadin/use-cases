package com.example;

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

import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.FileDownloadHandler;

/**
 * Gives access to the private sample media bundled under
 * {@code src/main/resources/media}. Those files are not reachable through a
 * static URL; the views serve them through {@code DownloadHandler}s, the way an
 * application would serve recordings that belong to one user.
 * <p>
 * The views serve the media as files on disk with
 * {@code DownloadHandler.forFile}, which streams the file and answers
 * byte-range requests by seeking in it. That is what a player needs to seek,
 * and what Safari needs to play video at all. A real application would keep its
 * media in a file store; this demo bundles it, so when it runs from the
 * packaged jar the files are copied out to a temporary directory once.
 */
public final class MediaLibrary {

    private static final Map<String, File> FILES = new ConcurrentHashMap<>();

    private MediaLibrary() {
    }

    /**
     * Serves a bundled media file inline, with byte-range support.
     *
     * @param path
     *            the path below {@code media/}, for example
     *            {@code "quarterly-review.mp4"}
     * @return a download handler for the file
     * @throws UncheckedIOException
     *             if there is no such file
     */
    public static FileDownloadHandler serve(String path) {
        return DownloadHandler.forFile(file(path)).inline();
    }

    /**
     * Returns a bundled media file as a file on disk. The file keeps its name,
     * so its MIME type can be derived from it.
     *
     * @param path
     *            the path below {@code media/}, for example
     *            {@code "quarterly-review.mp4"} or {@code "hls/index.m3u8"}
     * @return the file
     * @throws UncheckedIOException
     *             if there is no such file or it cannot be copied
     */
    public static File file(String path) {
        return FILES.computeIfAbsent(path, MediaLibrary::resolve);
    }

    /**
     * Checks whether a bundled media file exists.
     *
     * @param path
     *            the path below {@code media/}
     * @return {@code true} if the file exists
     */
    public static boolean exists(String path) {
        return FILES.containsKey(path) || resource(path) != null;
    }

    private static URL resource(String path) {
        return MediaLibrary.class.getResource("/media/" + path);
    }

    private static File resolve(String path) {
        URL url = resource(path);
        if (url == null) {
            throw new UncheckedIOException(
                    new IOException("No media file media/" + path));
        }
        try {
            if ("file".equals(url.getProtocol())) {
                return Path.of(url.toURI()).toFile();
            }
            return extract(url, Extracted.DIRECTORY, path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Copies a resource to a file below a directory, keeping its relative path
     * because HLS renditions share segment names. The file and the directories
     * created for it are deleted when the JVM exits.
     *
     * @param url
     *            the resource to copy
     * @param directory
     *            the directory to copy to
     * @param path
     *            the relative path of the copy
     * @return the copy
     * @throws IOException
     *             if the resource cannot be copied
     */
    static File extract(URL url, Path directory, String path)
            throws IOException {
        Path copy = directory.resolve(path);
        // Registered parent first: deleteOnExit deletes in reverse order, so
        // each directory is empty by the time it is deleted.
        Path current = directory;
        for (Path name : directory.relativize(copy.getParent())) {
            current = current.resolve(name);
            Files.createDirectories(current);
            current.toFile().deleteOnExit();
        }
        try (InputStream in = url.openStream()) {
            Files.copy(in, copy);
        }
        copy.toFile().deleteOnExit();
        return copy.toFile();
    }

    /** The temporary directory, created on first use. */
    private static final class Extracted {

        static final Path DIRECTORY = create();

        private static Path create() {
            try {
                Path directory = Files.createTempDirectory("media");
                directory.toFile().deleteOnExit();
                return directory;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
