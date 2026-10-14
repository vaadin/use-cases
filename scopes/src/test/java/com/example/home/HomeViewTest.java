package com.example.home;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.example.uc1.MaintenanceBannerView;
import com.example.uc2.SharedCartView;
import com.example.uc3.OrderExportView;
import com.example.uc4.PriceListEditorView;
import com.example.uc5.CustomerDetailsView;
import com.example.uc6.SeatSelectionView;
import com.example.uc7.TicketPerTabView;
import com.example.uc8.ScopePlaygroundView;
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
@ViewPackages(classes = { HomeView.class, MaintenanceBannerView.class,
        SharedCartView.class, OrderExportView.class, PriceListEditorView.class,
        CustomerDetailsView.class, SeatSelectionView.class,
        TicketPerTabView.class, ScopePlaygroundView.class })
class HomeViewTest extends SpringBrowserlessTest {

    private record UseCase(String tag, String title, String problem) {
    }

    private record Group(String heading, List<UseCase> useCases) {
    }

    private static final UseCase UC1 = new UseCase("UC1", "Maintenance banner",
            "Showing a maintenance notice to every user at once");
    private static final UseCase UC2 = new UseCase("UC2",
            "Shared shopping cart",
            "Keeping one shopping cart across all of a user's tabs");
    private static final UseCase UC3 = new UseCase("UC3", "Export settings",
            "Sharing export settings between a view and a plain Spring MVC endpoint");
    private static final UseCase UC4 = new UseCase("UC4", "Undo history",
            "Keeping one undo history per editor window, shared by its components");
    private static final UseCase UC5 = new UseCase("UC5", "Customer draft",
            "Keeping a draft across the pages of an editor and dropping it on leaving");
    private static final UseCase UC6 = new UseCase("UC6", "Booking wizard",
            "Keeping a multi-step booking through reloads, separate in each tab");
    private static final UseCase UC7 = new UseCase("UC7", "Ticket per tab",
            "Keeping a different ticket and reply draft in each browser tab");
    private static final UseCase UC8 = new UseCase("UC8", "Scope playground",
            "Seeing which scope survives a reload, a new tab or another browser");

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
                new Group("Shared by users and sessions",
                        List.of(UC1, UC2, UC3)),
                new Group("Kept for one window or editor", List.of(UC4, UC5)),
                new Group("Kept per browser tab", List.of(UC6, UC7)),
                new Group("All scopes side by side", List.of(UC8))), groups);
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
