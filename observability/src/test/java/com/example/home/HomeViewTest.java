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
import com.vaadin.flow.component.html.Paragraph;
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

    private static final List<UseCase> USE_CASES = List.of(
            new UseCase("UC1", "Interaction latency",
                    "Finding which action is slow and where its time goes"),
            new UseCase("UC2", "Application health",
                    "Finding the query behind an app-wide hiccup"),
            new UseCase("UC3", "Capacity & scaling",
                    "Knowing when to add another server"),
            new UseCase("UC4", "Interaction tracing",
                    "Finding the cause of a slow interaction"),
            new UseCase("UC5", "Connection & client problems",
                    "Catching lost connections and browser errors"),
            new UseCase("UC6", "Failure insights",
                    "Tracing a failed action to the line of code"),
            new UseCase("UC7", "Monitoring stack",
                    "Getting the metrics into Prometheus and Grafana"),
            new UseCase("UC8", "Data query insights",
                    "Finding why a lazy list is slow"));

    @Test
    void cardsShowTheShortNameAsTitleAndTheProblemAsDescription() {
        navigate(HomeView.class);

        List<UseCase> cards = findInView(Card.class).all().stream()
                .map(card -> new UseCase(card.getHeader().getElement().getText(),
                        card.getTitle().getElement().getText(),
                        card.getChildren()
                                .filter(Paragraph.class::isInstance)
                                .map(p -> ((Paragraph) p).getText())
                                .findFirst().orElse(null)))
                .toList();

        assertEquals(USE_CASES, cards);
    }

    @Test
    void navItemsKeepTheShortNameAndShowTheProblemAsTooltip() {
        navigate(HomeView.class);

        Map<String, SideNavItem> items = find(SideNavItem.class).all()
                .stream().collect(Collectors.toMap(SideNavItem::getLabel,
                        Function.identity()));

        USE_CASES.forEach(useCase -> assertEquals(useCase.problem(),
                items.get(useCase.tag() + " — " + useCase.title())
                        .getTooltip().getText(),
                "the nav tooltip and the home card read the same text"));
    }
}
