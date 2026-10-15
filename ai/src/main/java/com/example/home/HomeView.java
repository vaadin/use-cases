package com.example.home;

import java.util.List;

import com.example.common.BaseHomeView;
import com.example.common.UseCaseDescription;
import com.example.uc1.OrderAssistantView;
import com.example.uc2.InvoiceReaderView;
import com.example.uc3.HandbookView;
import com.example.uc4.DocsAssistantView;
import com.example.uc5.VoiceOverView;
import com.example.uc6.PresenterVideoView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@UseCaseDescription("Using AI inside a business application: answers from your own data, documents into forms, generated audio and video")
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("AI in business apps — use cases",
                "AI helps users of a business application in more ways than "
                        + "a chat window. These use cases answer questions "
                        + "from the application's own data and documents, "
                        + "turn an invoice into form data, and produce "
                        + "narration and presenter videos. The chat-based "
                        + "ones are built on Vaadin's AIOrchestrator with "
                        + "Spring AI.");
        addGroup("Answers from your own data and documents",
                VaadinIcon.COMMENTS_O, Accent.BLUE,
                List.of(OrderAssistantView.class, HandbookView.class,
                        DocsAssistantView.class));
        addGroup("Work taken off the user's hands", VaadinIcon.MAGIC,
                Accent.PURPLE, List.of(InvoiceReaderView.class,
                        VoiceOverView.class, PresenterVideoView.class));
    }
}
