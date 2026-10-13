package com.example.home;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.H2;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(packages = "com.example")
class HomeViewTest extends SpringBrowserlessTest {

    @Test
    void cardsAreGroupedByKindOfProblem() {
        navigate(HomeView.class);

        assertEquals(
                List.of("The application's look",
                        "Light, dark and accessibility", "Styling components"),
                findInView(H2.class).all().stream().map(H2::getText).toList());
        assertEquals(9, findInView(Card.class).all().size());
    }
}
