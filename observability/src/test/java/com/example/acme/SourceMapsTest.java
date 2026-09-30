package com.example.acme;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Resolves against {@code stock-chart-fixture.js.map}, which is the map a
 * production build of this module wrote for the chunk holding
 * {@code acme/stock-chart.ts}, copied verbatim into the test classpath under
 * the path the build packages it at. Frozen on purpose: the columns below are
 * positions in the minified chunk that map was written for, and a map that
 * followed the source would move them on every edit.
 */
class SourceMapsTest {

    private static final String CHUNK = "https://observability-cases.fly.dev"
            + "/VAADIN/build/stock-chart-fixture.js";

    @Test
    void aMinifiedFrameResolvesToTheFileLineAndFunctionItWasBuiltFrom() {
        // "function S(e){let t=Math.max(...e.bins.map(" — the chart's broken
        // read, at column 496 of the chunk's only line.
        SourceMaps.Original original = SourceMaps.resolve(CHUNK + ":1:496")
                .orElseThrow();

        assertEquals("src/main/frontend/acme/stock-chart.ts", original.source(),
                "the path the build writes relative to the map, read from the "
                        + "module root");
        assertEquals(43, original.line());
        assertEquals(
                "const tallest = Math.max(...response.bins.map((level) "
                        + "=> level.onHand));",
                original.code(), "the line itself, quoted from sourcesContent");
        assertTrue(
                original.location().startsWith(
                        "src/main/frontend/acme/stock-chart.ts:43:"),
                original.location());
    }

    @Test
    void aColumnBetweenTwoMappingsBelongsToTheOneBeforeIt() {
        // "throw Error(`GET …": the map has a segment where Error starts, at
        // 641, and 644 is inside the name, which that segment still covers.
        SourceMaps.Original original = SourceMaps.resolve(CHUNK + ":1:644")
                .orElseThrow();

        assertEquals("src/main/frontend/acme/stock-chart.ts:58:13",
                original.location(), "where `Error` starts in the source");
        assertTrue(original.code().startsWith("throw new Error("),
                original.code());
    }

    @Test
    void onlyChunksWithAMapOnTheClasspathResolve() {
        assertTrue(SourceMaps.resolve(null).isEmpty());
        assertTrue(SourceMaps.resolve("/VAADIN/build/no-such-chunk.js:1:10")
                .isEmpty(), "a build without maps resolves nothing");
        assertTrue(SourceMaps.resolve(
                "http://localhost:8080/VAADIN/@fs/src/main/frontend/acme/"
                        + "stock-chart.ts:43:40")
                .isEmpty(), "a development-mode module is already the source");
        assertTrue(SourceMaps.resolve(CHUNK + ":1").isEmpty(),
                "a source without a column is not a frame");
        assertTrue(SourceMaps
                .resolve("/VAADIN/build/../../../application.properties:1:1")
                .isEmpty(), "a chunk name cannot leave the build folder");
    }

    @Test
    void chunkNamesWithoutAMapAreNotRemembered() {
        // The chunk name comes from a browser's report, so a client inventing
        // names must not be able to grow the cache.
        for (int i = 0; i < 100; i++) {
            SourceMaps.resolve("/VAADIN/build/invented-" + i + ".js:1:1");
        }
        SourceMaps.resolve(CHUNK + ":1:496");

        assertTrue(SourceMaps.MAPS.keySet().stream()
                .noneMatch(name -> name.startsWith("invented-")),
                "misses are looked up again rather than cached: "
                        + SourceMaps.MAPS.keySet());
        assertTrue(SourceMaps.MAPS.containsKey("stock-chart-fixture.js"),
                "a map that was found is kept");
    }

    @Test
    void aPositionPastTheMappedCodeHasNoOriginal() {
        assertTrue(SourceMaps.resolve(CHUNK + ":2:1").isEmpty(),
                "the chunk is one line; there is no line 2 to map");
    }
}
