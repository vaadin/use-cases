package com.example.uc10;

import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC10 — Smooth scrolling through 100,000 cards.
 * <p>
 * A {@link VirtualList} renders only the cards in and near the viewport and
 * fetches the rest from the server as the user scrolls, so a list of 100,000
 * contacts costs about as much as a list of 30. Each card is a
 * {@link LitRenderer} template filled in the browser: no server-side component
 * exists per card at all.
 * <p>
 * For comparison, the button on the right builds cards the naive way, one
 * server-side component tree per contact, and reports how long that took and
 * how many components it created — for just 2,000 contacts.
 */
@Route(value = "uc10", layout = MainLayout.class)
@PageTitle("UC10 — 100,000 cards")
@UseCaseDescription("Rendering a huge list with a virtual list and a lightweight renderer")
@Menu(order = 10, title = "UC10 — 100,000 cards")
@StyleSheet("uc10.css")
public class VirtualScrollingView extends VerticalLayout {

    static final int CONTACT_COUNT = 100_000;
    static final int NAIVE_COUNT = 2_000;

    private static final List<String> FIRST_NAMES = List.of("Ada", "Ben",
            "Chloe", "Dmitri", "Elif", "Farah", "Goran", "Hana", "Iker",
            "Jonas", "Kaija", "Luis", "Mei", "Nils", "Olga", "Priya");
    private static final List<String> LAST_NAMES = List.of("Anders", "Brook",
            "Costa", "Dahl", "Eriksen", "Fischer", "Garcia", "Holm", "Ito",
            "Jansen", "Kowalski", "Laine", "Moreau", "Novak");

    record Contact(int number, String name, String email) {

        String initials() {
            return name.chars().filter(Character::isUpperCase)
                    .collect(StringBuilder::new, StringBuilder::appendCodePoint,
                            StringBuilder::append)
                    .toString();
        }
    }

    private final VirtualList<Contact> list = new VirtualList<>();
    private final Div naive = new Div();
    private final Span naiveStats = new Span();

    public VirtualScrollingView() {
        addClassName("uc10-view");
        setSizeFull();

        add(new H1("UC10 — 100,000 cards"));
        add(new Paragraph("Scroll the list on the left as fast as you like: "
                + "only the visible cards exist in the browser, and none of "
                + "them exist as components on the server. Then build "
                + "2,000 cards the naive way on the right and compare."));

        list.setRenderer(LitRenderer.<Contact> of("""
                <div class="contact-card">
                  <span class="avatar">${item.initials}</span>
                  <span class="name">${item.name}</span>
                  <span class="email">${item.email}</span>
                  <span class="number">#${item.number}</span>
                </div>""").withProperty("initials", Contact::initials)
                .withProperty("name", Contact::name)
                .withProperty("email", Contact::email)
                .withProperty("number", Contact::number));
        list.setDataProvider(
                DataProvider.fromCallbacks(
                        query -> IntStream
                                .range(query.getOffset(),
                                        Math.min(CONTACT_COUNT,
                                                query.getOffset()
                                                        + query.getLimit()))
                                .mapToObj(VirtualScrollingView::contact),
                        query -> CONTACT_COUNT));
        list.addClassName("contact-list");

        Button build = new Button(
                "Build %,d cards as components".formatted(NAIVE_COUNT),
                event -> buildNaive());
        naive.addClassName("contact-list");
        naiveStats.addClassName("naive-stats");

        VerticalLayout left = new VerticalLayout(
                new H2("VirtualList, %,d contacts".formatted(CONTACT_COUNT)),
                list);
        VerticalLayout right = new VerticalLayout(
                new H2("One component tree per contact"), build, naiveStats,
                naive);
        HorizontalLayout columns = new HorizontalLayout(left, right);
        columns.setSizeFull();
        left.setSizeFull();
        right.setSizeFull();
        add(columns);
    }

    static Contact contact(int index) {
        String first = FIRST_NAMES.get(index % FIRST_NAMES.size());
        String last = LAST_NAMES
                .get(index / FIRST_NAMES.size() % LAST_NAMES.size());
        return new Contact(index + 1, first + " " + last,
                (first + "." + last + index + "@example.com")
                        .toLowerCase(Locale.ROOT));
    }

    private void buildNaive() {
        naive.removeAll();
        long start = System.nanoTime();
        for (int i = 0; i < NAIVE_COUNT; i++) {
            Contact contact = contact(i);
            Span avatar = new Span(contact.initials());
            avatar.addClassName("avatar");
            Span name = new Span(contact.name());
            name.addClassName("name");
            Span email = new Span(contact.email());
            email.addClassName("email");
            Span number = new Span("#" + contact.number());
            number.addClassName("number");
            Div card = new Div(avatar, name, email, number);
            card.addClassName("contact-card");
            naive.add(card);
        }
        long millis = (System.nanoTime() - start) / 1_000_000;
        naiveStats.setText("Built %,d server-side components in %d ms — and "
                .formatted(NAIVE_COUNT * 5, millis)
                + "every one of them is sent to the browser and kept in the "
                + "session.");
    }

    // Package-private test seams.
    VirtualList<Contact> list() {
        return list;
    }

    Div naiveCards() {
        return naive;
    }
}
