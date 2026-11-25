package com.example.uc1;

import java.util.List;

import com.example.common.BuiltWith;
import com.example.common.UseCaseDescription;
import com.example.demoapp.DemoApp;
import com.example.tour.Tour;
import com.example.tour.TourStep;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.WebStorage;
import com.vaadin.flow.component.popover.PopoverPosition;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC1 — App tour.
 * <p>
 * The tour of the whole app, as it fits each user. One stored value decides
 * what opens with the app: the last app version whose tour the user has seen. A
 * user who has never seen it gets the full five-step tour; a user who saw an
 * older version gets only the features added since, marked with a "New" badge;
 * a user who saw the current version gets no tour. Finishing or skipping the
 * tour stores the current version. The value is kept per account on the server
 * ({@link SeenVersions}) or per browser in its local storage
 * ({@link WebStorage}). The app's help menu starts the full tour at any time,
 * or resumes a skipped tour at the step where the user left.
 */
@Route(value = "uc1", layout = MainLayout.class)
@PageTitle("UC1 — App tour")
@UseCaseDescription("A tour of the app for new users, what's new for returning ones, and a replay from the help menu")
@Menu(order = 1, title = "UC1 — App tour")
public class AppTourView extends VerticalLayout {

    static final String CURRENT_VERSION = "2.2";
    static final String STORAGE_KEY = "walkthrough-uc1-seen-version";
    /** The item of the "seen" select for a user who never saw the tour. */
    static final String NEVER = "never";
    private static final String NEW_BADGE = "tour-new";

    enum Memory {
        ACCOUNT("Per account, on the server"),
        BROWSER("Per browser, in local storage");

        private final String label;

        Memory(String label) {
            this.label = label;
        }
    }

    private final SeenVersions seenVersions;
    private final Tour tour = new Tour(List.of());
    private final List<TourStep> fullTour;
    private final List<NewFeature> features;
    private final Select<String> account = new Select<>();
    private final RadioButtonGroup<Memory> memory = new RadioButtonGroup<>();
    private final Select<String> seenVersion = new Select<>();
    private final ValueSignal<String> status = new ValueSignal<>("");
    private List<NewFeature> badged = List.of();
    private boolean showingSeenVersion;
    private int openCount;

    public AppTourView(SeenVersions seenVersions) {
        this.seenVersions = seenVersions;

        add(new H1("UC1 — App tour"));
        add(new Paragraph("A tour has to fit the user. A new user gets the "
                + "full tour; a user who has seen it gets none; after an "
                + "update, a returning user sees only what changed. Anyone "
                + "can take the tour again from the Help menu. The app "
                + "remembers the last version whose tour the user saw. Use "
                + "the controls to change the account, where that is "
                + "remembered, or the version seen, and open the app again."));

        DemoApp app = new DemoApp();
        fullTour = List.of(
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
                        "Change your profile and settings, or sign out."));
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
            showBadges(List.of());
            storeSeenVersion(CURRENT_VERSION);
            showSeenVersion(CURRENT_VERSION);
            status.set(who() + " has seen the tour of " + CURRENT_VERSION
                    + " now.");
        });

        MenuBar help = new MenuBar();
        help.addThemeVariants(MenuBarVariant.TERTIARY);
        SubMenu helpMenu = help.addItem("Help").getSubMenu();
        helpMenu.addItem("Take the tour", e -> {
            // A pending answer from local storage must not replace this tour.
            openCount++;
            showBadges(List.of());
            tour.setSteps(fullTour);
            tour.start();
        });
        MenuItem resume = helpMenu.addItem("Resume the tour",
                e -> tour.startAt(tour.state().peek().step()));
        Signal.effect(this, () -> resume.setEnabled(
                tour.state().get().status() == Tour.Status.SKIPPED));
        app.addToHeader(help);

        account.setLabel("Signed in as");
        account.setItems("Alex", "Sam");
        account.setValue("Alex");
        memory.setLabel("Remember the version seen");
        memory.setItems(Memory.values());
        memory.setItemLabelGenerator(m -> m.label);
        memory.setValue(Memory.ACCOUNT);
        seenVersion.setLabel("Last version seen");
        seenVersion.setItems(NEVER, "2.0", "2.1", CURRENT_VERSION);
        seenVersion.setItemLabelGenerator(v -> NEVER.equals(v) ? "Never" : v);
        seenVersion.setValue(NEVER);
        // Changing the account or the memory is like signing in again.
        account.addValueChangeListener(e -> openApp());
        memory.addValueChangeListener(e -> openApp());
        seenVersion.addValueChangeListener(e -> {
            if (!showingSeenVersion) {
                storeSeenVersion(
                        NEVER.equals(e.getValue()) ? null : e.getValue());
                openApp();
            }
        });

        Span statusLine = new Span();
        statusLine.addClassName("tour-status");
        statusLine.bindText(status);
        Button reopen = new Button("Open the app again", e -> openApp());
        Div settings = new Div(account, memory, seenVersion, reopen);
        settings.addClassName("tour-settings");

        add(settings, statusLine, app);
        add(new BuiltWith("One stored version decides which tour opens").point(
                "The app keeps the last version whose tour the user saw. None: "
                        + "the full tour. An older one: only the features added since. "
                        + "The current one: no tour.")
                .point("Per account, an application-wide bean keeps it. Per browser, "
                        + "WebStorage keeps it in local storage; reading it is asynchronous, "
                        + "so the tour starts in the callback.")
                .point("Finishing or skipping the tour stores the current version. The "
                        + "help menu starts the full tour, or resumes a skipped tour.")
                .java("uc1/AppTourView").java("uc1/NewFeature")
                .java("uc1/SeenVersions").java("tour/Tour")
                .java("tour/TourPopover"));

        whenAttached(ui -> {
            openApp();
            return tour::reset;
        });
    }

    private void openApp() {
        tour.reset();
        showBadges(List.of());
        int open = ++openCount;
        if (memory.getValue() == Memory.ACCOUNT) {
            decide(seenVersions.get(account.getValue()));
        } else {
            status.set("Asking the browser which version it has seen…");
            WebStorage.getItem(STORAGE_KEY, value -> {
                // Ignore an answer that comes after the app was opened again.
                if (open == openCount) {
                    decide(value);
                }
            });
        }
    }

    private void decide(@Nullable String seen) {
        showSeenVersion(seen == null ? NEVER : seen);
        if (seen == null) {
            status.set(who() + " has never seen the tour, so the full tour "
                    + "opens.");
            tour.setSteps(fullTour);
            tour.start();
            return;
        }
        List<NewFeature> news = NewFeature.since(features, seen);
        if (news.isEmpty()) {
            status.set(who() + " has seen the tour of " + seen
                    + ", so no tour opens.");
            return;
        }
        status.set(who() + " last saw " + seen + ", so the tour shows only "
                + "what is new since then.");
        showBadges(news);
        tour.setSteps(news.stream()
                .map(f -> TourStep.of(f.target(), f.title(), f.text()))
                .toList());
        tour.start();
    }

    private void storeSeenVersion(@Nullable String version) {
        if (memory.getValue() == Memory.ACCOUNT) {
            seenVersions.set(account.getValue(), version);
        } else if (version == null) {
            WebStorage.removeItem(STORAGE_KEY);
        } else {
            WebStorage.setItem(STORAGE_KEY, version);
        }
    }

    /** Shows the given version in the select without storing it again. */
    private void showSeenVersion(String version) {
        showingSeenVersion = true;
        seenVersion.setValue(version);
        showingSeenVersion = false;
    }

    private void showBadges(List<NewFeature> news) {
        badged.forEach(
                f -> f.target().getElement().getClassList().remove(NEW_BADGE));
        badged = news;
        badged.forEach(
                f -> f.target().getElement().getClassList().add(NEW_BADGE));
    }

    private String who() {
        return memory.getValue() == Memory.ACCOUNT ? account.getValue()
                : "This browser";
    }
}
