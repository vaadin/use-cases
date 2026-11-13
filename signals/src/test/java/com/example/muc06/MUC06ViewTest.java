package com.example.muc06;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;

import com.vaadin.browserless.BrowserlessApplicationContext;
import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.browserless.SpringBrowserlessApplicationContext;
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
    private ApplicationContext applicationContext;

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
    void bothUsersAddTasksConcurrently() {
        try (var app = createApp()) {
            var alice = app.newUser().newWindow();
            var bob = app.newUser().newWindow();
            alice.navigate(MUC06View.class);
            bob.navigate(MUC06View.class);
            assertEquals("Total: 4", statText(alice, "Total:"));

            alice.test(addTaskButton(alice)).click();
            bob.test(addTaskButton(bob)).click();
            bob.runPendingSignalsTasks();

            for (var window : List.of(alice, bob)) {
                assertEquals("Total: 6", statText(window, "Total:"),
                        "Should show 6 tasks (4 seed + 1 from each user)");
                assertEquals(6,
                        window.findInView(TextField.class).all().size());
            }
        }
    }

    @Test
    void otherUserCompletingTaskUpdatesCheckboxAndStatistics() {
        try (var app = createApp()) {
            var alice = app.newUser().newWindow();
            var bob = app.newUser().newWindow();
            alice.navigate(MUC06View.class);
            bob.navigate(MUC06View.class);

            // Seed data: task-2 is the only completed task, task-1 is first
            assertEquals("Completed: 1", statText(alice, "Completed:"));
            Checkbox aliceFirstCheckbox = alice.findInView(Checkbox.class)
                    .first();
            assertFalse(aliceFirstCheckbox.getValue(),
                    "Checkbox should initially be unchecked");

            bob.test(bob.findInView(Checkbox.class).first()).click();
            bob.runPendingSignalsTasks();

            assertEquals("Completed: 2", statText(alice, "Completed:"),
                    "Completed count should increase when Bob completes a task");
            assertTrue(aliceFirstCheckbox.getValue(),
                    "Checkbox should update when Bob completes the task");
        }
    }

    @Test
    void otherUserRenamingTaskUpdatesTitleField() {
        try (var app = createApp()) {
            var alice = app.newUser().newWindow();
            var bob = app.newUser().newWindow();
            alice.navigate(MUC06View.class);
            bob.navigate(MUC06View.class);
            TextField aliceFirstTitle = alice.findInView(TextField.class)
                    .first();

            bob.test(bob.findInView(TextField.class).first())
                    .setValue("Renamed by Bob");
            bob.runPendingSignalsTasks();

            assertEquals("Renamed by Bob", aliceFirstTitle.getValue(),
                    "Title field should update when Bob renames the task");
        }
    }

    @Test
    void otherUserChangingDueDateUpdatesDatePicker() {
        try (var app = createApp()) {
            var alice = app.newUser().newWindow();
            var bob = app.newUser().newWindow();
            alice.navigate(MUC06View.class);
            bob.navigate(MUC06View.class);
            DatePicker aliceFirstDatePicker = alice.findInView(DatePicker.class)
                    .first();
            LocalDate newDate = LocalDate.of(2030, 12, 25);

            bob.test(bob.findInView(DatePicker.class).first())
                    .setValue(newDate);
            bob.runPendingSignalsTasks();

            assertEquals(newDate, aliceFirstDatePicker.getValue(),
                    "Date picker should update when Bob changes the due date");
        }
    }

    private BrowserlessApplicationContext createApp() {
        return SpringBrowserlessApplicationContext.create(applicationContext,
                b -> b.withViewPackages(MUC06View.class)
                        .withSessionLockIsolation(true));
    }

    private static Button addTaskButton(BrowserlessUIContext window) {
        return window.findInView(Button.class).withText("Add Task").single();
    }

    private static String statText(BrowserlessUIContext window, String prefix) {
        return window.findInView(Span.class).all().stream().map(Span::getText)
                .filter(text -> text != null && text.startsWith(prefix))
                .findFirst().orElse("");
    }
}
