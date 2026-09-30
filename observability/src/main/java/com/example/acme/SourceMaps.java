package com.example.acme;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Maps a location in the production frontend bundle back to the source it was
 * built from, using the sourcemaps the build leaves next to each chunk.
 * <p>
 * A production build minifies the frontend into chunks under
 * {@code /VAADIN/build/}, so a browser error's first stack frame — which the
 * kit retains as the {@code frame} of a {@code client-error} insight — names a
 * column on the first line of a file called {@code indexhtml-<hash>.js}. This
 * module's Vite config emits <em>hidden</em> maps: the build writes a
 * {@code .map} beside every chunk but no {@code sourceMappingURL} comment, so
 * browsers never fetch them and the maps only matter where they are read on
 * purpose. They are packaged with the chunks, under
 * {@value #BUILD_ON_CLASSPATH}, so the server that received the report can
 * resolve it without the browser's help.
 * <p>
 * Only the Source Map v3 fields this needs are read: {@code sources},
 * {@code mappings} and, when present, {@code sourcesContent}, for the line of
 * code itself. A location outside the bundle — a development-mode module served
 * by Vite, an executeJs frame, a third-party script — resolves to nothing, and
 * so does one whose map is not on the classpath, which is the case whenever the
 * frontend was not built with maps.
 */
public final class SourceMaps {

    /** Where the production chunks and their maps are on the classpath. */
    public static final String BUILD_ON_CLASSPATH = "META-INF/VAADIN/webapp/VAADIN/build/";

    /**
     * A chunk location as a stack frame writes it: an absolute URL or a path
     * ending in {@code /VAADIN/build/<chunk>.js:<line>:<column>}. The chunk
     * name may not contain a slash, so a location cannot reach a map outside
     * the build folder.
     */
    private static final Pattern CHUNK_LOCATION = Pattern
            .compile("/VAADIN/build/([\\w.-]+\\.js):(\\d+):(\\d+)$");

    private static final String BASE64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    private static final ObjectMapper JSON = new ObjectMapper();

    /**
     * Parsed maps by chunk name. Chunk names carry a content hash, so an entry
     * never goes stale, and only chunks that exist on the classpath can be
     * entries, so the cache is bounded by the build output.
     */
    private static final Map<String, Optional<JsonNode>> MAPS = new ConcurrentHashMap<>();

    private SourceMaps() {
    }

    /**
     * Where a bundle location came from.
     *
     * @param source
     *            the source file, relative to the module — for example
     *            {@code src/main/frontend/acme/stock-chart.ts}
     * @param line
     *            the 1-based line in that file
     * @param column
     *            the 1-based column in that file
     * @param code
     *            the source line itself, trimmed, or {@code null} when the map
     *            does not embed the sources
     */
    public record Original(String source, int line, int column,
            @Nullable String code) {

        /** {@code source:line:column}, the way a stack frame writes one. */
        public String location() {
            return source + ":" + line + ":" + column;
        }
    }

    /**
     * Resolves a location from the production bundle to its source.
     *
     * @param location
     *            a stack frame's location, as the kit retains it — for example
     *            {@code https://host/VAADIN/build/indexhtml-Bx3a.js:1:48213}
     * @return where it came from, or empty when the location is not in the
     *         bundle, the chunk has no map on the classpath, or the map has no
     *         mapping for that position
     */
    public static Optional<Original> resolve(@Nullable String location) {
        if (location == null) {
            return Optional.empty();
        }
        Matcher matcher = CHUNK_LOCATION.matcher(location);
        if (!matcher.find()) {
            return Optional.empty();
        }
        int line;
        int column;
        try {
            line = Integer.parseInt(matcher.group(2));
            column = Integer.parseInt(matcher.group(3));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        return MAPS.computeIfAbsent(matcher.group(1), SourceMaps::load)
                .flatMap(map -> find(map, line - 1, column - 1));
    }

    private static Optional<JsonNode> load(String chunk) {
        try (InputStream in = SourceMaps.class.getClassLoader()
                .getResourceAsStream(BUILD_ON_CLASSPATH + chunk + ".map")) {
            return in == null ? Optional.empty()
                    : Optional.of(JSON.readTree(in));
        } catch (IOException | RuntimeException e) {
            return Optional.empty();
        }
    }

    /**
     * Walks the {@code mappings} of a map to the segment covering a generated
     * position. Segments are Base64 VLQ, each field relative to the same field
     * of the previous segment — the generated column only within its line, the
     * rest across the whole map — so a line cannot be decoded without the ones
     * before it. The walk decodes up to the target line and stops there, which
     * for a minified chunk, one very long line, is most of the string;
     * resolving a handful of insight frames on a readout refresh is well within
     * that.
     */
    private static Optional<Original> find(JsonNode map, int targetLine,
            int targetColumn) {
        String mappings = map.path("mappings").asString("");
        int[] segment = new int[4];
        int generatedLine = 0;
        int generatedColumn = 0;
        int source = 0;
        int sourceLine = 0;
        int sourceColumn = 0;
        int[] best = null;
        int i = 0;
        while (i < mappings.length() && generatedLine <= targetLine) {
            char c = mappings.charAt(i);
            if (c == ';') {
                generatedLine++;
                generatedColumn = 0;
                i++;
                continue;
            }
            if (c == ',') {
                i++;
                continue;
            }
            int fields = 0;
            while (i < mappings.length() && mappings.charAt(i) != ','
                    && mappings.charAt(i) != ';') {
                int value = 0;
                int shift = 0;
                int digit;
                do {
                    digit = BASE64.indexOf(mappings.charAt(i++));
                    if (digit < 0) {
                        return Optional.empty();
                    }
                    value += (digit & 31) << shift;
                    shift += 5;
                } while ((digit & 32) != 0);
                int decoded = (value & 1) == 0 ? value >>> 1 : -(value >>> 1);
                if (fields < segment.length) {
                    segment[fields] = decoded;
                }
                fields++;
            }
            generatedColumn += segment[0];
            if (fields >= 4) {
                source += segment[1];
                sourceLine += segment[2];
                sourceColumn += segment[3];
            }
            if (generatedLine == targetLine) {
                if (generatedColumn > targetColumn) {
                    break;
                }
                best = fields >= 4
                        ? new int[] { source, sourceLine, sourceColumn }
                        : null;
            }
        }
        return best == null ? Optional.empty()
                : Optional.of(original(map, best[0], best[1], best[2]));
    }

    private static Original original(JsonNode map, int source, int line,
            int column) {
        String code = map.path("sourcesContent").path(source).isString()
                ? lineOf(map.path("sourcesContent").path(source).asString(),
                        line)
                : null;
        return new Original(
                sourcePath(map.path("sources").path(source).asString("")),
                line + 1, column + 1, code);
    }

    /**
     * A source path as the build writes it, relative to the map — a run of
     * {@code ../} up to the module — with that run dropped, so it reads from
     * the module root the way a developer would open it.
     */
    private static String sourcePath(String path) {
        String result = path;
        while (result.startsWith("../") || result.startsWith("./")) {
            result = result.substring(result.indexOf('/') + 1);
        }
        return result;
    }

    private static @Nullable String lineOf(String content, int line) {
        String[] lines = content.split("\r?\n", -1);
        return line < lines.length ? lines[line].strip() : null;
    }
}
