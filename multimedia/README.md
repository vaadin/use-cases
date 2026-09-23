# Video & Audio — use cases

A standalone Spring Boot demo of the `Video`, `Audio` and `Source` components
from vaadin/flow#25900. Each view exercises one realistic scenario;
[`API-GAPS.md`](API-GAPS.md) lists what still needs a workaround.

| # | View | What it shows |
| - | ---- | ------------- |
| UC1 | Private recording | Per-user recordings whose video and poster are served through `DownloadHandler`s, so they have no public URL. |
| UC2 | Seekable streaming | The same recording served by a built-in handler and by a range-aware one; only the second can be scrubbed. |
| UC3 | Adaptive streaming | HLS with 240p / 360p / 720p renditions served from a single handler, with an `hls.js` fallback for browsers without native HLS. |
| UC4 | Format fallback | WebM (VP9) listed before MP4 (H.264); the browser plays the first one it supports, and the view reports which. |
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

All media is generated, so it can be committed without licensing questions.
[`generate-media.sh`](generate-media.sh) rebuilds every file with `ffmpeg`
(narration comes from its `flite` filter). Files the application serves itself
live in `src/main/resources/media`; public files served as static resources
live in `src/main/resources/META-INF/resources/media`. The subtitle files in
`src/main/resources/media/subtitles` are written by hand to match the
narration.
