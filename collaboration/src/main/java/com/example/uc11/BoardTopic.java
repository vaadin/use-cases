package com.example.uc11;

import jakarta.annotation.PostConstruct;

import java.util.List;

import com.example.collab.Peer;
import org.springframework.stereotype.Component;

import com.vaadin.flow.signals.shared.SharedListSignal;

/**
 * UC11's shared board: a list of tasks and nothing else.
 * <p>
 * No counts, no per-user totals, no "unassigned" list. That is the point of the
 * use case: everything else the views show is computed from this one structure,
 * so there is no second copy to keep in step.
 */
@Component
public class BoardTopic {

    public enum Status {
        TODO, DOING, DONE
    }

    /**
     * A task and who claimed it. The claimer is kept by peer id as well as by
     * name: names are handed out again once a user leaves, ids never are, so
     * only the id says whether a task is <em>yours</em>. {@code 0} is nobody.
     */
    public record Task(String id, String title, Status status, String assignee,
            int assigneeId) {

        public boolean hasAssignee() {
            return assigneeId != 0;
        }

        public boolean isAssignedTo(Peer peer) {
            return assigneeId == peer.id();
        }

        public Task withStatus(Status value) {
            return new Task(id, title, value, assignee, assigneeId);
        }

        public Task withAssignee(Peer peer) {
            return new Task(id, title, status, peer.name(), peer.id());
        }
    }

    /** Id of the seeded user who is not online; no peer is ever given it. */
    private static final int OFFLINE_USER = -1;

    private final SharedListSignal<Task> tasks = new SharedListSignal<>(
            Task.class);

    public SharedListSignal<Task> tasks() {
        return tasks;
    }

    @PostConstruct
    void seed() {
        if (tasks.peek().isEmpty()) {
            // Two tasks belong to a user who is not online, so that the board
            // does not start out as everybody's.
            tasks.insertAllLast(List.of(
                    new Task("t1", "Count the returns bin", Status.TODO, "", 0),
                    new Task("t2", "Call the courier", Status.TODO, "", 0),
                    new Task("t3", "Restock aisle 4", Status.DOING, "Mika",
                            OFFLINE_USER),
                    new Task("t4", "File the damage report", Status.TODO, "",
                            0),
                    new Task("t5", "Close the till", Status.DONE, "Mika",
                            OFFLINE_USER)));
        }
    }
}
