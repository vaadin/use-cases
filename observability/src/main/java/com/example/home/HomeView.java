package com.example.home;

import java.util.List;

import com.example.common.BaseHomeView;
import com.example.common.UseCaseDescription;
import com.example.uc1.InteractionLatencyView;
import com.example.uc2.ApplicationHealthView;
import com.example.uc3.ScalingSignalsView;
import com.example.uc4.InteractionTraceView;
import com.example.uc5.ConnectionInsightsView;
import com.example.uc6.FailureInsightsView;
import com.example.uc7.MonitoringStackView;
import com.example.uc8.LazyListLatencyView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Observability Use Cases")
@UseCaseDescription("Finding the cause of slow screens, failing actions and lost connections in production")
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    private static final String API_GAPS_URL = "https://github.com/vaadin/use-cases/blob/main/observability/API-GAPS.md";

    /**
     * The use cases grouped by the kind of problem they are about. The side
     * navigation keeps the UC1 to UC8 order; within a group the cards follow it
     * too. The accent is one of Aura's accent classes, so each kind of problem
     * reads in its own color.
     */
    private record Group(String heading, VaadinIcon icon, String accent,
            List<Class<? extends Component>> views) {
    }

    private static final List<Group> GROUPS = List.of(
            new Group("Something is slow", VaadinIcon.TIMER,
                    "aura-accent-orange",
                    List.of(InteractionLatencyView.class,
                            ApplicationHealthView.class,
                            InteractionTraceView.class,
                            LazyListLatencyView.class)),
            new Group("Something fails", VaadinIcon.EXCLAMATION_CIRCLE_O,
                    "aura-accent-red",
                    List.of(ConnectionInsightsView.class,
                            FailureInsightsView.class)),
            new Group("Running in production", VaadinIcon.SERVER,
                    "aura-accent-blue", List.of(ScalingSignalsView.class,
                            MonitoringStackView.class)));

    public HomeView() {
        super("Observability — use cases",
                "Acme is a made-up business app with real problems: slow screens, "
                        + "failing actions, lost connections. Each card below is one "
                        + "problem a developer hits in production, and shows how "
                        + "Vaadin's observability kit helps find the cause.");
        addClassName("home-view");
        Paragraph howTo = new Paragraph(new Text(
                "Open a card, use the Acme app at the top, and follow the steps "
                        + "that appear below it. Where the kit falls short, the use "
                        + "case says so and links to "),
                new Anchor(API_GAPS_URL, "API-GAPS.md"), new Text("."));
        howTo.addClassName("home-how-to");
        add(howTo);
        GROUPS.forEach(group -> add(groupSection(group)));
    }

    private Section groupSection(Group group) {
        Icon icon = group.icon().create();
        icon.addClassName("home-group-icon");
        H2 heading = new H2(group.heading());
        heading.addClassName("home-group-heading");
        Span count = new Span(group.views().size() + " use cases");
        count.addClassName("home-group-count");
        Div header = new Div(icon, heading, count);
        header.addClassName("home-group-header");

        Section section = new Section(header, menuCards(group.views()));
        section.addClassNames("home-group", group.accent());
        return section;
    }
}
