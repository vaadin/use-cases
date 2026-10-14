package com.example.views;

import java.io.IOException;

import com.example.common.SlackFeedback;
import com.example.common.SlackFeedback.Feedback;
import com.example.security.SecurityConfiguration;
import com.example.usecase01.UseCase01View;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Location;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ViewPackages(classes = { HomeView.class, UseCase01View.class })
@WithMockUser
class MainLayoutTest extends SpringBrowserlessTest {

    @MockitoBean
    private SlackFeedback slackFeedback;

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

    @Test
    void feedbackIsSentForTheOpenUseCase()
            throws IOException, InterruptedException {
        when(slackFeedback.isEnabled()).thenReturn(true);
        navigate(UseCase01View.class);

        test($(Button.class).withText("Feedback").single()).click();
        test($(TextArea.class).single()).setValue("Clear example, thanks!");
        test($(Button.class).withText("Send").single()).click();

        ArgumentCaptor<Feedback> feedback = ArgumentCaptor
                .forClass(Feedback.class);
        verify(slackFeedback).send(feedback.capture());
        assertEquals(UseCase01View.class.getName(),
                feedback.getValue().useCase().viewClass());
        assertEquals("Clear example, thanks!", feedback.getValue().message());
    }
}
