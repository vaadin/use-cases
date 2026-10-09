package com.example.uc10;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.data.provider.Query;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = VirtualScrollingView.class)
class VirtualScrollingViewTest extends SpringBrowserlessTest {

    @Test
    void virtualListServesAllContactsWithoutComponents() {
        VirtualScrollingView view = navigate(VirtualScrollingView.class);

        assertEquals("UC10 — 100,000 cards",
                findInView(H1.class).single().getText());
        assertEquals(VirtualScrollingView.CONTACT_COUNT,
                view.list().getDataProvider().size(new Query<>()));
        assertEquals(0, view.list().getChildren().count(),
                "cards are rendered in the browser, not as components");

        // A page from the far end of the list is produced on demand.
        var last = view.list().getDataProvider()
                .fetch(new Query<>(VirtualScrollingView.CONTACT_COUNT - 1, 1,
                        null, null, null))
                .findFirst().orElseThrow();
        assertEquals(VirtualScrollingView.CONTACT_COUNT, last.number());
        assertEquals(2, last.initials().length());
    }

    @Test
    void naiveBuildCreatesOneComponentTreePerContact() {
        VirtualScrollingView view = navigate(VirtualScrollingView.class);

        test(findInView(Button.class).single()).click();

        assertEquals(VirtualScrollingView.NAIVE_COUNT,
                view.naiveCards().getComponentCount());
    }
}
