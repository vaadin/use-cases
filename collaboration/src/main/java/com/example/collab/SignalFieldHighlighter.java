package com.example.collab;

import java.util.Collection;
import java.util.List;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.fieldhighlighter.FieldHighlighterInitializer;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.Signal;

/**
 * Signal-driven access to the {@code vaadin-field-highlighter} web component —
 * the coloured outline and name tag that Collaboration Kit puts on a field
 * somebody else is editing.
 * <p>
 * The component ships with the platform, but its Java side is
 * {@link FieldHighlighterInitializer}: one {@code protected static init()} and
 * nothing else, so the users have to be pushed over with {@code executeJs}.
 * Extending the initializer is what makes its {@code @JsModule} and
 * {@code @NpmPackage} annotations apply, which is why this class inherits from
 * a class it uses none of. See API-GAPS.md #6.
 * <p>
 * {@link #bind} reads the outer signal <em>and</em> every inner one inside the
 * effect, so both a peer entering a field and a peer being renamed while in it
 * update the highlight.
 */
public class SignalFieldHighlighter extends FieldHighlighterInitializer {

    private static final String FH_CLASS = "customElements.get('vaadin-field-highlighter')";

    private SignalFieldHighlighter() {
    }

    /**
     * Highlights {@code field} for every peer in {@code editors} except
     * {@code localUser}, who does not need to be told that they are typing.
     *
     * @return a registration that removes the highlight and stops observing
     */
    public static <C extends Component & HasElement> Registration bind(C field,
            Signal<? extends List<? extends Signal<Peer>>> editors,
            @Nullable Peer localUser) {
        Registration initialization = init(field.getElement());
        Registration effect = Signal.effect(field, () -> {
            List<Peer> peers = editors.get().stream().map(Signal::get)
                    .filter(peer -> peer != null && (localUser == null
                            || peer.id() != localUser.id()))
                    .toList();
            setUsers(field.getElement(), peers);
        });
        return Registration.combine(effect, initialization);
    }

    /** Replaces the peers currently highlighting a field. */
    public static void setUsers(Element element, Collection<Peer> peers) {
        element.executeJs(FH_CLASS + ".setUsers(this, $0)", toJson(peers));
    }

    private static ArrayNode toJson(Collection<Peer> peers) {
        return peers.stream().map(SignalFieldHighlighter::toJson)
                .collect(JacksonUtils.asArray());
    }

    private static ObjectNode toJson(Peer peer) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("id", peer.id());
        node.put("name", peer.name());
        node.put("colorIndex", peer.colorIndex());
        return node;
    }
}
