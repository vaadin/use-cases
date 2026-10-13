package com.example.uc10;

import com.example.components.Money;
import com.example.components.RatingStars;
import com.example.components.RatingStarsTester;
import com.example.desk.Ticket;
import com.example.desk.TicketDetail;
import com.example.desk.TicketStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.ComponentTesterPackages;
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

    @AfterEach
    void resetStore() {
        store.reset();
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
    void aSaveInOneSessionShowsInTheNext() {
        ServiceDeskView first = navigate(ServiceDeskView.class);
        test(first.grid()).select(0);
        test(RatingStarsTester.class, first.detail().rating()).rate(3);
        test(first.detail().saveButton()).click();

        // Another user, in another session, opens the desk.
        cleanVaadinEnvironment();
        initVaadinEnvironment();
        ServiceDeskView second = navigate(ServiceDeskView.class);

        RatingStars rating = (RatingStars) test(second.grid())
                .getCellComponent(0, "rating");
        assertEquals(3, rating.getValue());
    }
}
