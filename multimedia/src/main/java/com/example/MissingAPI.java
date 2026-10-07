package com.example;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.html.Media;
import com.vaadin.flow.component.html.Video;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.server.streams.AbstractDownloadHandler;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.shared.Registration;

/**
 * Temporary helpers for things the {@code Media} / {@code Video} /
 * {@code Audio} API from vaadin/flow#25900 does not cover yet. Each method is a
 * thin wrapper over the element API or a small piece of JavaScript; replace the
 * call with the real API once it lands. {@code API-GAPS.md} lists the gaps one
 * by one.
 */
public final class MissingAPI {

    private MissingAPI() {
    }

    /**
     * Starts playback, like {@code HTMLMediaElement.play()}. The browser may
     * refuse, for example when autoplay with sound is blocked; the refusal is
     * not reported back.
     *
     * @param media
     *            the player
     */
    public static void play(Media media) {
        media.getElement().executeJs(
                "const p = this.play(); if (p) { p.catch(() => {}); }");
    }

    /**
     * Pauses playback, like {@code HTMLMediaElement.pause()}.
     *
     * @param media
     *            the player
     */
    public static void pause(Media media) {
        media.getElement().callJsFunction("pause");
    }

    /**
     * Makes the player pick up changed {@code <source>} children, like
     * {@code HTMLMediaElement.load()}. Without this, adding or removing sources
     * on a player that already chose one has no effect.
     *
     * @param media
     *            the player
     */
    public static void load(Media media) {
        media.getElement().callJsFunction("load");
    }

    /**
     * Moves the playhead, like setting {@code HTMLMediaElement.currentTime}.
     *
     * @param media
     *            the player
     * @param seconds
     *            the new position in seconds
     */
    public static void seek(Media media, double seconds) {
        media.getElement().executeJs("this.currentTime = $0", seconds);
    }

    /**
     * Moves the playhead by an offset from the current position, clamped to the
     * media.
     *
     * @param media
     *            the player
     * @param seconds
     *            the offset in seconds, negative to go back
     */
    public static void seekBy(Media media, double seconds) {
        media.getElement().executeJs(
                "this.currentTime = Math.max(0, Math.min(this.duration || 0,"
                        + " this.currentTime + $0))",
                seconds);
    }

    /**
     * Starts playback at the given position once the metadata has loaded.
     * Setting {@code currentTime} before that is ignored by the browser.
     *
     * @param media
     *            the player
     * @param seconds
     *            the start position in seconds
     */
    public static void startAt(Media media, double seconds) {
        media.getElement().executeJs(
                """
                        const seek = () => { this.currentTime = $0; };
                        if (this.readyState >= 1) { seek(); }
                        else { this.addEventListener('loadedmetadata', seek, { once: true }); }
                        """,
                seconds);
    }

    /**
     * Mutes or unmutes the player right now. {@link Media#setMuted(boolean)}
     * only sets the {@code muted} attribute, which the browser reads when the
     * element is created; changing it later does not mute or unmute. This sets
     * the {@code muted} property too.
     *
     * @param media
     *            the player
     * @param muted
     *            {@code true} to mute
     */
    public static void setMutedNow(Media media, boolean muted) {
        media.setMuted(muted);
        media.getElement().setProperty("muted", muted);
    }

    /**
     * Sets the {@code playsinline} attribute, without which iOS Safari opens a
     * video full screen and ignores {@code autoplay}.
     *
     * @param video
     *            the player
     * @param playsInline
     *            {@code true} to play inside the page
     */
    public static void setPlaysInline(Video video, boolean playsInline) {
        video.getElement().setAttribute("playsinline", playsInline);
    }

    /**
     * Listens to the playback position. The browser fires {@code timeupdate}
     * about four times a second; the listener is throttled to one call per
     * {@code intervalMillis}.
     *
     * @param media
     *            the player
     * @param intervalMillis
     *            the minimum time between two calls
     * @param listener
     *            receives the position in seconds
     * @return a handle to remove the listener
     */
    public static Registration addTimeUpdateListener(Media media,
            int intervalMillis, SerializableConsumer<Double> listener) {
        return media.getElement()
                .addEventListener("timeupdate",
                        event -> listener.accept(event.getEventData()
                                .get("element.currentTime").asDouble()))
                .addEventData("element.currentTime").throttle(intervalMillis);
    }

    /**
     * Listens to the media duration becoming known.
     *
     * @param media
     *            the player
     * @param listener
     *            receives the duration in seconds
     * @return a handle to remove the listener
     */
    public static Registration addDurationListener(Media media,
            SerializableConsumer<Double> listener) {
        return media.getElement()
                .addEventListener("loadedmetadata",
                        event -> listener.accept(event.getEventData()
                                .get("element.duration").asDouble()))
                .addEventData("element.duration");
    }

    /**
     * Listens to playback starting and stopping.
     *
     * @param media
     *            the player
     * @param listener
     *            receives {@code true} when playback starts and {@code false}
     *            when it pauses or ends
     * @return a handle to remove the listener
     */
    public static Registration addPlayingListener(Media media,
            SerializableConsumer<Boolean> listener) {
        Element element = media.getElement();
        return Registration.combine(
                element.addEventListener("play", e -> listener.accept(true)),
                element.addEventListener("pause", e -> listener.accept(false)),
                element.addEventListener("ended", e -> listener.accept(false)));
    }

    /**
     * Listens to playback pausing, with the position it paused at.
     *
     * @param media
     *            the player
     * @param listener
     *            receives the position in seconds
     * @return a handle to remove the listener
     */
    public static Registration addPauseListener(Media media,
            SerializableConsumer<Double> listener) {
        return media.getElement()
                .addEventListener("pause",
                        event -> listener.accept(event.getEventData()
                                .get("element.currentTime").asDouble()))
                .addEventData("element.currentTime");
    }

    /**
     * Listens to playback reaching the end.
     *
     * @param media
     *            the player
     * @param listener
     *            called when the media has played to the end
     * @return a handle to remove the listener
     */
    public static Registration addEndedListener(Media media,
            SerializableRunnable listener) {
        return media.getElement().addEventListener("ended",
                e -> listener.run());
    }

    /**
     * Listens to which {@code <source>} the browser picked.
     *
     * @param media
     *            the player
     * @param listener
     *            receives the absolute URL of the chosen source
     * @return a handle to remove the listener
     */
    public static Registration addSourceChosenListener(Media media,
            SerializableConsumer<String> listener) {
        return media.getElement()
                .addEventListener("loadedmetadata",
                        event -> listener.accept(event.getEventData()
                                .get("element.currentSrc").asString()))
                .addEventData("element.currentSrc");
    }

    /**
     * Adds a {@code <track>} with subtitles, captions or chapters.
     * {@code Media} only accepts {@code Source} children, so the element is
     * appended directly.
     *
     * @param media
     *            the player
     * @param kind
     *            {@code subtitles}, {@code captions}, {@code chapters}, ...
     * @param language
     *            the BCP 47 language of the track, for example {@code en}
     * @param label
     *            the name the browser shows in its track menu
     * @param src
     *            serves the WebVTT file
     * @return the track element
     */
    public static Element addTextTrack(Media media, String kind,
            String language, String label, DownloadHandler src) {
        if (src instanceof AbstractDownloadHandler<?> handler) {
            handler.inline();
        }
        Element track = new Element("track");
        track.setAttribute("kind", kind);
        track.setAttribute("srclang", language);
        track.setAttribute("label", label);
        track.setAttribute("src", src.allowDisabled());
        media.getElement().appendChild(track);
        return track;
    }

    /**
     * Shows the text track in the given language and hides the others, like
     * picking a language from the player's subtitle menu.
     *
     * @param media
     *            the player
     * @param language
     *            the language to show, or {@code null} to hide all tracks
     */
    public static void showTextTrack(Media media, @Nullable String language) {
        media.getElement().executeJs("""
                for (const track of this.textTracks) {
                  track.mode = track.language === $0 ? 'showing' : 'disabled';
                }
                """, language);
    }
}
