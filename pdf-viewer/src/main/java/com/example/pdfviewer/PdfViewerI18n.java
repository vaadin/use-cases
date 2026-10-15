package com.example.pdfviewer;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

/**
 * The texts of {@link PdfViewer}, for localization. Texts left {@code null}
 * keep the web component's English default. Some texts take placeholders that
 * the viewer fills in: {@code {pageCount}}, {@code {page}}, {@code {current}}
 * and {@code {total}}, as the defaults show.
 * <p>
 * The fields mirror the {@code PdfViewerI18n} interface of the web component.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PdfViewerI18n implements Serializable {

    private @Nullable String loadError;
    private @Nullable String passwordError;
    private @Nullable String toolbar;
    private @Nullable String toolbarToggle;
    private @Nullable String sidebar;
    private @Nullable String sidebarView;
    private @Nullable String thumbnails;
    private @Nullable String thumbnailsView;
    private @Nullable String outline;
    private @Nullable String previousPage;
    private @Nullable String nextPage;
    private @Nullable String page;
    private @Nullable String pageOf;
    private @Nullable String pageError;
    private @Nullable String pageAnnouncement;
    private @Nullable String zoom;
    private @Nullable String zoomIn;
    private @Nullable String zoomOut;
    private @Nullable String pageWidth;
    private @Nullable String pageFit;
    private @Nullable String find;
    private @Nullable String previousMatch;
    private @Nullable String nextMatch;
    private @Nullable String closeFind;
    private @Nullable String findResult;
    private @Nullable String findResultAnnouncement;
    private @Nullable String findNoMatches;
    private @Nullable String download;
    private @Nullable String print;
    private @Nullable String printing;
    private @Nullable String cancelPrint;
    private @Nullable String printError;
    private @Nullable String document;
    private @Nullable String pages;
    private @Nullable String pageLabel;
    private @Nullable String link;
    private @Nullable String goToPage;
    private @Nullable String externalLink;

    public @Nullable String getLoadError() {
        return loadError;
    }

    public PdfViewerI18n setLoadError(@Nullable String loadError) {
        this.loadError = loadError;
        return this;
    }

    public @Nullable String getPasswordError() {
        return passwordError;
    }

    public PdfViewerI18n setPasswordError(@Nullable String passwordError) {
        this.passwordError = passwordError;
        return this;
    }

    public @Nullable String getToolbar() {
        return toolbar;
    }

    public PdfViewerI18n setToolbar(@Nullable String toolbar) {
        this.toolbar = toolbar;
        return this;
    }

    public @Nullable String getToolbarToggle() {
        return toolbarToggle;
    }

    public PdfViewerI18n setToolbarToggle(@Nullable String toolbarToggle) {
        this.toolbarToggle = toolbarToggle;
        return this;
    }

    public @Nullable String getSidebar() {
        return sidebar;
    }

    public PdfViewerI18n setSidebar(@Nullable String sidebar) {
        this.sidebar = sidebar;
        return this;
    }

    public @Nullable String getSidebarView() {
        return sidebarView;
    }

    public PdfViewerI18n setSidebarView(@Nullable String sidebarView) {
        this.sidebarView = sidebarView;
        return this;
    }

    public @Nullable String getThumbnails() {
        return thumbnails;
    }

    public PdfViewerI18n setThumbnails(@Nullable String thumbnails) {
        this.thumbnails = thumbnails;
        return this;
    }

    public @Nullable String getThumbnailsView() {
        return thumbnailsView;
    }

    public PdfViewerI18n setThumbnailsView(@Nullable String thumbnailsView) {
        this.thumbnailsView = thumbnailsView;
        return this;
    }

    public @Nullable String getOutline() {
        return outline;
    }

    public PdfViewerI18n setOutline(@Nullable String outline) {
        this.outline = outline;
        return this;
    }

    public @Nullable String getPreviousPage() {
        return previousPage;
    }

    public PdfViewerI18n setPreviousPage(@Nullable String previousPage) {
        this.previousPage = previousPage;
        return this;
    }

    public @Nullable String getNextPage() {
        return nextPage;
    }

    public PdfViewerI18n setNextPage(@Nullable String nextPage) {
        this.nextPage = nextPage;
        return this;
    }

    public @Nullable String getPage() {
        return page;
    }

    public PdfViewerI18n setPage(@Nullable String page) {
        this.page = page;
        return this;
    }

    public @Nullable String getPageOf() {
        return pageOf;
    }

    public PdfViewerI18n setPageOf(@Nullable String pageOf) {
        this.pageOf = pageOf;
        return this;
    }

    public @Nullable String getPageError() {
        return pageError;
    }

    public PdfViewerI18n setPageError(@Nullable String pageError) {
        this.pageError = pageError;
        return this;
    }

    public @Nullable String getPageAnnouncement() {
        return pageAnnouncement;
    }

    public PdfViewerI18n setPageAnnouncement(
            @Nullable String pageAnnouncement) {
        this.pageAnnouncement = pageAnnouncement;
        return this;
    }

    public @Nullable String getZoom() {
        return zoom;
    }

    public PdfViewerI18n setZoom(@Nullable String zoom) {
        this.zoom = zoom;
        return this;
    }

    public @Nullable String getZoomIn() {
        return zoomIn;
    }

    public PdfViewerI18n setZoomIn(@Nullable String zoomIn) {
        this.zoomIn = zoomIn;
        return this;
    }

    public @Nullable String getZoomOut() {
        return zoomOut;
    }

    public PdfViewerI18n setZoomOut(@Nullable String zoomOut) {
        this.zoomOut = zoomOut;
        return this;
    }

    public @Nullable String getPageWidth() {
        return pageWidth;
    }

    public PdfViewerI18n setPageWidth(@Nullable String pageWidth) {
        this.pageWidth = pageWidth;
        return this;
    }

    public @Nullable String getPageFit() {
        return pageFit;
    }

    public PdfViewerI18n setPageFit(@Nullable String pageFit) {
        this.pageFit = pageFit;
        return this;
    }

    public @Nullable String getFind() {
        return find;
    }

    public PdfViewerI18n setFind(@Nullable String find) {
        this.find = find;
        return this;
    }

    public @Nullable String getPreviousMatch() {
        return previousMatch;
    }

    public PdfViewerI18n setPreviousMatch(@Nullable String previousMatch) {
        this.previousMatch = previousMatch;
        return this;
    }

    public @Nullable String getNextMatch() {
        return nextMatch;
    }

    public PdfViewerI18n setNextMatch(@Nullable String nextMatch) {
        this.nextMatch = nextMatch;
        return this;
    }

    public @Nullable String getCloseFind() {
        return closeFind;
    }

    public PdfViewerI18n setCloseFind(@Nullable String closeFind) {
        this.closeFind = closeFind;
        return this;
    }

    public @Nullable String getFindResult() {
        return findResult;
    }

    public PdfViewerI18n setFindResult(@Nullable String findResult) {
        this.findResult = findResult;
        return this;
    }

    public @Nullable String getFindResultAnnouncement() {
        return findResultAnnouncement;
    }

    public PdfViewerI18n setFindResultAnnouncement(
            @Nullable String findResultAnnouncement) {
        this.findResultAnnouncement = findResultAnnouncement;
        return this;
    }

    public @Nullable String getFindNoMatches() {
        return findNoMatches;
    }

    public PdfViewerI18n setFindNoMatches(@Nullable String findNoMatches) {
        this.findNoMatches = findNoMatches;
        return this;
    }

    public @Nullable String getDownload() {
        return download;
    }

    public PdfViewerI18n setDownload(@Nullable String download) {
        this.download = download;
        return this;
    }

    public @Nullable String getPrint() {
        return print;
    }

    public PdfViewerI18n setPrint(@Nullable String print) {
        this.print = print;
        return this;
    }

    public @Nullable String getPrinting() {
        return printing;
    }

    public PdfViewerI18n setPrinting(@Nullable String printing) {
        this.printing = printing;
        return this;
    }

    public @Nullable String getCancelPrint() {
        return cancelPrint;
    }

    public PdfViewerI18n setCancelPrint(@Nullable String cancelPrint) {
        this.cancelPrint = cancelPrint;
        return this;
    }

    public @Nullable String getPrintError() {
        return printError;
    }

    public PdfViewerI18n setPrintError(@Nullable String printError) {
        this.printError = printError;
        return this;
    }

    public @Nullable String getDocument() {
        return document;
    }

    public PdfViewerI18n setDocument(@Nullable String document) {
        this.document = document;
        return this;
    }

    public @Nullable String getPages() {
        return pages;
    }

    public PdfViewerI18n setPages(@Nullable String pages) {
        this.pages = pages;
        return this;
    }

    public @Nullable String getPageLabel() {
        return pageLabel;
    }

    public PdfViewerI18n setPageLabel(@Nullable String pageLabel) {
        this.pageLabel = pageLabel;
        return this;
    }

    public @Nullable String getLink() {
        return link;
    }

    public PdfViewerI18n setLink(@Nullable String link) {
        this.link = link;
        return this;
    }

    public @Nullable String getGoToPage() {
        return goToPage;
    }

    public PdfViewerI18n setGoToPage(@Nullable String goToPage) {
        this.goToPage = goToPage;
        return this;
    }

    public @Nullable String getExternalLink() {
        return externalLink;
    }

    public PdfViewerI18n setExternalLink(@Nullable String externalLink) {
        this.externalLink = externalLink;
        return this;
    }
}
