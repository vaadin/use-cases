package com.example.usecase33;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = UseCase33View.class)
@WithMockUser
class UseCase33ViewTest extends SpringBrowserlessTest {

    @Test
    void initialRenderShowsOrdersAndNoSelection() {
        navigate(UseCase33View.class);
        runPendingSignalsTasks();

        UseCase33View view = (UseCase33View) getCurrentView();
        assertEquals(5, view.orders.peek().size());
        assertTrue(
                $view(Span.class).all().stream().anyMatch(
                        s -> "(No order selected)".equals(s.getText())),
                "Details pane should show empty placeholder initially");
        assertEquals(0, view.detailsOpenAnimations.get(),
                "No animation should have played yet");
    }

    @Test
    void selectingOrderFiresOpenAnimationOnce() {
        navigate(UseCase33View.class);
        runPendingSignalsTasks();

        UseCase33View view = (UseCase33View) getCurrentView();
        view.selected.set(view.orders.peek().get(0).peek());
        runPendingSignalsTasks();

        assertEquals(1, view.detailsOpenAnimations.get(),
                "Animation should fire exactly once on first selection");
    }

    @Test
    void serverStatusPushDoesNotReplayAnimation() {
        navigate(UseCase33View.class);
        runPendingSignalsTasks();

        UseCase33View view = (UseCase33View) getCurrentView();
        // Select order 1001
        view.selected.set(view.orders.peek().get(0).peek());
        runPendingSignalsTasks();
        int afterSelect = view.detailsOpenAnimations.get();

        // Simulate server push: rotate status on the selected order
        Button push = $view(Button.class).all().stream()
                .filter(b -> "Push status update".equals(b.getText()))
                .findFirst().orElseThrow();
        test(push).click();
        runPendingSignalsTasks();

        assertEquals(afterSelect, view.detailsOpenAnimations.get(),
                "Server status push must NOT replay the open animation"
                        + " (id-equality)");
    }

    @Test
    void serverPushIsVisibleInDetailsPane() {
        navigate(UseCase33View.class);
        runPendingSignalsTasks();

        UseCase33View view = (UseCase33View) getCurrentView();
        view.selected.set(view.orders.peek().get(0).peek()); // 1001, Pending
        runPendingSignalsTasks();

        Button push = $view(Button.class).all().stream()
                .filter(b -> "Push status update".equals(b.getText()))
                .findFirst().orElseThrow();
        test(push).click();
        runPendingSignalsTasks();

        // Status moves Pending -> Shipped
        assertTrue(
                $view(Span.class).all().stream()
                        .anyMatch(s -> "Status: Shipped".equals(s.getText())),
                "New status should be reflected in the details pane");
    }

    @Test
    void switchingToDifferentOrderFiresAnimationAgain() {
        navigate(UseCase33View.class);
        runPendingSignalsTasks();

        UseCase33View view = (UseCase33View) getCurrentView();
        view.selected.set(view.orders.peek().get(0).peek());
        runPendingSignalsTasks();
        view.selected.set(view.orders.peek().get(2).peek());
        runPendingSignalsTasks();

        assertEquals(2, view.detailsOpenAnimations.get(),
                "Switching to a different order id must replay the animation");
    }
}
