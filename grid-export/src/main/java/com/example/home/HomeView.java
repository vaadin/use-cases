package com.example.home;

import com.example.common.BaseHomeView;
import com.example.uc1.CsvOfCurrentViewView;
import com.example.uc2.ExportSelectionView;
import com.example.uc3.ColumnChoiceView;
import com.example.uc4.RenderedValuesView;
import com.example.uc5.ExcelReportView;
import com.example.uc6.RedactedExportView;
import com.example.uc7.LargeDatasetExportView;
import com.example.uc8.MultiPagePdfView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("Grid data export — use cases",
                "Each card below exports a Vaadin Grid for a different "
                        + "real-world reason: a plain CSV of what's on screen, "
                        + "a report of just the selected rows, a spreadsheet "
                        + "with grouped headers and totals. Flow has no Grid "
                        + "export API yet (vaadin/platform#7196), so every "
                        + "view is built on the hand-written facade in "
                        + "com.example.export, and API-GAPS.md records what "
                        + "that facade had to work around.");

        Div cards = new Div();
        cards.addClassName("home-cards");
        cards.add(homeCard("UC1", "CSV of the current view",
                "Filter and sorting applied, nothing else.",
                CsvOfCurrentViewView.class));
        cards.add(homeCard("UC2", "Export the selection",
                "Only the ticked rows — in the grid's row order.",
                ExportSelectionView.class));
        cards.add(homeCard("UC3", "The user's column choices",
                "Hidden columns dropped, dragged order kept.",
                ColumnChoiceView.class));
        cards.add(homeCard("UC4", "Rendered columns as text",
                "Badges, checkboxes, currency and buttons as plain text.",
                RenderedValuesView.class));
        cards.add(homeCard("UC5", "Spreadsheet report",
                "Grouped headers as merged cells, footers as totals.",
                ExcelReportView.class));
        cards.add(homeCard("UC6", "Redact on export",
                "Drop the internal id, mask the card number.",
                RedactedExportView.class));
        cards.add(homeCard("UC7", "Large lazy data set",
                "24 500 rows, streamed a page at a time.",
                LargeDatasetExportView.class));
        cards.add(homeCard("UC8", "Multi-page PDF",
                "Headers repeated on every page of the report.",
                MultiPagePdfView.class));
        add(cards);
    }
}
