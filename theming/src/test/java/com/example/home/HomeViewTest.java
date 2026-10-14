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

    private static final UseCase UC1 = new UseCase("UC1",
            "Brand with design tokens",
            "Making the application look like the company's own with a few CSS variables");
    private static final UseCase UC2 = new UseCase("UC2", "Dark mode",
            "Following the system's dark mode and letting the user override it");
    private static final UseCase UC3 = new UseCase("UC3", "Theme per customer",
            "Giving each customer of a multi-tenant application its own look, at runtime");
    private static final UseCase UC4 = new UseCase("UC4", "User's own look",
            "Letting each user pick an accent color and text size, applied live");
    private static final UseCase UC5 = new UseCase("UC5", "Compact mode",
            "Fitting more data on screen with a compact density for one view");
    private static final UseCase UC6 = new UseCase("UC6",
            "Variants or custom CSS",
            "Choosing between built-in component variants and custom CSS");
    private static final UseCase UC7 = new UseCase("UC7",
            "Styling one component",
            "Highlighting single rows, fields and cards based on their data");
    private static final UseCase UC8 = new UseCase("UC8", "Accessible theming",
            "Respecting contrast, motion and focus needs, and checking brand colors");
    private static final UseCase UC9 = new UseCase("UC9", "Aura or Lumo",
            "Comparing the two built-in themes on the same screens");

    private static final List<UseCase> USE_CASES = List.of(UC1, UC2, UC3, UC4,
            UC5, UC6, UC7, UC8, UC9);

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
                new Group("The application's look",
                        List.of(UC1, UC3, UC4, UC9)),
                new Group("Light, dark and accessibility", List.of(UC2, UC8)),
                new Group("Styling components", List.of(UC5, UC6, UC7))),
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
