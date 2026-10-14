package com.example.home;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.example.uc1.CsvOfCurrentViewView;
import com.example.uc2.ExportSelectionView;
import com.example.uc3.ColumnChoiceView;
import com.example.uc4.RenderedValuesView;
import com.example.uc5.ExcelReportView;
import com.example.uc6.RedactedExportView;
import com.example.uc7.LargeDatasetExportView;
import com.example.uc8.MultiPagePdfView;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.sidenav.SideNavItem;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = { HomeView.class, CsvOfCurrentViewView.class,
        ExportSelectionView.class, ColumnChoiceView.class,
        RenderedValuesView.class, ExcelReportView.class,
        RedactedExportView.class, LargeDatasetExportView.class,
        MultiPagePdfView.class })
class HomeViewTest extends SpringBrowserlessTest {

    private record UseCase(String tag, String title, String problem) {
    }

    private record Group(String heading, List<UseCase> useCases) {
    }

    private static final UseCase UC1 = new UseCase("UC1",
            "CSV of the current view",
            "Exporting the filtered and sorted rows a grid shows as CSV");
    private static final UseCase UC2 = new UseCase("UC2",
            "Export the selection",
            "Exporting only the rows the user selected, in the grid's order");
    private static final UseCase UC3 = new UseCase("UC3",
            "The user's column choices",
            "Exporting only the columns the user kept, in the order they dragged them");
    private static final UseCase UC4 = new UseCase("UC4",
            "Rendered columns as text",
            "Turning badges, checkboxes and formatted cells into plain text for export");
    private static final UseCase UC5 = new UseCase("UC5", "Spreadsheet report",
            "Exporting a spreadsheet with merged group headers and a totals row");
    private static final UseCase UC6 = new UseCase("UC6", "Redact on export",
            "Dropping and masking sensitive columns when exporting");
    private static final UseCase UC7 = new UseCase("UC7", "Large lazy data set",
            "Streaming a large, lazily loaded grid to a file a page at a time");
    private static final UseCase UC8 = new UseCase("UC8", "Multi-page PDF",
            "Printing a multi-page PDF that repeats the headers on every page");

    private static final List<UseCase> USE_CASES = List.of(UC1, UC2, UC3, UC4,
            UC5, UC6, UC7, UC8);

    @Test
    void cardsAreGroupedByKindOfProblemInMenuOrder() {
        HomeView home = navigate(HomeView.class);

        List<Group> groups = home.getChildren()
                .filter(Section.class::isInstance)
                .map(section -> new Group(
                        find(H2.class).from(section).single().getText(),
                        find(Card.class).from(section).all().stream()
                                .map(HomeViewTest::useCaseOf).toList()))
                .toList();

        assertEquals(List.of(
                new Group("Exporting what the user sees",
                        List.of(UC1, UC2, UC3)),
                new Group("Turning cells into text", List.of(UC4, UC6)),
                new Group("Reports and large data", List.of(UC5, UC7, UC8))),
                groups);
    }

    private static UseCase useCaseOf(Card card) {
        return new UseCase(card.getHeaderPrefix().getElement().getText(),
                card.getTitleAsText(),
                card.getChildren().filter(Paragraph.class::isInstance)
                        .map(p -> ((Paragraph) p).getText()).findFirst()
                        .orElse(null));
    }

    @Test
    void navItemsKeepTheShortNameAndShowTheProblemAsTooltip() {
        navigate(HomeView.class);

        Map<String, SideNavItem> items = find(SideNavItem.class).all().stream()
                .collect(Collectors.toMap(SideNavItem::getLabel,
                        Function.identity()));

        USE_CASES.forEach(useCase -> assertEquals(useCase.problem(),
                items.get(useCase.tag() + " — " + useCase.title()).getTooltip()
                        .getText(),
                "the nav tooltip and the home card read the same text"));
    }
}
