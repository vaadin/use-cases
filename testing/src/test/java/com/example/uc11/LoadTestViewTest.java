package com.example.uc11;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@WithAnonymousUser
@ViewPackages(classes = LoadTestView.class)
class LoadTestViewTest extends SpringBrowserlessTest {

    @Test
    void showsServerStatsAndPollsWhileOpen() {
        LoadTestView view = navigate(LoadTestView.class);

        assertTrue(view.stats().matches(
                "\\d+ open sessions · \\d+ open UIs · \\d+ sessions since "
                        + "start · \\d+ orders placed"),
                view.stats());
        assertEquals(1000, UI.getCurrent().getPollInterval());
    }
}
