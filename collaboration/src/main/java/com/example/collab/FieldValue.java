package com.example.collab;

/**
 * One form field's shared value, together with who last wrote it.
 * <p>
 * The author is part of the payload because it cannot be recovered afterwards:
 * an effect that observes the field sees the new value and nothing else — not
 * the previous value, not who caused the change. Collaboration Kit's
 * {@code PropertyChangeHandler} is handed both. See API-GAPS.md #9.
 */
public record FieldValue(String value, int authorId, String author) {

    public static final FieldValue EMPTY = new FieldValue("", 0, "");
}
