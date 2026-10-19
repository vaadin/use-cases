package com.example.uc2;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.clipboard.Clipboard;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.webshare.ShareContent;
import com.vaadin.flow.component.webshare.WebShare;
import com.vaadin.flow.component.webshare.WebShareSupport;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;

/**
 * UC2 — Copy-link fallback.
 * <p>
 * One place on the page holds either a "Share" button (while
 * {@link WebShare#supportSignal()} is {@link WebShareSupport#SUPPORTED}) or a
 * "Copy link" button (while it is {@link WebShareSupport#UNSUPPORTED}). Both
 * buttons are created once and their visibility is bound to the signal, so the
 * swap needs no feature-detection callback. Both hand over the same link: the
 * address of this page, read from the browser into a read-only field. The copy
 * uses {@link Clipboard#onClick}, which writes to the clipboard in the
 * browser's click handler, so it also works in browsers that require a user
 * gesture for clipboard writes.
 */
@Route(value = "uc2", layout = MainLayout.class)
@PageTitle(CopyLinkFallbackView.TITLE)
@UseCaseDescription("Offering a copy-link button where native sharing is unsupported")
@Menu(order = 2, title = CopyLinkFallbackView.TITLE)
@StyleSheet("uc2.css")
public class CopyLinkFallbackView extends VerticalLayout {

    static final String TITLE = "UC2 — Copy-link fallback";

    public CopyLinkFallbackView() {
        addClassName("uc2-view");
        add(new H1(TITLE));
        add(new Paragraph("Browsers with navigator.share (mobile browsers, "
                + "Safari, Edge) get a Share button that opens the native "
                + "share sheet. Other browsers, such as desktop Firefox, get "
                + "a Copy link button instead. Both hand over the same link: "
                + "the address of this page."));

        TextField pageUrl = new TextField("Link");
        pageUrl.setReadOnly(true);
        pageUrl.addClassName("uc2-url");

        Signal<WebShareSupport> support = WebShare.supportSignal();

        Button share = new Button("Share", VaadinIcon.SHARE.create());
        share.addThemeVariants(ButtonVariant.PRIMARY);
        share.bindVisible(support.map(s -> s == WebShareSupport.SUPPORTED));
        WebShare.onClick(share).share(
                ShareContent.create().title(TITLE).url(pageUrl),
                () -> Notification.show("Shared", 2000,
                        Notification.Position.BOTTOM_START),
                err -> Notification.show("Share failed: " + err.message(), 2000,
                        Notification.Position.BOTTOM_START));

        Button copy = new Button("Copy link", VaadinIcon.LINK.create());
        copy.addThemeVariants(ButtonVariant.PRIMARY);
        copy.bindVisible(support.map(s -> s == WebShareSupport.UNSUPPORTED));
        Clipboard.onClick(copy).writeText(pageUrl,
                copied -> Notification.show("Link copied: " + copied, 2500,
                        Notification.Position.BOTTOM_START),
                err -> Notification.show("Copy failed: " + err.message(), 2500,
                        Notification.Position.BOTTOM_START));

        Button detecting = new Button("Detecting…");
        detecting.setEnabled(false);
        detecting.bindVisible(support.map(s -> s == WebShareSupport.UNKNOWN));

        Div actionSlot = new Div(share, copy, detecting);
        actionSlot.addClassName("uc2-action");

        Paragraph hint = new Paragraph();
        hint.addClassName("uc2-hint");
        hint.bindText(support.map(CopyLinkFallbackView::hintText));

        // Only the browser knows the address it shows, so ask it on each
        // attach. Nothing keeps running, so there is nothing to clean up.
        whenAttached(ui -> {
            ui.getPage()
                    .fetchCurrentURL(url -> pageUrl.setValue(url.toString()));
            return null;
        });

        add(pageUrl, actionSlot, hint);
    }

    private static String hintText(WebShareSupport state) {
        return switch (state) {
        case SUPPORTED -> "This browser has navigator.share: Share opens the "
                + "native share sheet.";
        case UNSUPPORTED -> "This browser has no navigator.share: Copy link "
                + "puts the link on the clipboard instead.";
        case UNKNOWN ->
            "Waiting for the browser to report whether it can " + "share.";
        };
    }
}
