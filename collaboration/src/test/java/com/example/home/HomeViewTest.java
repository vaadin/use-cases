package com.example.home;

import com.example.uc1.PresenceView;
import com.example.uc13.PendingStateView;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.router.RouterLink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The home page is the module's index, and it is generated from the
 * {@code @Menu} annotations rather than written out — so this test is really
 * about those: a use case that forgets its annotation disappears from the app
 * without any other test noticing.
 */
@SpringBootTest
@ViewPackages(packages = "com.example")
class HomeViewTest extends SpringBrowserlessTest {

    @Test
    void listsEveryUseCase() {
        navigate(HomeView.class);

        assertEquals(
                1, findInView(H1.class).withText("Collaboration — use cases")
                        .all().size(),
                "the home view should render its heading");

        long links = findInView(RouterLink.class).all().size()
                + findInView(Anchor.class).all().size();
        assertTrue(links >= 13,
                "the menu should list the thirteen use cases, found " + links);
    }

    @Test
    void reachesTheFirstAndLastUseCase() {
        // Both ends of the range, because the numbering is hand-written in the
        // @Menu order and a duplicate order silently reorders the index.
        navigate(PresenceView.class);
        assertEquals(1, findInView(H1.class).all().size());
        navigate(PendingStateView.class);
        assertEquals(1, findInView(H1.class).all().size());
    }
}
