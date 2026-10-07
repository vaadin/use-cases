package com.example.uc14;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = ResponsiveCardsView.class)
class ResponsiveCardsViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithGridAndSixCards() {
        navigate(ResponsiveCardsView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h -> "UC14 — Responsive card grid".equals(h.getText())));
        assertNotNull(findInView(Div.class).id("grid"));
        assertEquals(6, findInView(Card.class).all().size());
    }
}
