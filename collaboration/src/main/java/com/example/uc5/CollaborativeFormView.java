package com.example.uc5;

import com.example.collab.FormState;
import com.example.collab.Peer;
import com.example.collab.PeerRig;
import com.example.collab.Presence;
import com.example.collab.UseCaseView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.formlayout.FormLayout;
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
 * UC5 — Edit a form together. The sampler's {@code collaboration-binder}
 * sample, and the reason Collaboration Kit exists: value synchronization plus
 * field highlighting on top of a form.
 * <p>
 * Both halves work. Values synchronize because each field is bound to its own
 * entry in a shared map — {@code HasValue.bindValue} in one direction, a
 * {@code put} in the other — and highlighting works because each field also has
 * a shared list of the peers currently in it, which the field highlighter
 * renders.
 * <p>
 * What is missing is the third thing {@code CollaborationBinder} inherits for
 * free: {@code Binder} itself. A signal binding and a {@code Binder} both want
 * to own a field's value, so validation here is written as an effect over the
 * shared value instead of as a validator on a binding. It works, and it is not
 * the same thing — see API-GAPS.md #7.
 */
@Route(value = "form", layout = MainLayout.class)
@RouteAlias(value = "uc5", layout = MainLayout.class)
@PageTitle("UC5 — Edit a form together")
@Menu(order = 5, title = "UC5 Collaborative form")
public class CollaborativeFormView extends UseCaseView {

    static final String TOPIC = "uc5-form";

    public CollaborativeFormView(FormTopic topic, Presence presence) {
        super("UC5 — Edit a form together",
                "One customer record, several editors. Type in any panel and the other panels follow per "
                        + "keystroke; put the cursor in a field and the other panels outline it with your name and "
                        + "colour.");

        add(new PeerRig(peer -> peerView(topic.form(), presence, peer)));

        addNote("Value sync is HasValue.bindValue plus a put per field, and the outline is the platform's "
                + "vaadin-field-highlighter — reachable only through a protected initializer and executeJs "
                + "(API-GAPS.md #6). The e-mail check is an effect that sets invalid by hand, because a signal "
                + "cannot be bound to a field's validity and Binder cannot own a field a signal is bound to "
                + "(API-GAPS.md #7, #8).");
    }

    private static Component peerView(FormState form, Presence presence,
            Peer peer) {
        var topic = presence.topic(TOPIC);

        AvatarGroup group = new AvatarGroup();
        group.setMaxItemsVisible(4);
        group.bindItems(Presence.avatarItems(topic));

        TextField name = new TextField("Name");
        EmailField email = new EmailField("E-mail address");
        TextField address = new TextField("Address");

        // Per keystroke, not per blur: a collaborative form that only shows
        // the other user's work when they leave the field is not one.
        name.setValueChangeMode(ValueChangeMode.EAGER);
        email.setValueChangeMode(ValueChangeMode.EAGER);
        address.setValueChangeMode(ValueChangeMode.EAGER);

        form.bindField(name, FormTopic.NAME, peer);
        form.bindField(email, FormTopic.EMAIL, peer);
        form.bindField(address, FormTopic.ADDRESS, peer);

        // Validity is not bindable, so it is an effect: read the shared value,
        // decide, set the flag. A Binder validator would have been the obvious
        // place for this if the field's value were not already owned by a
        // signal binding.
        Signal.effect(email, () -> {
            String current = form.value(FormTopic.EMAIL).get();
            boolean invalid = !current.isEmpty() && !current.contains("@");
            email.setInvalid(invalid);
            email.setErrorMessage(invalid ? "Not an e-mail address" : null);
        });

        FormLayout layout = new FormLayout(name, email, address);
        layout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Div content = new Div(group, layout);
        presence.bind(content, TOPIC, peer);
        return content;
    }
}
