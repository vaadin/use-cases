package com.example.uc24;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.example.views.MainLayout;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.trigger.internal.ClickTrigger;
import com.vaadin.flow.component.trigger.internal.SetPropertyAction;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC24 — Disable a group of buttons immediately on click.
 * <p>
 * Three mutually exclusive actions on one request: Approve, Reject, Escalate.
 * Clicking any of them must lock out all three before the server has even seen
 * the click, otherwise a fast second click can submit a conflicting decision
 * while the first one is still in flight.
 * <p>
 * {@link Button#setDisableOnClick(boolean)} covers only the clicked button. The
 * siblings are handled by one {@link ClickTrigger} per button that fans out
 * {@link SetPropertyAction SetPropertyAction(sibling, "disabled", true)} to the
 * other two — so the whole group is disabled in the same browser event, with no
 * round-trip. The server then mirrors that state with
 * {@code setEnabled(false)}, runs the slow decision in the background, and
 * re-enables the group through Push.
 * <p>
 * The server-side mirror is required: a property written by an action is not
 * synced back, so without it the server still believes the siblings are enabled
 * and the final {@code setEnabled(true)} would be a no-op (see API-GAPS.md).
 */
@Route(value = "uc24", layout = MainLayout.class)
@PageTitle("UC24 — Disable a button group on click")
@Menu(order = 24, title = "UC24 — Disable button group")
@StyleSheet("uc24.css")
public class DisableButtonGroupView extends VerticalLayout {

    static final long PROCESSING_MILLIS = 1500;

    private final List<Button> group;
    private final Span status;

    public DisableButtonGroupView() {
        addClassName("uc24-view");
        add(new H1("UC24 — Disable a button group on click"));
        add(new Paragraph(
                "Click one decision. All three buttons disable in the same "
                        + "browser event — setDisableOnClick handles the "
                        + "clicked one, a ClickTrigger fans SetPropertyAction"
                        + "(disabled) out to its two siblings — so a quick "
                        + "second click can't send a conflicting decision. "
                        + "The server takes a moment to process, then "
                        + "re-enables the group via Push."));

        Button approve = new Button("Approve");
        approve.setId("approve");
        approve.addThemeVariants(ButtonVariant.PRIMARY);

        Button reject = new Button("Reject");
        reject.setId("reject");
        reject.addThemeVariants(ButtonVariant.ERROR);

        Button escalate = new Button("Escalate");
        escalate.setId("escalate");

        group = List.of(approve, reject, escalate);

        status = new Span("Request #1042 is waiting for a decision.");
        status.setId("status");
        status.addClassName("status");

        wire(approve, "Approved", reject, escalate);
        wire(reject, "Rejected", approve, escalate);
        wire(escalate, "Escalated", approve, reject);

        HorizontalLayout buttons = new HorizontalLayout(approve, reject,
                escalate);
        buttons.addClassName("decisions");
        add(buttons, status);
    }

    private void wire(Button self, String outcome, Button sibling1,
            Button sibling2) {
        // The clicked button: built-in, and the server knows about it.
        self.setDisableOnClick(true);

        // The siblings: disabled client-side in the same click event.
        new ClickTrigger(self).triggers(
                new SetPropertyAction<>(sibling1, "disabled", true),
                new SetPropertyAction<>(sibling2, "disabled", true));

        self.addClickListener(event -> {
            // Mirror what the client already shows so the later
            // setEnabled(true) actually reaches the browser.
            group.forEach(button -> button.setEnabled(false));
            status.setText("Processing \"" + self.getText() + "\"…");

            UI ui = event.getSource().getUI().orElseThrow();
            CompletableFuture.runAsync(() -> {
                // Stand-in for a slow backend call
            }, CompletableFuture.delayedExecutor(PROCESSING_MILLIS,
                    TimeUnit.MILLISECONDS)).thenRun(ui.accessLater(() -> {
                        status.setText("Request #1042: " + outcome
                                + ". Pick again to change the decision.");
                        group.forEach(button -> button.setEnabled(true));
                    }, null));
        });
    }
}
