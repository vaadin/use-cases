package com.example;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.vaadin.flow.server.streams.DownloadEvent;
import com.vaadin.flow.server.streams.DownloadHandler;

/**
 * Serves an HLS stream — a master playlist, its variant playlists, their init
 * segments and media segments — from one {@link DownloadHandler}.
 * <p>
 * Workaround for a gap in Flow: a handler gets exactly one URL, and the
 * resource registry only matches that exact URL. A playlist that refers to
 * {@code 360p/index.m3u8} or {@code segment00.m4s} next to itself would make
 * the browser request paths the registry does not know. This handler therefore
 * rewrites every URI in the playlists it serves to {@code ?file=<path>} on its
 * own URL and serves the requested file from the query parameter. See
 * {@code API-GAPS.md}.
 */
public class HlsDownloadHandler implements DownloadHandler {

    private static final Pattern SAFE_PATH = Pattern
            .compile("[A-Za-z0-9_-]+(/[A-Za-z0-9_-]+)*\\.(m3u8|mp4|m4s)");
    private static final Pattern URI_ATTRIBUTE = Pattern
            .compile("URI=\"([^\"]*)\"");

    private final String directory;

    /**
     * Creates a handler serving a bundled HLS stream.
     *
     * @param directory
     *            the directory below {@code media/} holding {@code index.m3u8},
     *            see {@link MediaLibrary}
     */
    public HlsDownloadHandler(String directory) {
        this.directory = directory;
    }

    @Override
    public String getUrlPostfix() {
        return "index.m3u8";
    }

    @Override
    public void handleDownloadRequest(DownloadEvent event) throws IOException {
        String file = event.getRequest().getParameter("file");
        if (file == null) {
            file = "index.m3u8";
        }
        if (!SAFE_PATH.matcher(file).matches()
                || !MediaLibrary.exists(directory + "/" + file)) {
            event.getResponse().setStatus(404);
            return;
        }
        byte[] data = MediaLibrary.bytes(directory + "/" + file);
        if (file.endsWith(".m3u8")) {
            event.setContentType("application/vnd.apple.mpegurl");
            data = rewritePlaylist(file,
                    new String(data, StandardCharsets.UTF_8))
                    .getBytes(StandardCharsets.UTF_8);
        } else {
            // The init segment (.mp4) and the media segments (.m4s) are
            // fragmented MP4.
            event.setContentType("video/mp4");
        }
        event.setContentLength(data.length);
        event.getOutputStream().write(data);
    }

    /**
     * Points every URI of a playlist back at this handler: the URI lines and
     * the {@code URI="..."} attributes of tags such as {@code #EXT-X-MAP}.
     *
     * @param playlistPath
     *            the path of the playlist, relative to the stream directory
     * @param playlist
     *            the playlist content
     * @return the rewritten playlist
     */
    static String rewritePlaylist(String playlistPath, String playlist) {
        int slash = playlistPath.lastIndexOf('/');
        String base = slash < 0 ? "" : playlistPath.substring(0, slash + 1);
        return playlist.lines().map(line -> {
            if (line.isBlank()) {
                return line;
            }
            if (line.startsWith("#")) {
                return URI_ATTRIBUTE.matcher(line)
                        .replaceAll(match -> Matcher.quoteReplacement("URI=\""
                                + toHandlerUri(base, match.group(1)) + "\""));
            }
            return toHandlerUri(base, line.trim());
        }).collect(Collectors.joining("\n", "", "\n"));
    }

    private static String toHandlerUri(String base, String uri) {
        return "?file=" + URLEncoder.encode(base + uri, StandardCharsets.UTF_8);
    }
}
