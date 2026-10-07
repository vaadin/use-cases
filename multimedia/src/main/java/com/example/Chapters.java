package com.example;

import java.util.List;

/**
 * The chapters of the bundled {@code quarterly-review.mp4}, one per scene of
 * the recording.
 */
public final class Chapters {

    /**
     * A chapter of the recording.
     *
     * @param title
     *            the chapter title
     * @param start
     *            where the chapter starts, in seconds
     */
    public record Chapter(String title, int start) {
    }

    public static final List<Chapter> QUARTERLY_REVIEW = List.of(
            new Chapter("Welcome", 0), new Chapter("Roadmap", 11),
            new Chapter("Demo", 23), new Chapter("Questions", 36));

    private Chapters() {
    }

    /**
     * Finds the chapter playing at a position.
     *
     * @param seconds
     *            the playback position
     * @return the index of the chapter in {@link #QUARTERLY_REVIEW}
     */
    public static int indexAt(double seconds) {
        int index = 0;
        for (int i = 0; i < QUARTERLY_REVIEW.size(); i++) {
            if (seconds >= QUARTERLY_REVIEW.get(i).start()) {
                index = i;
            }
        }
        return index;
    }

    /**
     * Formats a position as {@code m:ss}.
     *
     * @param seconds
     *            the position in seconds
     * @return the formatted position
     */
    public static String format(double seconds) {
        long whole = (long) Math.max(0, seconds);
        return "%d:%02d".formatted(whole / 60, whole % 60);
    }
}
