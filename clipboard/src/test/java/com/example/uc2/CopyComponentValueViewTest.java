package com.example.uc2;

import com.example.ClipboardTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = CopyComponentValueView.class)
class CopyComponentValueViewTest extends SpringBrowserlessTest {

    @Test
    void theNotificationShowsWhatTheBrowserCopied() {
        navigate(CopyComponentValueView.class);
        Button copy = findInView(Button.class).withText("Copy").single();

        // The browser reads the field at click time, so the copied text can
        // be an edit the server has not seen yet.
        ClipboardTestSupport.reportCopied(copy,
                "https://example.com/share/typed-but-not-synced");

        assertEquals("Copied https://example.com/share/typed-but-not-synced",
                test(find(Notification.class).single()).getText());
    }
}
