package com.example.uc1;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.wakelock.WakeLock;
import com.vaadin.flow.component.wakelock.WakeLockAvailability;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC1 — Keep-awake toggle.
 * <p>
 * A single button switches between <em>Keep screen awake</em> and <em>Allow
 * screen to sleep</em>. The button follows what the user asked for; the badge
 * next to it is bound to {@link WakeLock#activeSignal()}, so the user can see
 * when the browser actually holds the lock. That can lag the click, and never
 * happens where {@link WakeLock#availabilitySignal()} reports
 * {@link WakeLockAvailability#UNSUPPORTED} (a browser without the API, or a
 * page served over plain HTTP), in which case the button is disabled.
 * <p>
 * The lock belongs to the browser tab, not to this view, so the view releases
 * it when the user leaves.
 */
@Route(value = "uc1", layout = MainLayout.class)
@PageTitle("UC1 — Keep-awake toggle")
@UseCaseDescription("Letting users switch keep-screen-awake on and off")
@Menu(order = 1, title = "UC1 — Keep-awake toggle")
public class ManualToggleView extends VerticalLayout {

    // Whether the user asked to keep the screen awake. WakeLock keeps this
    // flag only in the browser (API-GAPS.md #7), so the view keeps its own.
    private final ValueSignal<Boolean> wanted = new ValueSignal<>(
            Boolean.FALSE);

    public ManualToggleView() {
        add(new H1("UC1 — Keep-awake toggle"));
        add(new Paragraph("Use the button to request or release a screen "
                + "wake lock. The badge shows what the browser reports. "
                + "Open this page on a phone or laptop, click the button, "
                + "and the screen does not dim until you click again or "
                + "leave this page."));

        Signal<Boolean> active = WakeLock.activeSignal();
        Signal<WakeLockAvailability> availability = WakeLock
                .availabilitySignal();

        Span statusBadge = new Span();
        statusBadge.addClassName("status-badge");
        statusBadge
                .bindText(Signal.computed(() -> statusText(availability.get(),
                        Boolean.TRUE.equals(wanted.get()),
                        Boolean.TRUE.equals(active.get()))));
        statusBadge.bindClassName("active", active);

        Button toggleButton = new Button();
        toggleButton.bindText(
                wanted.map(w -> Boolean.TRUE.equals(w) ? "Allow screen to sleep"
                        : "Keep screen awake"));
        toggleButton.bindEnabled(
                availability.map(a -> a != WakeLockAvailability.UNSUPPORTED));
        toggleButton.addClickListener(e -> {
            if (Boolean.TRUE.equals(wanted.peek())) {
                WakeLock.release();
                wanted.set(Boolean.FALSE);
            } else {
                WakeLock.request();
                wanted.set(Boolean.TRUE);
            }
        });

        HorizontalLayout controls = new HorizontalLayout(toggleButton,
                statusBadge);
        controls.setAlignItems(Alignment.CENTER);
        controls.setWrap(true);
        add(controls);

        whenAttached(ui -> () -> {
            if (Boolean.TRUE.equals(wanted.peek())) {
                WakeLock.release(ui);
                wanted.set(Boolean.FALSE);
            }
        });
    }

    private static String statusText(WakeLockAvailability availability,
            boolean wanted, boolean active) {
        if (active) {
            return "Holding the lock — the screen stays on";
        }
        if (availability == WakeLockAvailability.UNSUPPORTED) {
            return "Not available — needs a supporting browser and HTTPS";
        }
        return wanted ? "Requested — waiting for the browser"
                : "Released — the screen may sleep";
    }
}
