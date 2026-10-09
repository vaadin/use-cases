package com.example.collab;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.vaadin.flow.component.avatar.AvatarGroup.AvatarGroupItem;

/**
 * One simulated user, this module's counterpart to Collaboration Kit's
 * {@code UserInfo}.
 * <p>
 * A peer is a plain record because it is stored as a shared signal value, and
 * shared signal values travel through Jackson: whatever goes in has to come
 * back out of {@code fromJson}. That rules out holding a component, a session
 * or a {@code Registration} here.
 */
public record Peer(int id, String name, int colorIndex) {

    /**
     * The absence of a peer, for shared values that mean "nobody". A sentinel
     * rather than null because a compare-and-set has to name the value it
     * expects, and "expected: null" is not a value.
     */
    public static final Peer NOBODY = new Peer(0, "nobody", 0);

    /**
     * Key under which this peer is stored in a presence map. Map keys are
     * strings, and the peer id is what makes a peer unique.
     */
    public String key() {
        return "peer-" + id;
    }

    /** This peer under a new display name, for a rename in place. */
    public Peer withName(String newName) {
        return new Peer(id, newName, colorIndex);
    }

    public String abbreviation() {
        return Arrays.stream(name.split(" ")).filter(part -> !part.isEmpty())
                .map(part -> part.substring(0, 1)).collect(Collectors.joining())
                .toUpperCase();
    }

    /**
     * CSS colour assigned to this peer, from the palette in {@code styles.css}.
     * The same index is handed to the field highlighter, so a peer has one
     * colour everywhere it shows up.
     */
    public String cssColor() {
        return "var(--vaadin-user-color-" + colorIndex + ")";
    }

    public AvatarGroupItem avatarItem() {
        AvatarGroupItem item = new AvatarGroupItem(name);
        item.setAbbreviation(abbreviation());
        item.setColorIndex(colorIndex);
        return item;
    }
}
