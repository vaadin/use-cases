package com.example.uc8;

import java.util.concurrent.atomic.AtomicInteger;

import com.example.collab.Peer;
import com.example.collab.PeerRig;
import com.example.collab.SignalBindings;
import com.example.collab.UseCaseView;
import com.example.uc8.RosterTopic.Employee;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.shared.SharedValueSignal;

/**
 * UC8 — Collaborate in a Grid. The sampler's {@code grid} sample: several users
 * working on one table.
 * <p>
 * The grid is where the signal API runs out of bindings. {@code AvatarGroup}
 * and {@code MessageList} both take a signal of item signals; {@code Grid}
 * takes a data provider, so the list has to be pushed into one by hand — see
 * {@link SignalBindings#bindItems}, and API-GAPS.md #5.
 * <p>
 * The trick that makes it work is to use the row signals as the grid's items
 * rather than the row values. A signal keeps its identity when its value
 * changes, so an edit refreshes a row instead of replacing it, and selection
 * survives.
 */
@Route(value = "grid", layout = MainLayout.class)
@RouteAlias(value = "uc8", layout = MainLayout.class)
@PageTitle("UC8 — Collaborate in a Grid")
@Menu(order = 8, title = "UC8 Collaborative Grid")
public class CollaborativeGridView extends UseCaseView {

    private static final AtomicInteger NEXT_ROW = new AtomicInteger();

    public CollaborativeGridView(RosterTopic topic) {
        super("UC8 — Collaborate in a Grid",
                "One employee table, several users. Adding or removing a row shows up in every panel, and "
                        + "selecting a row tells the others where you are looking — the highlighted rows are the "
                        + "ones somebody else has selected.");

        add(new PeerRig(peer -> peerView(topic, peer)));

        addNote("Grid has no signal-aware items API, so the shared list is pushed into a ListDataProvider by an "
                + "effect that also reads every row, and the row signals are the grid's items so that identity "
                + "survives an edit (API-GAPS.md #5).");
    }

    private static Component peerView(RosterTopic topic, Peer peer) {
        Grid<SharedValueSignal<Employee>> grid = new Grid<>();
        grid.setHeight("14rem");
        grid.addColumn(row -> value(row).firstName()).setHeader("First name");
        grid.addColumn(row -> value(row).lastName()).setHeader("Last name");
        grid.addColumn(row -> value(row).email()).setHeader("E-mail");

        SignalBindings.bindItems(grid, topic.rows());

        // Rows somebody else is looking at. peek() rather than get(), because
        // a part-name generator is not an effect: the refresh below is what
        // re-runs it.
        grid.setPartNameGenerator(row -> topic.watching().peek().entrySet()
                .stream()
                .anyMatch(entry -> !entry.getKey().equals(peer.key())
                        && value(row).id().equals(entry.getValue().peek()))
                                ? "peer-watched"
                                : null);

        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.addSelectionListener(
                event -> event.getFirstSelectedItem().ifPresentOrElse(
                        row -> topic.watching().put(peer.key(),
                                value(row).id()),
                        () -> topic.watching().remove(peer.key())));

        Signal.effect(grid, () -> {
            // Reading the entries subscribes to peers moving between rows;
            // the grid then has to be told, since the part name is not bound
            // to anything.
            topic.watching().get().values().forEach(Signal::get);
            grid.getDataProvider().refreshAll();
        });

        Span watchers = new Span();
        watchers.bindText(Signal.computed(() -> {
            long others = topic.watching().get().entrySet().stream()
                    .filter(entry -> !entry.getKey().equals(peer.key()))
                    .count();
            return others + " other user(s) have a row selected";
        }));

        Button add = new Button("Add row", event -> {
            int index = NEXT_ROW.incrementAndGet();
            topic.rows().insertLast(new Employee("new-" + index, "New",
                    "Colleague " + index, "new" + index + "@example.com"));
        });

        Button remove = new Button("Remove selected", event -> grid
                .getSelectedItems().stream().findFirst().ifPresent(row -> {
                    topic.rows().remove(row);
                    topic.watching().remove(peer.key());
                }));

        Div content = new Div(grid, watchers,
                new HorizontalLayout(add, remove));
        content.addDetachListener(event -> topic.watching().remove(peer.key()));
        return content;
    }

    private static Employee value(SharedValueSignal<Employee> row) {
        Employee employee = row.peek();
        return employee == null ? new Employee("", "", "", "") : employee;
    }
}
