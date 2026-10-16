package com.example.uc4;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.UnaryOperator;

import com.example.common.UseCaseDescription;
import com.example.uc4.PriceList.Price;
import com.example.views.MainLayout;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC4 — Undo history for one editor window (UI scope).
 * <p>
 * A shop manager adjusts a price list. The undo button lives in a separate
 * toolbar component, a third component lists the whole history, and the editor
 * records every change in the same {@link UndoHistory}. All of them get it by
 * injection because it is {@code @UIScope}: shared by everything in this UI,
 * but not with the price list open in another tab, where undoing would revert
 * changes the user cannot see.
 * <p>
 * The price list is UI-scoped too ({@link PriceList}), so navigating away and
 * back keeps both, and undo still acts on the list on screen. Reload the page
 * to see the other side of UI scope: the reload creates a new UI and the
 * history is gone.
 */
@Route(value = "uc4", layout = MainLayout.class)
@UseCaseDescription("Keeping one undo history per editor window, shared by its components")
@PageTitle("UC4 — Undo history")
@Menu(order = 4, title = "UC4 — Undo history")
public class PriceListEditorView extends VerticalLayout {

    private final ValueSignal<List<Price>> prices;
    private final UndoHistory history;

    public PriceListEditorView(PriceList priceList, UndoHistory history,
            UndoToolbar toolbar, UndoHistoryList historyList) {
        this.prices = priceList.prices();
        this.history = history;

        add(new H1("UC4 — Undo history"));
        add(new Paragraph("The toolbar, the editor and the history list are "
                + "separate components that share a @UIScope undo history. "
                + "Another tab has its own history, and a reload starts a "
                + "new one."));

        Span uiId = new Span("UI #" + UI.getCurrentOrThrow().getUIId());
        uiId.addClassName("scope-badge");
        add(uiId, toolbar);

        HorizontalLayout actions = new HorizontalLayout(
                new Button("Raise all prices by 10%",
                        e -> change("Raised all prices by 10%",
                                PriceListEditorView::raiseByTenPercent)),
                new Button("Remove last product",
                        e -> change("Removed the last product",
                                PriceListEditorView::removeLast)));
        add(actions);

        UnorderedList list = new UnorderedList();
        Signal.effect(list, () -> {
            list.removeAll();
            prices.get().forEach(p -> list
                    .add(new ListItem(p.product() + " — " + p.amount())));
        });
        add(list, historyList);
    }

    private void change(String description,
            UnaryOperator<List<Price>> operation) {
        List<Price> before = prices.peek();
        prices.set(List.copyOf(operation.apply(before)));
        history.record(description, () -> prices.set(before));
    }

    private static List<Price> raiseByTenPercent(List<Price> list) {
        return list
                .stream().map(
                        p -> new Price(p.product(),
                                p.amount().multiply(new BigDecimal("1.10"))
                                        .setScale(2, RoundingMode.HALF_UP)))
                .toList();
    }

    private static List<Price> removeLast(List<Price> list) {
        return list.isEmpty() ? list : list.subList(0, list.size() - 1);
    }
}
