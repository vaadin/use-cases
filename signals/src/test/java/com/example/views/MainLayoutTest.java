package com.example.views;

import com.example.security.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.router.Location;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    @WithAnonymousUser
    void loginButtonReloadsCurrentPageWithLoginParameter() {
        navigate("?tab=2", HomeView.class);
        UI.getCurrent().getInternals().dumpPendingJavaScriptInvocations();

        test($(Button.class).withText("Log in").single()).click();

        String reload = UI.getCurrent().getInternals()
                .dumpPendingJavaScriptInvocations().stream()
                .flatMap(invocation -> invocation.getInvocation()
                        .getParameters().stream())
                .map(String::valueOf).filter(p -> p.contains("tab=2"))
                .findFirst().orElse("");
        assertTrue(
                reload.contains("tab=2") && reload
                        .contains(SecurityConfiguration.LOGIN_PARAMETER),
                () -> "Expected a reload with the login parameter, got '"
                        + reload + "'");
    }
}
