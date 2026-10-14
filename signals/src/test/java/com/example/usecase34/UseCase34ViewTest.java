package com.example.usecase34;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = UseCase34View.class)
@WithMockUser
class UseCase34ViewTest extends SpringBrowserlessTest {

    @Autowired
    FeatureFlagService flags;

    @BeforeEach
    void resetFlagsBetweenTests() {
        // FeatureFlagService is application-scoped — reset state so tests
        // don't depend on execution order.
        flags.setNewCheckoutFlow(false);
        flags.setBetaUi(false);
    }

    @Test
    void readingConsumerSignalReturnsCurrentValue() {
        navigate(UseCase34View.class);
        runPendingSignalsTasks();

        UseCase34View view = (UseCase34View) getCurrentView();
        assertEquals(false, view.newCheckoutFlowSignal.peek(),
                "Consumer can read the current flag value");
        assertEquals(false, view.betaUiSignal.peek(),
                "Consumer can read the current flag value");
    }

    @Test
    void defaultsRenderClassicCheckoutAndNoBetaBadge() {
        navigate(UseCase34View.class);
        runPendingSignalsTasks();

        assertTrue(
                $view(Span.class).all().stream()
                        .anyMatch(s -> "Variant: classic multi-step checkout"
                                .equals(s.getText())),
                "Default flag state should show the classic checkout");

        // Beta badge should not be visible
        assertFalse(
                $view(Span.class).all().stream().anyMatch(
                        s -> "BETA".equals(s.getText()) && s.isVisible()),
                "BETA badge must be hidden by default");
    }

    @Test
    void flippingFlagsThroughServiceUpdatesAllConsumers() {
        navigate(UseCase34View.class);
        runPendingSignalsTasks();

        UseCase34View view = (UseCase34View) getCurrentView();
        view.flags.setNewCheckoutFlow(true);
        view.flags.setBetaUi(true);
        runPendingSignalsTasks();

        assertTrue(
                $view(Span.class).all().stream()
                        .anyMatch(s -> "Variant: NEW one-page checkout"
                                .equals(s.getText())),
                "New checkout label should appear");
        assertTrue(
                $view(Span.class).all().stream().anyMatch(
                        s -> "BETA".equals(s.getText()) && s.isVisible()),
                "BETA badge should now be visible");
    }

    @Test
    void adminCheckboxFlipsFlagThroughService() {
        navigate(UseCase34View.class);
        runPendingSignalsTasks();

        UseCase34View view = (UseCase34View) getCurrentView();
        Checkbox newCheckout = $view(Checkbox.class).all().stream()
                .filter(c -> "Enable new checkout flow".equals(c.getLabel()))
                .findFirst().orElseThrow();
        test(newCheckout).click();
        runPendingSignalsTasks();

        assertTrue(view.newCheckoutFlowSignal.peek(),
                "Checkbox click should flip the flag via the service");
    }

    @Test
    void newFlowParagraphsAreVisibleOnlyWhenFlagIsOn() {
        navigate(UseCase34View.class);
        runPendingSignalsTasks();

        UseCase34View view = (UseCase34View) getCurrentView();
        // Initially OFF — the "1) Cart + shipping + payment..." line must
        // be in the classic-only flow, not the new flow.
        long beforeOn = $view(Paragraph.class).all().stream()
                .filter(p -> p.getText() != null
                        && p.getText().startsWith("1) Cart + shipping"))
                .filter(Paragraph::isVisible).count();

        view.flags.setNewCheckoutFlow(true);
        runPendingSignalsTasks();

        long afterOn = $view(Paragraph.class).all().stream()
                .filter(p -> p.getText() != null
                        && p.getText().startsWith("1) Cart + shipping"))
                .filter(Paragraph::isVisible).count();

        assertTrue(afterOn > beforeOn,
                "Enabling the flag should reveal the new-flow paragraphs");
    }
}
