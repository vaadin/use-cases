package com.example.uc8;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.vaadin.flow.server.VaadinSession;

/**
 * Remembers how far the user got in each recording. Kept in the
 * {@link VaadinSession} for the demo, so it survives navigation and reloads but
 * not a new browser; a real application would store it per user account.
 */
public class PlaybackPositions implements Serializable {

    private final Map<String, Double> positions = new ConcurrentHashMap<>();

    /**
     * Gets the positions of the current session, creating them on first use.
     *
     * @return the positions of the current session
     */
    public static PlaybackPositions current() {
        VaadinSession session = VaadinSession.getCurrent();
        PlaybackPositions positions = session
                .getAttribute(PlaybackPositions.class);
        if (positions == null) {
            positions = new PlaybackPositions();
            session.setAttribute(PlaybackPositions.class, positions);
        }
        return positions;
    }

    /**
     * Gets the saved position in a recording.
     *
     * @param recording
     *            the recording
     * @return the position in seconds, or empty if the user has not started it
     *         or has finished it
     */
    public Optional<Double> get(String recording) {
        return Optional.ofNullable(positions.get(recording));
    }

    /**
     * Saves the position in a recording.
     *
     * @param recording
     *            the recording
     * @param seconds
     *            the position in seconds
     */
    public void save(String recording, double seconds) {
        positions.put(recording, seconds);
    }

    /**
     * Forgets the position in a recording, so the next visit starts from the
     * beginning.
     *
     * @param recording
     *            the recording
     */
    public void clear(String recording) {
        positions.remove(recording);
    }
}
