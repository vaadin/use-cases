package com.example.uc9;

import java.util.List;
import java.util.Locale;

import com.example.MediaTester;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Video;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.dom.Element;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = SubtitlesView.class)
class SubtitlesViewTest extends SpringBrowserlessTest {

    @Test
    void addsOneServedTrackPerLanguage() {
        navigate(SubtitlesView.class);

        Video video = findInView(Video.class).single();
        List<Element> tracks = video.getElement().getChildren()
                .filter(child -> "track".equals(child.getTag())).toList();
        assertEquals(List.of("en", "de", "fi"),
                tracks.stream().map(t -> t.getAttribute("srclang")).toList());
        tracks.forEach(track -> {
            assertEquals("subtitles", track.getAttribute("kind"));
            assertTrue(track.getAttribute("src")
                    .startsWith("VAADIN/dynamic/resource/"));
        });
        assertEquals(1,
                tracks.stream().filter(t -> t.hasAttribute("default")).count());
    }

    @Test
    void pickerSwitchesTheShowingTrack() {
        navigate(SubtitlesView.class);
        MediaTester media = new MediaTester(findInView(Video.class).single());
        media.takeJavaScript();

        @SuppressWarnings("unchecked")
        RadioButtonGroup<String> picker = find(RadioButtonGroup.class).single();
        picker.setValue("Suomi");

        assertTrue(media.takeJavaScript().stream()
                .anyMatch(js -> js.contains("textTracks")));
    }

    @Test
    void initialLanguageFollowsTheBrowserWhenThereIsATrack() {
        assertEquals("de", SubtitlesView.initialLanguage(Locale.GERMANY));
        assertEquals("fi", SubtitlesView.initialLanguage(Locale.of("fi")));
        assertEquals("en", SubtitlesView.initialLanguage(Locale.JAPAN));
        assertEquals("en", SubtitlesView.initialLanguage(null));
    }

    @ParameterizedTest
    @CsvSource({ "fi, Suomi", "off, Off", "xx, English" })
    void languageInTheUrlWinsOverTheBrowserLanguage(String parameter,
            String label) {
        navigate("uc9?subtitles=" + parameter, SubtitlesView.class);

        @SuppressWarnings("unchecked")
        RadioButtonGroup<String> picker = find(RadioButtonGroup.class).single();
        assertEquals(label, picker.getValue());
    }

    @Test
    void pickingALanguagePutsItInTheUrl() {
        navigate(SubtitlesView.class);

        @SuppressWarnings("unchecked")
        RadioButtonGroup<String> picker = find(RadioButtonGroup.class).single();
        picker.setValue("Deutsch");

        assertEquals("uc9?subtitles=de", location());
    }

    private static String location() {
        return UI.getCurrent().getInternals().getActiveViewLocation()
                .getPathWithQueryParameters();
    }
}
