package com.example.uc1;

import com.example.views.MainLayout;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.wakelock.WakeLock;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;

/**
 * UC1 — Manual keep-awake toggle.
 * <p>
 * The simplest possible exercise of the API: a single button flips between
 * <em>Keep screen awake</em> and <em>Allow screen to sleep</em>. The badge next
 * to it is bound to {@link WakeLock#activeSignal()} so the user can see exactly
 * when the browser actually grants or drops the lock — which may lag the click,
 * or never happen at all on an insecure origin or an unsupported browser.
 */
@Route(value = "uc1", layout = MainLayout.class)
@Menu(order = 1, title = "UC1 — Manual toggle")
public class ManualToggleView extends VerticalLayout {

    private final Span statusBadge = new Span();
    private final Button toggleButton = new Button();

    public ManualToggleView() {
        add(new H1("UC1 — Manual keep-awake toggle"));
        add(new Paragraph("Use the button to request or release a screen "
                + "wake lock. The badge reflects the actual lock state "
                + "reported by the browser. Open this page on a phone or "
                + "laptop, click the button, then watch the screen no "
                + "longer dim until you toggle it back off."));

        statusBadge.addClassName("status-badge");
        toggleButton.setText("Keep screen awake");

        HorizontalLayout controls = new HorizontalLayout(toggleButton,
                statusBadge);
        controls.setAlignItems(Alignment.CENTER);
        add(controls);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        Signal<Boolean> active = WakeLock.activeSignal();

        statusBadge.bindText(active.map(held -> Boolean.TRUE.equals(held)
                ? "Holding lock — screen " + "will stay on"
                : "Released — screen may sleep"));
        statusBadge.bindClassName("active", active);

        toggleButton.bindText(active
                .map(held -> Boolean.TRUE.equals(held) ? "Allow screen to sleep"
                        : "Keep screen awake"));

        toggleButton.addClickListener(e -> {
            if (Boolean.TRUE.equals(active.peek())) {
                WakeLock.release();
            } else {
                WakeLock.request();
            }
        });
    }
}
