package com.example.uc9;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.internal.StateNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ViewPackages(classes = ScrollIntoViewView.class)
class ScrollIntoViewViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersClientAndServerRowsAndThreeSections() {
        navigate(ScrollIntoViewView.class);

        assertEquals("UC9 — Scroll into view",
                findInView(H1.class).single().getText());
        for (String section : List.of("a", "b", "c")) {
            assertNotNull(findInView(Button.class).id("client-" + section));
            assertNotNull(findInView(Button.class).id("server-" + section));
            assertNotNull(findInView(Div.class).id("section-" + section));
        }
    }

    @Test
    void onlyTheServerRowNeedsTheServerToScroll() {
        navigate(ScrollIntoViewView.class);
        StateNode sectionB = findInView(Div.class).id("section-b").getElement()
                .getNode();

        test(findInView(Button.class).id("client-b")).click();
        assertEquals(0, scrollCallsFor(sectionB),
                "the client row scrolls in the browser, not from the server");

        test(findInView(Button.class).id("server-b")).click();
        assertEquals(1, scrollCallsFor(sectionB));
    }

    private static long scrollCallsFor(StateNode node) {
        UI ui = UI.getCurrent();
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        return ui.getInternals().dumpPendingJavaScriptInvocations().stream()
                .filter(pending -> pending.getOwner() == node)
                .filter(pending -> pending.getInvocation().getExpression()
                        .contains("scrollIntoView"))
                .count();
    }
}
