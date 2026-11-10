package com.example.uc3;

import java.util.List;

import com.example.common.BuiltWith;
import com.example.common.UseCaseDescription;
import com.example.demoapp.DemoApp;
import com.example.tour.Tour;
import com.example.tour.TourStep;
import com.example.views.MainLayout;

import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.PopoverPosition;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;

/**
 * UC3 — Replay from the help menu.
 * <p>
 * The tour does not open by itself here. The app's help menu has "Take the
 * tour", which starts it from the first step at any time, and "Resume the
 * tour", which is enabled after the user skipped the tour and continues at the
 * step where they left.
 */
@Route(value = "uc3", layout = MainLayout.class)
@PageTitle("UC3 — Replay from help")
@UseCaseDescription("Starting the tour again, or resuming it, from the help menu")
@Menu(order = 3, title = "UC3 — Replay from help")
public class ReplayFromHelpView extends VerticalLayout {

    public ReplayFromHelpView() {
        add(new H1("UC3 — Replay from help"));
        add(new Paragraph("Users skip a tour when they are busy, and want it "
                + "later. Open the Help menu in the app's header and choose "
                + "\"Take the tour\". Skip it half way, then choose \"Resume "
                + "the tour\": it continues where you left."));

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

        MenuBar help = new MenuBar();
        help.addThemeVariants(MenuBarVariant.TERTIARY);
        SubMenu helpMenu = help.addItem("Help").getSubMenu();
        helpMenu.addItem("Take the tour", e -> tour.start());
        MenuItem resume = helpMenu.addItem("Resume the tour",
                e -> tour.startAt(tour.state().peek().step()));
        app.addToHeader(help);

        Signal.effect(this, () -> resume.setEnabled(
                tour.state().get().status() == Tour.Status.SKIPPED));

        Span status = new Span();
        status.addClassName("tour-status");
        status.bindText(tour.state().map(Tour.State::describe));
        Div controls = new Div(new Span("Tour:"), status);
        controls.addClassName("tour-controls");

        add(controls, app);
        add(new BuiltWith("Start the same tour at any step")
                .point("The help menu starts the tour from the first step.")
                .point("The tour remembers the step it was skipped at, so \"Resume\" "
                        + "starts it there.")
                .point("An effect enables \"Resume the tour\" only while the tour is skipped.")
                .java("uc3/ReplayFromHelpView").java("tour/Tour"));

        whenAttached(ui -> tour::reset);
    }
}
