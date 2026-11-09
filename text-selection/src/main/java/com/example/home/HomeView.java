package com.example.home;

import java.util.List;

import com.example.common.BaseHomeView;
import com.example.common.UseCaseDescription;
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
@UseCaseDescription("Selecting text and moving the cursor in text fields from the server")
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("Text Selection API — use cases",
                "HasTextSelection lets the server select text and move the "
                        + "cursor in a TextField, TextArea, PasswordField or "
                        + "BigDecimalField. The cards below show what that is "
                        + "for, including cases that also need to read the "
                        + "user's selection, which has no API yet.");
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
