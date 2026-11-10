package com.example.uc4;

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
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC4 — What's new.
 * <p>
 * After an update, the tour points only at what changed since the version the
 * user last used: two features came with 2.1 and one with 2.2. A user coming
 * from 2.0 sees three steps, from 2.1 one step, and from 2.2 no tour at all.
 * The new features carry a "New" badge until the user has seen the tour.
 */
@Route(value = "uc4", layout = MainLayout.class)
@PageTitle("UC4 — What's new")
@UseCaseDescription("Pointing only at the features added since the user's last visit")
@Menu(order = 4, title = "UC4 — What's new")
public class WhatsNewView extends VerticalLayout {

    static final String CURRENT_VERSION = "2.2";
    private static final String NEW_BADGE = "tour-new";

    private final List<NewFeature> features;
    private final Tour tour = new Tour(List.of());
    private final Select<String> lastSeen = new Select<>();
    private final ValueSignal<String> status = new ValueSignal<>("");
    private List<NewFeature> shown = List.of();

    public WhatsNewView() {
        add(new H1("UC4 — What's new"));
        add(new Paragraph("Users who know the app do not need the full tour "
                + "again, only what changed. The app is now at version "
                + CURRENT_VERSION + ". Pick the version you used last and "
                + "open the app: the tour shows only the features added "
                + "after it."));

        DemoApp app = new DemoApp();
        features = List.of(
                new NewFeature("2.1", app.savedFilters(), "Saved filters",
                        "New in 2.1: save a search and open it again with "
                                + "one click."),
                new NewFeature("2.1", app.export(), "Export",
                        "New in 2.1: download the orders as a spreadsheet."),
                new NewFeature("2.2", app.darkMode(), "Dark mode",
                        "New in 2.2: switch to a dark theme for late "
                                + "shifts."));
        tour.addEndListener(state -> {
            shown.forEach(f -> f.target().getElement().getClassList()
                    .remove(NEW_BADGE));
            lastSeen.setValue(CURRENT_VERSION);
            status.set("You have seen what is new in " + CURRENT_VERSION
                    + ". The badges are gone.");
        });

        lastSeen.setLabel("Version you used last");
        lastSeen.setItems("2.0", "2.1", CURRENT_VERSION);
        lastSeen.setValue("2.0");
        Button open = new Button("Open the app", e -> openApp());
        Span statusLine = new Span();
        statusLine.addClassName("tour-status");
        statusLine.bindText(status);
        Div controls = new Div(lastSeen, open, statusLine);
        controls.addClassName("tour-controls");

        add(controls, app);
        add(new BuiltWith("Steps picked by version")
                .point("Each feature knows the app version it came with.")
                .point("When the app opens, only the features after the user's last "
                        + "version become tour steps, and they get a \"New\" badge.")
                .point("When the tour ends, the user's last version becomes the "
                        + "current one, so the tour does not open again.")
                .java("uc4/WhatsNewView").java("uc4/NewFeature"));

        whenAttached(ui -> {
            openApp();
            return tour::reset;
        });
    }

    private void openApp() {
        shown.forEach(
                f -> f.target().getElement().getClassList().remove(NEW_BADGE));
        shown = NewFeature.since(features, lastSeen.getValue());
        tour.setSteps(shown.stream()
                .map(f -> TourStep.of(f.target(), f.title(), f.text()))
                .toList());
        if (shown.isEmpty()) {
            status.set("Nothing is new since " + lastSeen.getValue()
                    + ", so no tour opens.");
            return;
        }
        shown.forEach(
                f -> f.target().getElement().getClassList().add(NEW_BADGE));
        status.set(shown.size()
                + (shown.size() == 1 ? " feature is" : " features are")
                + " new since " + lastSeen.getValue() + ".");
        tour.start();
    }
}
