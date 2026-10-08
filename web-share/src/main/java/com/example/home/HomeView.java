package com.example.home;

import java.util.List;

import com.example.common.BaseHomeView;
import com.example.uc1.ShareThisPageView;
import com.example.uc2.CopyLinkFallbackView;
import com.example.uc3.CustomMessageView;
import com.example.uc4.ShareListItemsView;
import com.example.uc5.ShareFeedbackView;
import com.example.uc6.ShareInviteLinkView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("Web Share API — use cases",
                "Each card below hands content to the browser's native share "
                        + "sheet with WebShare.onClick(button).share(...), and "
                        + "uses WebShare.supportSignal() to adapt the UI where "
                        + "the browser can't share (navigator.share is mostly "
                        + "available on mobile and recent desktop Safari/Edge).");
        addGroup("Sharing what's on screen", VaadinIcon.SHARE, Accent.BLUE,
                List.of(ShareThisPageView.class, ShareListItemsView.class));
        addGroup("Sharing content the app builds", VaadinIcon.EDIT,
                Accent.PURPLE,
                List.of(CustomMessageView.class, ShareInviteLinkView.class));
        addGroup("Fallbacks and feedback", VaadinIcon.CHECK_CIRCLE_O,
                Accent.GREEN,
                List.of(CopyLinkFallbackView.class, ShareFeedbackView.class));
    }
}
