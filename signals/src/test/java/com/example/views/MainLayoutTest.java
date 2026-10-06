package com.example.views;

import com.example.security.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.Location;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = HomeView.class)
@WithMockUser
class MainLayoutTest extends SpringBrowserlessTest {

    @Test
    void loginParametersAreDroppedAfterReturningFromLogin() {
        navigate(
                "?" + SecurityConfiguration.LOGIN_PARAMETER + "&continue&tab=2",
                HomeView.class);

        Location location = UI.getCurrent().getActiveViewLocation();
        assertEquals("tab=2", location.getQueryParameters().getQueryString());
    }
}
