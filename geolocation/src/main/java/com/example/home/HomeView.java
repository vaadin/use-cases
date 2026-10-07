package com.example.home;

import java.util.List;

import com.example.common.BaseHomeView;
import com.example.uc1.OneShotOnClickView;
import com.example.uc2.TrackingView;
import com.example.uc3.AutoFetchView;
import com.example.uc4.DenialView;
import com.example.uc5.DetailedDataView;
import com.example.uc6.OptionsView;
import com.example.uc7.FormFieldView;
import com.example.uc8.DbTrackingView;
import com.example.views.MainLayout;

import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Geolocation API Use Cases")
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("Geolocation API — use cases",
                "Each card below exercises one use case of the Vaadin Flow Geolocation API.");
        addMenuCards(List.of(OneShotOnClickView.class, TrackingView.class,
                AutoFetchView.class, DenialView.class, DetailedDataView.class,
                OptionsView.class, FormFieldView.class, DbTrackingView.class));
    }
}
