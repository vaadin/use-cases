package com.example.uc3;

import com.example.WebShareTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.webshare.WebShareSupport;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = CustomMessageView.class)
class CustomMessageViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithFormAndButton() {
        navigate(CustomMessageView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h -> "UC3 — Share a custom message".equals(h.getText())));
        assertTrue(findInView(TextField.class).all().stream()
                .anyMatch(f -> "Title".equals(f.getLabel())));
        assertTrue(findInView(TextArea.class).all().stream()
                .anyMatch(f -> "Text".equals(f.getLabel())));
        assertTrue(findInView(TextField.class).all().stream()
                .anyMatch(f -> "URL".equals(f.getLabel())));
        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> "Share".equals(b.getText())));
    }

    @Test
    void previewReflectsFieldChanges() {
        navigate(CustomMessageView.class);
        runPendingSignalsTasks();

        TextField title = findTextField("Title");
        TextField url = findTextField("URL");
        TextArea text = findTextArea("Text");

        title.setValue("Hello");
        text.setValue("");
        url.setValue("https://example.com/x");
        runPendingSignalsTasks();

        // Preview is JSON, so look for the field values appearing in it.
        String previewText = findPreview().getText();
        assertTrue(previewText.contains("\"title\" : \"Hello\""),
                "preview should contain title; was: " + previewText);
        assertTrue(previewText.contains("\"text\" : null"),
                "blank text should be serialised as null; was: " + previewText);
        assertTrue(previewText.contains("\"url\" : \"https://example.com/x\""),
                "preview should contain url; was: " + previewText);
    }

    @Test
    void shareButtonReflectsSupportSignal() {
        navigate(CustomMessageView.class);
        runPendingSignalsTasks();

        Button share = findButton();
        assertFalse(share.isEnabled(), "should start disabled while UNKNOWN");

        WebShareTestSupport.setSupport(WebShareSupport.SUPPORTED);
        runPendingSignalsTasks();
        assertTrue(share.isEnabled(), "should enable once SUPPORTED arrives");

        WebShareTestSupport.setSupport(WebShareSupport.UNSUPPORTED);
        runPendingSignalsTasks();
        assertFalse(share.isEnabled(),
                "should disable when UNSUPPORTED arrives");
    }

    private TextField findTextField(String label) {
        return findInView(TextField.class).all().stream()
                .filter(f -> label.equals(f.getLabel())).findFirst()
                .orElseThrow();
    }

    private TextArea findTextArea(String label) {
        return findInView(TextArea.class).all().stream()
                .filter(f -> label.equals(f.getLabel())).findFirst()
                .orElseThrow();
    }

    private Div findPreview() {
        return findInView(Div.class).all().stream()
                .filter(d -> d.getClassNames().contains("share-preview"))
                .findFirst().orElseThrow();
    }

    private Button findButton() {
        return findInView(Button.class).all().stream()
                .filter(b -> "Share".equals(b.getText())).findFirst()
                .orElseThrow();
    }
}
