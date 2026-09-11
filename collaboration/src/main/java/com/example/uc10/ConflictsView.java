package com.example.uc10;

import com.example.collab.Outcomes;
import com.example.collab.Peer;
import com.example.collab.PeerRig;
import com.example.collab.UseCaseView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.signals.operations.SignalOperation;

/**
 * UC10 — Make concurrent edits provably safe. Beyond the sampler: this is what
 * shared signals have and Collaboration Kit does not.
 * <p>
 * Collaboration Kit's map is last-write-wins. Two users editing the same field
 * from the same starting value produce one value and no complaint, and the user
 * whose edit vanished is never told. The three buttons here are the three
 * answers signals give instead:
 * <ul>
 * <li><strong>Save (set)</strong> — last write wins, on purpose. Fine for a
 * field that belongs to one user.</li>
 * <li><strong>Save (replace)</strong> — a compare-and-set against the value
 * this panel last read. If somebody else wrote in the meantime the operation is
 * <em>rejected</em> and says so, which is the whole point: the edit is not lost
 * quietly, it is refused loudly.</li>
 * <li><strong>+1</strong> — {@code incrementBy} on a number, which cannot lose
 * a concurrent increment because it never sends a total.</li>
 * </ul>
 * <p>
 * The fourth button moves an item between two lists inside
 * {@code Signal.runInTransaction}, with a {@code verifyChild} on the item: if
 * another peer already moved it, nothing happens in either list. A transaction
 * across two collections has no Collaboration Kit equivalent at all.
 * <p>
 * To see a rejection: press <em>Read</em> in two panels, write in both, then
 * save with {@code replace} in both.
 */
@Route(value = "conflicts", layout = MainLayout.class)
@RouteAlias(value = "uc10", layout = MainLayout.class)
@PageTitle("UC10 — Make concurrent edits provably safe")
@Menu(order = 10, title = "UC10 Conflicts & atomicity")
public class ConflictsView extends UseCaseView {

    public ConflictsView(RaceTopic topic) {
        super("UC10 — Make concurrent edits provably safe",
                "Two users editing the same note. Press Read in both panels, change the text in both, then save "
                        + "in both: with set() the second save wins silently, with replace() it is rejected because "
                        + "the value it was based on is no longer there.");

        add(new PeerRig(peer -> peerView(topic, peer)));

        addNote("replace(), incrementBy() and runInTransaction() are the three things Collaboration Kit's "
                + "last-write-wins map cannot express. Every operation returns a SignalOperation whose result "
                + "says whether the shared state accepted it — the log in each panel is that result, not a guess.");
    }

    private static Component peerView(RaceTopic topic, Peer peer) {
        Div log = new Div();
        log.addClassName("event-log");

        Span current = new Span();
        current.bindText(topic.note().map(value -> "Shared note: " + value));

        // What this panel last read. A compare-and-set needs a base value, and
        // the base is the state the user was looking at when they started
        // typing — not whatever the value happens to be at save time.
        ValueSignal<String> base = new ValueSignal<>(topic.note().peek());

        TextField draft = new TextField("Your edit");
        draft.setValue(topic.note().peek());

        Button read = new Button("Read", event -> {
            String value = topic.note().peek();
            base.set(value);
            draft.setValue(value);
            log(log, "read \"" + value + "\"");
        });

        Button saveSet = new Button("Save (set)", event -> Outcomes.report(log,
                "set", topic.note().set(draft.getValue())));

        Button saveReplace = new Button("Save (replace)",
                event -> Outcomes.report(log, "replace",
                        topic.note().replace(base.peek(), draft.getValue())));

        Span tally = new Span();
        tally.bindText(
                topic.tally().map(value -> "Tally: " + value.intValue()));
        Button increment = new Button("+1", event -> Outcomes.report(log,
                "incrementBy", topic.tally().incrementBy(1)));

        Span lists = new Span();
        lists.bindText(
                Signal.computed(() -> "Backlog " + topic.backlog().get().size()
                        + " / done " + topic.done().get().size()));

        Button move = new Button("Move first backlog item (transaction)",
                event -> Outcomes.report(log, "transaction", moveFirst(topic)));

        Div content = new Div(current, draft,
                new HorizontalLayout(read, saveSet, saveReplace), tally,
                new HorizontalLayout(increment), lists,
                new HorizontalLayout(move), log);
        content.add(new Span("Editing as " + peer.name()));
        return content;
    }

    /**
     * Moves the first backlog item to the done list, or does nothing at all.
     * <p>
     * The {@code verifyChild} is what makes it safe: it adds the condition
     * "this item is still where I found it" to the transaction, so a peer that
     * lost the race removes nothing and appends nothing rather than appending a
     * duplicate.
     */
    private static SignalOperation<Void> moveFirst(RaceTopic topic) {
        return Signal.runInTransaction(() -> {
            var first = topic.backlog().peek().stream().findFirst()
                    .orElse(null);
            if (first == null) {
                return;
            }
            topic.backlog().verifyChild(first);
            String value = first.peek();
            topic.backlog().remove(first);
            topic.done().insertLast(value);
        });
    }

    private static void log(Div log, String line) {
        log.addComponentAsFirst(new Div(line));
    }
}
