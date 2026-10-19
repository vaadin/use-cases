package com.example.uc6;

import com.example.WebShareTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.webshare.WebShareSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = ShareInviteLinkView.class)
class ShareInviteLinkViewTest extends SpringBrowserlessTest {

    private static final String INVITE = "https://example\\.com/join/[A-HJ-NP-Z2-9]{8}";

    @Test
    void eachGenerateMakesANewInviteLink() {
        navigate(ShareInviteLinkView.class);
        assertEquals("", inviteLink().getValue());

        click("Generate invite");
        String first = inviteLink().getValue();
        assertTrue(first.matches(INVITE), first);

        click("Generate invite");
        String second = inviteLink().getValue();
        assertTrue(second.matches(INVITE), second);
        assertNotEquals(first, second);
    }

    @Test
    void shareNeedsBothSupportAndAGeneratedLink() {
        navigate(ShareInviteLinkView.class);
        runPendingSignalsTasks();
        assertFalse(button("Share invite").isEnabled(), "UNKNOWN, no link");

        WebShareTestSupport.setSupport(WebShareSupport.SUPPORTED);
        runPendingSignalsTasks();
        assertFalse(button("Share invite").isEnabled(), "SUPPORTED, no link");

        click("Generate invite");
        runPendingSignalsTasks();
        assertTrue(button("Share invite").isEnabled(), "SUPPORTED, link");

        WebShareTestSupport.setSupport(WebShareSupport.UNSUPPORTED);
        runPendingSignalsTasks();
        assertFalse(button("Share invite").isEnabled(), "UNSUPPORTED, link");
    }

    private TextField inviteLink() {
        return findInView(TextField.class).all().stream()
                .filter(f -> "Invite link".equals(f.getLabel())).findFirst()
                .orElseThrow();
    }

    private Button button(String text) {
        return findInView(Button.class).all().stream()
                .filter(b -> text.equals(b.getText())).findFirst()
                .orElseThrow();
    }

    private void click(String text) {
        test(button(text)).click();
    }
}
