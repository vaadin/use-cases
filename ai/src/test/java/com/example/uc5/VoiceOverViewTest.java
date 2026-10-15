package com.example.uc5;

import java.util.concurrent.TimeUnit;

import com.example.AiTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Audio;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.select.Select;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(AiTestConfiguration.class)
@ViewPackages(classes = VoiceOverView.class)
class VoiceOverViewTest extends SpringBrowserlessTest {

    @Autowired
    private VoiceOverService voiceOver;

    @Test
    void generatedSpeechIsPlayedAndOfferedForDownloadWithTheChosenVoice() {
        navigate(VoiceOverView.class);
        VoiceOverService.Voice sarah = VoiceOverService.VOICES.get(1);
        @SuppressWarnings("unchecked")
        Select<VoiceOverService.Voice> voice = findInView(Select.class)
                .single();
        test(voice).selectItem(sarah.label());
        int remainingBefore = voiceOver.quota().remaining();

        test(find(Button.class).withText("Generate voice-over").single())
                .click();
        // The speech is generated on a background thread.
        long deadline = System.currentTimeMillis() + 5000;
        while (find(Audio.class).all().isEmpty()
                && System.currentTimeMillis() < deadline) {
            runPendingSignalsTasks(100, TimeUnit.MILLISECONDS);
        }

        assertEquals(1, find(Audio.class).all().size());
        assertTrue(find(Paragraph.class).all().stream()
                .anyMatch(p -> p.getText().equals("Read by " + sarah.label())));
        assertEquals(1,
                find(Anchor.class).withText("Download MP3").all().size());
        assertEquals(remainingBefore - 1, voiceOver.quota().remaining());
    }
}
