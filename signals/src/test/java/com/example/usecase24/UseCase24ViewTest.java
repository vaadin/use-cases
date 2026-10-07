package com.example.usecase24;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.signals.local.ListSignal;
import com.vaadin.flow.signals.local.ValueSignal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = UseCase24View.class)
@WithMockUser
class UseCase24ViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithFilters() {
        navigate(UseCase24View.class);

        // Type filter and Status filter
        assertEquals(2, findInView(ComboBox.class).all().size());
    }

    @Test
    void viewRendersWithActionButtons() {
        navigate(UseCase24View.class);

        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> "Mark All Read".equals(b.getText())));
        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> "Clear All".equals(b.getText())));
    }

    @Test
    void viewRendersWithAddButtons() {
        navigate(UseCase24View.class);

        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> "Add Info".equals(b.getText())));
        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> "Add Warning".equals(b.getText())));
        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> "Add Error".equals(b.getText())));
        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> "Add Success".equals(b.getText())));
    }

    @Test
    void initialNotificationCount() {
        navigate(UseCase24View.class);
        runPendingSignalsTasks();

        // 8 seed notifications, 5 unread
        Span countLabel = findInView(Span.class).all().stream().filter(
                s -> s.getText() != null && s.getText().contains("Showing"))
                .findFirst().orElseThrow();
        assertTrue(countLabel.getText().contains("8 notification"));
        assertTrue(countLabel.getText().contains("5 unread"));
    }

    @Test
    void unreadBadgePresent() {
        navigate(UseCase24View.class);
        runPendingSignalsTasks();

        assertTrue(findInView(Span.class).all().stream().anyMatch(
                s -> s.getText() != null && s.getText().contains("unread")));
    }

    @Test
    void addInfoIncreasesCount() {
        navigate(UseCase24View.class);
        runPendingSignalsTasks();

        Button addInfo = findInView(Button.class).all().stream()
                .filter(b -> "Add Info".equals(b.getText())).findFirst()
                .orElseThrow();
        test(addInfo).click();
        runPendingSignalsTasks();

        Span countLabel = findInView(Span.class).all().stream().filter(
                s -> s.getText() != null && s.getText().contains("Showing"))
                .findFirst().orElseThrow();
        assertTrue(countLabel.getText().contains("9 notification"));
    }

    @Test
    void clearAllRemovesNotifications() {
        navigate(UseCase24View.class);
        runPendingSignalsTasks();

        Button clearAll = findInView(Button.class).all().stream()
                .filter(b -> "Clear All".equals(b.getText())).findFirst()
                .orElseThrow();
        test(clearAll).click();
        runPendingSignalsTasks();

        Span countLabel = findInView(Span.class).all().stream().filter(
                s -> s.getText() != null && s.getText().contains("Showing"))
                .findFirst().orElseThrow();
        assertTrue(countLabel.getText().contains("0 notification"));
    }

    @Test
    void markAllReadSetsUnreadToZero() {
        navigate(UseCase24View.class);
        runPendingSignalsTasks();

        // Verify we start with 5 unread
        Span countLabel = findInView(Span.class).all().stream().filter(
                s -> s.getText() != null && s.getText().contains("Showing"))
                .findFirst().orElseThrow();
        assertTrue(countLabel.getText().contains("5 unread"));

        // Click Mark All Read
        Button markAllRead = findInView(Button.class).all().stream()
                .filter(b -> "Mark All Read".equals(b.getText())).findFirst()
                .orElseThrow();
        test(markAllRead).click();
        runPendingSignalsTasks();

        // Verify 0 unread
        countLabel = findInView(Span.class).all().stream().filter(
                s -> s.getText() != null && s.getText().contains("Showing"))
                .findFirst().orElseThrow();
        assertTrue(countLabel.getText().contains("0 unread"),
                "Expected 0 unread but got: " + countLabel.getText());
    }

    @Test
    void markReadOnSingleNotificationTogglesReadState() {
        navigate(UseCase24View.class);

        // Create a ListSignal with one unread notification
        ListSignal<Notification> signal = new ListSignal<>();
        Notification unread = new Notification(UUID.randomUUID().toString(),
                "Test", "msg", NotificationType.INFO, false,
                LocalDateTime.now());
        signal.insertLast(unread);

        // Render a card and attach it to the view so test framework can click
        // Before the fix, the click handler used get() outside a reactive
        // context which throws IllegalStateException
        UseCase24View view = findInView(UseCase24View.class).first();
        Div card = view.createNotificationCard(unread, signal);
        view.add(card);

        Button markRead = findButton(card, "Mark Read");
        test(markRead).click();

        // The notification should now be read
        ValueSignal<Notification> entry = signal.peek().getFirst();
        assertTrue(entry.peek().read(),
                "Notification should be marked as read after clicking Mark Read");
    }

    @Test
    void dismissRemovesNotificationFromSignal() {
        navigate(UseCase24View.class);

        // Create a ListSignal with one notification
        ListSignal<Notification> signal = new ListSignal<>();
        Notification n = new Notification(UUID.randomUUID().toString(), "Test",
                "msg", NotificationType.WARNING, false, LocalDateTime.now());
        signal.insertLast(n);
        assertEquals(1, signal.peek().size());

        // Render a card and attach it to the view so test framework can click
        // Before the fix, the click handler used get() outside a reactive
        // context which throws IllegalStateException
        UseCase24View view = findInView(UseCase24View.class).first();
        Div card = view.createNotificationCard(n, signal);
        view.add(card);

        Button dismiss = findButton(card, "Dismiss");
        test(dismiss).click();

        // The notification should be removed
        assertEquals(0, signal.peek().size(),
                "Notification should be removed after clicking Dismiss");
    }

    private Button findButton(Div card, String text) {
        return card.getChildren()
                .flatMap(c -> c instanceof Div div ? div.getChildren()
                        : java.util.stream.Stream.of(c))
                .flatMap(c -> c instanceof Div div ? div.getChildren()
                        : java.util.stream.Stream.of(c))
                .filter(Button.class::isInstance).map(Button.class::cast)
                .filter(b -> text.equals(b.getText())).findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Button '" + text + "' not found in card"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void filterByTypeNarrowsResults() {
        navigate(UseCase24View.class);
        runPendingSignalsTasks();

        ComboBox<String> typeFilter = (ComboBox<String>) findInView(
                ComboBox.class).all().stream()
                .filter(c -> "Type".equals(c.getLabel())).findFirst()
                .orElseThrow();
        test(typeFilter).selectItem("ERROR");
        runPendingSignalsTasks();

        Span countLabel = findInView(Span.class).all().stream().filter(
                s -> s.getText() != null && s.getText().contains("Showing"))
                .findFirst().orElseThrow();
        assertTrue(countLabel.getText().contains("2 notification"));
    }
}
