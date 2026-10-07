package com.example.uc4;

import java.util.List;

import com.example.views.MainLayout;

import com.vaadin.flow.component.AttachEvent;
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
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;

/**
 * UC4 — Per-item share in a list.
 * <p>
 * Models the common mobile pattern of a feed/list with a Share icon on every
 * row: tapping a row's icon hands that row's specific {@code title}/
 * {@code url} payload to the native sheet, so the receiving app sees a coherent
 * per-item preview. All buttons stay in sync with
 * {@link WebShare#supportSignal()} — they disable together when the API is
 * unavailable.
 */
@Route(value = "uc4", layout = MainLayout.class)
@Menu(order = 4, title = "UC4 — Per-item share")
@StyleSheet("uc4.css")
public class ShareListItemsView extends VerticalLayout {

    private record Article(String title, String summary, String url) {
    }

    private static final List<Article> ARTICLES = List.of(
            new Article("Reactive UIs with Vaadin Signals",
                    "The signal API gives Flow a fine-grained reactivity "
                            + "story that pairs naturally with Web APIs.",
                    "https://vaadin.com/blog/signals"),
            new Article("Geolocation in plain Java",
                    "Acquiring a position, asking for permission, and "
                            + "feeding the value back into the UI.",
                    "https://vaadin.com/blog/geolocation"),
            new Article("Page Visibility for free",
                    "How a tiny browser signal can halve your server traffic.",
                    "https://vaadin.com/blog/page-visibility"));

    private final Div list = new Div();

    public ShareListItemsView() {
        addClassName("uc4-view");
        add(new H1("UC4 — Share each item in a list"));
        add(new Paragraph("Each row has its own Share button bound to its "
                + "own payload. On a phone, tapping a row's icon opens the "
                + "share sheet with that article's title and URL — perfect "
                + "for blog feeds, recipe lists, product catalogs."));

        list.addClassName("share-list");
        ARTICLES.forEach(a -> list.add(row(a)));
        add(list);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        Signal<WebShareSupport> support = WebShare.supportSignal();

        Signal.effect(this, () -> {
            boolean enabled = support.get() == WebShareSupport.SUPPORTED;
            list.getChildren().flatMap(r -> r.getChildren())
                    .filter(Button.class::isInstance).map(Button.class::cast)
                    .forEach(b -> b.setEnabled(enabled));
        });
    }

    private Div row(Article article) {
        Div row = new Div();
        row.addClassName("share-list-item");

        Div text = new Div();
        Span title = new Span(article.title());
        title.addClassName("share-list-item-title");
        Div summary = new Div(article.summary());
        summary.addClassName("share-list-item-summary");
        text.add(title, summary);

        Button share = new Button(VaadinIcon.SHARE.create());
        share.addThemeVariants(ButtonVariant.TERTIARY);
        share.getElement().setAttribute("aria-label",
                "Share \"" + article.title() + "\"");
        share.setEnabled(false);
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
