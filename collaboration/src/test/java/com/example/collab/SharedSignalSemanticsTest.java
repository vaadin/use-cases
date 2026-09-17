package com.example.collab;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.operations.SignalOperation;
import com.vaadin.flow.signals.shared.SharedListSignal;
import com.vaadin.flow.signals.shared.SharedNodeSignal;
import com.vaadin.flow.signals.shared.SharedValueSignal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shared-signal behaviour the use cases are built on, pinned down without a
 * UI in the way.
 * <p>
 * These are not tests of a view: they are the evidence behind the claims in
 * API-GAPS.md, kept executable so that a framework change either keeps them
 * true or makes the gap list obsolete. Every assertion here corresponds to a
 * design decision somewhere in the module — why a lock is a compare-and-set,
 * why an edit is an {@code update}, why an outcome is read from a callback and
 * not from {@code getNow}.
 */
class SharedSignalSemanticsTest {

    @Test
    void aStaleCompareAndSetIsRejectedWithAReason() {
        SharedValueSignal<String> value = new SharedValueSignal<>("a");

        assertTrue(outcome(value.replace("a", "b")).successful());
        assertEquals("b", value.peek());

        // The write UC10 is about: based on a value that is no longer there.
        var stale = outcome(value.replace("a", "c"));
        assertFalse(stale.successful());
        assertEquals("Unexpected value",
                assertInstanceOf(SignalOperation.Error.class, stale).reason());
        assertEquals("b", value.peek(),
                "a rejected write must not change anything");
    }

    @Test
    void aTransactionCannotSpanIndependentSharedSignals() {
        // Why UC10 keeps its two lists in one node tree. Each top-level
        // shared signal commits on its own and may be owned by a different
        // cluster node, so a transaction across two of them is refused
        // outright rather than committing halfway (API-GAPS.md #15).
        SharedListSignal<String> from = new SharedListSignal<>(String.class);
        SharedListSignal<String> to = new SharedListSignal<>(String.class);
        var item = from.insertLast("one").signal();

        var refused = assertThrows(IllegalStateException.class,
                () -> move(from, to, item));
        assertTrue(refused.getMessage().contains(
                "multiple independent shared signals in the same transaction"),
                refused.getMessage());
        assertEquals(1, from.peek().size(), "nothing may have moved");
    }

    @Test
    void aTransactionWithAFailedConditionChangesNothing() {
        SharedNodeSignal board = new SharedNodeSignal();
        SharedListSignal<String> from = list(board, "from");
        SharedListSignal<String> to = list(board, "to");
        var item = from.insertLast("one").signal();

        assertTrue(outcome(move(from, to, item)).successful());
        assertEquals(0, from.peek().size());
        assertEquals(1, to.peek().size());

        // The same move again: the item is not a child of the source list any
        // more, so neither list may change.
        var second = outcome(move(from, to, item));
        assertFalse(second.successful());
        assertEquals("Not a child",
                assertInstanceOf(SignalOperation.Error.class, second).reason());
        assertEquals(1, to.peek().size(),
                "a failed transaction must not have appended a duplicate");
    }

    @Test
    void aValidatorRefusesBySignallingAnErrorRatherThanRejecting() {
        SharedValueSignal<String> value = new SharedValueSignal<>("a");
        SharedValueSignal<String> guarded = value
                .withValidator(command -> false);

        // Not a rejected operation: the submit itself fails. Every other
        // refusal in this API comes back as an Error on the operation, which
        // is why UC7 and UC13 have to catch instead of report (API-GAPS.md
        // #11).
        assertThrows(UnsupportedOperationException.class,
                () -> guarded.set("b"));
        assertEquals("a", value.peek());

        // The unguarded handle to the same data is unaffected, which is what
        // makes a validator a capability and not an access rule.
        assertTrue(outcome(value.set("b")).successful());
        assertEquals("b", value.peek());
    }

    @Test
    void anUpdateKeepsAConcurrentChangeToTheSameValue() {
        SharedListSignal<Record2> rows = new SharedListSignal<>(Record2.class);
        var row = rows.insertLast(new Record2("name", "category")).signal();

        // Two edits to different fields of one row, as UC9 does them.
        row.update(current -> new Record2("edited", current.category()));
        row.update(current -> new Record2(current.name(), "edited too"));

        assertEquals(new Record2("edited", "edited too"), row.peek());
    }

    /** Two lists in one tree, the shape UC10's topic uses. */
    private static SharedListSignal<String> list(SharedNodeSignal board,
            String key) {
        board.putChildIfAbsent(key);
        return board.peek().mapChildren().get(key).asList(String.class);
    }

    private static SignalOperation<Void> move(SharedListSignal<String> from,
            SharedListSignal<String> to, SharedValueSignal<String> item) {
        return Signal.runInTransaction(() -> {
            from.verifyChild(item);
            String value = item.peek();
            from.remove(item);
            to.insertLast(value);
        });
    }

    /**
     * Outside a session, commands are applied as they are submitted, so the
     * result is already there. Inside a UI callback it is not — see
     * {@code PendingStateViewTest}, which is the whole of UC13.
     */
    private static SignalOperation.ResultOrError<?> outcome(
            SignalOperation<?> operation) {
        assertTrue(operation.result().isDone(),
                "outside a session an operation is decided immediately");
        return operation.result().getNow(null);
    }

    record Record2(String name, String category) {
    }
}
