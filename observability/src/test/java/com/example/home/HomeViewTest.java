package com.example.home;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.example.uc1.InteractionLatencyView;
import com.example.uc2.ApplicationHealthView;
import com.example.uc3.ScalingSignalsView;
import com.example.uc4.InteractionTraceView;
import com.example.uc5.ConnectionInsightsView;
import com.example.uc6.FailureInsightsView;
import com.example.uc7.MonitoringStackView;
import com.example.uc8.LazyListLatencyView;
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
@ViewPackages(classes = { HomeView.class, InteractionLatencyView.class,
        ApplicationHealthView.class, ScalingSignalsView.class,
        InteractionTraceView.class, ConnectionInsightsView.class,
        FailureInsightsView.class, MonitoringStackView.class,
        LazyListLatencyView.class })
class HomeViewTest extends SpringBrowserlessTest {

    private record UseCase(String tag, String title, String problem) {
    }

    private record Group(String heading, List<UseCase> useCases) {
    }

    private static final UseCase UC1 = new UseCase("UC1", "Slow user actions",
            "Finding which action is slow and where its time goes");
    private static final UseCase UC2 = new UseCase("UC2", "App-wide hiccups",
            "Finding the query behind an app-wide hiccup");
    private static final UseCase UC3 = new UseCase("UC3", "When to scale out",
            "Knowing when to add another server");
    private static final UseCase UC4 = new UseCase("UC4", "Tracing one click",
            "Finding the cause of a slow interaction");
    private static final UseCase UC5 = new UseCase("UC5",
            "Browser-side failures",
            "Catching lost connections and browser errors");
    private static final UseCase UC6 = new UseCase("UC6",
            "From error to code line",
            "Tracing a failed action to the line of code");
    private static final UseCase UC7 = new UseCase("UC7", "Metrics in Grafana",
            "Getting the metrics into Prometheus and Grafana");
    private static final UseCase UC8 = new UseCase("UC8", "Slow lazy lists",
            "Finding why a lazy list is slow");

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

        assertEquals(
                List.of(new Group("Something is slow",
                        List.of(UC1, UC2, UC4, UC8)),
                        new Group("Something fails", List.of(UC5, UC6)),
                        new Group("Running in production", List.of(UC3, UC7))),
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
