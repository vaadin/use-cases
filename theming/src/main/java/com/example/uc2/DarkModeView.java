package com.example.uc2;

import com.example.Appearance;
import com.example.MissingAPI;
import com.example.common.UseCaseDescription;
import com.example.views.ComponentShowcase;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;

/**
 * UC2 — Dark mode.
 * <p>
 * By default the application follows the operating system: light during the
 * day, dark at night if the user set it up that way. A switch lets the user
 * override it, and the choice is remembered for the next visit. The view shows
 * what the system prefers, which the server only learns through a media query
 * the browser reports back.
 * <p>
 * The components switch on their own; the application's own CSS has to
 * cooperate. The two cards show why: one hard-codes white and dark grey and
 * stays light, the other uses theme tokens and follows along.
 */
@Route(value = "uc2", layout = MainLayout.class)
@PageTitle("UC2 — Dark mode")
@UseCaseDescription("Following the system's dark mode and letting the user override it")
@Menu(order = 2, title = "UC2 — Dark mode")
@StyleSheet("uc2.css")
public class DarkModeView extends VerticalLayout {

    static final String DARK_QUERY = "(prefers-color-scheme: dark)";

    private final RadioButtonGroup<ColorScheme.Value> scheme = new RadioButtonGroup<>(
            "Appearance");
    private final Span status = new Span();

    public DarkModeView() {
        Appearance appearance = Appearance.current();
        addClassName("uc2-view");

        add(new H1("UC2 — Dark mode"));
        add(new Paragraph("Pick light or dark, or follow your system. Change "
                + "the system setting while \"Same as the system\" is "
                + "picked and the app follows at once. Reload: your choice "
                + "is remembered."));

        scheme.setItems(ColorScheme.Value.LIGHT, ColorScheme.Value.DARK,
                ColorScheme.Value.SYSTEM);
        scheme.setItemLabelGenerator(value -> switch (value) {
        case LIGHT -> "Light";
        case DARK -> "Dark";
        default -> "Same as the system";
        });
        scheme.bindValue(appearance.colorScheme(), value -> {
            if (value != null) {
                appearance.setColorScheme(value);
            }
        });

        Signal<@Nullable Boolean> systemDark = MissingAPI.mediaQuery(this,
                DARK_QUERY);
        status.addClassName("status");
        status.bindText(Signal.computed(() -> {
            Boolean dark = systemDark.get();
            String system = dark == null ? "not known yet"
                    : dark ? "dark" : "light";
            String showing = switch (appearance.colorScheme().get()) {
            case LIGHT -> "light";
            case DARK -> "dark";
            default -> dark == null ? "the system's" : system;
            };
            return "System prefers " + system + " · showing " + showing;
        }));

        Div cards = new Div(
                card("hardcoded", "Hard-coded colors",
                        "background: #fff; color: #222"),
                card("tokens", "Theme tokens",
                        "background: var(--vaadin-background-container);"
                                + " color: var(--vaadin-text-color)"));
        cards.addClassName("cards");

        add(scheme, status, cards, new ComponentShowcase());
    }

    private static Div card(String kind, String title, String css) {
        Div card = new Div(new H3(title), new Paragraph(css));
        card.addClassNames("card", kind);
        return card;
    }

    // Package-private test seams.
    RadioButtonGroup<ColorScheme.Value> scheme() {
        return scheme;
    }

    String status() {
        return status.getText();
    }
}
