package com.example.uc10;

import com.example.collab.Outcomes;
import com.example.collab.Peer;
import com.example.collab.PeerRig;
import com.example.collab.UseCaseView;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC10 — Make concurrent edits safe. Beyond the sampler: this is what shared
 * signals have and Collaboration Kit does not.
 * <p>
 * Collaboration Kit's map is last-write-wins. Two users editing the same field
 * from the same starting value produce one value and no complaint, and the user
 * whose edit vanished is never told.
 * <p>
 * Here each panel remembers the note as it was when its user loaded it, and
 * <em>Save</em> is {@code replace(loaded, edited)} — a compare-and-set. If
 * somebody else saved in the meantime, the shared value is no longer the one
 * this edit was based on, the operation is rejected, and the user is told to
 * load the latest note and redo the edit. Nothing is lost quietly.
 */
@Route(value = "conflicts", layout = MainLayout.class)
@RouteAlias(value = "uc10", layout = MainLayout.class)
@PageTitle("UC10 — Make concurrent edits safe")
@UseCaseDescription("Keeping concurrent edits from losing updates")
@Menu(order = 10, title = "UC10 — Make concurrent edits safe")
public class ConflictsView extends UseCaseView {

    public ConflictsView(RaceTopic topic) {
        super("UC10 — Make concurrent edits safe",
                "Two users editing the same note. Change the text in both panels and save in both: the first "
                        + "save goes through, the second is refused because the note changed after that user "
                        + "loaded it. Load latest picks up the other user's edit, and saving again works.");

        add(new PeerRig(peer -> peerView(topic, peer)));

        addNote("Save is replace(loaded, edited), a compare-and-set that only writes if the shared value is "
                + "still the one this panel loaded. Collaboration Kit's map has no such operation: the second "
                + "save would silently overwrite the first.");
    }

    private static Component peerView(RaceTopic topic, Peer peer) {
        Div log = new Div();
        log.addClassName("event-log");

        Span current = new Span();
        current.bindText(topic.note().map(value -> "Shared note: " + value));

        // What this panel last loaded. A compare-and-set needs a base value,
        // and the base is the state the user was looking at when they started
        // typing — not whatever the value happens to be at save time.
        ValueSignal<String> loaded = new ValueSignal<>(topic.note().peek());

        TextField draft = new TextField("Your edit");
        draft.setValue(topic.note().peek());

        Button load = new Button("Load latest", event -> {
            String value = topic.note().peek();
            loaded.set(value);
            draft.setValue(value);
        });

        Button save = new Button("Save", event -> {
            String edited = draft.getValue();
            Outcomes.whenDecided(topic.note().replace(loaded.peek(), edited),
                    accepted -> {
                        if (accepted) {
                            loaded.set(edited);
                            log(log, "Saved");
                        } else {
                            log(log, "Not saved: somebody else changed the "
                                    + "note after you loaded it. Load latest "
                                    + "and redo your edit.");
                        }
                    });
        });
        save.addThemeVariants(ButtonVariant.PRIMARY);

        return new Div(current, draft, new HorizontalLayout(load, save), log);
    }

    private static void log(Div log, String line) {
        log.addComponentAsFirst(new Div(line));
    }
}
