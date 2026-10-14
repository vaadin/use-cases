package com.example.home;

import java.util.List;

import com.example.common.BaseHomeView;
import com.example.common.UseCaseDescription;
import com.example.uc1.DamageReportView;
import com.example.uc2.AttachDocumentsView;
import com.example.uc3.ProfilePictureView;
import com.example.uc4.PhotoAlbumView;
import com.example.uc5.LargeFileView;
import com.example.uc6.ImportCsvView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@UseCaseDescription("Sending files to the server and handling them once they arrive")
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("File uploads — use cases",
                "Each card below is something a user wants to get done by "
                        + "sending a file to the application, and what the "
                        + "application does with the file once it arrives.");
        addGroup("Files that come with a form", VaadinIcon.PAPERCLIP,
                Accent.BLUE,
                List.of(DamageReportView.class, AttachDocumentsView.class));
        addGroup("Pictures", VaadinIcon.PICTURE, Accent.GREEN,
                List.of(ProfilePictureView.class, PhotoAlbumView.class));
        addGroup("Large files and imports", VaadinIcon.FILE_TABLE,
                Accent.ORANGE,
                List.of(LargeFileView.class, ImportCsvView.class));
    }
}
