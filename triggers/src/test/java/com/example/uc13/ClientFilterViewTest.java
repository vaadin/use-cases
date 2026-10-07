package com.example.uc13;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = ClientFilterView.class)
class ClientFilterViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithSearchAndFullList() {
        navigate(ClientFilterView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h -> "UC13 — Client-side filter".equals(h.getText())));
        assertNotNull(findInView(TextField.class).id("search"));
        Div list = findInView(Div.class).id("list");
        assertNotNull(list);
        assertTrue(list.getChildren().count() > 20,
                "server-rendered list should hold all items");
    }
}
