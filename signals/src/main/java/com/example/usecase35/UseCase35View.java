package com.example.usecase35;

import java.util.List;

import com.example.usecase35.Card.Priority;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ListSignal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * Kanban "To do" column. A team lead can reorder cards with the up/down arrows,
 * add a single card via the form, paste a batch of three cards from a
 * hypothetical clipboard at a chosen index, and pull a fresh batch of backlog
 * items from the server (prepended in one shot).
 * <p>
 * Reorder uses {@code ListSignal.moveTo} — the entry's {@code ValueSignal}
 * identity is preserved across the move so children bound to that entry don't
 * lose their subscriptions. {@code insertAllFirst} and {@code insertAllAt}
 * bulk-insert with a single change notification, so subscribers like the "card
 * count" badge update once per batch instead of once per card.
 */
@PageTitle("Use Case 35: Kanban column")
@Route(value = "use-case-35", layout = MainLayout.class)
@Menu(order = 35, title = "UC 35: Kanban column")
@AnonymousAllowed
public class UseCase35View extends VerticalLayout {

    final ListSignal<Card> cards = new ListSignal<>();

    public UseCase35View() {
        setSpacing(true);
        setPadding(true);

        cards.insertAllLast(
                List.of(new Card("Wire up the new dashboard", Priority.HIGH),
                        new Card("Document the SSE endpoint", Priority.MEDIUM),
                        new Card("Audit dependency licenses", Priority.LOW)));

        add(new H2("Use Case 35: Kanban \"To do\" column"), new Paragraph(
                "Reorder cards with the arrows (moveTo preserves the"
                        + " card's signal identity). Add a single"
                        + " card with the form below. Use 'Pull"
                        + " backlog from server' to prepend a batch"
                        + " of fresh cards, and 'Paste 3 cards from"
                        + " clipboard' to insert a batch at a chosen"
                        + " position — both use insertAll* so the"
                        + " count badge updates only once per" + " batch."));

        add(buildAddCardRow(), buildBulkRow(), buildColumn(),
                buildExplanation());
    }

    private HorizontalLayout buildAddCardRow() {
        TextField titleField = new TextField("New card title");
        titleField.setWidth("280px");
        Select<Priority> priorityField = new Select<>();
        priorityField.setLabel("Priority");
        priorityField.setItems(Priority.values());
        priorityField.setValue(Priority.MEDIUM);

        Button add = new Button("Add to top", e -> {
            String title = titleField.getValue();
            if (title == null || title.isBlank()) {
                Notification.show("Type a title first");
                return;
            }
            cards.insertFirst(new Card(title, priorityField.getValue()));
            titleField.clear();
        });

        HorizontalLayout row = new HorizontalLayout(titleField, priorityField,
                add);
        row.setAlignItems(
                com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.BASELINE);
        return row;
    }

    private HorizontalLayout buildBulkRow() {
        Button pullBacklog = new Button("Pull backlog from server", e -> {
            // Server returns a batch of 4 — insert all of them at the top in
            // one shot so list-level subscribers re-render only once.
            cards.insertAllFirst(List.of(
                    new Card("From server: triage on-call alert",
                            Priority.HIGH),
                    new Card("From server: review OAuth migration",
                            Priority.MEDIUM),
                    new Card("From server: rotate API keys", Priority.MEDIUM),
                    new Card("From server: write Q3 retrospective",
                            Priority.LOW)));
        });

        Button paste = new Button("Paste 3 cards from clipboard at position 2",
                e -> {
                    if (cards.peek().size() < 1) {
                        Notification.show("Column is empty — paste at top");
                        cards.insertAllFirst(
                                List.of(new Card("Pasted A", Priority.MEDIUM),
                                        new Card("Pasted B", Priority.MEDIUM),
                                        new Card("Pasted C", Priority.MEDIUM)));
                        return;
                    }
                    cards.insertAllAt(1,
                            List.of(new Card("Pasted A", Priority.MEDIUM),
                                    new Card("Pasted B", Priority.MEDIUM),
                                    new Card("Pasted C", Priority.MEDIUM)));
                });

        Button clear = new Button("Clear column", e -> cards.clear());

        return new HorizontalLayout(pullBacklog, paste, clear);
    }

    private Div buildColumn() {
        Div column = new Div();
        column.getStyle().set("background-color", "var(--lumo-contrast-5pct)")
                .set("padding", "var(--lumo-space-m)")
                .set("border-radius", "8px").set("min-height", "300px");

        Span count = new Span();
        count.bindText(Signal
                .computed(() -> "To do (" + cards.get().size() + " cards):"));
        count.getStyle().set("font-weight", "bold").set("display", "block")
                .set("margin-bottom", "var(--lumo-space-s)");

        Div stack = new Div();
        stack.getStyle().set("display", "flex").set("flex-direction", "column")
                .set("gap", "var(--lumo-space-xs)");
        stack.bindChildren(cards, this::buildCard);

        column.add(count, stack);
        return column;
    }

    private Div buildCard(ValueSignal<Card> entry) {
        Div card = new Div();
        card.getStyle().set("padding", "var(--lumo-space-s)")
                .set("background-color", "var(--lumo-base-color)")
                .set("border-radius", "4px")
                .set("box-shadow", "var(--lumo-box-shadow-xs)")
                .set("display", "flex").set("align-items", "center")
                .set("gap", "var(--lumo-space-s)");

        Button up = new Button("↑", e -> {
            int idx = cards.peek().indexOf(entry);
            if (idx > 0) {
                cards.moveTo(entry, idx - 1);
            }
        });
        Button down = new Button("↓", e -> {
            int idx = cards.peek().indexOf(entry);
            if (idx >= 0 && idx < cards.peek().size() - 1) {
                cards.moveTo(entry, idx + 1);
            }
        });

        Span title = new Span();
        title.bindText(entry.map(Card::title));
        title.getStyle().set("flex", "1");

        Span priority = new Span();
        priority.bindText(entry.map(c -> c.priority().name()));
        priority.getStyle().set("font-size", "var(--lumo-font-size-s)")
                .set("padding", "2px 8px").set("border-radius", "12px");
        priority.getStyle().bind("background-color",
                entry.map(c -> switch (c.priority()) {
                case HIGH -> "var(--lumo-error-color-10pct)";
                case MEDIUM -> "var(--lumo-primary-color-10pct)";
                case LOW -> "var(--lumo-contrast-10pct)";
                }));

        Button remove = new Button("✕", e -> cards.remove(entry));

        card.add(up, down, title, priority, remove);
        return card;
    }

    private Div buildExplanation() {
        Div box = new Div();
        box.getStyle().set("background-color", "var(--lumo-contrast-5pct)")
                .set("padding", "var(--lumo-space-m)")
                .set("border-radius", "8px");

        H3 title = new H3("Why moveTo / insertAllAt / insertAllFirst?");
        title.getStyle().set("margin-top", "0");

        Paragraph p = new Paragraph(
                "moveTo keeps the card's ValueSignal identity stable across a"
                        + " reorder, so the row component bound to that entry"
                        + " doesn't have to be re-created. insertAll* batches"
                        + " several inserts into a single change notification"
                        + " — useful when a bulk-fetch from the server arrives"
                        + " or when the user pastes multiple items at once.");

        box.add(title, p);
        return box;
    }
}
