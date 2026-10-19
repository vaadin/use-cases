package com.example.uc9;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Locale;

import com.example.MediaTester;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Video;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.StreamResourceRegistry.ElementStreamResource;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.streams.DownloadEvent;
import com.vaadin.flow.server.streams.DownloadHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ViewPackages(classes = SubtitlesView.class)
class SubtitlesViewTest extends SpringBrowserlessTest {

    @Test
    void addsOneServedTrackPerLanguage() throws IOException {
        navigate(SubtitlesView.class);

        Video video = findInView(Video.class).single();
        List<Element> tracks = video.getElement().getChildren()
                .filter(child -> "track".equals(child.getTag())).toList();
        assertEquals(List.of("en", "de", "fi"),
                tracks.stream().map(t -> t.getAttribute("srclang")).toList());
        for (Element track : tracks) {
            assertEquals("subtitles", track.getAttribute("kind"));
            assertTrue(track.getAttribute("src")
                    .startsWith("VAADIN/dynamic/resource/"));
            // Browsers ignore a track that is not served as text/vtt, and the
            // servlet container has no mapping for .vtt.
            assertEquals("text/vtt", contentTypeServed(track));
        }
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

        // Applied after loadedmetadata, or the browser's own pick also shows.
        assertTrue(media.takeJavaScript().stream()
                .anyMatch(js -> js.contains("textTracks")
                        && js.contains("loadedmetadata")));
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

    private static String contentTypeServed(Element track) throws IOException {
        DownloadHandler handler = (DownloadHandler) VaadinSession.getCurrent()
                .getResourceRegistry()
                .getResource(ElementStreamResource.class,
                        URI.create(track.getAttribute("src")))
                .orElseThrow().getElementRequestHandler();
        DownloadEvent event = mock(DownloadEvent.class);
        when(event.getRequest()).thenReturn(mock(VaadinRequest.class));
        // The servlet container maps no file names to MIME types.
        when(event.getResponse())
                .thenReturn(mock(VaadinResponse.class, RETURNS_DEEP_STUBS));
        when(event.getOutputStream()).thenReturn(new ByteArrayOutputStream());

        handler.handleDownloadRequest(event);

        ArgumentCaptor<String> contentType = ArgumentCaptor
                .forClass(String.class);
        verify(event).setContentType(contentType.capture());
        return contentType.getValue();
    }

    private static String location() {
        return UI.getCurrent().getInternals().getActiveViewLocation()
                .getPathWithQueryParameters();
    }
}
