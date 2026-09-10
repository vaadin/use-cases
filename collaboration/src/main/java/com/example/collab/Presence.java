package com.example.collab;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.vaadin.flow.component.avatar.AvatarGroup.AvatarGroupItem;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.shared.SharedMapSignal;

/**
 * Who is currently in a topic — this module's replacement for Collaboration
 * Kit's {@code PresenceManager}.
 * <p>
 * A topic is a {@link SharedMapSignal} keyed by {@link Peer#key()}. That is the
 * whole data structure: no manager, no connection, no subscriber. Reading the
 * map inside an effect <em>is</em> the subscription, and
 * {@link #bind(com.vaadin.flow.component.Component, String, Peer)} ties
 * membership to a component's attach state the way a
 * {@code ComponentConnectionContext} used to.
 * <p>
 * A map rather than a list because presence is keyed by identity: a peer is
 * either in it or not, {@code put} is idempotent, and a rename is a {@code put}
 * on the same key rather than a search for the right list entry.
 * <p>
 * The bean is application-scoped, so the state outlives every session that
 * reads it — which is exactly what makes the demo work across browser tabs, and
 * exactly why leaving has to be wired up by hand. See API-GAPS.md #1: a killed
 * session removes nothing on its own.
 */
@Component
public class Presence {

    private final Map<String, SharedMapSignal<Peer>> topics = new ConcurrentHashMap<>();

    /**
     * The presence map for a topic, created on first use.
     */
    public SharedMapSignal<Peer> topic(String topic) {
        return topics.computeIfAbsent(topic,
                key -> new SharedMapSignal<>(Peer.class));
    }

    /**
     * Marks a peer as present in a topic for as long as {@code owner} is
     * attached.
     * <p>
     * Both listeners are needed: a component can be detached and re-attached (a
     * {@code PeerPanel} reconnecting, a view revisited), and presence has to
     * follow. Callers that want to control membership independently of attach
     * state — UC2 — should write to {@link #topic(String)} themselves instead.
     *
     * @return a registration that leaves the topic
     */
    public Registration bind(com.vaadin.flow.component.Component owner,
            String topic, Peer peer) {
        SharedMapSignal<Peer> presence = topic(topic);
        if (owner.isAttached()) {
            presence.put(peer.key(), peer);
        }
        Registration attach = owner
                .addAttachListener(event -> presence.put(peer.key(), peer));
        Registration detach = owner
                .addDetachListener(event -> presence.remove(peer.key()));
        return Registration.combine(attach, detach,
                () -> presence.remove(peer.key()));
    }

    /**
     * The peers present in a topic, ordered by id so that every client renders
     * them in the same order.
     * <p>
     * {@code get()} rather than {@code peek()} inside the comparator and the
     * mapping on purpose: a computed signal only re-runs for what it read, and
     * a peer being renamed has to reorder nothing but still re-render.
     */
    public static Signal<List<Peer>> peers(SharedMapSignal<Peer> presence) {
        return Signal.computed(() -> presence.get().values().stream()
                .map(Signal::get).filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(Peer::id)).toList());
    }

    /**
     * The same peers as {@link AvatarGroupItem}s, in the shape
     * {@code AvatarGroup.bindItems} wants: a signal of a list of signals, so
     * that adding a peer and renaming a peer are different updates.
     */
    public static Signal<List<Signal<AvatarGroupItem>>> avatarItems(
            SharedMapSignal<Peer> presence) {
        return Signal.computed(() -> presence.get().entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> {
                    Peer peer = entry.getValue().get();
                    return peer == null ? Integer.MAX_VALUE : peer.id();
                }))
                .<Signal<AvatarGroupItem>> map(entry -> entry.getValue()
                        .map(peer -> peer == null ? new AvatarGroupItem()
                                : peer.avatarItem()))
                .toList());
    }
}
