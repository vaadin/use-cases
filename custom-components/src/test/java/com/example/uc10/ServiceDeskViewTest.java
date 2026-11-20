package com.example.uc10;

import java.util.List;

import com.example.components.Money;
import com.example.components.RatingStars;
import com.example.components.RatingStarsTester;
import com.example.desk.Ticket;
import com.example.desk.TicketDetail;
import com.example.desk.TicketStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.browserless.BrowserlessApplicationContext;
import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.ComponentTesterPackages;
import com.vaadin.browserless.SpringBrowserlessApplicationContext;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.dom.DebouncePhase;
import com.vaadin.flow.shared.JsonConstants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = ServiceDeskView.class)
@ComponentTesterPackages("com.example.components")
class ServiceDeskViewTest extends SpringBrowserlessTest {

    @Autowired
    private TicketStore store;

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * The store is shared by every test. It is reset inside the test's session,
     * where a change to a shared signal applies with the pending signal tasks.
     */
    @BeforeEach
    void resetStore() {
        store.reset();
        runAllPendingSignalsTasks();
    }

    private void runAllPendingSignalsTasks() {
        while (runPendingSignalsTasks()) {
            // until nothing is pending
        }
    }

    @Test
    void rowsShowTheComponentsReadOnly() {
        ServiceDeskView view = navigate(ServiceDeskView.class);

        RatingStars rating = (RatingStars) test(view.grid()).getCellComponent(1,
                "rating");

        assertEquals(4, rating.getValue());
        assertTrue(rating.isReadOnly());
    }

    @Test
    void selectingARowFillsTheDetailAndSavingStoresIt() {
        ServiceDeskView view = navigate(ServiceDeskView.class);
        TicketDetail detail = view.detail();
        assertFalse(detail.getContent().isVisible());

        test(view.grid()).select(0);
        assertTrue(detail.getContent().isVisible());
        assertEquals("https://example.com/track/4521",
                detail.tracking().getData());

        test(RatingStarsTester.class, detail.rating()).rate(5);
        detail.refund().setValue(Money.of("15.00", "EUR"));
        test(detail.saveButton()).click();

        Ticket saved = store.all().getFirst();
        assertEquals(5, saved.rating());
        assertEquals(Money.of("15.00", "EUR"), saved.refund());
        assertEquals("Ticket #4521 saved",
                test($(Notification.class).single()).getText());
    }

    @Test
    void closingNeedsTheCustomersSignature() {
        ServiceDeskView view = navigate(ServiceDeskView.class);
        TicketDetail detail = view.detail();
        test(view.grid()).select(2);
        assertFalse(detail.closeButton().isEnabled());

        detail.signature().setValue("data:image/png;base64,iVBORw0KGgo=");
        assertTrue(detail.closeButton().isEnabled());
        test(detail.closeButton()).click();

        assertTrue(store.all().get(2).closed());
        assertEquals("Closed", test(view.grid()).getCellText(2, 5));
        assertFalse(detail.saveButton().isEnabled());
        // A closed ticket's pad ignores the browser, not just looks grey.
        assertFalse(detail.signature().isEnabled());
    }

    @Test
    void listAndDetailStackOnPhones() {
        ServiceDeskView view = navigate(ServiceDeskView.class);
        resizeWindow(1400, 900);
        runPendingSignalsTasks();
        assertEquals(SplitLayout.Orientation.HORIZONTAL,
                view.split().getOrientation());

        resizeWindow(390, 844);
        runPendingSignalsTasks();
        assertEquals(SplitLayout.Orientation.VERTICAL,
                view.split().getOrientation());

        resizeWindow(1024, 768);
        runPendingSignalsTasks();
        assertEquals(SplitLayout.Orientation.HORIZONTAL,
                view.split().getOrientation());
    }

    /** Plays the browser's debounced resize event on the UI. */
    private static void resizeWindow(int width, int height) {
        ObjectNode data = JsonNodeFactory.instance.objectNode();
        data.put("event.w", width);
        data.put("event.h", height);
        data.put(JsonConstants.EVENT_DATA_PHASE,
                DebouncePhase.TRAILING.getIdentifier());
        new ComponentTester<UI>(UI.getCurrent()) {
            void fire() {
                fireDomEvent("window-resize", data);
            }
        }.fire();
    }

    @Test
    void aSaveInAnotherSessionShowsInAnOpenDesk() {
        try (var app = multiUser()) {
            var alice = app.newUser().newWindow();
            var bob = app.newUser().newWindow();
            ServiceDeskView aliceDesk = alice.navigate(ServiceDeskView.class);
            ServiceDeskView bobDesk = bob.navigate(ServiceDeskView.class);
            alice.activate();
            alice.test(aliceDesk.grid()).select(1);

            // Another user saves a ticket in their own desk.
            save(bob, bobDesk, 0, 5);
            // The change applies in one round of pending signal tasks, and
            // the effect that redraws the list runs in the next.
            runAllPendingSignalsTasks(alice);

            RatingStars rating = (RatingStars) alice.test(aliceDesk.grid())
                    .getCellComponent(0, "rating");
            assertEquals(5, rating.getValue());
            // The ticket being worked on here stays selected.
            assertEquals(4522,
                    aliceDesk.grid().asSingleSelect().getValue().id());
            assertTrue(aliceDesk.detail().getContent().isVisible());
        }
    }

    @Test
    void anotherSessionsSaveOfTheOpenTicketRefreshesTheDetail() {
        try (var app = multiUser()) {
            var alice = app.newUser().newWindow();
            var bob = app.newUser().newWindow();
            ServiceDeskView aliceDesk = alice.navigate(ServiceDeskView.class);
            ServiceDeskView bobDesk = bob.navigate(ServiceDeskView.class);
            alice.activate();
            alice.test(aliceDesk.grid()).select(0);
            assertEquals(0, aliceDesk.detail().rating().getValue());

            save(bob, bobDesk, 0, 4);
            runAllPendingSignalsTasks(alice);

            // The form shows the stored ticket, so saving here can not undo
            // the other session's change without the user seeing it.
            assertEquals(4, aliceDesk.detail().rating().getValue());
            assertEquals("Ticket #4521 was changed in another session", alice
                    .test(alice.find(Notification.class).last()).getText());
        }
    }

    @Test
    void ownSaveIsNotReportedAsAnotherSessions() {
        ServiceDeskView view = navigate(ServiceDeskView.class);
        test(view.grid()).select(0);

        test(RatingStarsTester.class, view.detail().rating()).rate(2);
        test(view.detail().saveButton()).click();
        runAllPendingSignalsTasks();

        assertEquals(List.of("Ticket #4521 saved"), $(Notification.class).all()
                .stream().map(n -> test(n).getText()).toList());
    }

    @Test
    void aSaveInOneSessionShowsInTheNext() {
        try (var app = multiUser()) {
            var alice = app.newUser().newWindow();
            save(alice, alice.navigate(ServiceDeskView.class), 0, 3);

            // Another user opens the desk.
            var bob = app.newUser().newWindow();
            ServiceDeskView bobDesk = bob.navigate(ServiceDeskView.class);

            RatingStars rating = (RatingStars) bob.test(bobDesk.grid())
                    .getCellComponent(0, "rating");
            assertEquals(3, rating.getValue());
        }
    }

    /**
     * A multi-user context. The typed {@code test(Grid)}, {@code test(Button)}
     * overloads of a window do not activate it, so a window is activated before
     * its components are driven.
     */
    private BrowserlessApplicationContext multiUser() {
        return SpringBrowserlessApplicationContext.create(applicationContext,
                b -> b.withViewPackages(ServiceDeskView.class)
                        .withComponentTesterPackages("com.example.components")
                        .withSessionLockIsolation(true));
    }

    /** Rates a ticket in the given window's desk and saves it. */
    private static void save(BrowserlessUIContext window, ServiceDeskView desk,
            int row, int rating) {
        window.activate();
        window.test(desk.grid()).select(row);
        window.test(RatingStarsTester.class, desk.detail().rating())
                .rate(rating);
        window.test(desk.detail().saveButton()).click();
    }

    private static void runAllPendingSignalsTasks(BrowserlessUIContext window) {
        while (window.runPendingSignalsTasks()) {
            // until nothing is pending
        }
    }
}
