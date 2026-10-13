package com.example.uc2;

import com.example.pdfviewer.PdfViewer;
import com.example.pdfviewer.PdfViewerTester;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = ReadBeforeAcceptingView.class)
class ReadBeforeAcceptingViewTest extends SpringBrowserlessTest {

    @Test
    void acceptingTurnsOnOnlyAtTheLastPage() {
        navigate(ReadBeforeAcceptingView.class);
        PdfViewerTester viewer = new PdfViewerTester(
                findInView(PdfViewer.class).single());
        Checkbox accept = findInView(Checkbox.class).single();
        assertFalse(accept.isEnabled(), "Nothing has loaded yet");

        viewer.load(3, "Acme Cloud — Terms of Service");
        assertEquals("You have reached page 1 of 3.", progress());
        assertFalse(accept.isEnabled());

        viewer.scrollTo(2);
        assertFalse(accept.isEnabled());

        viewer.scrollTo(3);
        assertEquals("You have reached the last page.", progress());
        assertTrue(accept.isEnabled());
    }

    @Test
    void scrollingBackKeepsTheFurthestPage() {
        navigate(ReadBeforeAcceptingView.class);
        PdfViewerTester viewer = new PdfViewerTester(
                findInView(PdfViewer.class).single());
        viewer.load(3, "");
        viewer.scrollTo(3);
        viewer.scrollTo(1);

        Checkbox accept = findInView(Checkbox.class).single();
        assertTrue(accept.isEnabled());
        Button proceed = findInView(Button.class).withText("Create account")
                .single();
        assertFalse(proceed.isEnabled(), "Not accepted yet");
        test(accept).click();
        assertTrue(proceed.isEnabled());
    }

    private String progress() {
        return findInView(Span.class).withClassName("uc2-progress").single()
                .getText();
    }
}
