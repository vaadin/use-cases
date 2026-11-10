package com.example.uc2;

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
import com.vaadin.flow.component.page.WebStorage;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC2 — Show it only once.
 * <p>
 * The tour opens only for someone who has not finished or skipped it yet. The
 * view remembers that in one of two places: per account on the server (the
 * {@link SeenTours} bean), so the tour does not come back on another device, or
 * per browser in its local storage ({@link WebStorage}), which works without an
 * account but forgets the user on another browser. Switching the account shows
 * the difference: per account, a new account gets the tour; per browser, it
 * does not.
 */
@Route(value = "uc2", layout = MainLayout.class)
@PageTitle("UC2 — Show it only once")
@UseCaseDescription("Remembering that a user has seen the tour, per account or per browser")
@Menu(order = 2, title = "UC2 — Show it only once")
public class ShowOnceView extends VerticalLayout {

    static final String STORAGE_KEY = "walkthrough-uc2-tour-seen";

    enum Memory {
        ACCOUNT("Per account, on the server"),
        BROWSER("Per browser, in local storage");

        private final String label;

        Memory(String label) {
            this.label = label;
        }
    }

    private final SeenTours seenTours;
    private final Tour tour;
    private final Select<String> account = new Select<>();
    private final RadioButtonGroup<Memory> memory = new RadioButtonGroup<>();
    private final ValueSignal<String> status = new ValueSignal<>("");

    public ShowOnceView(SeenTours seenTours) {
        this.seenTours = seenTours;

        add(new H1("UC2 — Show it only once"));
        add(new Paragraph("A tour that opens every time quickly annoys. "
                + "This page opens the tour only for someone who has not "
                + "finished or skipped it yet. Finish or skip it, then press "
                + "\"Open the app again\": the tour stays closed. Switch the "
                + "account to see where the choice is remembered."));

        DemoApp app = new DemoApp();
        tour = new Tour(List.of(
                TourStep.of(app.navigation(), "Find your way",
                        "Move between the parts of the app here."),
                TourStep.of(app.search(), "Search",
                        "Find any order by its number or customer."),
                TourStep.of(app.newOrder(), "Create an order",
                        "Start a new order from anywhere in the app.")));
        tour.addEndListener(state -> rememberSeen());

        account.setLabel("Signed in as");
        account.setItems("Alex", "Sam");
        account.setValue("Alex");
        memory.setLabel("Remember that the tour was seen");
        memory.setItems(Memory.values());
        memory.setItemLabelGenerator(m -> m.label);
        memory.setValue(Memory.ACCOUNT);
        // Changing either is like signing in again.
        account.addValueChangeListener(e -> openApp());
        memory.addValueChangeListener(e -> openApp());

        Span statusLine = new Span();
        statusLine.addClassName("tour-status");
        statusLine.bindText(status);
        Button reopen = new Button("Open the app again", e -> openApp());
        Button forget = new Button("Forget that it was seen", e -> forget());
        Div settings = new Div(account, memory);
        settings.addClassName("tour-settings");
        Div controls = new Div(statusLine, reopen, forget);
        controls.addClassName("tour-controls");

        add(settings, controls, app);
        add(new BuiltWith("A flag checked before the tour starts").point(
                "Per account: an application-wide bean keeps the accounts that "
                        + "have seen the tour; a real app keeps it with the user's profile.")
                .point("Per browser: WebStorage reads and writes the browser's local "
                        + "storage. Reading is asynchronous, so the tour starts in the callback.")
                .point("The tour's end listener sets the flag, whether the user "
                        + "finished the tour or skipped it.")
                .java("uc2/ShowOnceView").java("uc2/SeenTours"));

        whenAttached(ui -> {
            openApp();
            return tour::reset;
        });
    }

    private void openApp() {
        tour.reset();
        String user = account.getValue();
        if (memory.getValue() == Memory.ACCOUNT) {
            if (seenTours.hasSeen(user)) {
                status.set(user + " has seen the tour, so it stays closed.");
            } else {
                status.set(user + " has not seen the tour yet.");
                tour.start();
            }
        } else {
            status.set("Asking the browser whether the tour was seen…");
            WebStorage.getItem(STORAGE_KEY, value -> {
                if (value == null) {
                    status.set("This browser has not seen the tour yet.");
                    tour.start();
                } else {
                    status.set("This browser has seen the tour, so it stays "
                            + "closed — for any account.");
                }
            });
        }
    }

    private void rememberSeen() {
        if (memory.getValue() == Memory.ACCOUNT) {
            seenTours.markSeen(account.getValue());
            status.set(account.getValue() + " has seen the tour now.");
        } else {
            WebStorage.setItem(STORAGE_KEY, "true");
            status.set("This browser has seen the tour now.");
        }
    }

    private void forget() {
        if (memory.getValue() == Memory.ACCOUNT) {
            seenTours.forget(account.getValue());
        } else {
            WebStorage.removeItem(STORAGE_KEY);
        }
        status.set("Forgotten. The tour opens the next time the app opens.");
    }
}
