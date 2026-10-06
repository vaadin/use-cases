package com.example.home;

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

import com.vaadin.flow.component.html.Div;
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

        Div cards = new Div();
        cards.addClassName("home-cards");
        cards.add(homeCard("UC1", "Private recording",
                "Video and poster served per user, not from a public URL.",
                PrivateRecordingView.class));
        cards.add(homeCard("UC2", "Seekable streaming",
                "Byte-range requests so a long recording can be scrubbed.",
                SeekableStreamView.class));
        cards.add(homeCard("UC3", "Adaptive streaming",
                "HLS with three renditions from a single handler.",
                AdaptiveStreamingView.class));
        cards.add(homeCard("UC4", "Format fallback",
                "AV1 first, H.264 for browsers that cannot play it.",
                FormatFallbackView.class));
        cards.add(homeCard("UC5", "Background video",
                "Muted, looping hero clip with a pause button.",
                BackgroundVideoView.class));
        cards.add(homeCard("UC6", "Podcast playlist",
                "One audio player that moves on to the next episode.",
                PodcastPlaylistView.class));
        cards.add(homeCard("UC7", "Chapters & controls",
                "Server-side play, pause, skip and chapter jumps.",
                ChapterControlsView.class));
        cards.add(homeCard("UC8", "Resume playback",
                "Continue where you left off after a reload.",
                ResumePlaybackView.class));
        cards.add(homeCard("UC9", "Subtitles",
                "WebVTT tracks in three languages with a picker.",
                SubtitlesView.class));
        add(cards);
    }
}
