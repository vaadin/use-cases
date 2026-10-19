package com.example.uc3;

import com.example.WebShareTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.webshare.WebShareSupport;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = CustomMessageView.class)
class CustomMessageViewTest extends SpringBrowserlessTest {

    @Test
    void previewShowsTheFieldValuesThatWillBeShared() {
        navigate(CustomMessageView.class);

        test(findTextField("Title")).setValue("Hello");
        test(findInView(TextArea.class).single()).setValue("");
        test(findTextField("URL")).setValue("https://example.com/x");

        String preview = findInView(Div.class).all().stream()
                .filter(d -> d.hasClassName("uc3-preview")).findFirst()
                .orElseThrow().getText();
        assertTrue(preview.contains("\"title\" : \"Hello\""), preview);
        // An empty field is still sent, as an empty string.
        assertTrue(preview.contains("\"text\" : \"\""), preview);
        assertTrue(preview.contains("\"url\" : \"https://example.com/x\""),
                preview);
    }

    @Test
    void shareButtonFollowsTheSupportSignal() {
        navigate(CustomMessageView.class);
        runPendingSignalsTasks();

        Button share = findInView(Button.class).all().stream()
                .filter(b -> "Share".equals(b.getText())).findFirst()
                .orElseThrow();
        assertFalse(share.isEnabled(), "disabled while UNKNOWN");

        WebShareTestSupport.setSupport(WebShareSupport.SUPPORTED);
        runPendingSignalsTasks();
        assertTrue(share.isEnabled(), "enabled once SUPPORTED");

        WebShareTestSupport.setSupport(WebShareSupport.UNSUPPORTED);
        runPendingSignalsTasks();
        assertFalse(share.isEnabled(), "disabled when UNSUPPORTED");
    }

    private TextField findTextField(String label) {
        return findInView(TextField.class).all().stream()
                .filter(f -> label.equals(f.getLabel())).findFirst()
                .orElseThrow();
    }
}
