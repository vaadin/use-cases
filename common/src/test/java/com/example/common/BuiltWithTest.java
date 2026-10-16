package com.example.common;

import com.example.common.layout.BuiltWithView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Pre;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ViewPackages(classes = BuiltWithView.class)
class BuiltWithTest extends BrowserlessTest {

    @Test
    void showsThePackagedFileAndLinksToItInTheApp() {
        navigate(BuiltWithView.class);

        assertEquals("com.example.common.OpenGraphTags",
                $(Pre.class).single().getText().strip());
        assertEquals(
                "https://github.com/vaadin/use-cases/blob/main/signals/"
                        + "src/main/resources/META-INF/services/"
                        + "com.vaadin.flow.server.VaadinServiceInitListener",
                $(Anchor.class).all().stream()
                        .filter(a -> "Open on GitHub".equals(a.getText()))
                        .findFirst().orElseThrow().getHref());
    }

    @ParameterizedTest
    @CsvSource(nullValues = "none", value = { "ShoppingCart.java, java",
            "uc6.css, css", "signature-pad.ts, typescript",
            "color-picker.tsx, typescript",
            "translations.properties, properties",
            "com.vaadin.flow.server.VaadinServiceInitListener, none" })
    void language_isTheHighlightJsLanguageOfTheFileExtension(String fileName,
            String language) {
        assertEquals(language, BuiltWith.language(fileName));
    }

    @Test
    void aFileWithoutALanguageIsShownAsPlainText() {
        navigate(BuiltWithView.class);

        assertFalse($(Pre.class).single().getClassNames().stream()
                .anyMatch(name -> name.startsWith("language-")));
    }

    @Test
    void excerpt_showsOnlyTheTaggedRegionsEachWithoutItsIndentation() {
        String source = """
                package com.example;

                // tag::scope[]
                @UIScope
                public class History {
                    // end::scope[]

                    private int size;

                    public History(Store store) {
                        // tag::scope[]
                        // tag::other[]
                        store.listen(this);
                        // end::other[]

                        store.load();
                        // end::scope[]
                    }
                }
                """;

        assertEquals("""
                @UIScope
                public class History {
                // ...
                store.listen(this);

                store.load();
                """, BuiltWith.excerpt(source, "scope"));
        assertThrows(IllegalArgumentException.class,
                () -> BuiltWith.excerpt(source, "missing"));
    }
}
