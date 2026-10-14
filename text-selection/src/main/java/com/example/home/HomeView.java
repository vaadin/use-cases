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

import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Text Selection API Use Cases")
@UseCaseDescription("Controlling selection, cursor position and clipboard in text fields")
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("Text Selection API — use cases",
                "Each card below exercises one use case of the Text Selection "
                        + "API on TextField and TextArea. The API gives the "
                        + "server programmatic control over selection, cursor "
                        + "position, and clipboard, and exposes the current "
                        + "selection as a Signal so it can drive reactive UI.");
        addMenuCards(List.of(SelectAllOnFocusView.class,
                FormatAndSelectView.class, FindAndHighlightView.class,
                ValidationJumpView.class, InsertTemplateView.class,
                LiveSelectionInfoView.class, SelectionToolbarView.class));
    }
}
