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
            "Composite of existing components",
            "Building a reusable form from existing components with Composite");
    private static final UseCase UC2 = new UseCase("UC2",
            "Field with Binder and signals",
            "Making a custom field that Binder, validation and signals treat like any other");
    private static final UseCase UC3 = new UseCase("UC3", "Own API and events",
            "Giving a component a typed value, its own events and keyboard support");
    private static final UseCase UC4 = new UseCase("UC4", "Server-side DOM",
            "Drawing a chart as SVG from Java with the Element API, no JavaScript");
    private static final UseCase UC5 = new UseCase("UC5", "Own web component",
            "Writing a Lit web component and giving it a Java API");
    private static final UseCase UC6 = new UseCase("UC6", "npm web component",
            "Wrapping a third-party web component from npm in a typed Java API");
    private static final UseCase UC7 = new UseCase("UC7", "React component",
            "Using a React component from npm as a Vaadin component");
    private static final UseCase UC8 = new UseCase("UC8", "Styling and theming",
            "Giving components theme variants, CSS variables and parts that follow the theme");
    private static final UseCase UC9 = new UseCase("UC9",
            "Testing the components",
            "Shipping a browserless tester and a TestBench element with a component");
    private static final UseCase UC10 = new UseCase("UC10", "The composite app",
            "Assembling a screen from custom components that talk through events");
    private static final UseCase UC11 = new UseCase("UC11",
            "Packaging as an add-on",
            "Laying out components so they can ship as a reusable add-on jar");

    private static final List<UseCase> USE_CASES = List.of(UC1, UC2, UC3, UC4,
            UC5, UC6, UC7, UC8, UC9, UC10, UC11);

    @Test
    void cardsAreGroupedByHowTheComponentIsBuiltInMenuOrder() {
        HomeView home = navigate(HomeView.class);

        List<Group> groups = home.getChildren()
                .filter(Section.class::isInstance)
                .map(section -> new Group(
                        find(H2.class).from(section).single().getText(),
                        find(Card.class).from(section).all().stream()
                                .map(HomeViewTest::useCaseOf).toList()))
                .toList();

        assertEquals(
                List.of(new Group("Built in Java", List.of(UC1, UC2, UC3, UC4)),
                        new Group("Built on web components",
                                List.of(UC5, UC6, UC7)),
                        new Group("Around the components",
                                List.of(UC8, UC9, UC10, UC11))),
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
