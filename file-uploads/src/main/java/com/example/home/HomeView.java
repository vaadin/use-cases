package com.example.home;

import java.util.List;

import com.example.common.BaseHomeView;
import com.example.uc1.DamageReportView;
import com.example.uc2.AttachDocumentsView;
import com.example.uc3.ProfilePictureView;
import com.example.uc4.PhotoAlbumView;
import com.example.uc5.LargeFileView;
import com.example.uc6.ImportCsvView;
import com.example.views.MainLayout;

import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@PageTitle("File Upload Use Cases")
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("File uploads — use cases",
                "Each card below is something a user wants to get done by "
                        + "sending a file to the application, and what the "
                        + "application does with the file once it arrives.");
        addMenuCards(List.of(DamageReportView.class, AttachDocumentsView.class,
                ProfilePictureView.class, PhotoAlbumView.class,
                LargeFileView.class, ImportCsvView.class));
    }
}
