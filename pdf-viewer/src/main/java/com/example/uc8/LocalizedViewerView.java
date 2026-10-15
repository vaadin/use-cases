package com.example.uc8;

import java.util.List;
import java.util.Locale;

import com.example.common.UseCaseDescription;
import com.example.documents.SampleDocuments;
import com.example.pdfviewer.PdfViewer;
import com.example.views.ApiGap;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC8 — Viewer in your language.
 * <p>
 * The viewer's buttons, tooltips, messages and screen reader texts follow the
 * user's language. The language starts as the browser's
 * ({@code WebBrowser#getLocale}), and can be switched. The UI's locale would
 * not do: without an {@code I18NProvider}, Vaadin leaves it at the server's
 * default. Each switch sets the texts with
 * {@code PdfViewer#setI18n(PdfViewerI18n)}; English sets none and keeps the
 * component's defaults.
 * <p>
 * The document itself stays in English: translating content is the document's
 * business, not the viewer's.
 */
@Route(value = "uc8", layout = MainLayout.class)
@PageTitle("UC8 — Viewer in your language")
@UseCaseDescription("Showing the viewer's controls and messages in the user's language")
@Menu(order = 8, title = "UC8 — Viewer in your language")
@StyleSheet("uc8.css")
public class LocalizedViewerView extends VerticalLayout {

    static final List<Locale> LANGUAGES = List.of(Locale.ENGLISH,
            Locale.of("fi"), Locale.GERMAN);

    private final Select<Locale> language = new Select<>();
    private final PdfViewer viewer = new PdfViewer();

    public LocalizedViewerView() {
        addClassName("uc8-view");
        add(new H1("UC8 — Viewer in your language"));
        add(new Paragraph("Pick a language. Hover the toolbar buttons to see "
                + "their tooltips, open the zoom menu, or press Ctrl+F (Cmd+F "
                + "on a Mac) in the viewer to see the find bar. The guide "
                + "itself stays in English."));

        language.setLabel("Language");
        language.setItems(LANGUAGES);
        language.setItemLabelGenerator(
                locale -> locale.getDisplayLanguage(locale));
        language.addValueChangeListener(
                event -> viewer.setI18n(ViewerTexts.of(event.getValue())));

        viewer.addClassName("uc8-viewer");
        viewer.setSidebarOpened(true);
        viewer.setFileNameVisible(true);
        viewer.setSrc(SampleDocuments.userGuide().pdf().handler());

        add(language, viewer);
        add(ApiGap.note(8, "the viewer has no built-in translations and does "
                + "not follow the UI's locale, so the app supplies all 38 "
                + "texts for every language."));

        // Start from the browser's language, unless the reader has picked one
        // already.
        whenAttached(ui -> {
            if (language.isEmpty()) {
                language.setValue(
                        languageOf(ui.getSession().getBrowser().getLocale()));
            }
            return null;
        });
    }

    /**
     * The language of the select that matches a locale, English when none does
     * or the locale is unknown.
     */
    static Locale languageOf(@Nullable Locale locale) {
        if (locale == null) {
            return Locale.ENGLISH;
        }
        return LANGUAGES.stream().filter(
                language -> language.getLanguage().equals(locale.getLanguage()))
                .findFirst().orElse(Locale.ENGLISH);
    }
}
