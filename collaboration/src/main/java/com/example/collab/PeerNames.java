package com.example.collab;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hands out the simulated users. The sampler this module mirrors invents a
 * random person per panel; a rotating pool is used here instead so that the
 * same peer always gets the same colour, and so a second browser tab gets
 * different names than the first one — which is the whole point of a
 * collaboration demo.
 */
public final class PeerNames {

    private static final List<String> NAMES = List.of("Ada Bergström",
            "Bruno Falk", "Cecilia Nyström", "Dexter Ojo", "Elin Kovač",
            "Farid Haddad", "Greta Lindqvist", "Hugo Marchetti", "Ines Duarte",
            "Jonas Virtanen", "Kaisa Ranta", "Léa Boucher");

    private static final AtomicInteger NEXT = new AtomicInteger();

    /** Number of colours defined in {@code styles.css}. */
    public static final int COLOR_COUNT = 8;

    private PeerNames() {
    }

    /**
     * Creates the next peer. Ids are unique for the lifetime of the JVM, so two
     * peers are never confused for one another in a shared presence map.
     */
    public static Peer next() {
        int id = NEXT.incrementAndGet();
        int index = (id - 1) % NAMES.size();
        int round = (id - 1) / NAMES.size();
        // Once round the pool, names would repeat — and two peers with the
        // same name in a collaboration demo look like one peer behaving
        // strangely, which is the opposite of the point.
        String name = round == 0 ? NAMES.get(index)
                : NAMES.get(index) + " " + (round + 1);
        return new Peer(id, name, index % COLOR_COUNT);
    }

}
