package com.example.home;

import java.util.List;

import com.example.common.BaseHomeView;
import com.example.uc1.CopyStaticTextView;
import com.example.uc2.CopyComponentValueView;
import com.example.uc3.CopyRichContentView;
import com.example.uc4.CopyImageView;
import com.example.uc5.PasteSpreadsheetView;
import com.example.uc6.CopyFromContextMenuView;
import com.example.uc7.PasteFilesView;
import com.example.uc8.CopyFromGridView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Clipboard API Use Cases")
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("Clipboard API — use cases",
                "Each card below shows one way to copy to or paste from the "
                        + "clipboard with the Vaadin Flow Clipboard API.");
        addGroup("Copying content", VaadinIcon.COPY_O, Accent.BLUE,
                List.of(CopyStaticTextView.class, CopyComponentValueView.class,
                        CopyRichContentView.class, CopyImageView.class));
        addGroup("Copy actions in menus and grids", VaadinIcon.TABLE,
                Accent.PURPLE,
                List.of(CopyFromContextMenuView.class, CopyFromGridView.class));
        addGroup("Pasting into the app", VaadinIcon.PASTE, Accent.GREEN,
                List.of(PasteSpreadsheetView.class, PasteFilesView.class));
        Anchor inspector = new Anchor(
                "https://evercoder.github.io/clipboard-inspector/",
                "Clipboard Inspector");
        inspector.setTarget("_blank");
        add(new Paragraph(new Span("Tip: use "), inspector, new Span(
                " to check what the copy use cases actually put on the clipboard.")));
    }
}
