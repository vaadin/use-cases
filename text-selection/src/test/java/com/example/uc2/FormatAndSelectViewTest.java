package com.example.uc2;

import java.util.Optional;

import com.example.TextSelectionTestSupport.Requested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextField;

import static com.example.TextSelectionTestSupport.clickAndReadSelection;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@ViewPackages(classes = FormatAndSelectView.class)
class FormatAndSelectViewTest extends SpringBrowserlessTest {

    @Test
    void formatSlugifiesTheValueAndSelectsAllOfIt() {
        navigate(FormatAndSelectView.class);
        TextField title = find(TextField.class).single();
        test(title).setValue("Hello, Awesome World!!!");

        Optional<Requested> selection = clickAndReadSelection(
                find(Button.class).withText("Format").single(), title);

        assertEquals("hello-awesome-world", title.getValue());
        assertEquals(Optional.of(new Requested(0, Integer.MAX_VALUE)),
                selection);
        // Without autoselect, a later click into the field places the cursor
        // instead of selecting everything again.
        assertFalse(title.isAutoselect());
    }
}
