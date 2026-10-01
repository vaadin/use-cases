package com.example.acme;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.debugging.sourcemap.SourceMapConsumerV3;
import com.google.debugging.sourcemap.SourceMapParseException;
import com.google.debugging.sourcemap.proto.Mapping.OriginalMapping;
import org.jspecify.annotations.Nullable;

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
 * The maps are read with Closure Compiler's Source Map v3 consumer; the line of
 * code comes from the map's {@code sourcesContent}, when the build embeds it,
 * which Vite does by default. A location outside the bundle — a
 * development-mode module served by Vite, an executeJs frame, a third-party
 * script — resolves to nothing, and so does one whose map is not on the
 * classpath, which is the case whenever the frontend was not built with maps.
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

    /**
     * Parsed maps by chunk name. Chunk names carry a content hash, so an entry
     * never goes stale. Only maps that were found are kept — the chunk name
     * comes from a browser's report, so caching misses would let any client
     * grow this without bound — which bounds it by the build output.
     */
    static final Map<String, Chunk> MAPS = new ConcurrentHashMap<>();

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
        // A null from load leaves no entry, so a miss is looked up again.
        Chunk chunk = MAPS.computeIfAbsent(matcher.group(1), SourceMaps::load);
        return chunk == null ? Optional.empty() : chunk.resolve(line, column);
    }

    private static @Nullable Chunk load(String name) {
        try (InputStream in = SourceMaps.class.getClassLoader()
                .getResourceAsStream(BUILD_ON_CLASSPATH + name + ".map")) {
            if (in == null) {
                return null;
            }
            SourceMapConsumerV3 consumer = new SourceMapConsumerV3();
            consumer.parse(
                    new String(in.readAllBytes(), StandardCharsets.UTF_8));
            return new Chunk(consumer);
        } catch (IOException | SourceMapParseException | RuntimeException e) {
            return null;
        }
    }

    /** One chunk's parsed map, with its sources lined up with their content. */
    static final class Chunk {

        private final SourceMapConsumerV3 consumer;
        private final List<String> sources;
        private final List<@Nullable String> contents;

        /**
         * The first mapped generated column of each generated line, both
         * 0-based. Closure answers a position before a line's first mapping —
         * or on a line with none — with the last mapping of an earlier line,
         * which for a chunk with a banner or a line of imports above the code
         * would be a confident, wrong answer; this is what tells that case
         * apart.
         */
        private final Map<Integer, Integer> firstColumns = new HashMap<>();

        Chunk(SourceMapConsumerV3 consumer) {
            this.consumer = consumer;
            this.sources = List.copyOf(consumer.getOriginalSources());
            Collection<String> embedded = consumer.getOriginalSourcesContent();
            // A source the build did not embed is a null entry, which
            // List.copyOf would reject.
            List<@Nullable String> copy = embedded == null ? new ArrayList<>()
                    : new ArrayList<@Nullable String>(embedded);
            this.contents = Collections.unmodifiableList(copy);
            consumer.visitMappings(
                    (source, name, from, start, end) -> firstColumns.merge(
                            start.getLine(), start.getColumn(), Math::min));
        }

        Optional<Original> resolve(int line, int column) {
            Integer first = firstColumns.get(line - 1);
            if (first == null || column - 1 < first) {
                return Optional.empty();
            }
            OriginalMapping mapping;
            try {
                mapping = consumer.getMappingForLine(line, column);
            } catch (RuntimeException e) {
                return Optional.empty();
            }
            if (mapping == null) {
                return Optional.empty();
            }
            String file = mapping.getOriginalFile();
            int index = sources.indexOf(file);
            String content = index >= 0 && index < contents.size()
                    ? contents.get(index)
                    : null;
            return Optional.of(new Original(sourcePath(file),
                    mapping.getLineNumber(), mapping.getColumnPosition(),
                    content == null ? null
                            : lineOf(content, mapping.getLineNumber() - 1)));
        }
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
        return line >= 0 && line < lines.length ? lines[line].strip() : null;
    }
}
