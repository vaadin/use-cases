package com.example.uc1;

import java.util.List;

import com.example.common.BuiltWith;
import com.example.common.UseCaseDescription;
import com.example.demoapp.DemoApp;
import com.example.tour.Tour;
import com.example.tour.TourStep;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.PopoverPosition;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC1 — First-run tour.
 * <p>
 * When the page opens, a tour walks the user through the main parts of the demo
 * app: the navigation, the search, the "New order" button, the order list and
 * the user menu. Each step shows "Step n of 5" with "Back", "Next" and "Skip
 * tour". The status line shows whether the user finished the tour or skipped
 * it, and at which step.
 */
@Route(value = "uc1", layout = MainLayout.class)
@PageTitle("UC1 — First-run tour")
@UseCaseDescription("Introducing the main parts of the app when it opens")
@Menu(order = 1, title = "UC1 — First-run tour")
public class FirstRunTourView extends VerticalLayout {

    public FirstRunTourView() {
        add(new H1("UC1 — First-run tour"));
        add(new Paragraph("A new user opens the app and does not know where "
                + "things are. The tour starts when the page opens and points "
                + "at the five parts of the app below, one at a time. Use "
                + "Next and Back, or skip the tour with the button or the "
                + "Escape key."));

        DemoApp app = new DemoApp();
        Tour tour = new Tour(List.of(
                TourStep.of(app.navigation(), "Find your way",
                        "Move between the dashboard, orders, customers and "
                                + "reports here."),
                TourStep.of(app.search(), "Search",
                        "Find any order by its number or customer."),
                TourStep.of(app.newOrder(), "Create an order",
                        "Start a new order from anywhere in the app."),
                TourStep.of(app.orders(), "Your orders",
                        "The latest orders, with their status and total.")
                        .at(PopoverPosition.TOP),
                TourStep.of(app.userMenu(), "Your account",
                        "Change your profile and settings, or sign out.")));

        Span status = new Span();
        status.addClassName("tour-status");
        status.bindText(tour.state().map(Tour.State::describe));
        Button restart = new Button("Start the tour again", e -> tour.start());
        Div controls = new Div(new Span("Tour:"), status, restart);
        controls.addClassName("tour-controls");

        add(controls, app);
        add(new BuiltWith("One popover that moves from target to target").point(
                "One Popover moves to the component of the current step; its own "
                        + "opening on click, hover and focus is turned off.")
                .point("A small Tour class keeps the steps in order and fills the popover "
                        + "with \"Step n of m\", Back, Next and Skip tour.")
                .point("The browser opens the popover once its target has a size: "
                        + "opened while the page is still being shown, it would close again.")
                .point("A signal holds the tour state, so the status line above follows it.")
                .java("uc1/FirstRunTourView").java("tour/Tour")
                .java("tour/TourPopover").java("tour/TourStep")
                .java("tour/TourCard"));

        whenAttached(ui -> {
            tour.start();
            return tour::reset;
        });
    }
}
