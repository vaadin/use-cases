package com.example.uc20;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = DoubleClickOpenView.class)
class DoubleClickOpenViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersListOfRows() {
        navigate(DoubleClickOpenView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h -> "UC20 — Double-click → new tab".equals(h.getText())));
        Div list = findInView(Div.class).id("list");
        assertNotNull(list);
        assertTrue(list.getChildren().count() >= 3,
                "list should hold several link rows");
    }
}
