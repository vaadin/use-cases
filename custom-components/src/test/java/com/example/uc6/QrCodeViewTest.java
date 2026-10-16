package com.example.uc6;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.select.Select;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = QrCodeView.class)
class QrCodeViewTest extends SpringBrowserlessTest {

    @Test
    void typedSettersBecomeTheElementsAttributes() {
        QrCodeView view = navigate(QrCodeView.class);
        var element = view.code().getElement();

        assertEquals("qr-code", element.getTag());
        assertEquals(QrCodeView.INITIAL, element.getAttribute("data"));
        assertEquals("svg", element.getAttribute("format"));

        test(view.data()).setValue("https://example.com/track/9999");
        @SuppressWarnings("unchecked")
        Select<Object> correction = findInView(Select.class).single();
        test(correction).selectItem("HIGH");

        assertEquals("https://example.com/track/9999", view.code().getData());
        assertEquals("QR code: https://example.com/track/9999",
                element.getAttribute("aria-label"));
        assertEquals("H", element.getAttribute("ecclevel"));
    }
}
