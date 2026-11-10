package com.example.uc8;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.example.common.BuiltWith;
import com.example.common.UseCaseDescription;
import com.example.demoapp.DemoApp;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverPosition;
import com.vaadin.flow.component.popover.PopoverVariant;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC8 — Hotspots.
 * <p>
 * Instead of a tour that takes over the screen, small pulsing dots sit next to
 * three new features. Clicking a dot opens a hint in a {@link Popover}; "Got
 * it" removes that dot for the rest of the session. The user can ignore the
 * dots and keep working. "Show the hints again" brings them all back.
 */
@Route(value = "uc8", layout = MainLayout.class)
@PageTitle("UC8 — Hotspots")
@UseCaseDescription("Hints next to new features that the user opens when they want")
@Menu(order = 8, title = "UC8 — Hotspots")
public class HotspotsView extends VerticalLayout {

    /** The session attribute holding the titles of the dismissed hints. */
    static final String DISMISSED = HotspotsView.class.getName() + ".dismissed";

    private record Hint(Component feature, String title, String text) {
    }

    private final ValueSignal<Integer> left = new ValueSignal<>(0);
    private final List<Button> dots;
    private final List<Hint> hints;

    public HotspotsView() {
        add(new H1("UC8 — Hotspots"));
        add(new Paragraph("A tour stops the user; a hotspot waits. The "
                + "pulsing dots in the app mark three new features. Click a "
                + "dot to read its hint, and \"Got it\" to remove the dot. "
                + "The app keeps working around them."));

        DemoApp app = new DemoApp();
        hints = List.of(
                new Hint(app.savedFilters(), "Saved filters",
                        "Save a search and open it again with one click."),
                new Hint(app.export(), "Export",
                        "Download the orders as a spreadsheet."),
                new Hint(app.darkMode(), "Dark mode",
                        "Switch to a dark theme for late shifts."));
        dots = hints.stream().map(hint -> {
            Button dot = createHotspot(hint);
            app.addNextTo(hint.feature(), dot);
            return dot;
        }).toList();

        Span count = new Span();
        count.addClassName("tour-status");
        count.bindText(left.map(n -> n == 0 ? "No hints left"
                : n + " of " + hints.size() + " hints left"));
        Button reset = new Button("Show the hints again", e -> {
            dismissed().clear();
            refresh();
        });
        Div controls = new Div(count, reset);
        controls.addClassName("tour-controls");

        add(controls, app);
        add(new BuiltWith("A popover per hotspot").point(
                "Each dot is a small button with its own Popover, which opens "
                        + "on click like any popover and closes on a click outside.")
                .point("\"Got it\" hides the dot and keeps its title in the session, so "
                        + "it stays hidden when the user comes back.")
                .point("The pulse is a CSS animation; it stops for users who prefer "
                        + "reduced motion.")
                .java("uc8/HotspotsView").css("styles.css"));

        refresh();
    }

    private Button createHotspot(Hint hint) {
        Button dot = new Button();
        dot.addClassName("hotspot");
        dot.addThemeVariants(ButtonVariant.TERTIARY);
        dot.setAriaLabel("New: " + hint.title());

        Popover popover = new Popover();
        popover.setTarget(dot);
        popover.setPosition(PopoverPosition.BOTTOM);
        popover.addThemeVariants(PopoverVariant.ARROW);
        popover.setAriaLabel(hint.title());
        Button gotIt = new Button("Got it", e -> {
            popover.close();
            dismissed().add(hint.title());
            refresh();
        });
        gotIt.addThemeVariants(ButtonVariant.PRIMARY);
        Div card = new Div(new H3(hint.title()), new Paragraph(hint.text()),
                gotIt);
        card.addClassName("tour-card");
        popover.add(card);
        return dot;
    }

    private void refresh() {
        Set<String> dismissed = dismissed();
        for (int i = 0; i < hints.size(); i++) {
            dots.get(i).setVisible(!dismissed.contains(hints.get(i).title()));
        }
        left.set((int) hints.stream()
                .filter(hint -> !dismissed.contains(hint.title())).count());
    }

    @SuppressWarnings("unchecked")
    private static Set<String> dismissed() {
        VaadinSession session = VaadinSession.getCurrent();
        Set<String> dismissed = (Set<String>) session.getAttribute(DISMISSED);
        if (dismissed == null) {
            dismissed = new HashSet<>();
            session.setAttribute(DISMISSED, dismissed);
        }
        return dismissed;
    }
}
