package com.example.desk;

import java.util.stream.Stream;

import com.example.components.Money;
import com.example.components.RatingStars;
import com.example.components.RatingStarsTester;
import com.example.uc10.ServiceDeskView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.ComponentTesterPackages;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC10, from the outside: the view and the TicketDetail it is made of. */
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

    /**
     * The detail starts hidden, and component queries only find visible
     * components, so it is looked up in the component tree.
     */
    private TicketDetail detail() {
        return descendants(
                getCurrentView().getElement().getComponent().orElseThrow())
                .filter(TicketDetail.class::isInstance)
                .map(TicketDetail.class::cast).findFirst().orElseThrow();
    }

    private static Stream<Component> descendants(Component root) {
        return Stream.concat(Stream.of(root),
                root.getChildren().flatMap(ServiceDeskViewTest::descendants));
    }

    @Test
    void rowsShowTheComponentsReadOnly() {
        navigate(ServiceDeskView.class);
        @SuppressWarnings("unchecked")
        Grid<Ticket> grid = findInView(Grid.class).single();

        RatingStars rating = (RatingStars) test(grid).getCellComponent(1,
                "rating");

        assertEquals(4, rating.getValue());
        assertTrue(rating.isReadOnly());
    }

    @Test
    void selectingARowFillsTheDetailAndSavingStoresIt() {
        navigate(ServiceDeskView.class);
        @SuppressWarnings("unchecked")
        Grid<Ticket> grid = findInView(Grid.class).single();
        TicketDetail detail = detail();
        assertFalse(detail.getContent().isVisible());

        test(grid).select(0);
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
        navigate(ServiceDeskView.class);
        @SuppressWarnings("unchecked")
        Grid<Ticket> grid = findInView(Grid.class).single();
        TicketDetail detail = detail();
        test(grid).select(2);
        assertFalse(detail.closeButton().isEnabled());

        detail.signature().setValue("data:image/png;base64,iVBORw0KGgo=");
        assertTrue(detail.closeButton().isEnabled());
        test(detail.closeButton()).click();

        assertTrue(store.all().get(2).closed());
        assertEquals("Closed", test(grid).getCellText(2, 5));
        assertFalse(detail.saveButton().isEnabled());
    }
}
