package com.example.uc13;

import com.example.collab.Outcomes;
import com.example.collab.Peer;
import com.example.collab.PeerRig;
import com.example.collab.UseCaseView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.signals.operations.SignalOperation;

/**
 * UC13 — Show pending versus confirmed state. Beyond the sampler.
 * <p>
 * A shared signal can be read two ways: {@code get()} / {@code peek()} is what
 * this client believes, including writes it has submitted, and
 * {@code peekConfirmed()} is what the shared state has actually accepted. The
 * two differ for a real and easily missed reason — inside a UI callback a
 * command has been <em>submitted</em>, not applied. It is applied when the
 * session flushes its pending signal commands, which happens after the callback
 * returns.
 * <p>
 * So the interesting moment is inside the click handler, and that is what this
 * view records: the two readings before the write, the two readings immediately
 * after submitting it — where they disagree — and the outcome once the shared
 * state has decided.
 * <p>
 * The same detail is why {@code operation.result().getNow(null)} is never
 * useful in a UI: in the handler the answer is always "not yet". A view that
 * wants to report a rejection has to attach a callback and come back later,
 * which is what {@link Outcomes} does. See API-GAPS.md #14.
 * <p>
 * Rejection is the other half. Writing more than {@value PendingTopic#LIMIT}
 * characters goes through a handle whose validator refuses it — and that does
 * not come back as a rejected operation at all, it throws at submit time. See
 * API-GAPS.md #11.
 */
@Route(value = "pending", layout = MainLayout.class)
@RouteAlias(value = "uc13", layout = MainLayout.class)
@PageTitle("UC13 — Pending versus confirmed state")
@Menu(order = 13, title = "UC13 Pending vs confirmed")
public class PendingStateView extends UseCaseView {

    public PendingStateView(PendingTopic topic) {
        super("UC13 — Show pending versus confirmed state",
                "Write a new label in any panel. The readout records what this client believed and what the shared "
                        + "state had confirmed at three points: before the write, in the instant after submitting "
                        + "it, and once it landed. Labels longer than "
                        + PendingTopic.LIMIT
                        + " characters are refused by the shared state itself.");

        add(new PeerRig(peer -> peerView(topic, peer)));

        addNote("The middle line is the point: immediately after set() the client's own reading has changed and "
                + "the confirmed reading has not, because the command is only applied when the session flushes. "
                + "That is also why an operation's result is unavailable in the handler that submitted it "
                + "(API-GAPS.md #14) — and why a validator's refusal, which is thrown at submit time instead, "
                + "is the odd one out (API-GAPS.md #11).");
    }

    private static Component peerView(PendingTopic topic, Peer peer) {
        var label = topic.label();
        var guarded = topic.labelFor(peer);

        Span current = new Span();
        current.bindText(label.map(value -> "Shared label: " + value));

        TextField draft = new TextField("New label");
        draft.setValue(label.peek());

        // Three fixed lines rather than a log, because the three readings of
        // one write are a sequence and a newest-first log would print them
        // backwards.
        Span before = new Span("before: —");
        Span submitted = new Span("submitted: —");
        Span outcome = new Span("outcome: —");

        Button write = new Button("Write", event -> {
            before.setText("before: get() " + label.peek()
                    + " / peekConfirmed() " + label.peekConfirmed());
            try {
                SignalOperation<String> operation = guarded
                        .set(draft.getValue());
                // Read again straight away, still inside the handler: the
                // command has been submitted and not yet applied, so these two
                // readings disagree.
                submitted.setText("submitted: get() " + label.peek()
                        + " / peekConfirmed() " + label.peekConfirmed());
                outcome.setText("outcome: waiting for the flush");
                Outcomes.report("outcome", operation, outcome::setText);
            } catch (UnsupportedOperationException rejected) {
                // A validator does not reject the operation, it refuses to
                // submit it at all.
                submitted.setText("submitted: nothing was submitted");
                outcome.setText(
                        "outcome: refused at submit time by the validator (at most "
                                + PendingTopic.LIMIT + " characters)");
            }
        });

        Div readout = new Div(before, submitted, outcome);
        readout.addClassName("event-log");
        readout.getChildren().forEach(
                child -> child.getElement().getStyle().set("display", "block"));

        return new Div(current, draft, write, readout);
    }
}
