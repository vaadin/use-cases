package com.example.uc6;

import java.util.List;

import com.example.collab.FieldValue;
import com.example.collab.FormState;
import com.example.collab.Peer;
import com.example.collab.PeerRig;
import com.example.collab.UseCaseView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.signals.Signal;

/**
 * UC6 — See the raw collaboration events. The sampler's {@code custom-form}
 * sample, where {@code FormManager.setPropertyChangeHandler} and
 * {@code setHighlightHandler} print to a log.
 * <p>
 * This is where signals and Collaboration Kit differ most, and it is worth
 * being precise about how:
 * <ul>
 * <li><strong>Property changes.</strong> {@code SignalBinding.onChange} does
 * report old and new value, so "name: Ada → Ada B" is available without
 * bookkeeping. What it cannot report is <em>who</em>: a binding fires on the
 * value, and the value is all it knows. This form therefore stores the author
 * inside the {@link FieldValue}, which is bookkeeping the application pays for
 * (API-GAPS.md #9).</li>
 * <li><strong>Highlight changes.</strong> CE calls a handler per user with a
 * callback for that user leaving. An effect over the editor list is called with
 * the current set, so "started editing" and "stopped editing" are again a diff
 * against the previous set — the same shape as UC3's join/leave log.</li>
 * </ul>
 */
@Route(value = "form-events", layout = MainLayout.class)
@RouteAlias(value = "uc6", layout = MainLayout.class)
@PageTitle("UC6 — See the raw collaboration events")
@Menu(order = 6, title = "UC6 Collaboration events")
public class FormEventsView extends UseCaseView {

    public FormEventsView(FormEventsTopic topic) {
        super("UC6 — See the raw collaboration events",
                "The same collaborative form as UC5, with every change printed as it happens: what changed, "
                        + "from what to what, and who is in which field. Each panel logs what it observed, so the "
                        + "logs are not copies of one another.");

        add(new PeerRig(peer -> peerView(topic.form(), peer)));

        addNote("Old and new values come from the binding's onChange, which is more than an effect can see. "
                + "The acting user does not: it is carried in the stored value on purpose. Editor arrivals and "
                + "departures are diffed from the editor list, because there is no enter/leave event "
                + "(API-GAPS.md #9).");
    }

    private static Component peerView(FormState form, Peer peer) {
        Div log = new Div();
        log.addClassName("event-log");

        TextField name = new TextField("Name");
        name.setValueChangeMode(ValueChangeMode.EAGER);
        EmailField email = new EmailField("E-mail address");
        email.setValueChangeMode(ValueChangeMode.EAGER);

        logChanges(form, peer, name, FormEventsTopic.NAME, log);
        logChanges(form, peer, email, FormEventsTopic.EMAIL, log);

        Div content = new Div(name, email, log);
        return content;
    }

    private static <F extends Component & com.vaadin.flow.component.HasValue<?, String>> void logChanges(
            FormState form, Peer peer, F field, String name, Div log) {
        form.bindField(field, name, peer).onChange(context -> {
            if (nullToEmpty(context.getOldValue())
                    .equals(nullToEmpty(context.getNewValue()))) {
                // The binding fires once when it is set up, with the same
                // value on both sides. That is not a change anybody made.
                return;
            }
            String author = form.entry(name).peek().author();
            log(log, "%s: \"%s\" → \"%s\"%s".formatted(name,
                    nullToEmpty(context.getOldValue()),
                    nullToEmpty(context.getNewValue()),
                    author.isEmpty() ? "" : " (by " + author + ")"));
        });

        // The editor list gives a set, so arrivals and departures are the
        // difference between two sets this panel remembers itself.
        List<Peer> known = new java.util.ArrayList<>();
        Signal.effect(field, () -> {
            List<Peer> current = form.editors(name).getValues()
                    .filter(value -> value != null)
                    .filter(value -> value.id() != peer.id()).toList();
            current.stream()
                    .filter(entry -> known.stream()
                            .noneMatch(seen -> seen.id() == entry.id()))
                    .forEach(entry -> log(log,
                            entry.name() + " started editing " + name));
            known.stream()
                    .filter(seen -> current.stream()
                            .noneMatch(entry -> entry.id() == seen.id()))
                    .forEach(seen -> log(log,
                            seen.name() + " stopped editing " + name));
            known.clear();
            known.addAll(current);
        });
    }

    private static void log(Div log, String line) {
        log.addComponentAsFirst(new Div(line));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
