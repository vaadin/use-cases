package com.example.uc11;

import jakarta.annotation.PostConstruct;

import java.util.List;

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

    public record Task(String id, String title, Status status,
            String assignee) {

        public Task withStatus(Status value) {
            return new Task(id, title, value, assignee);
        }

        public Task withAssignee(String value) {
            return new Task(id, title, status, value);
        }
    }

    private final SharedListSignal<Task> tasks = new SharedListSignal<>(
            Task.class);

    public SharedListSignal<Task> tasks() {
        return tasks;
    }

    @PostConstruct
    void seed() {
        if (tasks.peek().isEmpty()) {
            tasks.insertAllLast(List.of(
                    new Task("t1", "Count the returns bin", Status.TODO, ""),
                    new Task("t2", "Call the courier", Status.TODO, ""),
                    new Task("t3", "Restock aisle 4", Status.DOING, ""),
                    new Task("t4", "File the damage report", Status.TODO, ""),
                    new Task("t5", "Close the till", Status.DONE, "")));
        }
    }
}
