package com.example.uc23;

import com.example.TriggerCallbacks;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = KonamiCodeView.class)
class KonamiCodeViewTest extends SpringBrowserlessTest {

    @Test
    void eachCompletedSequenceCountsAsOneUnlock() {
        KonamiCodeView view = navigate(KonamiCodeView.class);
        assertEquals("UC23 — Konami code",
                findInView(H1.class).single().getText());
        Span hint = findInView(Span.class).id("hint");
        assertEquals("(no unlocks yet)", hint.getText());

        TriggerCallbacks.fire(view);
        runPendingSignalsTasks();
        assertEquals("🎉 Unlocked 1 time", hint.getText());

        TriggerCallbacks.fire(view);
        runPendingSignalsTasks();
        assertEquals("🎉 Unlocked 2 times", hint.getText());
    }
}
