package com.example;

import com.vaadin.flow.component.UI;

/**
 * The printing primitives Vaadin Flow does not have.
 * <p>
 * Flow ships no printing API at all: there is no {@code Page#print()}, no
 * server-side print lifecycle event, no way to mark a component as "not
 * printable", and no API for the CSS page box. Every use case in this module
 * therefore starts with a line of hand-written JavaScript, and they all live
 * here rather than being copy-pasted into each view.
 * <p>
 * See {@code API-GAPS.md} for what the missing API should look like.
 */
public final class MissingAPI {

    /**
     * The id of the {@code <style>} element that carries the {@code @page}
     * rule.
     */
    private static final String PAGE_RULE_ID = "use-case-page-rule";

    private MissingAPI() {
    }

    /**
     * Opens the browser's print dialog for the current page.
     * <p>
     * The server learns nothing about what happens next — whether the user
     * printed, saved to PDF or cancelled. Use
     * {@link com.example.print.PrintEvents} to observe that.
     *
     * @param ui
     *            the UI to print
     */
    public static void print(UI ui) {
        ui.getPage().executeJs("window.print()");
    }

    /**
     * Prints once the browser has painted whatever the server sent in this
     * round trip.
     * <p>
     * A print-only route that calls {@link #print(UI)} straight from
     * {@code onAttach} prints an empty page: the JavaScript is delivered in
     * the same response as the DOM changes, and {@code window.print()} blocks
     * the main thread before the browser has laid them out. Two nested
     * animation frames are the portable way to wait for that.
     *
     * @param ui
     *            the UI to print
     */
    public static void printAfterRender(UI ui) {
        ui.getPage().executeJs(
                "requestAnimationFrame(() => requestAnimationFrame(() => window.print()))");
    }

    /**
     * Closes the browser window as soon as printing is over — the last step of
     * the "open a print window, print it, get out of the way" pattern. Only
     * works for a window the application opened itself.
     *
     * @param ui
     *            the UI whose window should close
     */
    public static void closeWindowAfterPrint(UI ui) {
        ui.getPage().executeJs(
                "window.addEventListener('afterprint', () => window.close(), { once: true })");
    }

    /**
     * Opens a URL in a separate, deliberately small browser window — the
     * classic "print window" that shows nothing but the document.
     * <p>
     * {@link com.vaadin.flow.component.page.Page#open(String, String)} takes a
     * window name but no window features, so the size and the chrome of the
     * new window cannot be influenced through it. That leaves
     * {@code window.open} and a string of features.
     *
     * @param ui
     *            the UI that opens the window
     * @param url
     *            the URL to open, relative to the application root
     */
    public static void openPrintWindow(UI ui, String url) {
        ui.getPage().executeJs(
                "window.open($0, '_blank', 'width=900,height=1000')", url);
    }

    /**
     * Sets the document's {@code @page} rule — paper size, orientation and
     * margins.
     * <p>
     * {@code @page} is a document-level at-rule, so it cannot be expressed
     * through {@code Element#getStyle()} or a component class name; the only
     * way to change it at runtime is to write a {@code <style>} element into
     * the head. The rule is replaced, not appended, so repeated calls do not
     * pile up — but it is a property of the <em>document</em>, not of the
     * view that set it, so a view that sets it must also
     * {@link #clearPageRule(UI) clear it} when the user navigates away.
     *
     * @param ui
     *            the UI whose page box to set
     * @param pageRule
     *            the full rule, e.g. {@code @page { size: A4 landscape; }}
     */
    public static void setPageRule(UI ui, String pageRule) {
        ui.getPage().executeJs("""
                let style = document.getElementById($1);
                if (!style) {
                    style = document.createElement('style');
                    style.id = $1;
                    document.head.appendChild(style);
                }
                style.textContent = $0;
                """, pageRule, PAGE_RULE_ID);
    }

    /**
     * Removes the {@code @page} rule set by
     * {@link #setPageRule(UI, String)}, so that the next view prints on the
     * browser's default paper rather than on the last one someone chose.
     *
     * @param ui
     *            the UI whose page box to reset
     */
    public static void clearPageRule(UI ui) {
        ui.getPage().executeJs("document.getElementById($0)?.remove()",
                PAGE_RULE_ID);
    }

    /**
     * Aborts the window-level print listeners registered under {@code key}.
     * <p>
     * Both {@link com.example.print.PrintEvents} and
     * {@link com.example.print.ChartPrintReflow} listen on {@code window},
     * which outlives any view, and JavaScript scheduled on an element that is
     * being detached is dropped before it reaches the browser. They therefore
     * park their {@code AbortController} in a registry on {@code window} under
     * a key of their own, and cancel it from {@code onDetach} through the
     * Page.
     *
     * @param ui
     *            the UI the listeners were registered in
     * @param key
     *            the registry key they were registered under
     */
    public static void abortPrintListeners(UI ui, String key) {
        ui.getPage().executeJs("""
                const registry = window.__printListenerRegistry;
                if (registry && registry[$0]) {
                    registry[$0].abort();
                    delete registry[$0];
                }
                """, key);
    }

    /**
     * The JavaScript prologue both window-listener shims share: abort what an
     * earlier instance registered under the same key, then open a fresh
     * {@code AbortController} and expose its {@code signal} to the statements
     * that follow.
     * <p>
     * Concatenated into the caller's script rather than executed on its own,
     * so that the listeners are registered in the same round trip that opens
     * the controller.
     *
     * @param script
     *            the JavaScript that registers the listeners; it can use
     *            {@code signal} and {@code self}, and {@code $0} is the key
     * @return the full script to pass to {@code Element#executeJs}
     */
    public static String withPrintListenerRegistry(String script) {
        return """
                const self = this;
                const registry = (window.__printListenerRegistry ??= {});
                registry[$0]?.abort();
                const controller = new AbortController();
                registry[$0] = controller;
                const signal = controller.signal;
                """ + script;
    }
}
