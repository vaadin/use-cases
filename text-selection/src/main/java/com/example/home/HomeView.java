package com.example.home;

import java.util.List;

import com.example.common.BaseHomeView;
import com.example.uc1.SelectAllOnFocusView;
import com.example.uc2.FormatAndSelectView;
import com.example.uc3.FindAndHighlightView;
import com.example.uc4.ValidationJumpView;
import com.example.uc5.InsertTemplateView;
import com.example.uc6.LiveSelectionInfoView;
import com.example.uc7.SelectionToolbarView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Text Selection API Use Cases")
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("Text Selection API — use cases",
                "Each card below uses the Text Selection API on TextField and "
                        + "TextArea: the server controls the selection, the cursor "
                        + "and the clipboard, and reads the current selection as a Signal "
                        + "that can drive reactive UI.");
        addGroup("Selecting the whole value", VaadinIcon.INPUT, Accent.BLUE,
                List.of(SelectAllOnFocusView.class, FormatAndSelectView.class));
        addGroup("Pointing at part of the text", VaadinIcon.SEARCH,
                Accent.ORANGE,
                List.of(FindAndHighlightView.class, ValidationJumpView.class));
        addGroup("Acting on the user's selection", VaadinIcon.EDIT,
                Accent.PURPLE,
                List.of(InsertTemplateView.class, LiveSelectionInfoView.class,
                        SelectionToolbarView.class));
    }
}
