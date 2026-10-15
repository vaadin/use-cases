package com.example.uc13;

import com.example.collab.Outcomes;
import com.example.collab.Peer;
import com.example.collab.PeerRig;
import com.example.collab.UseCaseView;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

/**
 * UC13 — Know if a write went through. Beyond the sampler.
 * <p>
 * Collaboration Kit's writes are fire-and-forget: a {@code put} on a map
 * returns nothing a view can wait on. A shared signal write returns a
 * {@code SignalOperation} whose result says whether the shared state accepted
 * it, so a view can tell its user "saved" only once that is true.
 * <p>
 * Two details make that harder than it looks, and the view handles both:
 * <ul>
 * <li>Inside the click handler the write has been submitted, not applied — it
 * is applied when the session flushes its pending signal commands, after the
 * handler returns. So the status says <em>Saving…</em> in the handler and is
 * set to the outcome from a callback ({@link Outcomes}, API-GAPS.md #14). In a
 * single-server deployment the flush happens before the response, so a user
 * never actually sees <em>Saving…</em>; with a slower shared state they would
 * (API-GAPS.md #13).</li>
 * <li>A rule on the shared state — here, at most {@value PendingTopic#LIMIT}
 * characters — does not come back as a failed result. It throws at submit time,
 * so the refusal is handled in a different place from every other outcome
 * (API-GAPS.md #11).</li>
 * </ul>
 */
@Route(value = "pending", layout = MainLayout.class)
@RouteAlias(value = "uc13", layout = MainLayout.class)
@PageTitle("UC13 — Know if a write went through")
@UseCaseDescription("Telling the user whether their change was accepted")
@Menu(order = 13, title = "UC13 — Know if a write went through")
public class PendingStateView extends UseCaseView {

    public PendingStateView(PendingTopic topic) {
        super("UC13 — Know if a write went through",
                "Save a new shelf label in any window. The window tells you whether the shared state accepted "
                        + "it, rather than assuming so because you clicked. A label longer than "
                        + PendingTopic.LIMIT
                        + " characters is refused by a rule on the shared state: you see Not saved, and the "
                        + "other windows keep the old label.");

        add(new PeerRig(peer -> peerView(topic, peer)));

        addNote("Whether a write was accepted is only known after the click handler returns, when the session "
                + "applies its signal commands, so the status is set from a callback on the operation "
                + "(API-GAPS.md #14). A rule on the shared state refuses a write by throwing instead of returning "
                + "a failed result, so that outcome is handled separately (API-GAPS.md #11).");
    }

    private static Component peerView(PendingTopic topic, Peer peer) {
        var label = topic.label();
        var guarded = topic.labelFor(peer);

        Span current = new Span();
        current.bindText(label.map(value -> "Shelf label: " + value));

        TextField draft = new TextField("New label");
        draft.setValue(label.peek());

        Span status = new Span();
        status.addClassName("save-status");

        Button save = new Button("Save", event -> {
            String value = draft.getValue();
            try {
                // Submitted, not yet applied: the outcome is not known here.
                status.setText("Saving…");
                Outcomes.whenDecided(guarded.set(value),
                        accepted -> status.setText(accepted ? "Saved: " + value
                                : "Not saved: the shared state refused it"));
            } catch (UnsupportedOperationException refused) {
                // A validator does not reject the operation, it refuses to
                // submit it at all.
                status.setText("Not saved: a label can be at most "
                        + PendingTopic.LIMIT + " characters");
            }
        });

        HorizontalLayout actions = new HorizontalLayout(save, status);
        actions.setAlignItems(Alignment.CENTER);

        return new Div(current, draft, actions);
    }
}
