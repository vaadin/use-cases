package com.example.uc5;

import java.io.ByteArrayInputStream;

import com.example.ai.AiProvider;
import com.example.ai.AiProviders;
import com.example.ai.ProviderNotice;
import com.example.common.UseCaseDescription;
import com.example.uc5.VoiceOverService.Voice;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Audio;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC5 — Voice-over from text.
 * <p>
 * A team turns written text — a product update, a safety instruction, a
 * training module — into narrated audio without booking a speaker. The
 * {@link VoiceOverService} sends the text and the chosen voice to ElevenLabs
 * through Spring AI's {@code TextToSpeechModel} and gets an MP3 back, which the
 * view plays in an {@link Audio} player and offers as a download. The call runs
 * on a background thread and writes the result to a signal, so the page stays
 * responsive while the speech is generated.
 */
@Route(value = "uc5", layout = MainLayout.class)
@UseCaseDescription("Turning written text into narrated audio for announcements and training")
@Menu(order = 5, title = "UC5 — Voice-over from text")
@StyleSheet("uc5.css")
public class VoiceOverView extends VerticalLayout {

    static final String SAMPLE_TEXT = "Hello team. Starting Monday, expense "
            + "reports are filed in the new travel portal. Snap a photo of "
            + "the receipt, pick the trip, and you are done. Reports filed by "
            + "the twentieth are paid with the same month's salary.";

    /**
     * A generated clip: the speech as MP3 and the voice that read it.
     */
    record Clip(byte[] mp3, Voice voice) {
    }

    private final ValueSignal<@Nullable Clip> clip = new ValueSignal<>(null);
    private final ValueSignal<String> status = new ValueSignal<>("");
    private final ValueSignal<Boolean> generating = new ValueSignal<>(false);

    public VoiceOverView(VoiceOverService voiceOver, AiProviders providers,
            @Qualifier("applicationTaskExecutor") TaskExecutor executor) {
        addClassName("uc5-view");
        add(new H1("UC5 — Voice-over from text"));
        add(new Paragraph("Write or paste a short text, pick a voice and "
                + "generate the narration. The text is read by ElevenLabs' "
                + "speech model; the MP3 plays below and can be downloaded, "
                + "for example to add to a training video or an intranet "
                + "post."));

        ProviderNotice notice = ProviderNotice.forMissing(providers,
                AiProvider.ELEVENLABS);
        if (notice != null) {
            add(notice);
        }

        TextArea text = new TextArea("Text to read");
        text.setValue(SAMPLE_TEXT);
        text.setMaxLength(VoiceOverService.MAX_CHARACTERS);
        text.setHelperText(
                "Up to " + VoiceOverService.MAX_CHARACTERS + " characters");
        text.setWidthFull();

        Select<Voice> voice = new Select<>();
        voice.setLabel("Voice");
        voice.setItems(VoiceOverService.VOICES);
        voice.setItemLabelGenerator(Voice::label);
        voice.setValue(VoiceOverService.VOICES.getFirst());
        voice.setWidth("20em");

        Paragraph quota = new Paragraph();
        quota.addClassName("uc5-quota");

        Button generate = new Button("Generate voice-over");
        generate.addThemeVariants(ButtonVariant.PRIMARY);
        generate.addClickListener(click -> {
            String script = text.getValue();
            Voice chosen = voice.getValue();
            generating.set(true);
            status.set("Generating the voice-over…");
            executor.execute(() -> {
                try {
                    clip.set(new Clip(voiceOver.speak(script, chosen), chosen));
                    status.set("");
                } catch (RuntimeException e) {
                    status.set("The voice-over could not be generated: "
                            + e.getMessage());
                } finally {
                    generating.set(false);
                }
            });
        });

        Paragraph statusText = new Paragraph();
        statusText.addClassName("uc5-status");
        statusText.bindText(status);

        Div player = new Div();
        player.addClassName("uc5-player");

        Signal.effect(this, () -> {
            generate.setEnabled(notice == null && !generating.get());
            // Read the clip so the quota text also updates after a call.
            clip.get();
            quota.setText(voiceOver.quota().remaining() + " of "
                    + voiceOver.quota().perDay()
                    + " voice-overs left today, for all users of this demo.");
        });
        Signal.effect(this, () -> {
            player.removeAll();
            Clip current = clip.get();
            if (current != null) {
                player.add(new H2("Your voice-over"),
                        new Paragraph("Read by " + current.voice().label()),
                        player(current), download(current));
            }
        });

        add(text, voice, generate, statusText, quota, player);
    }

    private static Audio player(Clip clip) {
        Audio audio = new Audio();
        audio.setControlsVisible(true);
        audio.addSource(mp3Handler(clip), "audio/mpeg");
        return audio;
    }

    private static Anchor download(Clip clip) {
        Anchor anchor = new Anchor(mp3Handler(clip), "Download MP3");
        anchor.setDownload(true);
        return anchor;
    }

    private static DownloadHandler mp3Handler(Clip clip) {
        return DownloadHandler.fromInputStream(event -> new DownloadResponse(
                new ByteArrayInputStream(clip.mp3()), "voice-over.mp3",
                "audio/mpeg", clip.mp3().length)).enableRangeRequests();
    }
}
