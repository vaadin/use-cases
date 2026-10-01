package com.example.home;

import java.util.List;

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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.RouterLink;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = { HomeView.class, InteractionLatencyView.class,
        ApplicationHealthView.class, ScalingSignalsView.class,
        InteractionTraceView.class, ConnectionInsightsView.class,
        FailureInsightsView.class, MonitoringStackView.class,
        LazyListLatencyView.class })
class HomeViewTest extends SpringBrowserlessTest {

    private static final List<Class<?>> USE_CASES = List.of(
            InteractionLatencyView.class, ApplicationHealthView.class,
            ScalingSignalsView.class, InteractionTraceView.class,
            ConnectionInsightsView.class, FailureInsightsView.class,
            MonitoringStackView.class, LazyListLatencyView.class);

    @Test
    void catalogNamesEachUseCaseAfterTheProblemItSolves() {
        navigate(HomeView.class);

        List<String> links = findInView(RouterLink.class).all().stream()
                .map(RouterLink::getText)
                .filter(text -> text.startsWith("UC")).toList();

        assertEquals(List.of(
                "UC1 — Finding which action is slow and where its time goes",
                "UC2 — Finding the query behind an app-wide hiccup",
                "UC3 — Knowing when to add another server",
                "UC4 — Finding the cause of a slow interaction",
                "UC5 — Catching lost connections and browser errors",
                "UC6 — Tracing a failed action to the line of code",
                "UC7 — Getting the metrics into Prometheus and Grafana",
                "UC8 — Finding why a lazy list is slow"), links);
        assertEquals(
                USE_CASES.stream()
                        .map(view -> view.getAnnotation(PageTitle.class)
                                .value())
                        .toList(),
                links,
                "the catalog, the menu and the page title name a use case "
                        + "the same way");
    }
}
