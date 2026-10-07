package com.example;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.streams.DownloadEvent;
import com.vaadin.flow.server.streams.DownloadHandler;

/**
 * A {@link DownloadHandler} that honours a single HTTP byte range, so a
 * {@code <video>} or {@code <audio>} player can seek in a file the application
 * serves.
 * <p>
 * Workaround for a gap in Flow: the built-in handlers ({@code forFile},
 * {@code forClassResource}, {@code fromInputStream}, ...) ignore the
 * {@code Range} request header and always answer {@code 200} with the whole
 * file. Browsers then report the media as not seekable — Chrome snaps the
 * playhead back when the user drags the scrubber, and Safari refuses to play
 * such a response at all. See {@code API-GAPS.md}.
 * <p>
 * Only single ranges ({@code bytes=start-end}, {@code bytes=start-},
 * {@code bytes=-suffix}) are served as {@code 206}; a multi-range request gets
 * the whole file, which RFC 9110 allows. Migrate by dropping this class once
 * the built-in handlers support ranges.
 */
public class RangeDownloadHandler implements DownloadHandler {

    private static final Pattern SINGLE_RANGE = Pattern
            .compile("bytes=(\\d*)-(\\d*)");

    private final SerializableSupplier<byte[]> content;
    private final String contentType;

    /**
     * Creates a handler serving the given content.
     *
     * @param content
     *            supplies the bytes to serve, called once per request
     * @param contentType
     *            the MIME type of the content, for example {@code video/mp4}
     */
    public RangeDownloadHandler(SerializableSupplier<byte[]> content,
            String contentType) {
        this.content = content;
        this.contentType = contentType;
    }

    /**
     * Creates a handler serving a bundled media file.
     *
     * @param path
     *            the path below {@code media/}, see {@link MediaLibrary}
     * @param contentType
     *            the MIME type of the file
     * @return the handler
     */
    public static RangeDownloadHandler forMedia(String path,
            String contentType) {
        return new RangeDownloadHandler(() -> MediaLibrary.bytes(path),
                contentType);
    }

    @Override
    public void handleDownloadRequest(DownloadEvent event) throws IOException {
        byte[] data = content.get();
        VaadinResponse response = event.getResponse();
        response.setHeader("Accept-Ranges", "bytes");
        event.setContentType(contentType);

        long[] range = parseRange(event.getRequest().getHeader("Range"),
                data.length);
        if (range == null) {
            event.setContentLength(data.length);
            event.getOutputStream().write(data);
            return;
        }
        if (range.length == 0) {
            response.setStatus(416);
            response.setHeader("Content-Range", "bytes */" + data.length);
            return;
        }
        long start = range[0];
        long end = range[1];
        response.setStatus(206);
        response.setHeader("Content-Range",
                "bytes " + start + "-" + end + "/" + data.length);
        event.setContentLength(end - start + 1);
        event.getOutputStream().write(data, (int) start,
                (int) (end - start + 1));
    }

    /**
     * Resolves a {@code Range} header against the content size.
     *
     * @param header
     *            the header value, or {@code null} if the request had none
     * @param size
     *            the content size in bytes
     * @return {@code null} to serve the whole content, an empty array when the
     *         range cannot be satisfied, or {@code {start, end}} (inclusive)
     */
    static long @Nullable [] parseRange(@Nullable String header, long size) {
        if (header == null) {
            return null;
        }
        Matcher matcher = SINGLE_RANGE.matcher(header.trim());
        if (!matcher.matches()) {
            // Multiple ranges or another unit: serving everything is allowed.
            return null;
        }
        String first = matcher.group(1);
        String last = matcher.group(2);
        long start;
        long end;
        if (first.isEmpty()) {
            if (last.isEmpty()) {
                return null;
            }
            long suffix = Long.parseLong(last);
            if (suffix == 0) {
                return new long[0];
            }
            start = Math.max(0, size - suffix);
            end = size - 1;
        } else {
            start = Long.parseLong(first);
            end = last.isEmpty() ? size - 1
                    : Math.min(size - 1, Long.parseLong(last));
        }
        if (start >= size || start > end) {
            return new long[0];
        }
        return new long[] { start, end };
    }
}
