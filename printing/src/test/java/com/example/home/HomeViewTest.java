package com.example.home;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.example.uc1.PrintCurrentViewView;
import com.example.uc2.PrintRouteView;
import com.example.uc3.PrintableListView;
import com.example.uc4.PrintPreviewView;
import com.example.uc5.HeaderFooterView;
import com.example.uc6.PrintDashboardView;
import com.example.uc7.ExpandForPrintView;
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
@ViewPackages(classes = { HomeView.class, PrintCurrentViewView.class,
        PrintRouteView.class, PrintableListView.class, PrintPreviewView.class,
        HeaderFooterView.class, PrintDashboardView.class,
        ExpandForPrintView.class })
class HomeViewTest extends SpringBrowserlessTest {

    private record UseCase(String tag, String title, String problem) {
    }

    private record Group(String heading, List<UseCase> useCases) {
    }

    private static final UseCase UC1 = new UseCase("UC1",
            "Print the current view",
            "Printing the current view without the app shell");
    private static final UseCase UC2 = new UseCase("UC2", "A print-only route",
            "Printing a document from a window of its own");
    private static final UseCase UC3 = new UseCase("UC3",
            "Printing a long list", "Printing every row of a long Grid");
    private static final UseCase UC4 = new UseCase("UC4",
            "Paper setup and preview",
            "Letting users pick paper size and margins");
    private static final UseCase UC5 = new UseCase("UC5",
            "Letterhead and page numbers",
            "Putting a letterhead and page numbers on every sheet");
    private static final UseCase UC6 = new UseCase("UC6",
            "Printing a dashboard", "Printing charts so they fit the paper");
    private static final UseCase UC7 = new UseCase("UC7",
            "Expand everything for print",
            "Printing sections the user has folded away");

    private static final List<UseCase> USE_CASES = List.of(UC1, UC2, UC3, UC4,
            UC5, UC6, UC7);

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

        assertEquals(
                List.of(new Group("Starting a print", List.of(UC1, UC2)),
                        new Group("Components that don't print",
                                List.of(UC3, UC6, UC7)),
                        new Group("Laying out the page", List.of(UC4, UC5))),
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
