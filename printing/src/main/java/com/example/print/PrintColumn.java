package com.example.print;

import com.vaadin.flow.function.SerializableFunction;

/**
 * One column of a list, described once and rendered twice: into the
 * {@link com.vaadin.flow.component.grid.Grid Grid} the user works with on
 * screen, and into the plain HTML table that actually reaches the paper.
 * <p>
 * Keeping the two in sync by hand is the tax a virtualised Grid charges for
 * being printable at all — see {@code API-GAPS.md}.
 *
 * @param <T>
 *            the row type
 * @param header
 *            the column header
 * @param value
 *            the cell text for one row
 * @param numeric
 *            whether the column should be right-aligned
 */
public record PrintColumn<T>(String header,
        SerializableFunction<T, String> value, boolean numeric) {

    /**
     * A left-aligned text column.
     *
     * @param <T>
     *            the row type
     * @param header
     *            the column header
     * @param value
     *            the cell text for one row
     * @return the column
     */
    public static <T> PrintColumn<T> of(String header,
            SerializableFunction<T, String> value) {
        return new PrintColumn<>(header, value, false);
    }

    /**
     * A right-aligned column for numbers and amounts.
     *
     * @param <T>
     *            the row type
     * @param header
     *            the column header
     * @param value
     *            the cell text for one row
     * @return the column
     */
    public static <T> PrintColumn<T> numeric(String header,
            SerializableFunction<T, String> value) {
        return new PrintColumn<>(header, value, true);
    }
}
