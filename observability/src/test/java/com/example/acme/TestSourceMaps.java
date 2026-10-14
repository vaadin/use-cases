package com.example.acme;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.atlassian.sourcemap.WritableSourceMap;
import com.atlassian.sourcemap.WritableSourceMapImpl;

/**
 * Writes a sourcemap onto the test classpath, where a production build packages
 * them ({@link SourceMaps#BUILD_ON_CLASSPATH}). Generated when a test runs
 * rather than committed: the frontend build runs after the tests, so its own
 * maps are not there yet, and a map frozen from an earlier build would go stale
 * with the source it describes.
 * <p>
 * {@link #write(String)} writes a made-up minified chunk, one line long, with
 * two mappings into {@link #SOURCE}: from column {@value #CHART_FROM} to the
 * chart's broken read on line 2, and from column {@value #FETCH_FROM} to the
 * rejected fetch on line 5. Like Vite's, a mapping runs until the next one
 * starts.
 */
public final class TestSourceMaps {

    /** The source the map points into, as {@link SourceMaps} reports it. */
    public static final String SOURCE = "src/main/frontend/acme/stock-chart.ts";

    /** The 1-based chunk column where the chart's broken read starts. */
    public static final int CHART_FROM = 101;

    /** The 1-based chunk column where the rejected fetch starts. */
    public static final int FETCH_FROM = 201;

    /** The source, as the map embeds it in {@code sourcesContent}. */
    public static final String CONTENT = """
            function drawStockChart(response) {
              const tallest = Math.max(...response.bins.map((level) => level.onHand));
            }
            async function fetchStock() {
              throw new Error('GET /api/warehouse/stock failed');
            }
            """;

    /**
     * The source path as Vite writes it: relative to the map, from the build
     * folder up to the module, which {@link SourceMaps} strips back off.
     */
    public static final String SOURCE_AS_BUILT = "../../../../../../../"
            + SOURCE;

    private TestSourceMaps() {
    }

    /**
     * Writes the stock chart's map for a chunk.
     *
     * @param chunk
     *            the chunk's file name, e.g. {@code stock-chart-test.js}
     * @return the chunk's path, to which a frame appends {@code :line:column}
     */
    public static String write(String chunk) {
        WritableSourceMap map = newMap();
        // Lines and columns are 0-based here, 1-based in a browser's frame.
        map.addMapping(0, CHART_FROM - 1, 1, 30, SOURCE_AS_BUILT);
        map.addMapping(0, FETCH_FROM - 1, 4, 2, SOURCE_AS_BUILT);
        return write(chunk, map);
    }

    /** An empty map whose only source is {@link #SOURCE}, with its content. */
    public static WritableSourceMap newMap() {
        return new WritableSourceMapImpl.Builder().withSourcesAndSourcesContent(
                List.of(SOURCE_AS_BUILT), List.of(CONTENT)).build();
    }

    /**
     * Writes a map for a chunk, replacing whatever an earlier run left in the
     * output folder, so the map always matches the test that wrote it.
     *
     * @param chunk
     *            the chunk's file name
     * @param map
     *            the map
     * @return the chunk's path, to which a frame appends {@code :line:column}
     */
    public static String write(String chunk, WritableSourceMap map) {
        try {
            Path file = Path.of(TestSourceMaps.class.getResource("/").toURI())
                    .resolve(SourceMaps.BUILD_ON_CLASSPATH + chunk + ".map");
            Files.createDirectories(file.getParent());
            Files.writeString(file, map.generate(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        return "/VAADIN/build/" + chunk;
    }
}
