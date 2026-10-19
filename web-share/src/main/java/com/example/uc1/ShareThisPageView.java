package com.example.uc1;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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
 * UC1 — Share this page.
 * <p>
 * A single "Share this page" button hands the title and the address of the page
 * the reader is on to the browser's native share sheet. The address is read
 * from the browser with {@code Page#fetchCurrentURL} and shown in a read-only
 * field. The share reads that field's value when the button is clicked, because
 * {@link ShareContent} takes a fixed string or a field, not a value the server
 * learns later (API-GAPS.md #4).
 * <p>
 * The button follows {@link WebShare#supportSignal()}: it is enabled only while
 * the signal is {@link WebShareSupport#SUPPORTED}, and the badge next to it
 * says why it is disabled otherwise.
 */
@Route(value = "uc1", layout = MainLayout.class)
@PageTitle(ShareThisPageView.TITLE)
@UseCaseDescription("Sharing the current page through the native share sheet")
@Menu(order = 1, title = ShareThisPageView.TITLE)
@StyleSheet("uc1.css")
public class ShareThisPageView extends VerticalLayout {

    static final String TITLE = "UC1 — Share this page";

    public ShareThisPageView() {
        addClassName("uc1-view");
        add(new H1(TITLE));
        add(new Paragraph("Shares the title and the address of this page "
                + "through the browser's native share sheet. Open the page "
                + "in a mobile browser, Safari or Edge to see the share "
                + "sheet. In a browser without navigator.share, such as "
                + "desktop Firefox, the button stays disabled and the badge "
                + "says why."));

        TextField pageUrl = new TextField("Page address");
        pageUrl.setReadOnly(true);
        pageUrl.addClassName("uc1-url");

        Button shareButton = new Button("Share this page",
                VaadinIcon.SHARE.create());
        shareButton.addThemeVariants(ButtonVariant.PRIMARY);

        Signal<WebShareSupport> support = WebShare.supportSignal();
        shareButton
                .bindEnabled(support.map(s -> s == WebShareSupport.SUPPORTED));

        Span statusBadge = new Span();
        statusBadge.addClassName("uc1-status");
        statusBadge.bindText(support.map(ShareThisPageView::statusText));
        statusBadge.bindClassName("uc1-unsupported",
                support.map(s -> s == WebShareSupport.UNSUPPORTED));
        statusBadge.bindClassName("uc1-unknown",
                support.map(s -> s == WebShareSupport.UNKNOWN));

        // Bound once: the share runs in the browser on each click, and reads
        // the address field's value at that moment.
        WebShare.onClick(shareButton).share(
                ShareContent.create().title(TITLE).url(pageUrl),
                () -> Notification.show("Shared " + pageUrl.getValue(), 2500,
                        Notification.Position.BOTTOM_START),
                err -> Notification.show("Share failed: " + err.message(), 2500,
                        Notification.Position.BOTTOM_START));

        // Only the browser knows the address it shows (host, port, any
        // query string), so ask it each time the view is attached. Nothing
        // keeps running, so there is nothing to clean up on detach.
        whenAttached(ui -> {
            ui.getPage()
                    .fetchCurrentURL(url -> pageUrl.setValue(url.toString()));
            return null;
        });

        HorizontalLayout row = new HorizontalLayout(shareButton, statusBadge);
        row.setAlignItems(Alignment.CENTER);
        add(pageUrl, row);
    }

    private static String statusText(WebShareSupport state) {
        return switch (state) {
        case SUPPORTED -> "Native sharing available";
        case UNSUPPORTED -> "This browser has no navigator.share";
        case UNKNOWN -> "Detecting…";
        };
    }
}
