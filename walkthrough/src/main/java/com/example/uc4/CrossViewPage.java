package com.example.uc4;

import java.util.HashMap;
import java.util.Map;

import com.example.common.BuiltWith;
import com.example.tour.Tour;
import com.example.tour.TourPopover;
import com.example.tour.TourStep;
import com.example.uc4.CrossViewTour.PageStep;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;

/**
 * What the three pages of UC4 share: the intro, the links between the pages,
 * the tour controls and the popover that shows a step of the
 * {@link CrossViewTour} when the step is on this page.
 */
abstract class CrossViewPage extends VerticalLayout {

    /** The key of the links between the pages, a target on every page. */
    static final String PAGES = "pages";

    private final Map<String, Component> targets = new HashMap<>();
    private final TourPopover popover;
    private final Div content = new Div();

    protected CrossViewPage(String pageName) {
        CrossViewTour tour = CrossViewTour.of(UI.getCurrentOrThrow());
        popover = new TourPopover(tour::back, tour::next, tour::skip);

        add(new H1("UC4 — Tour across views"));
        add(new Paragraph("A tour often has to show more than one page. Start "
                + "the tour on the dashboard: when a step is on another page, "
                + "the tour opens that page and continues there. The links "
                + "below are three separate routes."));

        Span status = new Span();
        status.addClassName("tour-status");
        status.bindText(tour.state().map(Tour.State::describe));
        Button start = new Button("Start the tour", e -> tour.start());
        Div controls = new Div(new Span("Tour:"), status, start);
        controls.addClassName("tour-controls");

        Nav pages = new Nav(
                new RouterLink("Dashboard", CrossViewTourView.class),
                new RouterLink("Orders", CrossViewOrdersView.class),
                new RouterLink("Reports", CrossViewReportsView.class));
        pages.addClassName("uc4-pages");
        addTarget(PAGES, pages);

        content.add(new H2(pageName));
        Div app = new Div(pages, content);
        app.addClassName("demo-app");

        add(controls, app);
        add(new BuiltWith("A tour that outlives the page").point(
                "The tour's state is kept with the browser tab (the UI), "
                        + "not with a page, so it survives navigation.")
                .point("A step names its page and the key of its target there: the "
                        + "next page's components do not exist before the tour opens it.")
                .point("When a step is on another page, the tour navigates; the new page "
                        + "shows the step when it is attached.")
                .java("uc4/CrossViewTour").java("uc4/CrossViewPage")
                .java("uc4/CrossViewTourView"));

        whenAttached(ui -> {
            tour.attach(this);
            return () -> tour.detach(this);
        });
    }

    /** Adds the given component to the page as the tour target of a key. */
    protected void addTarget(String key, Component target) {
        targets.put(key, target);
    }

    /** Adds the page's own content. */
    protected void addContent(Component... components) {
        content.add(components);
    }

    void showStep(PageStep step, int index, int total) {
        Component target = targets.get(step.target());
        if (target == null) {
            throw new IllegalStateException("No target " + step.target()
                    + " on " + getClass().getSimpleName());
        }
        popover.show(TourStep.of(target, step.title(), step.text()), index,
                total, index > 0);
    }

    void hideStep() {
        popover.hide();
    }
}
