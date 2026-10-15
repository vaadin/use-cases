package com.example.uc8;

import java.util.Locale;

import com.example.pdfviewer.PdfViewerI18n;

/**
 * The viewer's texts in the languages of UC8.
 * <p>
 * Every text of the web component has to be supplied by the application, for
 * every language it supports: there are no built-in translations, and the
 * component does not follow the UI's locale (API-GAPS.md #8).
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
        return new PdfViewerI18n().setLoadError("Asiakirjaa ei voitu ladata.")
                .setPasswordError(
                        "Salasanalla suojattuja asiakirjoja ei tueta.")
                .setToolbar("PDF-työkalurivi").setToolbarToggle("Työkalurivi")
                .setSidebar("Sivupalkki").setSidebarView("Sivupalkin näkymä")
                .setThumbnails("Sivujen pienoiskuvat")
                .setThumbnailsView("Pienoiskuvat").setOutline("Sisällys")
                .setPreviousPage("Edellinen sivu").setNextPage("Seuraava sivu")
                .setPage("Sivu").setPageOf("Sivu / {pageCount}")
                .setPageError("Anna 1–{pageCount}")
                .setPageAnnouncement("Sivu {page} / {pageCount}")
                .setZoom("Zoomaus").setZoomIn("Lähennä").setZoomOut("Loitonna")
                .setPageWidth("Sivun leveys").setPageFit("Koko sivu")
                .setFind("Etsi asiakirjasta")
                .setPreviousMatch("Edellinen osuma")
                .setNextMatch("Seuraava osuma").setCloseFind("Sulje haku")
                .setFindResult("{current} / {total}")
                .setFindResultAnnouncement("{current} / {total}, sivu {page}")
                .setFindNoMatches("Ei osumia").setDownload("Lataa")
                .setPrint("Tulosta").setPrinting("Valmistellaan tulostusta…")
                .setCancelPrint("Peruuta")
                .setPrintError("Asiakirjaa ei voitu tulostaa.")
                .setDocument("PDF-asiakirja").setPages("Sivut")
                .setPageLabel("Sivu {page}").setLink("Linkki")
                .setGoToPage("Siirry sivulle {page}")
                .setExternalLink("{text} (avautuu uuteen välilehteen)");
    }

    private static PdfViewerI18n german() {
        return new PdfViewerI18n()
                .setLoadError("Das Dokument konnte nicht geladen werden.")
                .setPasswordError(
                        "Passwortgeschützte Dokumente werden nicht unterstützt.")
                .setToolbar("PDF-Symbolleiste").setToolbarToggle("Symbolleiste")
                .setSidebar("Seitenleiste")
                .setSidebarView("Ansicht der Seitenleiste")
                .setThumbnails("Seitenvorschau").setThumbnailsView("Vorschau")
                .setOutline("Inhalt").setPreviousPage("Vorherige Seite")
                .setNextPage("Nächste Seite").setPage("Seite")
                .setPageOf("Seite von {pageCount}")
                .setPageError("1–{pageCount} eingeben")
                .setPageAnnouncement("Seite {page} von {pageCount}")
                .setZoom("Zoom").setZoomIn("Vergrößern")
                .setZoomOut("Verkleinern").setPageWidth("Seitenbreite")
                .setPageFit("Ganze Seite").setFind("Im Dokument suchen")
                .setPreviousMatch("Vorheriger Treffer")
                .setNextMatch("Nächster Treffer")
                .setCloseFind("Suche schließen")
                .setFindResult("{current} von {total}")
                .setFindResultAnnouncement(
                        "{current} von {total}, Seite {page}")
                .setFindNoMatches("Keine Treffer").setDownload("Herunterladen")
                .setPrint("Drucken").setPrinting("Druck wird vorbereitet…")
                .setCancelPrint("Abbrechen")
                .setPrintError("Das Dokument konnte nicht gedruckt werden.")
                .setDocument("PDF-Dokument").setPages("Seiten")
                .setPageLabel("Seite {page}").setLink("Link")
                .setGoToPage("Gehe zu Seite {page}")
                .setExternalLink("{text} (öffnet in einem neuen Tab)");
    }
}
