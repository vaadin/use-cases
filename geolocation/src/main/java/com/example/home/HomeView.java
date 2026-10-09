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

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Geolocation API Use Cases")
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("Geolocation API — use cases",
                "Each card below shows one way to work with the user's location "
                        + "using the Vaadin Flow Geolocation API.");
        addGroup("Getting a location", VaadinIcon.MAP_MARKER, Accent.BLUE,
                List.of(OneShotOnClickView.class, AutoFetchView.class,
                        FormFieldView.class));
        addGroup("Tracking movement", VaadinIcon.ROAD, Accent.GREEN,
                List.of(TrackingView.class, DbTrackingView.class));
        addGroup("Accuracy and failures", VaadinIcon.SLIDERS, Accent.ORANGE,
                List.of(DenialView.class, DetailedDataView.class,
                        OptionsView.class));
    }
}
