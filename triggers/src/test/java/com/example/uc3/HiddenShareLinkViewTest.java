package com.example.uc3;

import com.example.TriggerCallbacks;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = HiddenShareLinkView.class)
class HiddenShareLinkViewTest extends SpringBrowserlessTest {

    private static final String LINK = "https://example.com/r/spring-sale?from=demo";

    @Test
    void typingASlugSendsTheFullLinkToTheCopyButtonButShowsItNowhere() {
        navigate(HiddenShareLinkView.class);
        assertEquals("UC3 — Hidden share link",
                findInView(H1.class).single().getText());

        test(findInView(TextField.class).id("slug")).setValue("spring-sale");
        runPendingSignalsTasks();

        assertTrue(
                TriggerCallbacks.pendingJavaScriptParameters().contains(LINK),
                "the link is mirrored to the button");
        assertFalse(
                findInView(Span.class).all().stream()
                        .anyMatch(span -> span.getText().contains(LINK)),
                "the link is not shown before it is copied");
    }

    @Test
    void confirmationShowsWhatTheBrowserCopied() {
        navigate(HiddenShareLinkView.class);
        Span confirmation = findInView(Span.class).id("confirmation");
        assertEquals("(no copy yet)", confirmation.getText());

        TriggerCallbacks.resolve(findInView(Button.class).id("copy"), LINK);

        assertTrue(confirmation.getText().endsWith(": " + LINK),
                confirmation.getText());
    }
}
