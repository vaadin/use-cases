package com.example.uc7;

import com.example.TriggerCallbacks;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = CrossTabBroadcastView.class)
class CrossTabBroadcastViewTest extends SpringBrowserlessTest {

    @Test
    void messageFromAnotherTabShowsInTheBadge() {
        CrossTabBroadcastView view = navigate(CrossTabBroadcastView.class);
        assertEquals("UC7 — Cross-tab broadcast",
                findInView(H1.class).single().getText());
        Span badge = findInView(Span.class).id("last");
        assertEquals("Last received: (none yet)", badge.getText());

        TriggerCallbacks.fire(view, "Hi from tab 2");
        runPendingSignalsTasks();

        assertEquals("Last received: Hi from tab 2", badge.getText());
    }
}
