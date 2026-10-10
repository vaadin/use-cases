package com.example.uc7;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;
import com.example.views.TestsNote;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.geolocation.Geolocation;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.PageVisibility;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.Signal;

/**
 * UC7 — Browser APIs in tests.
 * <p>
 * A delivery screen that depends on the browser: the courier's position
 * (geolocation), the user's language and time zone for the delivery window, and
 * whether the tab is visible (live tracking pauses in a background tab).
 * <p>
 * Each of these comes from the browser, so a browserless test has to play the
 * browser's part. How well that works differs per API: geolocation has a proper
 * simulator, the locale can simply be set, page visibility can be faked with
 * the DOM event the browser would send, and the time zone cannot be set at all.
 * The tests show each case.
 */
@Route(value = "uc7", layout = MainLayout.class)
@PageTitle("UC7 — Browser APIs in tests")
@UseCaseDescription("Simulating geolocation, locale, time zone and page visibility in view tests")
@Menu(order = 7, title = "UC7 — Browser APIs in tests")
@AnonymousAllowed
public class BrowserApisView extends VerticalLayout {

    static final ZonedDateTime DELIVERY = ZonedDateTime
            .parse("2026-03-05T14:00:00Z");

    private final Span position = new Span("Position not requested");
    private final Span window = new Span();
    private final Span tracking = new Span();

    public BrowserApisView() {
        UI ui = UI.getCurrent();
        add(new H1("UC7 — Browser APIs in tests"));
        add(new Paragraph("Locate the courier, switch to another tab and "
                + "back: tracking pauses while the tab is hidden. The "
                + "delivery window is shown in your language and time zone."));

        Button locate = new Button("Locate courier",
                e -> Geolocation.getPosition(
                        pos -> position.setText("Courier at %.4f, %.4f"
                                .formatted(pos.coords().latitude(),
                                        pos.coords().longitude())),
                        error -> position.setText(
                                "Location unavailable: " + error.errorCode())));

        ZoneId zone = ui.getPage().getExtendedClientDetails().getZoneId();
        window.bindText(ui.localeSignal()
                .map(locale -> "Delivery "
                        + DateTimeFormatter
                                .ofLocalizedDateTime(FormatStyle.MEDIUM,
                                        FormatStyle.SHORT)
                                .withLocale(locale)
                                .format(DELIVERY.withZoneSameInstant(zone))
                        + " (" + zone.getId() + ")"));

        Signal<PageVisibility> visibility = ui.getPage().pageVisibilitySignal();
        tracking.bindText(visibility.map(state -> switch (state) {
        case HIDDEN -> "Live tracking paused (tab hidden)";
        case UNKNOWN -> "Live tracking starting";
        default -> "Live tracking on";
        }));

        UnorderedList facts = new UnorderedList(new ListItem(locate, position),
                new ListItem(window), new ListItem(tracking));
        Div sample = new Div(facts);
        sample.addClassName("sample");
        add(sample, new TestsNote(
                "View test: uc7/BrowserApisViewTest (browserless)"));
    }

    // Package-private test seams.
    String position() {
        return position.getText();
    }

    String window() {
        return window.getText();
    }

    String tracking() {
        return tracking.getText();
    }
}
