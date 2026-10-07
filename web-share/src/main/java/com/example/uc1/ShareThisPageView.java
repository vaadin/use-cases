package com.example.uc1;

import com.example.views.MainLayout;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.webshare.ShareContent;
import com.vaadin.flow.component.webshare.WebShare;
import com.vaadin.flow.component.webshare.WebShareSupport;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;

/**
 * UC1 — Share this page.
 * <p>
 * The most basic Web Share scenario: a single "Share this page" button hands
 * the current page's title and URL to the browser's native share sheet. The
 * button reflects {@link WebShare#supportSignal()} reactively: while the signal
 * is still {@link WebShareSupport#UNKNOWN} the button stays disabled with a
 * "Detecting…" label; once a real value arrives it either enables (on
 * {@link WebShareSupport#SUPPORTED}) or stays disabled with an explanation (on
 * {@link WebShareSupport#UNSUPPORTED}). The share itself is bound once via
 * {@link WebShare#onClick} and fires on each click while the browser has
 * transient user activation.
 */
@Route(value = "uc1", layout = MainLayout.class)
@Menu(order = 1, title = "UC1 — Share this page")
public class ShareThisPageView extends VerticalLayout {

    private static final String ARTICLE_TITLE = "Vaadin Flow — Web Share API";
    private static final String ARTICLE_URL = "https://vaadin.com/";

    private final Button shareButton = new Button("Share this page",
            VaadinIcon.SHARE.create());
    private final Span statusBadge = new Span();

    public ShareThisPageView() {
        add(new H1("UC1 — Share this page"));
        add(new Paragraph("Hands the current page's title and URL to the "
                + "browser's native share sheet. Use a mobile browser to "
                + "see the share sheet pop up — on desktop Firefox the "
                + "button stays disabled and explains why."));

        shareButton.addThemeVariants(ButtonVariant.PRIMARY);
        statusBadge.addClassName("status-badge");

        HorizontalLayout row = new HorizontalLayout(shareButton, statusBadge);
        row.setAlignItems(Alignment.CENTER);
        add(row);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        Signal<WebShareSupport> support = WebShare.supportSignal();

        statusBadge.bindText(support.map(ShareThisPageView::statusText));
        statusBadge.bindClassName("unsupported",
                support.map(s -> s == WebShareSupport.UNSUPPORTED));
        statusBadge.bindClassName("unknown",
                support.map(s -> s == WebShareSupport.UNKNOWN));

        Signal.effect(this, () -> shareButton
                .setEnabled(support.get() == WebShareSupport.SUPPORTED));

        // Bind the share once: it fires on each click while the browser has
        // transient user activation. The observed form surfaces developer
        // feedback once the client-side share resolves.
        WebShare.onClick(shareButton).share(
                ShareContent.create().title(ARTICLE_TITLE).url(ARTICLE_URL),
                () -> Notification.show(
                        "Shared: \"" + ARTICLE_TITLE + "\" → " + ARTICLE_URL,
                        2500, Notification.Position.BOTTOM_START),
                err -> Notification.show("Share failed: " + err.message(), 2500,
                        Notification.Position.BOTTOM_START));
    }

    private static String statusText(WebShareSupport state) {
        return switch (state) {
        case SUPPORTED -> "Native sharing available";
        case UNSUPPORTED -> "Browser does not support navigator.share";
        case UNKNOWN -> "Detecting…";
        };
    }
}
