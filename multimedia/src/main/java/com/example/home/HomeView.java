package com.example.home;

import java.util.List;

import com.example.common.BaseHomeView;
import com.example.uc1.PrivateRecordingView;
import com.example.uc2.SeekableStreamView;
import com.example.uc3.AdaptiveStreamingView;
import com.example.uc4.FormatFallbackView;
import com.example.uc5.BackgroundVideoView;
import com.example.uc6.PodcastPlaylistView;
import com.example.uc7.ChapterControlsView;
import com.example.uc8.ResumePlaybackView;
import com.example.uc9.SubtitlesView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("Video & Audio — use cases",
                "Each card below exercises one use case of the Video, Audio "
                        + "and Source components. Media is served either as "
                        + "static files or by the application through "
                        + "DownloadHandlers; see API-GAPS.md for what still "
                        + "needs a workaround.");
        addGroup("Delivering the media", VaadinIcon.CLOUD_DOWNLOAD, Accent.BLUE,
                List.of(PrivateRecordingView.class, SeekableStreamView.class,
                        AdaptiveStreamingView.class, FormatFallbackView.class));
        addGroup("Playing it back", VaadinIcon.PLAY_CIRCLE, Accent.GREEN,
                List.of(BackgroundVideoView.class, PodcastPlaylistView.class,
                        ChapterControlsView.class, ResumePlaybackView.class,
                        SubtitlesView.class));
    }
}
