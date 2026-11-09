package com.example.uc1;

import com.example.ClipboardTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = CopyStaticTextView.class)
class CopyStaticTextViewTest extends SpringBrowserlessTest {

    @Test
    void aSuccessfulCopyFlashesCopiedOnTheButtonForASecond() {
        navigate(CopyStaticTextView.class);
        Button button = copyButton();

        ClipboardTestSupport.reportCopied(button, CopyStaticTextView.LINK);
        assertEquals(CopyStaticTextView.FLASH_LABEL, button.getText());
        assertNotNull(button.getIcon());

        ClipboardTestSupport.elapseLatestTimer(UI.getCurrent());
        assertEquals(CopyStaticTextView.DEFAULT_LABEL, button.getText());
        assertNull(button.getIcon());
    }

    @Test
    void aFailedCopyLeavesTheButtonAndShowsTheError() {
        navigate(CopyStaticTextView.class);
        Button button = copyButton();

        ClipboardTestSupport.reportCopyFailed(button, "NotAllowedError",
                "Write permission denied.");

        assertEquals(CopyStaticTextView.DEFAULT_LABEL, button.getText());
        assertTrue(test(find(Notification.class).single()).getText()
                .contains("Write permission denied."));
    }

    private Button copyButton() {
        return findInView(Button.class)
                .withText(CopyStaticTextView.DEFAULT_LABEL).single();
    }
}
