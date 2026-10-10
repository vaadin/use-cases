package com.example.uc2;

import java.util.List;

import com.example.orders.Order;
import com.example.orders.OrderStore;
import com.example.orders.Product;
import com.example.uc4.OrdersView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@WithAnonymousUser
@ViewPackages(classes = { NewOrderView.class, OrdersView.class })
class NewOrderViewTest extends SpringBrowserlessTest {

    @Autowired
    private OrderStore store;

    @BeforeEach
    void emptyStore() {
        store.clear();
    }

    @Test
    void placingAnOrderConfirmsStoresAndOpensTheList() {
        navigate(NewOrderView.class);

        test(field("Customer")).setValue("Kestrel Air");
        test(findInView(Select.class).single()).selectItem("Burr grinder");
        test(findInView(IntegerField.class).single()).setValue(2);
        test(button("Place order")).click();

        List<Order> orders = store.all();
        assertEquals(1, orders.size());
        Order order = orders.getFirst();
        assertEquals("Kestrel Air", order.customer());
        assertEquals(Product.GRINDER, order.product());
        assertEquals(2, order.quantity());
        assertEquals("Order #" + order.number() + " placed",
                test($(Notification.class).single()).getText());
        assertInstanceOf(OrdersView.class, getCurrentView());
    }

    @Test
    void missingCustomerKeepsTheUserOnTheForm() {
        navigate(NewOrderView.class);

        test(button("Place order")).click();

        assertTrue(store.all().isEmpty());
        assertEquals("Fill in customer, quantity and delivery",
                test($(Notification.class).single()).getText());
        assertInstanceOf(NewOrderView.class, getCurrentView());
    }

    private TextField field(String label) {
        return findInView(TextField.class).all().stream()
                .filter(f -> label.equals(f.getLabel())).findFirst()
                .orElseThrow();
    }

    private Button button(String text) {
        return findInView(Button.class).all().stream()
                .filter(b -> text.equals(b.getText())).findFirst()
                .orElseThrow();
    }
}
