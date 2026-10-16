package com.example.uc4;

import java.util.List;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.webshare.ShareContent;
import com.vaadin.flow.component.webshare.WebShare;
import com.vaadin.flow.component.webshare.WebShareSupport;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;

/**
 * UC4 — Per-item share.
 * <p>
 * A feed with a Share button on every row, a common pattern on phones. Each
 * row's button is bound once to that row's {@code title} and {@code url}, so
 * the share sheet gets the article of the row that was tapped. All buttons are
 * bound to the same {@link WebShare#supportSignal()} condition, so they are
 * disabled together in a browser without {@code navigator.share}.
 */
@Route(value = "uc4", layout = MainLayout.class)
@PageTitle(ShareListItemsView.TITLE)
@UseCaseDescription("Sharing a specific list item from its own button")
@Menu(order = 4, title = ShareListItemsView.TITLE)
@StyleSheet("uc4.css")
public class ShareListItemsView extends VerticalLayout {

    static final String TITLE = "UC4 — Per-item share";

    private record Article(String title, String summary, String url) {
    }

    // Real pages, so a shared link opens something.
    private static final List<Article> ARTICLES = List.of(
            new Article("Server push",
                    "Updating the UI from a background thread, without a "
                            + "request from the browser.",
                    "https://vaadin.com/docs/latest/flow/advanced/server-push"),
            new Article("Routing and navigation",
                    "Mapping URLs to views, with parameters and layouts.",
                    "https://vaadin.com/docs/latest/flow/routing"),
            new Article("Browserless testing",
                    "Testing views in plain Java, without a browser.",
                    "https://vaadin.com/docs/latest/flow/testing/browserless"));

    public ShareListItemsView() {
        addClassName("uc4-view");
        add(new H1(TITLE));
        add(new Paragraph("Each row has its own Share button, bound to that "
                + "row's title and URL. On a phone, tap a row's button: the "
                + "share sheet opens with that article. The same pattern "
                + "fits blog feeds, recipe lists and product catalogs."));

        Signal<Boolean> supported = WebShare.supportSignal()
                .map(s -> s == WebShareSupport.SUPPORTED);

        Div list = new Div();
        list.addClassName("uc4-list");
        ARTICLES.forEach(a -> list.add(row(a, supported)));
        add(list);
    }

    private static Div row(Article article, Signal<Boolean> supported) {
        Div row = new Div();
        row.addClassName("uc4-item");

        Div text = new Div();
        Span title = new Span(article.title());
        title.addClassName("uc4-item-title");
        Div summary = new Div(article.summary());
        summary.addClassName("uc4-item-summary");
        text.add(title, summary);

        Button share = new Button(VaadinIcon.SHARE.create());
        share.addThemeVariants(ButtonVariant.TERTIARY);
        share.getElement().setAttribute("aria-label",
                "Share \"" + article.title() + "\"");
        share.bindEnabled(supported);
        // Each row's button binds its own per-item payload once.
        WebShare.onClick(share).share(
                ShareContent.create().title(article.title()).url(article.url()),
                () -> Notification.show(
                        "Shared: " + article.title() + " → " + article.url(),
                        2200, Notification.Position.BOTTOM_START),
                err -> Notification.show("Share failed: " + err.message(), 2200,
                        Notification.Position.BOTTOM_START));

        row.add(text, share);
        return row;
    }
}
