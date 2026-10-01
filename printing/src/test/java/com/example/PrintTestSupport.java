package com.example;

import java.util.Objects;

import com.vaadin.flow.component.UI;

/**
 * What the views asked the browser to do. Printing itself cannot be observed
 * without a browser, but the JavaScript a view queues can — and since Flow has
 * no printing API, that JavaScript <em>is</em> the behaviour under test.
 */
public final class PrintTestSupport {

    private PrintTestSupport() {
    }

    /** Whether something opened the browser's print dialog. */
    public static boolean printRequested() {
        return pendingJsContains("window.print()");
    }

    /**
     * Whether any JavaScript queued for the browser contains the given
     * fragment.
     *
     * @param fragment
     *            the fragment to look for
     * @return {@code true} when a pending invocation contains it
     */
    public static boolean pendingJsContains(String fragment) {
        return Objects.requireNonNull(UI.getCurrent()).getInternals()
                .containsPendingJavascript(fragment);
    }
}
