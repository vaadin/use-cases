package com.example.acme;

import com.atlassian.sourcemap.WritableSourceMap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Resolves against a map {@link TestSourceMaps} writes onto the test classpath
 * where the build packages them, so the lookup is the one production uses.
 */
class SourceMapsTest {

    private static String chunk;

    @BeforeAll
    static void writeTheMap() {
        chunk = "https://observability-cases.fly.dev"
                + TestSourceMaps.write("stock-chart-test.js");
    }

    @Test
    void aMinifiedFrameResolvesToTheFileAndLineItWasBuiltFrom() {
        SourceMaps.Original original = SourceMaps
                .resolve(chunk + ":1:" + TestSourceMaps.CHART_FROM)
                .orElseThrow();

        assertEquals(TestSourceMaps.SOURCE + ":2:31", original.location(),
                "the path the build writes relative to the map, read from the "
                        + "module root, with 1-based line and column");
        assertEquals(
                "const tallest = Math.max(...response.bins.map((level) "
                        + "=> level.onHand));",
                original.code(), "the line itself, quoted from sourcesContent");
    }

    @Test
    void aColumnInsideAMappingBelongsToTheSegmentThatStartsIt() {
        SourceMaps.Original original = SourceMaps
                .resolve(chunk + ":1:" + (TestSourceMaps.FETCH_FROM + 7))
                .orElseThrow();

        assertEquals(TestSourceMaps.SOURCE + ":5:3", original.location());
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
        assertTrue(SourceMaps.resolve(chunk + ":1").isEmpty(),
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
        SourceMaps.resolve(chunk + ":1:" + TestSourceMaps.CHART_FROM);

        assertTrue(
                SourceMaps.MAPS.keySet().stream()
                        .noneMatch(name -> name.startsWith("invented-")),
                "misses are looked up again rather than cached: "
                        + SourceMaps.MAPS.keySet());
        assertTrue(SourceMaps.MAPS.containsKey("stock-chart-test.js"),
                "a map that was found is kept");
    }

    @Test
    void aPositionOutsideTheMappedCodeHasNoOriginal() {
        assertTrue(SourceMaps.resolve(chunk + ":1:10").isEmpty(),
                "before the first mapping there is nothing to map to");
        assertTrue(SourceMaps.resolve(chunk + ":2:1").isEmpty(),
                "the chunk is one line; there is no line 2 to map");
    }

    @Test
    void aColumnBeforeALinesFirstMappingIsNotTheLineAbove() {
        // Line 1 maps from column 11 to the fetch on source line 5, line 2
        // only from column 51 to source line 1. The reader answers a column
        // before line 2's first mapping with the last mapping of line 1,
        // which here would be a confident, wrong "line 5".
        WritableSourceMap map = TestSourceMaps.newMap();
        map.addMapping(0, 10, 4, 2, TestSourceMaps.SOURCE_AS_BUILT);
        map.addMapping(1, 50, 0, 0, TestSourceMaps.SOURCE_AS_BUILT);
        String twoLines = "https://observability-cases.fly.dev"
                + TestSourceMaps.write("two-lines-test.js", map);

        assertTrue(SourceMaps.resolve(twoLines + ":2:50").isEmpty(),
                "line 2 has no mapping before column 51");
        assertEquals(TestSourceMaps.SOURCE + ":1:1",
                SourceMaps.resolve(twoLines + ":2:51").orElseThrow().location(),
                "while line 2's own mapping still resolves");
        assertEquals(TestSourceMaps.SOURCE + ":5:3", SourceMaps
                .resolve(twoLines + ":1:20").orElseThrow().location());
    }
}
