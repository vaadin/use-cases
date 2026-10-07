package com.example.uc7;

import java.util.List;

import com.example.MediaTester;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = ChapterControlsView.class)
class ChapterControlsViewTest extends SpringBrowserlessTest {

    @Test
    void usesOwnControlsInsteadOfTheBrowsers() {
        navigate(ChapterControlsView.class);
        runPendingSignalsTasks();

        assertFalse(findInView(Video.class).single().isControls());
        assertEquals(4, chapterButtons().size());
        assertTrue(chapterButtons().get(0).hasClassName("current"));
        assertEquals("0:00 / 0:00", time().getText());
    }

    @Test
    void playButtonFollowsThePlayerState() {
        navigate(ChapterControlsView.class);
        runPendingSignalsTasks();
        MediaTester media = new MediaTester(findInView(Video.class).single());
        media.takeJavaScript();
        Button playPause = find(Button.class).withText("Play").single();

        playPause.click();
        assertTrue(media.takeJavaScript().stream()
                .anyMatch(js -> js.contains("this.play()")));

        // The label only changes once the browser confirms playback.
        media.fire("play");
        runPendingSignalsTasks();
        assertEquals("Pause", playPause.getText());

        media.fire("pause");
        runPendingSignalsTasks();
        assertEquals("Play", playPause.getText());
    }

    @Test
    void timeUpdatesMoveTheChapterHighlight() {
        navigate(ChapterControlsView.class);
        runPendingSignalsTasks();
        MediaTester media = new MediaTester(findInView(Video.class).single());

        media.fire("loadedmetadata", "element.duration", 60);
        media.fire("timeupdate", "element.currentTime", 31.5);
        runPendingSignalsTasks();

        assertTrue(chapterButtons().get(2).hasClassName("current"));
        assertFalse(chapterButtons().get(0).hasClassName("current"));
        assertEquals("0:31 / 1:00", time().getText());
    }

    @Test
    void chapterButtonSeeksAndPlays() {
        navigate(ChapterControlsView.class);
        runPendingSignalsTasks();
        MediaTester media = new MediaTester(findInView(Video.class).single());
        media.takeJavaScript();

        chapterButtons().get(3).click();
        runPendingSignalsTasks();

        List<String> js = media.takeJavaScript();
        assertEquals(2, js.size(), "expected seek then play: " + js);
        assertTrue(js.get(0).contains("currentTime"), js.toString());
        assertTrue(js.get(1).contains("play"), js.toString());
        assertTrue(chapterButtons().get(3).hasClassName("current"));
    }

    private List<Button> chapterButtons() {
        return findInView(Button.class).all().stream()
                .filter(b -> b.hasClassName("chapter")).toList();
    }

    private Span time() {
        return findInView(Span.class).all().stream()
                .filter(s -> s.hasClassName("time")).findFirst().orElseThrow();
    }

    @Test
    void timeInTheUrlStartsPlaybackThere() {
        navigate("uc7?t=23", ChapterControlsView.class);
        runPendingSignalsTasks();
        MediaTester media = new MediaTester(findInView(Video.class).single());

        assertTrue(media.takeJavaScript().stream()
                .anyMatch(js -> js.contains("currentTime")));
        assertTrue(chapterButtons().get(2).hasClassName("current"));
    }

    @Test
    void jumpingToAChapterPutsItsTimeInTheUrl() {
        navigate(ChapterControlsView.class);
        runPendingSignalsTasks();

        chapterButtons().get(1).click();
        runPendingSignalsTasks();

        assertEquals("uc7?t=11", location());
    }

    private static String location() {
        return UI.getCurrent().getInternals().getActiveViewLocation()
                .getPathWithQueryParameters();
    }
}
