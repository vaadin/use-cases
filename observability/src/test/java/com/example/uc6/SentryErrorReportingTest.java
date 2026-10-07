package com.example.uc6;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.VaadinSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A failed interaction reaches Sentry through the log line Vaadin's error
 * handler writes, not through Spring MVC. A browserless click throws straight
 * into the test instead of going through the session's error handler, so the
 * test hands the exception to that handler itself, which is what Flow does with
 * a real UIDL request. The DSN points nowhere and the before-send callback
 * drops every event after recording it, so nothing leaves the JVM.
 */
@SpringBootTest(properties = "sentry.dsn=https://key@sentry.invalid/1")
@ViewPackages(classes = FailureInsightsView.class)
class SentryErrorReportingTest extends SpringBrowserlessTest {

    static final List<SentryEvent> SENT = new CopyOnWriteArrayList<>();

    @TestConfiguration
    static class CaptureEvents {
        @Bean
        SentryOptions.BeforeSendCallback captureAndDrop() {
            return (event, hint) -> {
                SENT.add(event);
                return null;
            };
        }
    }

    @BeforeEach
    void clearSent() {
        SENT.clear();
    }

    @Test
    void aDefectiveReturnIsReportedToSentry() {
        navigate(FailureInsightsView.class);
        test(findInView(Select.class).id("return-reason"), String.class)
                .selectItem(FailureInsightsView.REASON_DEFECTIVE);
        IllegalStateException failure = assertThrows(
                IllegalStateException.class, () -> test(findInView(Button.class)
                        .withText("Process return").single()).click());

        VaadinSession.getCurrent().getErrorHandler()
                .error(new ErrorEvent(failure));

        assertEquals(1, SENT.size(),
                "the ERROR line Vaadin logs is the one event Sentry gets");
        SentryEvent event = SENT.get(0);
        assertEquals(IllegalStateException.class.getName(),
                event.getThrowable().getClass().getName());
    }
}
