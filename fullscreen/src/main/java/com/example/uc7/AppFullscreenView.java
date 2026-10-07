package com.example.uc7;

import com.example.views.MainLayout;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.fullscreen.Fullscreen;
import com.vaadin.flow.component.fullscreen.FullscreenState;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;

/**
 * UC7 — View the whole app fullscreen via
 * {@link Fullscreen#onClick(com.vaadin.flow.component.Component)
 * Fullscreen.onClick(enter).enter()}.
 * <p>
 * With a MainLayout in place, page-level fullscreen really just means "hide the
 * browser's chrome." Nothing interesting reflows: the navigation drawer toggle,
 * the app title, the side nav and this view all stay exactly where they were.
 * The use case is kiosk-style installations and demoing the app itself with no
 * browser UI in the way — there's nothing more elaborate to stage.
 */
@Route(value = "uc7", layout = MainLayout.class)
@Menu(order = 7, title = "UC7 — View app fullscreen")
public class AppFullscreenView extends VerticalLayout {

    private final Span stateBadge = new Span();

    public AppFullscreenView() {
        add(new H1("UC7 — View this app fullscreen"));
        add(new Paragraph(
                "Fullscreen.onClick(enter).enter() asks the browser to "
                        + "fullscreen the whole document. With this app's "
                        + "MainLayout in "
                        + "place, that just means: browser chrome (URL bar, "
                        + "tabs, bookmarks) goes away, but everything inside "
                        + "the document — the drawer toggle, the app title, "
                        + "the side nav, this view — stays exactly where it "
                        + "is. No reflow, no special staging; the use case "
                        + "is kiosk installations and demoing the app itself "
                        + "without browser UI in the way."));

        stateBadge.addClassName("status-badge");

        Button enter = new Button("Enter fullscreen");
        // Fullscreen needs the click's user gesture, so bind the request to the
        // button's click trigger.
        Fullscreen.onClick(enter).enter();
        enter.addThemeVariants(ButtonVariant.PRIMARY);
        Button exit = new Button("Exit fullscreen", e -> Fullscreen.exit());

        add(new HorizontalLayout(enter, exit, stateBadge));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        Signal<FullscreenState> fs = Fullscreen.stateSignal();
        stateBadge.bindText(fs.map(AppFullscreenView::badgeText));
        stateBadge.bindClassName("fullscreen",
                fs.map(s -> s == FullscreenState.FULLSCREEN));
        stateBadge.bindClassName("unsupported",
                fs.map(s -> s == FullscreenState.UNSUPPORTED));
    }

    private static String badgeText(FullscreenState state) {
        // The whole document is fullscreen, so the badge stays on screen —
        // a FULLSCREEN-specific message is actually visible here.
        return switch (state) {
        case FULLSCREEN -> "App is fullscreen — Escape to exit";
        case NOT_FULLSCREEN -> "Click Enter fullscreen to hide browser chrome";
        case UNSUPPORTED -> "Fullscreen not supported in this browser";
        case UNKNOWN -> "Detecting fullscreen support…";
        };
    }
}
