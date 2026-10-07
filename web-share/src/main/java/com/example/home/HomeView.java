package com.example.home;

import com.example.common.BaseHomeView;
import com.example.uc1.ShareThisPageView;
import com.example.uc2.CopyLinkFallbackView;
import com.example.uc3.CustomMessageView;
import com.example.uc4.ShareListItemsView;
import com.example.uc5.ShareFeedbackView;
import com.example.uc6.ShareInviteLinkView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("Web Share API — use cases",
                "Each card below exercises one use case of "
                        + "WebShare.onClick(button).share(...) and "
                        + "WebShare.supportSignal(). The signal reports "
                        + "SUPPORTED, UNSUPPORTED, or UNKNOWN, and the bound "
                        + "share invokes the browser's native share sheet on "
                        + "platforms that expose navigator.share (most "
                        + "mobile browsers and recent desktop Safari/Edge).");

        Div cards = new Div();
        cards.addClassName("home-cards");
        cards.add(homeCard("UC1", "Share this page",
                "Hand the current URL + title to the native share sheet.",
                ShareThisPageView.class));
        cards.add(homeCard("UC2", "Copy-link fallback",
                "Swap between native Share and Copy-link based on the signal.",
                CopyLinkFallbackView.class));
        cards.add(homeCard("UC3", "Share a custom message",
                "Fill title/text/url, preview the payload, share.",
                CustomMessageView.class));
        cards.add(homeCard("UC4", "Share each item in a list",
                "Per-row share icon on a list of articles.",
                ShareListItemsView.class));
        cards.add(homeCard("UC5", "Share with completion feedback",
                "React to success, cancellation, and errors.",
                ShareFeedbackView.class));
        cards.add(homeCard("UC6", "Share an invite link",
                "Generate a one-shot join URL and hand it to the sheet.",
                ShareInviteLinkView.class));
        add(cards);
    }
}
