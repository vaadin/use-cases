package com.example.uc11;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.collab.Peer;
import com.example.collab.PeerRig;
import com.example.collab.UseCaseView;
import com.example.uc11.BoardTopic.Status;
import com.example.uc11.BoardTopic.Task;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.shared.SharedValueSignal;

/**
 * UC11 — Derive shared state instead of storing it. Beyond the sampler.
 * <p>
 * Collaboration Kit gives an application shared maps and lists, and every
 * number computed from them has to be maintained by hand: a counter written
 * alongside the data, kept in step by whoever remembers to. Two clients that
 * disagree about the count are then a real possibility, because the count is a
 * second source of truth.
 * <p>
 * {@code Signal.computed} removes the second copy. Every readout in this view —
 * the per-status counts, the per-user workload, "my tasks", the completion
 * share — is derived from the one shared list, on every client, and cannot be
 * stale by construction. Nothing in the application writes them.
 * <p>
 * The other half of the demonstration is that a derived value re-runs for what
 * it read and no more: claiming a task changes the workload table and the
 * "unassigned" count, and the completion share does not move.
 */
@Route(value = "computed", layout = MainLayout.class)
@RouteAlias(value = "uc11", layout = MainLayout.class)
@PageTitle("UC11 — Derive shared state instead of storing it")
@Menu(order = 11, title = "UC11 Computed shared state")
public class ComputedStateView extends UseCaseView {

    public ComputedStateView(BoardTopic topic) {
        super("UC11 — Derive shared state instead of storing it",
                "One shared task list. Claim a task or move it along in any panel: every readout in every panel "
                        + "follows, although no code writes any of them — they are all computed from the list.");

        add(new PeerRig(peer -> peerView(topic, peer)));

        addNote("There is no counter anywhere in this use case. Collaboration Kit would need one written next "
                + "to the data and kept in step; Signal.computed derives the same numbers per client from the "
                + "shared list, which is state that cannot drift.");
    }

    private static Component peerView(BoardTopic topic, Peer peer) {
        Signal<List<Task>> tasks = Signal
                .computed(() -> topic.tasks().get().stream().map(Signal::get)
                        .filter(task -> task != null).toList());

        Div rows = new Div();
        Signal.effect(rows, () -> {
            rows.removeAll();
            topic.tasks().get().forEach(entry -> rows.add(row(entry, peer)));
        });

        Span byStatus = new Span();
        byStatus.bindText(tasks.map(list -> {
            Map<Status, Long> counts = list.stream().collect(
                    Collectors.groupingBy(Task::status, Collectors.counting()));
            return "To do %d · doing %d · done %d".formatted(
                    counts.getOrDefault(Status.TODO, 0L),
                    counts.getOrDefault(Status.DOING, 0L),
                    counts.getOrDefault(Status.DONE, 0L));
        }));

        Span workload = new Span();
        workload.bindText(tasks.map(list -> {
            String assigned = list.stream()
                    .filter(task -> !task.assignee().isEmpty())
                    .collect(Collectors.groupingBy(Task::assignee,
                            Collectors.counting()))
                    .entrySet().stream()
                    .map(entry -> entry.getKey() + " " + entry.getValue())
                    .sorted().collect(Collectors.joining(", "));
            return assigned.isEmpty() ? "Nobody has claimed anything"
                    : "Workload: " + assigned;
        }));

        Span mine = new Span();
        mine.bindText(tasks.map(list -> "Yours: " + list.stream()
                .filter(task -> task.assignee().equals(peer.name())).count()));

        Span share = new Span();
        share.bindText(tasks.map(list -> {
            if (list.isEmpty()) {
                return "Nothing on the board";
            }
            long done = list.stream()
                    .filter(task -> task.status() == Status.DONE).count();
            return "Complete: " + (100 * done / list.size()) + "%";
        }));

        Div readout = new Div(byStatus, workload, mine, share);
        readout.addClassName("computed-readout");
        readout.getChildren().forEach(
                child -> child.getElement().getStyle().set("display", "block"));

        return new Div(rows, readout);
    }

    private static Component row(SharedValueSignal<Task> entry, Peer peer) {
        Span title = new Span();
        title.bindText(entry.map(task -> task == null ? ""
                : "%s — %s%s".formatted(task.title(), task.status(),
                        task.assignee().isEmpty() ? ""
                                : " (" + task.assignee() + ")")));

        Button claim = new Button("Claim",
                event -> entry.update(task -> task.withAssignee(peer.name())));
        claim.addThemeVariants(ButtonVariant.LUMO_SMALL,
                ButtonVariant.LUMO_TERTIARY);

        Button advance = new Button("Advance", event -> entry
                .update(task -> task.withStatus(next(task.status()))));
        advance.addThemeVariants(ButtonVariant.LUMO_SMALL,
                ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout row = new HorizontalLayout(title, claim, advance);
        row.setAlignItems(Alignment.CENTER);
        row.setPadding(false);
        return row;
    }

    private static Status next(Status status) {
        return switch (status) {
        case TODO -> Status.DOING;
        case DOING -> Status.DONE;
        case DONE -> Status.TODO;
        };
    }
}
