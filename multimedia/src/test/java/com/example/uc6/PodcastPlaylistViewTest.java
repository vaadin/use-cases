package com.example.uc6;

import java.util.List;

import com.example.MediaTester;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Audio;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = PodcastPlaylistView.class)
class PodcastPlaylistViewTest extends SpringBrowserlessTest {

    @Test
    void startsWithTheFirstEpisodeLoaded() {
        navigate(PodcastPlaylistView.class);
        runPendingSignalsTasks();

        Audio audio = findInView(Audio.class).single();
        assertTrue(audio.isControlsVisible());
        assertEquals(PodcastPlaylistView.EPISODES.get(0).file(),
                audio.getSources().get(0).getSrc());
        assertEquals(3, episodeButtons().size());
        assertTrue(episodeButtons().get(0).hasClassName("current"));
    }

    @Test
    void pickingAnEpisodeSwapsTheSourceAndPlaysIt() {
        navigate(PodcastPlaylistView.class);
        runPendingSignalsTasks();
        Audio audio = findInView(Audio.class).single();
        MediaTester media = new MediaTester(audio);
        media.takeJavaScript();

        episodeButtons().get(2).click();
        runPendingSignalsTasks();

        assertEquals(1, audio.getSources().size());
        assertEquals(PodcastPlaylistView.EPISODES.get(2).file(),
                audio.getSources().get(0).getSrc());
        List<String> js = media.takeJavaScript();
        assertEquals(2, js.size(), "expected load() then play(): " + js);
        assertTrue(js.get(0).contains("load"));
        assertTrue(js.get(1).contains("play"));
        assertTrue(episodeButtons().get(2).hasClassName("current"));
        assertNowPlaying("3. Moving a ten-year-old app to the cloud");
    }

    @Test
    void endedMovesOnOnlyWhenAutomaticPlaybackIsOn() {
        navigate(PodcastPlaylistView.class);
        runPendingSignalsTasks();
        Audio audio = findInView(Audio.class).single();
        MediaTester media = new MediaTester(audio);

        media.fire("ended");
        runPendingSignalsTasks();
        assertEquals(PodcastPlaylistView.EPISODES.get(1).file(),
                audio.getSources().get(0).getSrc());

        test(find(Checkbox.class).single()).click();
        media.fire("ended");
        runPendingSignalsTasks();
        assertEquals(PodcastPlaylistView.EPISODES.get(1).file(),
                audio.getSources().get(0).getSrc());
    }

    private List<Button> episodeButtons() {
        return findInView(Button.class).all().stream()
                .filter(b -> b.hasClassName("episode")).toList();
    }

    private void assertNowPlaying(String fragment) {
        assertTrue(
                findInView(Span.class).all().stream()
                        .anyMatch(s -> s.getText().contains(fragment)),
                "expected now playing to contain \"" + fragment + "\"");
    }

    @Test
    void episodeInTheUrlIsLoadedWithoutAutoplay() {
        navigate("uc6/3", PodcastPlaylistView.class);
        runPendingSignalsTasks();
        Audio audio = findInView(Audio.class).single();

        assertEquals(PodcastPlaylistView.EPISODES.get(2).file(),
                audio.getSources().get(0).getSrc());
        assertTrue(episodeButtons().get(2).hasClassName("current"));
        // Browsers block autoplay on page load; the user presses play.
        assertTrue(new MediaTester(audio).takeJavaScript().stream()
                .noneMatch(js -> js.contains("play()")));
    }

    @Test
    void pickingAnEpisodePutsItInTheUrl() {
        navigate(PodcastPlaylistView.class);
        runPendingSignalsTasks();

        episodeButtons().get(1).click();
        runPendingSignalsTasks();

        assertEquals("uc6/2", location());
    }

    private static String location() {
        return UI.getCurrent().getInternals().getActiveViewLocation()
                .getPathWithQueryParameters();
    }
}
