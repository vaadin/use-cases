package com.example.home;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.H2;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@WithAnonymousUser
@ViewPackages(packages = "com.example")
class HomeViewTest extends SpringBrowserlessTest {

    @Test
    void cardsAreGroupedByWhereTheTestsRun() {
        navigate(HomeView.class);

        assertEquals(
                List.of("Without a browser", "In a real browser", "Under load"),
                findInView(H2.class).all().stream().map(H2::getText).toList());
        // Every use case is listed for a visitor who is not signed in.
        assertEquals(11, findInView(Card.class).all().size());
    }
}
