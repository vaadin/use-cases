package com.example.home;

import java.util.List;

import com.example.common.BaseHomeView;
import com.example.common.UseCaseDescription;
import com.example.uc1.ReviewBeforeSendingView;
import com.example.uc10.ShowOnceView;
import com.example.uc11.ContentsPageView;
import com.example.uc12.FillFormView;
import com.example.uc13.TurnScanView;
import com.example.uc2.ReadBeforeAcceptingView;
import com.example.uc3.DocumentLibraryView;
import com.example.uc4.ManualDeepLinkView;
import com.example.uc5.CheckReceiptView;
import com.example.uc6.PrintLabelView;
import com.example.uc7.JumpToClauseView;
import com.example.uc8.LocalizedViewerView;
import com.example.uc9.ApproveInToolbarView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@UseCaseDescription("Showing, navigating and printing PDF documents inside a Vaadin app")
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    private static final String API_GAPS_URL = "https://github.com/vaadin/use-cases/blob/main/pdf-viewer/API-GAPS.md";

    public HomeView() {
        super("PDF Viewer — use cases",
                "Business apps are full of PDFs: invoices, contracts, "
                        + "receipts, manuals. Each card below is one thing an "
                        + "app does with them, built on the prototype "
                        + "<vaadin-pdf-viewer> web component.");
        addGroup("Show the right document", VaadinIcon.FILE_TEXT_O, Accent.BLUE,
                List.of(ReviewBeforeSendingView.class,
                        DocumentLibraryView.class, CheckReceiptView.class,
                        TurnScanView.class));
        addGroup("Guide the reader", VaadinIcon.BOOKMARK_O, Accent.GREEN,
                List.of(ReadBeforeAcceptingView.class, ManualDeepLinkView.class,
                        JumpToClauseView.class, ContentsPageView.class));
        addGroup("Act on the document", VaadinIcon.EDIT, Accent.PURPLE,
                List.of(ApproveInToolbarView.class, FillFormView.class));
        addGroup("Fit the app", VaadinIcon.COG_O, Accent.ORANGE,
                List.of(PrintLabelView.class, LocalizedViewerView.class,
                        ShowOnceView.class));
        add(new Paragraph(new Text("There is no Flow component for the viewer "
                + "yet, so this app wraps the web component itself. Where the "
                + "API falls short, the use case says so and links to "),
                new Anchor(API_GAPS_URL, "API-GAPS.md"), new Text(".")));
    }
}
