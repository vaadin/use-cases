package com.example.uc7;

import java.util.Comparator;
import java.util.List;

import com.example.data.Employee;
import com.example.uc7.LargeDatasetExportView.ExportRun;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.data.provider.SortDirection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = LargeDatasetExportView.class)
class LargeDatasetExportViewTest extends SpringBrowserlessTest {

    /** 24 500 rows in pages of 1 000: 24 full pages plus a partial one. */
    private static final int EXPECTED_FETCHES = 25;

    @Test
    void viewRendersTheLazyGridWithoutFetchingEverything() {
        LargeDatasetExportView view = navigate(LargeDatasetExportView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h1 -> h1.getText().startsWith("UC7 — Exporting a large")));
        assertEquals(LargeDatasetExportView.ROW_COUNT, test(view.grid).size());
        assertTrue(view.fetchCount() < EXPECTED_FETCHES,
                "rendering the grid should not walk the whole backend");
    }

    @Test
    void exportWalksTheBackendOnePageAtATime() {
        LargeDatasetExportView view = navigate(LargeDatasetExportView.class);

        ExportRun run = view.runExport();

        assertEquals(LargeDatasetExportView.ROW_COUNT, run.rows(),
                "every backend row should reach the report");
        assertEquals(EXPECTED_FETCHES, run.fetches(),
                "the export should page through the backend, not fetch it in "
                        + "one query");
    }

    @Test
    void exportFollowsTheGridsSorting() {
        LargeDatasetExportView view = navigate(LargeDatasetExportView.class);

        test(view.grid).sortByColumn("name", SortDirection.ASCENDING);
        ExportRun run = view.runExport();

        assertEquals(LargeDatasetExportView.ROW_COUNT, run.rows());
        List<String> previewNames = previewNames(run);
        assertEquals(previewNames.stream().sorted().toList(), previewNames,
                "the preview rows should come out in the sorted order");
        assertEquals(
                LargeDatasetExportView.BACKEND.stream().map(Employee::name)
                        .min(Comparator.naturalOrder()).orElseThrow(),
                previewNames.get(0),
                "the first exported row is the backend's smallest name");
    }

    @Test
    void previewKeepsOnlyTheFirstLines() {
        LargeDatasetExportView view = navigate(LargeDatasetExportView.class);

        ExportRun run = view.runExport();

        // header + PREVIEW_LINES rows + the "and N more rows" line
        assertEquals(LargeDatasetExportView.PREVIEW_LINES + 2,
                run.csvHead().lines().count());
        assertTrue(run.csvHead()
                .contains("and "
                        + (LargeDatasetExportView.ROW_COUNT
                                - LargeDatasetExportView.PREVIEW_LINES)
                        + " more rows"));
    }

    /** The Name column of the preview lines, header line skipped. */
    private static List<String> previewNames(ExportRun run) {
        return run.csvHead().lines().skip(1)
                .limit(LargeDatasetExportView.PREVIEW_LINES)
                .map(line -> line.split(",")[0]).toList();
    }
}
