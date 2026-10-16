package com.example.uc11;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.collab.Peer;
import com.example.collab.PeerRig;
import com.example.collab.UseCaseView;
import com.example.common.UseCaseDescription;
import com.example.uc11.BoardTopic.Status;
import com.example.uc11.BoardTopic.Task;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
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
 * UC11 — Keep totals always in step. Beyond the sampler.
 * <p>
 * Collaboration Kit gives an application shared maps and lists, and every
 * number computed from them has to be maintained by hand: a counter written
 * alongside the data, kept in step by whoever remembers to. Two clients that
 * disagree about the count are then a real possibility, because the count is a
 * second source of truth.
 * <p>
 * {@code Signal.computed} removes the second copy. Every total in this view —
 * the per-status counts, who has claimed how much, "your tasks", the completion
 * share — is derived from the one shared task list, on every client, and cannot
 * be stale by construction. Nothing in the application writes them.
 * <p>
 * The board itself is deliberately small: a task is claimed by one user, and
 * only that user can start and finish it. Each of those steps is one
 * {@code update} on the task's entry, and every total follows.
 */
@Route(value = "computed", layout = MainLayout.class)
@RouteAlias(value = "uc11", layout = MainLayout.class)
@PageTitle("UC11 — Keep totals always in step")
@UseCaseDescription("Deriving shared totals instead of storing them")
@Menu(order = 11, title = "UC11 — Keep totals always in step")
public class ComputedStateView extends UseCaseView {

    public ComputedStateView(BoardTopic topic) {
        super("UC11 — Keep totals always in step",
                "A shared task list. Claim a task, then start and finish it — only the user who claimed a task "
                        + "can move it on. The totals under the list are not stored anywhere: each window computes "
                        + "them from the task list, so they always match it.");

        add(new PeerRig(peer -> peerView(topic, peer)));

        addNote("No code writes any of the totals. They are Signal.computed over the shared task list, so they "
                + "change with it in every window and cannot disagree with it. With Collaboration Kit each total "
                + "would be a separate shared value that the code has to update next to the task.");
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
            return "To do %d · in progress %d · done %d".formatted(
                    counts.getOrDefault(Status.TODO, 0L),
                    counts.getOrDefault(Status.DOING, 0L),
                    counts.getOrDefault(Status.DONE, 0L));
        }));

        Span claimed = new Span();
        claimed.bindText(tasks.map(list -> {
            String assigned = list.stream().filter(Task::hasAssignee)
                    .collect(Collectors.groupingBy(Task::assignee,
                            Collectors.counting()))
                    .entrySet().stream()
                    .map(entry -> entry.getKey() + " " + entry.getValue())
                    .sorted().collect(Collectors.joining(", "));
            return assigned.isEmpty() ? "Nobody has claimed a task yet"
                    : "Claimed: " + assigned;
        }));

        Span mine = new Span();
        mine.bindText(tasks.map(list -> "Your tasks: " + list.stream()
                .filter(task -> task.isAssignedTo(peer)).count()));

        Span share = new Span();
        share.bindText(tasks.map(list -> {
            if (list.isEmpty()) {
                return "Nothing on the board";
            }
            long done = list.stream()
                    .filter(task -> task.status() == Status.DONE).count();
            return "Complete: " + (100 * done / list.size()) + "%";
        }));

        Div readout = new Div(new H4("Totals, computed from the list"),
                byStatus, claimed, mine, share);
        readout.addClassName("computed-readout");
        readout.getChildren().forEach(
                child -> child.getElement().getStyle().set("display", "block"));

        return new Div(rows, readout);
    }

    private static Component row(SharedValueSignal<Task> entry, Peer peer) {
        Span title = new Span();
        title.bindText(entry.map(task -> task == null ? ""
                : "%s — %s%s".formatted(task.title(), label(task.status()),
                        !task.hasAssignee() ? ""
                                : " (" + task.assignee() + ")")));

        // One button whose meaning follows the task: claim it while nobody
        // has, then start and finish it — and nothing for anybody else.
        Signal<String> caption = entry
                .map(task -> task == null ? "" : actionFor(task, peer));
        Button action = new Button(caption);
        action.addClickListener(event -> entry.update(task -> act(task, peer)));
        action.addThemeVariants(ButtonVariant.SMALL, ButtonVariant.TERTIARY);
        action.bindVisible(caption.map(text -> !text.isEmpty()));

        HorizontalLayout row = new HorizontalLayout(title, action);
        row.setAlignItems(Alignment.CENTER);
        row.setPadding(false);
        return row;
    }

    /** What this peer can do with a task next, empty for nothing. */
    private static String actionFor(Task task, Peer peer) {
        if (!task.hasAssignee()) {
            return "Claim";
        }
        if (!task.isAssignedTo(peer)) {
            return "";
        }
        return switch (task.status()) {
        case TODO -> "Start";
        case DOING -> "Finish";
        case DONE -> "";
        };
    }

    /**
     * The change a click makes, decided again on the current value: by the time
     * the update runs, somebody else may have claimed the task.
     */
    private static Task act(Task task, Peer peer) {
        if (!task.hasAssignee()) {
            return task.withAssignee(peer);
        }
        if (!task.isAssignedTo(peer)) {
            return task;
        }
        return switch (task.status()) {
        case TODO -> task.withStatus(Status.DOING);
        case DOING -> task.withStatus(Status.DONE);
        case DONE -> task;
        };
    }

    private static String label(Status status) {
        return switch (status) {
        case TODO -> "to do";
        case DOING -> "in progress";
        case DONE -> "done";
        };
    }
}
