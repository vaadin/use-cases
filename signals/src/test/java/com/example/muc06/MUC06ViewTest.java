package com.example.muc06;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = MUC06View.class)
@WithMockUser
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MUC06ViewTest extends SpringBrowserlessTest {

    @Autowired
    private MUC06Signals muc06Signals;

    private String getStatText(String prefix) {
        return findInView(Span.class).all().stream()
                .filter(s -> s.getText() != null
                        && s.getText().startsWith(prefix))
                .map(Span::getText).findFirst().orElse("");
    }

    @Test
    void initialTaskCount() {
        navigate(MUC06View.class);
        runPendingSignalsTasks();

        assertTrue(getStatText("Total:").contains("4"),
                "Should have 4 seed tasks");
    }

    @Test
    void userAddsTaskViaButton() {
        navigate(MUC06View.class);
        runPendingSignalsTasks();

        Button addButton = findInView(Button.class).all().stream()
                .filter(b -> "Add Task".equals(b.getText())).findFirst()
                .orElseThrow();
        test(addButton).click();
        runPendingSignalsTasks();

        assertTrue(getStatText("Total:").contains("5"),
                "Count should increase to 5 after adding");
    }

    @Test
    void otherUserAddingTaskUpdatesView() {
        navigate(MUC06View.class);
        runPendingSignalsTasks();

        assertTrue(getStatText("Total:").contains("4"));

        // Simulate User B adding a task via the shared signal
        muc06Signals.getTasksSignal().insertLast(new MUC06Signals.Task(
                "task-userB", "User B's task", false, LocalDate.now()));
        runPendingSignalsTasks();

        // User A's view should reflect the new task
        assertTrue(getStatText("Total:").contains("5"),
                "User A should see task added by User B");
    }

    @Test
    void bothUsersAddTasksConcurrently() {
        navigate(MUC06View.class);
        runPendingSignalsTasks();

        // User A adds a task via the UI
        Button addButton = findInView(Button.class).all().stream()
                .filter(b -> "Add Task".equals(b.getText())).findFirst()
                .orElseThrow();
        test(addButton).click();
        runPendingSignalsTasks();

        // User B adds a task via the shared signal
        muc06Signals.getTasksSignal().insertLast(new MUC06Signals.Task(
                "task-userB", "User B's task", false, LocalDate.now()));
        runPendingSignalsTasks();

        // Both tasks should be visible
        assertTrue(getStatText("Total:").contains("6"),
                "Should show 6 tasks (4 seed + 1 from A + 1 from B)");
    }

    @Test
    void otherUserCompletingTaskUpdatesStatistics() {
        navigate(MUC06View.class);
        runPendingSignalsTasks();

        // Initially 1 completed (task-2 from seed data)
        assertTrue(getStatText("Completed:").contains("1"));

        // Simulate User B completing another task via the shared signal
        var tasks = muc06Signals.getTasksSignal().peek();
        // Find an incomplete task and mark it complete
        for (var taskSignal : tasks) {
            MUC06Signals.Task task = taskSignal.peek();
            if (!task.completed()) {
                taskSignal.set(new MUC06Signals.Task(task.id(), task.title(),
                        true, task.dueDate()));
                break;
            }
        }
        runPendingSignalsTasks();

        // User A should see the updated completed count
        assertTrue(getStatText("Completed:").contains("2"),
                "Completed count should increase when other user completes a task");
    }

    @Test
    void otherUserCompletingTaskUpdatesCheckbox() {
        navigate(MUC06View.class);
        runPendingSignalsTasks();

        // Find the first unchecked checkbox (corresponds to first incomplete
        // task)
        Checkbox firstCheckbox = findInView(Checkbox.class).all().stream()
                .filter(cb -> !cb.getValue()).findFirst().orElseThrow();
        assertFalse(firstCheckbox.getValue(),
                "Checkbox should initially be unchecked");

        // Simulate User B completing the first incomplete task via the shared
        // signal
        var tasks = muc06Signals.getTasksSignal().peek();
        for (var taskSignal : tasks) {
            MUC06Signals.Task task = taskSignal.peek();
            if (!task.completed()) {
                taskSignal.set(new MUC06Signals.Task(task.id(), task.title(),
                        true, task.dueDate()));
                break;
            }
        }
        runPendingSignalsTasks();

        // User A's checkbox should now be checked
        assertTrue(firstCheckbox.getValue(),
                "Checkbox should update when other user completes the task");
    }

    @Test
    void otherUserRenamingTaskUpdatesTitleField() {
        navigate(MUC06View.class);
        runPendingSignalsTasks();

        // Get the first text field (first task's title)
        TextField firstTitle = findInView(TextField.class).first();
        String originalTitle = firstTitle.getValue();

        // Simulate User B renaming the first task via the shared signal
        var tasks = muc06Signals.getTasksSignal().peek();
        var firstTaskSignal = tasks.getFirst();
        MUC06Signals.Task task = firstTaskSignal.peek();
        firstTaskSignal.set(new MUC06Signals.Task(task.id(),
                "Renamed by User B", task.completed(), task.dueDate()));
        runPendingSignalsTasks();

        // User A's text field should show the new title
        assertEquals("Renamed by User B", firstTitle.getValue(),
                "Title field should update when other user renames the task");
    }

    @Test
    void otherUserChangingDueDateUpdatesDatePicker() {
        navigate(MUC06View.class);
        runPendingSignalsTasks();

        // Get the first date picker
        DatePicker firstDatePicker = findInView(DatePicker.class).first();
        LocalDate newDate = LocalDate.of(2030, 12, 25);

        // Simulate User B changing the due date via the shared signal
        var tasks = muc06Signals.getTasksSignal().peek();
        var firstTaskSignal = tasks.getFirst();
        MUC06Signals.Task task = firstTaskSignal.peek();
        firstTaskSignal.set(new MUC06Signals.Task(task.id(), task.title(),
                task.completed(), newDate));
        runPendingSignalsTasks();

        // User A's date picker should show the new date
        assertEquals(newDate, firstDatePicker.getValue(),
                "Date picker should update when other user changes due date");
    }
}
