package com.example.acme;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvestigationTest {

    @Test
    void startsHiddenWithTheLensDividerAndTheLead() {
        Investigation investigation = new Investigation("Noticed the wait?");

        assertFalse(investigation.isVisible(),
                "the readout must wait until the story's problem has been "
                        + "felt");
        assertEquals("What Observability Kit sees",
                investigation.getChildren().filter(H2.class::isInstance)
                        .map(H2.class::cast).findFirst().orElseThrow()
                        .getText());
        assertTrue(investigation.getElement().getTextRecursively()
                .contains("Noticed the wait?"));
    }

    @Test
    void stepsAreCollapsibleAndKeepTheirRequestedState() {
        Investigation investigation = new Investigation("lead");

        Details first = investigation.step("2 — First", true,
                new Span("a"));
        Details second = investigation.step("3 — Second", false,
                new Span("b"));

        assertEquals("2 — First", first.getSummaryText());
        assertTrue(first.isOpened(), "the first step is where the reader lands");
        assertFalse(second.isOpened(),
                "later steps wait until the reader has taken the earlier ones");
        assertTrue(first.getClassNames().contains("investigation-step"));
    }

    @Test
    void revealShowsTheReadoutAndRefreshesItAtOnce() {
        Investigation investigation = new Investigation("lead");
        AtomicInteger refreshes = new AtomicInteger();
        investigation.onRefresh(refreshes::incrementAndGet);

        investigation.reveal();

        assertTrue(investigation.isVisible());
        assertEquals(1, refreshes.get(),
                "the reveal shows what the kit has recorded so far; the "
                        + "second, deferred refresh needs a UI and a response");
    }

    @Test
    void revealIsIdempotent() {
        Investigation investigation = new Investigation("lead");
        AtomicInteger refreshes = new AtomicInteger();
        investigation.onRefresh(refreshes::incrementAndGet);

        investigation.reveal();
        investigation.reveal();

        assertTrue(investigation.isVisible());
        assertEquals(2, refreshes.get(),
                "every reveal refreshes, since each marks a new interaction");
    }

    @Test
    void aRefreshRequestedWhileDetachedDoesNotWedgeTheScheduler() {
        // Review finding on #326: arming the coalescing flag before checking
        // for a UI left it set forever when there was none, and every later
        // refreshSoon() returned early.
        Investigation investigation = new Investigation("lead");
        AtomicInteger refreshes = new AtomicInteger();
        investigation.onRefresh(refreshes::incrementAndGet);

        investigation.refreshSoon(); // detached: nothing to schedule on
        investigation.refreshSoon();

        UI ui = new UI();
        ui.add(investigation);
        investigation.refreshSoon();
        investigation.refreshSoon(); // coalesced with the one above
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        assertEquals(1, refreshes.get(),
                "one deferred refresh must run once attached, not zero "
                        + "(wedged) and not two (uncoalesced)");

        investigation.refreshSoon();
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        assertEquals(2, refreshes.get(),
                "the scheduler re-arms after each response");
    }

    @Test
    void refreshNowRunsTheRefresherWithoutRevealing() {
        Investigation investigation = new Investigation("lead");
        AtomicInteger refreshes = new AtomicInteger();
        investigation.onRefresh(refreshes::incrementAndGet);

        investigation.refreshNow();

        assertEquals(1, refreshes.get());
        assertFalse(investigation.isVisible(),
                "populating the readout ahead of time must not show it");
    }
}
