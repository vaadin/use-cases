package com.example.home;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
@ViewPackages(packages = "com.example")
class HomeViewTest extends SpringBrowserlessTest {

    private record UseCase(String tag, String title, String problem) {
    }

    private record Group(String heading, List<UseCase> useCases) {
    }

    private static final UseCase UC1 = new UseCase("UC1", "Translated texts",
            "Showing texts from translation files in the user's language");
    private static final UseCase UC2 = new UseCase("UC2", "Language switcher",
            "Letting the user pick a language and remembering the choice");
    private static final UseCase UC3 = new UseCase("UC3", "Dates and times",
            "Showing and entering dates and times in the user's format");
    private static final UseCase UC4 = new UseCase("UC4",
            "Numbers and currency",
            "Formatting and entering numbers, percentages and money per locale");
    private static final UseCase UC5 = new UseCase("UC5", "Time zones",
            "Showing stored times in each user's own time zone");
    private static final UseCase UC6 = new UseCase("UC6",
            "Right-to-left layout",
            "Mirroring the layout and icons for Arabic and Hebrew");
    private static final UseCase UC7 = new UseCase("UC7",
            "Mixed-direction text",
            "Keeping user-entered text readable inside translated sentences");
    private static final UseCase UC8 = new UseCase("UC8",
            "Sorting and searching names",
            "Sorting by the user's alphabet and searching without accents");
    private static final UseCase UC9 = new UseCase("UC9",
            "Texts that follow the language",
            "Binding texts to translations that follow the language by themselves");
    private static final UseCase UC10 = new UseCase("UC10", "Translated data",
            "Showing administrator-entered data in each user's language");

    private static final List<UseCase> USE_CASES = List.of(UC1, UC2, UC3, UC4,
            UC5, UC6, UC7, UC8, UC9, UC10);

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
                new Group("Texts and languages", List.of(UC1, UC2, UC9, UC10)),
                new Group("Dates, numbers and order",
                        List.of(UC3, UC4, UC5, UC8)),
                new Group("Writing direction", List.of(UC6, UC7))), groups);
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
