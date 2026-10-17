package com.example.ai;

import com.example.AiTestConfiguration;
import com.example.uc5.VoiceOverView;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@Import(AiTestConfiguration.class)
@TestPropertySource(properties = "ELEVENLABS_API_KEY=")
@ViewPackages(classes = VoiceOverView.class)
class ProviderNoticeTest extends SpringBrowserlessTest {

    @Test
    void viewWithoutItsKeyExplainsWhatIsMissingAndDoesNotCallTheProvider() {
        navigate(VoiceOverView.class);

        ProviderNotice notice = find(ProviderNotice.class).single();
        assertEquals("This use case calls ElevenLabs, and this app was "
                + "started without an API key for it. Set ELEVENLABS_API_KEY "
                + "and restart the app to try it.",
                find(Paragraph.class).from(notice).single().getText());
        assertFalse(find(Button.class).withText("Generate voice-over").single()
                .isEnabled());
    }
}
