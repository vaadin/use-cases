package com.example.common;

import java.util.Collection;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.card.CardVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.menu.MenuConfiguration;

/**
 * Shared scaffolding for the per-module home views. The constructor adds an H1
 * headline and an intro paragraph; subclasses choose how to render the body of
 * the page:
 * <ul>
 * <li>{@link #addMenuCards(Collection)} — the entries of the given views as
 * cards, each described by its view's {@link UseCaseDescription}; call it once
 * per group to show the cards under headings of the subclass's choosing.</li>
 * <li>{@link #homeCard(String, String, String, Class)} — build a single feature
 * card with a tag, title, description and "Open →" CTA, for modules that
 * hand-curate their home page.</li>
 * </ul>
 */
public abstract class BaseHomeView extends VerticalLayout {

    protected BaseHomeView(String headline, String intro) {
        add(new H1(headline));
        add(new Paragraph(intro));
    }

    /**
     * Appends one {@link #homeCard(String, String, String, Class) card} per
     * menu entry of the given views, in menu order. A title of the form
     * {@code "UC1 — Short name"} is split into the card's tag and title, and
     * the view's {@link UseCaseDescription} becomes the description.
     */
    protected void addMenuCards(Collection<Class<? extends Component>> views) {
        Div cards = new Div();
        cards.addClassName("home-cards");
        MenuConfiguration.getMenuEntries().stream()
                .filter(entry -> entry.menuClass() != null
                        && views.contains(entry.menuClass()))
                .forEach(entry -> {
                    String[] tagAndTitle = entry.title().split(" — ", 2);
                    UseCaseDescription description = BaseMainLayout
                            .descriptionOf(entry);
                    cards.add(homeCard(
                            tagAndTitle.length == 2 ? tagAndTitle[0] : "",
                            tagAndTitle[tagAndTitle.length - 1],
                            description == null ? "" : description.value(),
                            entry.menuClass()));
                });
        add(cards);
    }

    protected static Card homeCard(String tag, String title, String description,
            Class<? extends Component> target) {
        Card card = new Card();
        card.addThemeVariants(CardVariant.OUTLINED);
        card.addClassName("home-card");
        Div tagLabel = new Div(tag);
        tagLabel.addClassName("home-card-tag");
        card.setHeader(tagLabel);
        card.setTitle(new Div(title));
        card.add(new Paragraph(description));
        card.addToFooter(new RouterLink("Open →", target));
        return card;
    }
}
