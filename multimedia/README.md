# Video & Audio — use cases

A standalone Spring Boot demo of the `Video`, `Audio` and `Source` components
from vaadin/flow#25900. Each view exercises one realistic scenario;
[`API-GAPS.md`](API-GAPS.md) lists what still needs a workaround.

| # | View | What it shows |
| - | ---- | ------------- |
| UC1 | Private recording | Per-user recordings whose video and poster are served through `DownloadHandler`s, so they have no public URL. |
| UC2 | Seekable streaming | The same recording served by a built-in handler and by a range-aware one; only the second can be scrubbed. |
| UC3 | Adaptive streaming | HLS with 240p / 360p / 720p renditions served from a single handler, with an `hls.js` fallback for browsers without native HLS. |
| UC4 | Format fallback | AV1 listed before H.264; the browser plays the first one it supports (Safari without a hardware AV1 decoder falls back), and the view reports which. |
| UC5 | Background video | A muted, looping hero clip that autoplays inline, with a pause button for accessibility. |
| UC6 | Podcast playlist | One `Audio` player for a list of episodes, moving on to the next episode when one ends. |
| UC7 | Chapters & controls | Vaadin buttons for play/pause, ±10 s and chapter jumps, with the current chapter following playback. |
| UC8 | Resume playback | The playback position is remembered and restored after navigating away or reloading. |
| UC9 | Subtitles | WebVTT subtitles in English, German and Finnish, starting in the browser's language, with a language picker. |

## Run

```
cd multimedia
mvn spring-boot:run
```

Open <http://localhost:8080/>.

The module pins `flow.version` to the snapshot built from vaadin/flow#25900;
switch back to the mainline snapshot once that pull request is merged.

## Sample media

All media is generated. [`generate-media.sh`](generate-media.sh) rebuilds
every file with `ffmpeg`; the narration is spoken by
[ElevenLabs](https://elevenlabs.io) text to speech through
[`tts.mjs`](tts.mjs), which needs `ELEVENLABS_API_KEY` and caches its
responses.

The quarterly review used by UC2, UC3 and UC7–UC9 is an avatar video made
with [HeyGen](https://www.heygen.com) in four scenes, one per chapter. Pass
the HeyGen export as `REVIEW_SOURCE` to re-encode it; otherwise the script
rebuilds the HLS ladder and subtitles from the committed recording. The
subtitle cues are timed by hand to the pauses in the narration, and the
German and Finnish translations live in the script; the chapter starts in
`Chapters.java` follow the scene cuts. Update both when the recording
changes.

The two UC1 sprint review recordings are HeyGen avatar videos too, each with
its own presenter and setting. Pass the exports as `SPRINT_41_SOURCE` and
`SPRINT_42_SOURCE` to re-encode them and take new posters; otherwise the
committed files are kept.

The UC4 product trailer is a HeyGen avatar video as well. Pass the export as
`TRAILER_SOURCE` to re-encode it into the AV1 and H.264 files; otherwise the
committed files are kept. The `codecs` attributes in `FormatFallbackView`
match the encoder settings, so update them together.

Files the application serves itself live in `src/main/resources/media`;
public files served as static resources live in
`src/main/resources/META-INF/resources/media`.
