package com.example.usecase27;

import com.example.common.UseCaseDescription;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Use Case 27: Router state as a signal.
 * <p>
 * Demonstrates {@code UI.routerStateSignal()}: a read-only signal carrying the
 * current navigation target, location and route parameters. The breadcrumb in
 * {@link UseCase27Layout} is wired to that signal via {@code Signal.effect}, so
 * it updates without an {@code AfterNavigationObserver} or manual seeding on
 * attach.
 */
@Route(value = "use-case-27", layout = UseCase27Layout.class)
@PageTitle("Use Case 27: Router State Signal")
@UseCaseDescription("Reacting to navigation and route parameters through a router state signal")
@Menu(order = 27, title = "UC 27: Router State Signal")
@AnonymousAllowed
public class UseCase27View extends VerticalLayout {

    public UseCase27View() {
        setSpacing(true);
        setPadding(true);

        H2 title = new H2("Use Case 27: Router state as a signal");

        Paragraph description = new Paragraph(
                "The parent layout subscribes to UI.routerStateSignal() and "
                        + "rebuilds the breadcrumb above whenever a navigation "
                        + "completes. Click the links below to navigate to "
                        + "sibling routes — the leaf view changes, the parent "
                        + "layout instance stays, and the breadcrumb updates "
                        + "reactively. The signal also fires for navigations "
                        + "to the same view class with different route "
                        + "parameters.");

        Paragraph subDescription = new Paragraph(
                "Compare this with the BeforeEnterObserver / "
                        + "AfterNavigationObserver pattern: those need both an "
                        + "observer registration and a separate \"seed the "
                        + "initial state on attach\" step. The signal collapses "
                        + "both into a single Signal.effect that runs once on "
                        + "registration and again on each navigation.");

        RouterLink detailsForty = new RouterLink("Details for order #42",
                UseCase27DetailsView.class, new RouteParameters("id", "42"));
        RouterLink detailsHundred = new RouterLink("Details for order #100",
                UseCase27DetailsView.class, new RouteParameters("id", "100"));
        RouterLink settings = new RouterLink("Settings",
                UseCase27SettingsView.class);

        HorizontalLayout links = new HorizontalLayout(detailsForty,
                detailsHundred, settings);
        links.setSpacing(true);

        add(title, description, subDescription, links);
    }
}
