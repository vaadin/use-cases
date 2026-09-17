package com.example.uc9;

import com.example.collab.Peer;
import com.example.collab.PeerRig;
import com.example.collab.SignalBindings;
import com.example.collab.UseCaseView;
import com.example.uc9.CatalogTopic.Product;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.gridpro.GridPro;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.shared.SharedValueSignal;

/**
 * UC9 — Collaborate in a Grid Pro. The sampler's {@code grid-pro} sample:
 * editing cells while other people edit theirs.
 * <p>
 * Editing is the interesting part, and the answer is
 * {@code SharedValueSignal.update}: a read-modify-write applied to the row
 * signal, so two peers editing different columns of the same row both keep
 * their change. Writing {@code set(row.withPrice(...))} instead would send a
 * whole row built from a value read a moment earlier, and the slower of two
 * editors would silently undo the other — the bug this API exists to prevent.
 * <p>
 * What cannot be done as well as Collaboration Kit does it is the highlight.
 * {@code GridPro}'s {@code CellEditStartedEvent} reports the item, not the
 * column, so this view can only say <em>which row</em> a peer is editing. See
 * API-GAPS.md #12.
 */
@Route(value = "grid-pro", layout = MainLayout.class)
@RouteAlias(value = "uc9", layout = MainLayout.class)
@PageTitle("UC9 — Collaborate in a Grid Pro")
@Menu(order = 9, title = "UC9 Collaborative Grid Pro")
public class CollaborativeGridProView extends UseCaseView {

    public CollaborativeGridProView(CatalogTopic topic) {
        super("UC9 — Collaborate in a Grid Pro",
                "One price list, several editors. Double-click a cell in any panel and edit it: the change "
                        + "arrives in the other panels, and the row you are editing is highlighted for everybody "
                        + "else while you are in it.");

        add(new PeerRig(peer -> peerView(topic, peer)));

        addNote("Each cell edit is a SharedValueSignal.update on that row — a read-modify-write, so two peers "
                + "editing different columns of one row do not overwrite each other. The highlight is per row "
                + "rather than per cell because GridPro's cell-edit event does not say which column "
                + "(API-GAPS.md #12).");
    }

    private static Component peerView(CatalogTopic topic, Peer peer) {
        GridPro<SharedValueSignal<Product>> grid = new GridPro<>();
        grid.setHeight("14rem");
        grid.setSingleCellEdit(true);

        grid.addEditColumn(row -> value(row).name())
                .text((row, edited) -> row
                        .update(current -> current.withName(edited)))
                .setHeader("Product");
        grid.addEditColumn(row -> value(row).category())
                .text((row, edited) -> row
                        .update(current -> current.withCategory(edited)))
                .setHeader("Category");
        grid.addEditColumn(row -> value(row).price())
                .text((row, edited) -> row
                        .update(current -> current.withPrice(edited)))
                .setHeader("Price");

        SignalBindings.bindItems(grid, topic.rows());

        grid.setPartNameGenerator(row -> topic.editing().peek().entrySet()
                .stream()
                .anyMatch(entry -> !entry.getKey().equals(peer.key())
                        && value(row).id().equals(entry.getValue().peek()))
                                ? "peer-edited"
                                : null);

        grid.addCellEditStartedListener(event -> topic.editing().put(peer.key(),
                value(event.getItem()).id()));
        // There is no cell-edit-finished event either, so the write is what
        // ends the edit as far as the other peers can tell.
        grid.addItemPropertyChangedListener(
                event -> topic.editing().remove(peer.key()));

        Signal.effect(grid, () -> {
            topic.editing().get().values().forEach(Signal::get);
            grid.getDataProvider().refreshAll();
        });

        Span editors = new Span();
        editors.bindText(Signal.computed(() -> {
            long others = topic.editing().get().entrySet().stream()
                    .filter(entry -> !entry.getKey().equals(peer.key()))
                    .count();
            return others + " other user(s) editing a row";
        }));

        Div content = new Div(grid, editors);
        content.addDetachListener(event -> topic.editing().remove(peer.key()));
        return content;
    }

    private static Product value(SharedValueSignal<Product> row) {
        Product product = row.peek();
        return product == null ? new Product("", "", "", "") : product;
    }
}
