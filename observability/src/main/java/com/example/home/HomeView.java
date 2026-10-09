package com.example.home;

import java.util.List;

import com.example.common.BaseHomeView;
import com.example.uc1.InteractionLatencyView;
import com.example.uc2.ApplicationHealthView;
import com.example.uc3.ScalingSignalsView;
import com.example.uc4.InteractionTraceView;
import com.example.uc5.ConnectionInsightsView;
import com.example.uc6.FailureInsightsView;
import com.example.uc7.MonitoringStackView;
import com.example.uc8.LazyListLatencyView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Observability Use Cases")
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    private static final String API_GAPS_URL = "https://github.com/vaadin/use-cases/blob/main/observability/API-GAPS.md";

    public HomeView() {
        super("Observability — use cases",
                "Acme is a made-up business app with real problems: slow screens, "
                        + "failing actions, lost connections. Each card below is one "
                        + "problem a developer hits in production, and shows how "
                        + "Vaadin's observability kit helps find the cause.");
        add(new Paragraph(new Text(
                "Open a card, use the Acme app at the top, and follow the steps "
                        + "that appear below it. Where the kit falls short, the use "
                        + "case says so and links to "),
                new Anchor(API_GAPS_URL, "API-GAPS.md"), new Text(".")));
        addGroup("Something is slow", VaadinIcon.TIMER, Accent.ORANGE,
                List.of(InteractionLatencyView.class,
                        ApplicationHealthView.class, InteractionTraceView.class,
                        LazyListLatencyView.class));
        addGroup("Something fails", VaadinIcon.EXCLAMATION_CIRCLE_O, Accent.RED,
                List.of(ConnectionInsightsView.class,
                        FailureInsightsView.class));
        addGroup("Running in production", VaadinIcon.SERVER, Accent.BLUE,
                List.of(ScalingSignalsView.class, MonitoringStackView.class));
    }
}
