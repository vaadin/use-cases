package com.example.collab;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hands out the simulated users. The sampler this module mirrors invents a
 * random person per panel; a pool of names is used here instead, and a name is
 * only handed out while nobody else is using it, so that two browser tabs show
 * different people — which is the whole point of a collaboration demo.
 * <p>
 * A name is in use from {@link #next()} until its panel is detached, see
 * {@link PeerPanel}. Ids, unlike names, are never reused.
 */
public final class PeerNames {

    private static final List<String> NAMES = List.of("Ada Bergström",
            "Bruno Falk", "Cecilia Nyström", "Dexter Ojo", "Elin Kovač",
            "Farid Haddad", "Greta Lindqvist", "Hugo Marchetti", "Ines Duarte",
            "Jonas Virtanen", "Kaisa Ranta", "Léa Boucher", "Malik Osei",
            "Nora Halvorsen", "Oskar Wiik", "Priya Raman", "Quentin Arnaud",
            "Rosa Delgado", "Sami Heikkinen", "Tove Ek", "Umar Siddiqui",
            "Vera Novak", "Wilhelm Strand", "Yuki Tanaka");

    private static final AtomicInteger NEXT = new AtomicInteger();

    /**
     * Name to the id of the peer holding it. Keyed by holder so that a peer can
     * only hand back a name it holds: a window reattached after its name went
     * to somebody else must not free that somebody's name when it leaves.
     */
    private static final Map<String, Integer> IN_USE = new HashMap<>();

    /** Number of colours defined in {@code styles.css}. */
    public static final int COLOR_COUNT = 8;

    private PeerNames() {
    }

    /**
     * Creates the next peer, with the first name from the pool that nobody is
     * using. Ids are unique for the lifetime of the JVM, so two peers are never
     * confused for one another in a shared presence map.
     */
    public static Peer next() {
        int id = NEXT.incrementAndGet();
        synchronized (IN_USE) {
            // Only once every name is taken does a number go on the end: two
            // peers with the same name look like one peer behaving strangely.
            for (int round = 1;; round++) {
                for (int index = 0; index < NAMES.size(); index++) {
                    String name = round == 1 ? NAMES.get(index)
                            : NAMES.get(index) + " " + round;
                    if (IN_USE.putIfAbsent(name, id) == null) {
                        return new Peer(id, name, index % COLOR_COUNT);
                    }
                }
            }
        }
    }

    /**
     * Marks a peer's name as in use again, for a panel that is reattached. If
     * somebody else got the name in the meantime it stays theirs.
     */
    static void claim(Peer peer) {
        synchronized (IN_USE) {
            IN_USE.putIfAbsent(peer.name(), peer.id());
        }
    }

    /** Hands a peer's name back to the pool, if this peer holds it. */
    static void release(Peer peer) {
        synchronized (IN_USE) {
            IN_USE.remove(peer.name(), peer.id());
        }
    }
}
