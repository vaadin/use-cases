package com.example.uc4;

import com.example.ClipboardTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = CopyImageView.class)
class CopyImageViewTest extends SpringBrowserlessTest {

    @Test
    void aSuccessfulImageCopyIsConfirmed() {
        navigate(CopyImageView.class);
        Button copy = findInView(Button.class).withText("Copy chart").single();

        // An image write resolves without a value.
        ClipboardTestSupport.reportCopied(copy, null);

        assertEquals("Chart copied",
                test(find(Notification.class).single()).getText());
    }
}
