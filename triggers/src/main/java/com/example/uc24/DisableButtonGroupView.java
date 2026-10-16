package com.example.uc24;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;
import org.springframework.scheduling.TaskScheduler;

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
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC24 — Disable a button group on click.
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
 * round-trip. The server then sets a {@code processing} signal, an effect
 * mirrors it onto the enabled state of all three buttons, and the slow decision
 * runs on the {@link TaskScheduler}. When it finishes, it clears the signal and
 * Push re-enables the group.
 * <p>
 * The server-side mirror is required: a property written by an action is not
 * synced back, so without it the server still believes the siblings are enabled
 * and the final {@code setEnabled(true)} would be a no-op. The buttons can't
 * use {@code bindEnabled} for it, because Flow doesn't allow a signal binding
 * on the enabled state together with disable-on-click (see API-GAPS.md).
 */
@Route(value = "uc24", layout = MainLayout.class)
@PageTitle("UC24 — Disable button group")
@UseCaseDescription("Disabling a whole group of buttons in the browser as soon as one is clicked")
@Menu(order = 24, title = "UC24 — Disable button group")
@StyleSheet("uc24.css")
public class DisableButtonGroupView extends VerticalLayout {

    static final Duration PROCESSING_TIME = Duration.ofMillis(1500);

    private final TaskScheduler scheduler;
    private final ValueSignal<Boolean> processing = new ValueSignal<>(false);
    private final ValueSignal<String> status = new ValueSignal<>(
            "Request #1042 is waiting for a decision.");

    public DisableButtonGroupView(TaskScheduler scheduler) {
        this.scheduler = scheduler;

        addClassName("uc24-view");
        add(new H1("UC24 — Disable button group"));
        add(new Paragraph("Click one decision. All three buttons are disabled "
                + "in the same browser event, so a quick second click can't "
                + "send a conflicting decision. setDisableOnClick disables "
                + "the clicked button, and a ClickTrigger runs "
                + "SetPropertyAction(disabled) on the other two. The server "
                + "takes a moment to process, then enables the group again "
                + "via Push."));

        Button approve = new Button("Approve");
        approve.setId("approve");
        approve.addThemeVariants(ButtonVariant.PRIMARY);

        Button reject = new Button("Reject");
        reject.setId("reject");
        reject.addThemeVariants(ButtonVariant.ERROR);

        Button escalate = new Button("Escalate");
        escalate.setId("escalate");

        wire(approve, "Approved", reject, escalate);
        wire(reject, "Rejected", approve, escalate);
        wire(escalate, "Escalated", approve, reject);

        List<Button> group = List.of(approve, reject, escalate);
        // Mirror what the browser already shows, so that enabling the group
        // later is a real change that reaches the browser.
        Signal.effect(this, () -> {
            boolean enabled = !processing.get();
            group.forEach(button -> button.setEnabled(enabled));
        });

        Span statusText = new Span();
        statusText.setId("status");
        statusText.addClassName("status");
        statusText.bindText(status);

        HorizontalLayout buttons = new HorizontalLayout(approve, reject,
                escalate);
        buttons.addClassName("decisions");
        buttons.setWrap(true);
        add(buttons, statusText);
    }

    private void wire(Button self, String outcome, Button sibling1,
            Button sibling2) {
        // The clicked button: built-in, and the server knows about it.
        self.setDisableOnClick(true);

        // The siblings: disabled in the browser in the same click event.
        new ClickTrigger(self).triggers(
                new SetPropertyAction<>(sibling1, "disabled", true),
                new SetPropertyAction<>(sibling2, "disabled", true));

        self.addClickListener(event -> {
            processing.set(true);
            status.set("Processing \"" + self.getText() + "\"…");
            // Stand-in for a slow backend call
            scheduler.schedule(() -> {
                status.set("Request #1042: " + outcome
                        + ". Pick again to change the decision.");
                processing.set(false);
            }, Instant.now().plus(PROCESSING_TIME));
        });
    }
}
