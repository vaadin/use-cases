package com.example.acme;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.debugging.sourcemap.FilePosition;
import com.google.debugging.sourcemap.SourceMapGeneratorV3;

/**
 * Writes a sourcemap onto the test classpath, where a production build packages
 * them ({@link SourceMaps#BUILD_ON_CLASSPATH}). Generated when a test runs
 * rather than committed: the frontend build runs after the tests, so its own
 * maps are not there yet, and a map frozen from an earlier build would go stale
 * with the source it describes.
 * <p>
 * The map describes a made-up minified chunk with mappings into
 * {@link #SOURCE}. The chunk is one line long; columns
 * {@value #CHART_FROM}–{@value #CHART_TO} map to the chart's broken read on
 * line 2 and columns {@value #FETCH_FROM}–{@value #FETCH_TO} to the rejected
 * fetch on line 5.
 */
public final class TestSourceMaps {

    /** The source the map points into, as {@link SourceMaps} reports it. */
    public static final String SOURCE = "src/main/frontend/acme/stock-chart.ts";

    /** The 1-based chunk columns that map to the chart's broken read. */
    public static final int CHART_FROM = 101;
    public static final int CHART_TO = 140;

    /** The 1-based chunk columns that map to the rejected fetch. */
    public static final int FETCH_FROM = 201;
    public static final int FETCH_TO = 240;

    /** The source, as the map embeds it in {@code sourcesContent}. */
    public static final String CONTENT = """
            function drawStockChart(response) {
              const tallest = Math.max(...response.bins.map((level) => level.onHand));
            }
            async function fetchStock() {
              throw new Error('GET /api/warehouse/stock failed');
            }
            """;

    private TestSourceMaps() {
    }

    /**
     * Writes the map for a chunk, replacing whatever an earlier run left in the
     * output folder, so the map always matches the constants above.
     *
     * @param chunk
     *            the chunk's file name, e.g. {@code stock-chart-test.js}
     * @return the chunk's path, to which a frame appends {@code :line:column}
     */
    public static String write(String chunk) {
        // Vite writes the sources relative to the map, from the build folder
        // up to the module, which SourceMaps strips back off.
        String source = "../../../../../../../" + SOURCE;
        SourceMapGeneratorV3 generator = new SourceMapGeneratorV3();
        // FilePosition is 0-based in both line and column.
        generator.addMapping(source, null, new FilePosition(1, 30),
                new FilePosition(0, CHART_FROM - 1),
                new FilePosition(0, CHART_TO));
        generator.addMapping(source, null, new FilePosition(4, 2),
                new FilePosition(0, FETCH_FROM - 1),
                new FilePosition(0, FETCH_TO));
        generator.addSourcesContent(source, CONTENT);
        StringBuilder json = new StringBuilder();
        try {
            generator.appendTo(json, chunk);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return write(chunk, json.toString());
    }

    /**
     * Writes a map given as JSON, for a shape the generator does not produce.
     *
     * @param chunk
     *            the chunk's file name
     * @param json
     *            the map
     * @return the chunk's path, to which a frame appends {@code :line:column}
     */
    public static String write(String chunk, String json) {
        try {
            Path map = Path.of(TestSourceMaps.class.getResource("/").toURI())
                    .resolve(SourceMaps.BUILD_ON_CLASSPATH + chunk + ".map");
            Files.createDirectories(map.getParent());
            Files.writeString(map, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        return "/VAADIN/build/" + chunk;
    }
}
