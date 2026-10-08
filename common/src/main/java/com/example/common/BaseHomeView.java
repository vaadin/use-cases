package com.example.common;

import java.util.Collection;
import java.util.Locale;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.card.CardVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.menu.MenuConfiguration;

/**
 * Shared scaffolding for the per-module home views. The constructor adds an H1
 * headline and an intro paragraph, styled as the page's lead by
 * {@code home.css}; subclasses choose how to render the body of the page:
 * <ul>
 * <li>{@link #addGroup(String, VaadinIcon, Accent, Collection)} — one section
 * per group of related use cases, each with an icon, a heading, a use-case
 * count and the group's cards in an accent color of its own.</li>
 * <li>{@link #addMenuCards(Collection)} — the entries of the given views as a
 * single grid of cards, for modules with only a handful of use cases.</li>
 * </ul>
 * Either way, each card is described by its view's {@link UseCaseDescription}.
 */
@StyleSheet("home.css")
public abstract class BaseHomeView extends VerticalLayout {

    /**
     * One of Aura's accent colors. A group's accent tints its icon, the tags
     * and links of its cards, and their hover border.
     */
    protected enum Accent {
        BLUE, GREEN, ORANGE, PURPLE, RED, YELLOW;

        String className() {
            return "aura-accent-" + name().toLowerCase(Locale.ROOT);
        }
    }

    protected BaseHomeView(String headline, String intro) {
        addClassName("home-view");
        add(new H1(headline));
        add(new Paragraph(intro));
    }

    /**
     * Appends a section for a group of related use cases: a header with the
     * icon, the heading as an {@code h2} and the number of use cases, followed
     * by the {@link #addMenuCards(Collection) cards} of the given views in menu
     * order. On wide screens the header becomes a sticky rail beside the cards.
     */
    protected Section addGroup(String heading, VaadinIcon icon, Accent accent,
            Collection<Class<? extends Component>> views) {
        Icon groupIcon = icon.create();
        groupIcon.addClassName("home-group-icon");
        H2 groupHeading = new H2(heading);
        groupHeading.addClassName("home-group-heading");
        Span count = new Span(views.size()
                + (views.size() == 1 ? " use case" : " use cases"));
        count.addClassName("home-group-count");
        Div header = new Div(groupIcon, groupHeading, count);
        header.addClassName("home-group-header");

        Section section = new Section(header, menuCards(views));
        section.addClassNames("home-group", accent.className());
        add(section);
        return section;
    }

    /**
     * Appends one {@link #homeCard(String, String, String, Class) card} per
     * menu entry of the given views, in menu order. A title of the form
     * {@code "UC1 — Short name"} is split into the card's tag and title, and
     * the view's {@link UseCaseDescription} becomes the description.
     */
    protected void addMenuCards(Collection<Class<? extends Component>> views) {
        add(menuCards(views));
    }

    private Div menuCards(Collection<Class<? extends Component>> views) {
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
        return cards;
    }

    private static Card homeCard(String tag, String title, String description,
            Class<? extends Component> target) {
        Card card = new Card();
        card.addThemeVariants(CardVariant.OUTLINED);
        card.addClassName("home-card");
        // The tag goes in the header prefix: the header slot's own content is
        // the title, so setHeader would hide it.
        Div tagLabel = new Div(tag);
        tagLabel.addClassName("home-card-tag");
        card.setHeaderPrefix(tagLabel);
        card.setTitle(title, 3);
        card.add(new Paragraph(description));
        RouterLink open = new RouterLink("Open →", target);
        open.addClassName("home-card-link");
        card.addToFooter(open);
        return card;
    }
}
