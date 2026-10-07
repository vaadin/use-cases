package com.example;

import com.vaadin.flow.component.html.Anchor;

/**
 * The delivery primitives Vaadin Flow does not have.
 * <p>
 * Flow 25's {@code DownloadHandler} covers generating a file and sending it to
 * the browser well. What it does not cover is everything around that: the
 * server cannot learn the URL a handler ended up on, so it cannot open the
 * document in a new tab, hand it to an {@code <embed>}, or put it in a mail.
 * <p>
 * See {@code API-GAPS.md} for what the missing API should look like.
 */
public final class MissingAPI {

    private MissingAPI() {
    }

    /**
     * Opens what an anchor points at in a new browser tab, from server code.
     * <p>
     * {@link com.vaadin.flow.component.page.Page#open(String)} needs a URL, and
     * a {@code DownloadHandler} has no server-visible URL: it is resolved only
     * once it has been bound to an element, and {@code Anchor#getHref()} is the
     * only way to read the result. So "open this document in a new tab" means
     * keeping a hidden anchor around and clicking it from JavaScript.
     *
     * @param anchor
     *            the anchor whose target should open in a new tab; it must be
     *            attached and carry {@code target="_blank"}
     * @see <a href=
     *      "https://github.com/vaadin/flow/issues/21929">vaadin/flow#21929</a>
     */
    public static void openInNewTab(Anchor anchor) {
        anchor.getElement().executeJs("this.click()");
    }

}
