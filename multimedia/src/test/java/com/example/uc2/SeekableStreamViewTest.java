package com.example.uc2;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringBootTest
@ViewPackages(classes = SeekableStreamView.class)
class SeekableStreamViewTest extends SpringBrowserlessTest {

    @Test
    void showsBuiltInAndRangeAwarePlayersSideBySide() {
        navigate(SeekableStreamView.class);

        assertEquals("UC2 — Seekable streaming",
                findInView(H1.class).single().getText());
        assertEquals(2, findInView(H2.class).all().size());

        var videos = findInView(Video.class).all();
        assertEquals(2, videos.size());
        String builtIn = videos.get(0).getSources().get(0).getSrc();
        String ranged = videos.get(1).getSources().get(0).getSrc();
        assertNotEquals(builtIn, ranged);
        videos.forEach(video -> assertEquals("video/mp4",
                video.getSources().get(0).getType()));
    }
}
