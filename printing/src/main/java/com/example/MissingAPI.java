package com.example;

import com.vaadin.flow.component.Component;
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
     * {@code onAttach} prints an empty page: the JavaScript is delivered in the
     * same response as the DOM changes, and {@code window.print()} blocks the
     * main thread before the browser has laid them out. Two nested animation
     * frames are the portable way to wait for that.
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
     * window name but no window features, so the size and the chrome of the new
     * window cannot be influenced through it. That leaves {@code window.open}
     * and a string of features.
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
     * pile up.
     *
     * @param ui
     *            the UI whose page box to set
     * @param pageRule
     *            the full rule, e.g. {@code @page { size: A4 landscape; }}
     */
    public static void setPageRule(UI ui, String pageRule) {
        ui.getPage().executeJs("""
                let style = document.getElementById('use-case-page-rule');
                if (!style) {
                    style = document.createElement('style');
                    style.id = 'use-case-page-rule';
                    document.head.appendChild(style);
                }
                style.textContent = $0;
                """, pageRule);
    }

    /**
     * Makes every {@code vaadin-chart} inside {@code root} redraw itself for
     * the paper before the browser paginates, and again for the screen
     * afterwards.
     * <p>
     * Highcharts sizes its SVG once, in pixels, when the chart is drawn. The
     * print media query changes the layout width underneath it, but nothing
     * tells the chart to re-measure, so it prints at its screen width —
     * clipped, or spilling onto a second page. {@code Chart} has no server-side
     * "redraw now" API that would help here either, and printing is synchronous
     * on the client: by the time the server could react, the page has already
     * been rasterised.
     *
     * @param root
     *            the component whose charts should be reflowed
     */
    public static void reflowChartsWhenPrinting(Component root) {
        root.getElement().executeJs(
                """
                        const root = this;
                        const reflow = () => root.querySelectorAll('vaadin-chart')
                                .forEach(chart => chart.configuration && chart.configuration.reflow());
                        root.__printReflow?.abort();
                        const controller = new AbortController();
                        root.__printReflow = controller;
                        window.addEventListener('beforeprint', reflow, { signal: controller.signal });
                        window.addEventListener('afterprint', reflow, { signal: controller.signal });
                        """);
    }
}
