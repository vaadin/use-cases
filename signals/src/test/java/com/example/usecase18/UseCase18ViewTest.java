package com.example.usecase18;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = UseCase18View.class)
@WithMockUser
class UseCase18ViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithInitialTasks() {
        navigate(UseCase18View.class);
        runPendingSignalsTasks();

        assertEquals(1, findInView(MessageList.class).all().size());
        assertEquals(1, findInView(MessageInput.class).all().size());
    }

    @SuppressWarnings("unchecked")
    @Test
    void gridShowsThreeInitialTasks() {
        navigate(UseCase18View.class);
        runPendingSignalsTasks();

        Grid<Task> grid = findInView(Grid.class).single();
        assertEquals(3, test(grid).size());
    }

    @SuppressWarnings("unchecked")
    @Test
    void updatingTaskSignalUpdatesGrid() {
        navigate(UseCase18View.class);
        runPendingSignalsTasks();

        UseCase18View view = (UseCase18View) getCurrentView();

        // Find the "Write unit tests" task signal and change its status
        var taskSignals = view.tasksSignal.peek();
        var unitTestSignal = taskSignals.stream()
                .filter(sig -> sig.peek().title().equals("Write unit tests"))
                .findFirst().orElseThrow();

        assertEquals(Task.TaskStatus.IN_PROGRESS,
                unitTestSignal.peek().status(),
                "Precondition: task should start as IN_PROGRESS");

        // Update the status via the signal (simulates what the tool does)
        Task updated = unitTestSignal.peek().withStatus(Task.TaskStatus.DONE);
        unitTestSignal.set(updated);
        runPendingSignalsTasks();

        // Verify the signal was updated
        assertEquals(Task.TaskStatus.DONE, unitTestSignal.peek().status());

        // Verify the grid reflects the change
        Grid<Task> grid = findInView(Grid.class).single();
        boolean found = false;
        for (int i = 0; i < test(grid).size(); i++) {
            Task task = test(grid).getRow(i);
            if ("Write unit tests".equals(task.title())) {
                assertEquals(Task.TaskStatus.DONE, task.status(),
                        "Grid should show updated status DONE");
                found = true;
            }
        }
        assertTrue(found, "Should find 'Write unit tests' in the grid");
    }
}
