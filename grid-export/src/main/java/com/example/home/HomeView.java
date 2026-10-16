package com.example.home;

import java.util.List;

import com.example.common.BaseHomeView;
import com.example.common.UseCaseDescription;
import com.example.uc1.CsvOfCurrentViewView;
import com.example.uc2.ExportSelectionView;
import com.example.uc3.ColumnChoiceView;
import com.example.uc4.RenderedValuesView;
import com.example.uc5.ExcelReportView;
import com.example.uc6.RedactedExportView;
import com.example.uc7.LargeDatasetExportView;
import com.example.uc8.MultiPagePdfView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@UseCaseDescription("Exporting Vaadin Grid data to CSV, spreadsheets and PDF")
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
        addGroup("Exporting what the user sees", VaadinIcon.EYE, Accent.BLUE,
                List.of(CsvOfCurrentViewView.class, ExportSelectionView.class,
                        ColumnChoiceView.class));
        addGroup("Turning cells into text", VaadinIcon.TEXT_LABEL,
                Accent.PURPLE,
                List.of(RenderedValuesView.class, RedactedExportView.class));
        addGroup("Reports and large data", VaadinIcon.FILE_TABLE, Accent.GREEN,
                List.of(ExcelReportView.class, LargeDatasetExportView.class,
                        MultiPagePdfView.class));
    }
}
