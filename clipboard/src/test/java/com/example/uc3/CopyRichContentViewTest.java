package com.example.uc3;

import com.example.ClipboardTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = CopyRichContentView.class)
class CopyRichContentViewTest extends SpringBrowserlessTest {

    @Test
    void aFailedCopyShowsTheBrowsersError() {
        navigate(CopyRichContentView.class);
        Button copy = findInView(Button.class).withText("Copy rich content")
                .single();

        ClipboardTestSupport.reportCopyFailed(copy, "NotAllowedError",
                "Document is not focused.");

        assertEquals("Copy failed: Document is not focused.",
                test(find(Notification.class).single()).getText());
    }
}
