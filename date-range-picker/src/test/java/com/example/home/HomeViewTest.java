package com.example.home;

import java.util.List;
import java.util.stream.Stream;

import com.example.uc1.BookAStayView;
import com.example.uc2.FilterOrdersView;
import com.example.uc3.RequestTimeOffView;
import com.example.uc4.ChangeReturnDateView;
import com.example.uc5.DatesInYourFormatView;
import com.example.uc6.OneFieldView;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.RouterLink;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = { HomeView.class, BookAStayView.class,
        FilterOrdersView.class, RequestTimeOffView.class,
        ChangeReturnDateView.class, DatesInYourFormatView.class,
        OneFieldView.class })
class HomeViewTest extends SpringBrowserlessTest {

    private record UseCase(String tag, String title, String problem,
            String href) {
    }

    private record Group(String heading, List<UseCase> useCases) {
    }

    private static final UseCase UC1 = new UseCase("UC1", "Book a stay",
            "Picking check-in and check-out around fully booked nights", "uc1");
    private static final UseCase UC2 = new UseCase("UC2", "Filter by period",
            "Filtering a list to a period, with quick buttons for common periods",
            "uc2");
    private static final UseCase UC3 = new UseCase("UC3", "Request time off",
            "Asking for days off in a validated form that counts working days",
            "uc3");
    private static final UseCase UC4 = new UseCase("UC4",
            "Change the return date",
            "Moving the end of a range whose start can no longer change",
            "uc4");
    private static final UseCase UC5 = new UseCase("UC5",
            "Dates in your format",
            "Typing and showing a range in the user's date format and language",
            "uc5");
    private static final UseCase UC6 = new UseCase("UC6",
            "One field for the range",
            "Fitting a range into one input in a narrow toolbar", "uc6");

    private static final List<UseCase> USE_CASES = List.of(UC1, UC2, UC3, UC4,
            UC5, UC6);

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
                List.of(new Group("Book and request", List.of(UC1, UC3, UC4)),
                        new Group("Filter", List.of(UC2, UC6)),
                        new Group("Fit the user", List.of(UC5))),
                groups);
    }

    @Test
    void navItemsKeepTheShortNameAndShowTheProblemAsTooltip() {
        navigate(HomeView.class);

        assertEquals(
                USE_CASES.stream()
                        .map(useCase -> useCase.tag() + " — " + useCase.title()
                                + ": " + useCase.problem())
                        .toList(),
                find(SideNavItem.class).all().stream()
                        .filter(item -> !"Home".equals(item.getLabel()))
                        .map(item -> item.getLabel() + ": "
                                + item.getTooltip().getText())
                        .toList(),
                "the nav tooltip and the home card read the same text");
    }

    private static UseCase useCaseOf(Card card) {
        return new UseCase(card.getHeaderPrefix().getElement().getText(),
                card.getTitleAsText(),
                card.getChildren().filter(Paragraph.class::isInstance)
                        .map(p -> ((Paragraph) p).getText()).findFirst()
                        .orElse(null),
                Stream.of(card.getFooterComponents())
                        .filter(RouterLink.class::isInstance)
                        .map(link -> ((RouterLink) link).getHref()).findFirst()
                        .orElse(null));
    }
}
