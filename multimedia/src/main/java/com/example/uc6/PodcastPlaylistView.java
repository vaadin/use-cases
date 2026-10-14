package com.example.uc6;

import java.util.List;

import com.example.MissingAPI;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Audio;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC6 — Podcast playlist.
 * <p>
 * A podcast page lists the episodes of a show. Picking an episode plays it in a
 * single audio player, and when "Play next automatically" is on, the next
 * episode starts when one ends. The server keeps track of the current episode
 * and highlights it in the list. The current episode is part of the URL, such
 * as {@code uc6/2}, so it can be bookmarked and shared; opening such a link
 * loads the episode without starting it.
 * <p>
 * This needs more than the component offers: swapping the {@code <source>} of a
 * player only takes effect after {@code load()}, starting playback needs
 * {@code play()}, and moving on needs the {@code ended} event. Those come from
 * {@link MissingAPI}.
 */
@Route(value = "uc6/:episode?", layout = MainLayout.class)
@PageTitle("UC6 — Podcast playlist")
@UseCaseDescription("Playing podcast episodes in one player and continuing with the next")
@Menu(order = 6, title = "UC6 — Podcast playlist")
@StyleSheet("uc6.css")
public class PodcastPlaylistView extends VerticalLayout
        implements BeforeEnterObserver {

    /**
     * One episode of the show.
     */
    record Episode(String title, String file) {
    }

    static final List<Episode> EPISODES = List.of(
            new Episode("Four-day weeks and release cadence",
                    "media/podcast/episode-1.mp3"),
            new Episode("Accessibility testing with a screen reader",
                    "media/podcast/episode-2.mp3"),
            new Episode("Moving a ten-year-old app to the cloud",
                    "media/podcast/episode-3.mp3"));

    private final ValueSignal<Integer> current = new ValueSignal<>(0);
    private final Audio audio = new Audio();
    private final Checkbox autoAdvance = new Checkbox("Play next automatically",
            true);

    public PodcastPlaylistView() {
        addClassName("uc6-view");
        add(new H1("UC6 — Podcast playlist"));
        add(new Paragraph("Pick an episode to play it. With automatic "
                + "playback on, the next episode starts when the current "
                + "one ends."));

        Span nowPlaying = new Span();
        nowPlaying.addClassName("now-playing");
        nowPlaying.bindText(current.map(i -> "Now playing: " + (i + 1) + ". "
                + EPISODES.get(i).title()));

        audio.setControlsVisible(true);
        audio.setAriaLabel("Podcast player");
        audio.setWidthFull();
        audio.addSource(EPISODES.get(0).file(), "audio/mpeg");
        MissingAPI.addEndedListener(audio, () -> {
            int next = current.peek() + 1;
            if (autoAdvance.getValue() && next < EPISODES.size()) {
                playEpisode(next);
            }
        });
        add(new Div(nowPlaying, audio, autoAdvance));

        add(new H2("Episodes"));
        Div list = new Div();
        list.addClassName("episodes");
        for (int i = 0; i < EPISODES.size(); i++) {
            int index = i;
            Episode episode = EPISODES.get(i);
            Button play = new Button((i + 1) + ". " + episode.title(),
                    e -> playEpisode(index));
            play.addClassName("episode");
            play.bindClassName("current", current.map(c -> c == index));
            list.add(play);
        }
        add(list);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        int index = event.getRouteParameters().getInteger("episode")
                .filter(number -> number >= 1 && number <= EPISODES.size())
                .map(number -> number - 1).orElse(0);
        if (index != current.peek()) {
            // Browsers block autoplay on page load, so only load it.
            loadEpisode(index);
        }
    }

    /**
     * Switches the player to an episode, starts it and puts it in the URL.
     *
     * @param index
     *            the position of the episode in {@link #EPISODES}
     */
    void playEpisode(int index) {
        loadEpisode(index);
        MissingAPI.play(audio);
        // beforeEnter sees the episode is already current, so the navigation
        // only updates the URL and neither reloads nor plays it again.
        UI.getCurrent().navigate(PodcastPlaylistView.class,
                new RouteParameters("episode", String.valueOf(index + 1)));
    }

    private void loadEpisode(int index) {
        current.set(index);
        audio.removeAll();
        audio.addSource(EPISODES.get(index).file(), "audio/mpeg");
        MissingAPI.load(audio);
    }

    Audio getAudio() {
        return audio;
    }
}
