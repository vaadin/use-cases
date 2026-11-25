package com.example.uc8;

import java.util.Locale;

import com.vaadin.flow.component.pdfviewer.PdfViewerI18n;

/**
 * The viewer's texts in the languages of UC8.
 * <p>
 * Every text of the component has to be supplied by the application, for every
 * language it supports: there are no built-in translations, and the component
 * does not follow the UI's locale (API-GAPS.md #4).
 */
final class ViewerTexts {

    private ViewerTexts() {
    }

    /**
     * The texts for a language.
     *
     * @param locale
     *            the language
     * @return the texts; empty for English, which keeps the defaults
     */
    static PdfViewerI18n of(Locale locale) {
        return switch (locale.getLanguage()) {
        case "fi" -> finnish();
        case "de" -> german();
        default -> new PdfViewerI18n();
        };
    }

    private static PdfViewerI18n finnish() {
        return new PdfViewerI18n().setPreviousPage("Edellinen sivu")
                .setNextPage("Seuraava sivu").setPage("Sivu")
                .setPageError("Anna 1–{pageCount}").setZoomIn("Lähennä")
                .setZoomOut("Loitonna").setZoom("Zoomaus")
                .setPageWidth("Sivun leveys").setPageFit("Koko sivu")
                .setDocument("PDF-asiakirja").setPageLabel("Sivu {page}")
                .setLoading("Ladataan").setLink("Linkki")
                .setGoToPage("Siirry sivulle {page}")
                .setExternalLink("{text} (avautuu uuteen välilehteen)")
                .setDownload("Lataa").setSidebar("Sivupalkki")
                .setThumbnails("Sivujen pikkukuvat")
                .setThumbnailLabel("Sivu {page}");
    }

    private static PdfViewerI18n german() {
        return new PdfViewerI18n().setPreviousPage("Vorherige Seite")
                .setNextPage("Nächste Seite").setPage("Seite")
                .setPageError("1–{pageCount} eingeben").setZoomIn("Vergrößern")
                .setZoomOut("Verkleinern").setZoom("Zoom")
                .setPageWidth("Seitenbreite").setPageFit("Ganze Seite")
                .setDocument("PDF-Dokument").setPageLabel("Seite {page}")
                .setLoading("Wird geladen").setLink("Link")
                .setGoToPage("Gehe zu Seite {page}")
                .setExternalLink("{text} (öffnet in einem neuen Tab)")
                .setDownload("Herunterladen").setSidebar("Seitenleiste")
                .setThumbnails("Seitenminiaturen")
                .setThumbnailLabel("Seite {page}");
    }
}
