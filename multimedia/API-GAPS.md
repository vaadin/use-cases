# Video & Audio — API gaps

Where the components from vaadin/flow#25900 made a use case in this module
impossible or awkward:

- `Media` (abstract base of `Video` and `Audio`): `addSource(String, type)` /
  `addSource(DownloadHandler, type)`, `getSources()`, and the `controls`,
  `autoplay`, `loop`, `muted` and `preload` attributes.
- `Video#setPoster(String | DownloadHandler)`.
- `Source` with `src` (URL or `DownloadHandler`) and `type`.

The components cover the declarative part well: UC1 (private recording with a
poster), UC4 (format fallback) and most of UC5 (background video) need nothing
else. Playing media from the server, observing playback, and serving media
that is more than one file need a workaround. The workarounds live in
[`MissingAPI.java`](src/main/java/com/example/MissingAPI.java) and
[`HlsDownloadHandler.java`](src/main/java/com/example/HlsDownloadHandler.java).

The measurements below come from headless Chromium 153 against the running
app. Statements about iOS and Safari come from Apple's documentation and were
not tested.

## 1. Range requests are off by default, and nothing says so

**Where it bites:** every view that plays bundled media through a handler:
UC1, UC2, UC7, UC8 and UC9 / `MediaLibrary.java`.
**Symptom:** only `DownloadHandler.forFile` answers `Range` requests by
default. `forClassResource`, `forServletResource` and `fromInputStream` need
`enableRangeRequests()`, and `Media#addSource(DownloadHandler, type)` does not
check `DownloadHandler#isRangeRequestsEnabled()`. Without ranges a handler
answers `200` with the whole body and no `Accept-Ranges`. The browser then
reports `video.seekable` as empty, so dragging the scrubber snaps back.
Safari requires byte ranges for video and is not expected to play it at all
(not tested here). For a compressed jar entry the opt-in inflates the entry
from its start on every seek, so it does not suit long recordings.
**Workaround used:** `MediaLibrary` copies each bundled file to a temporary
directory once when the app runs from the packaged jar, and serves it with
`forFile`. `forFile` took the MIME type from the servlet container only, so
`application.properties` maps `.vtt` to `text/vtt` for the subtitles of UC9.
`AbstractDownloadHandler#withContentType(String)` now covers that part; the
mapping has not been replaced yet.
**Suggested API:** `Media#addSource(DownloadHandler, type)` logs a
development-mode warning when `isRangeRequestsEnabled()` is `false`.

## 2. `setMuted(true)` does not mute a player created from the server

**Where it bites:** UC5 / `BackgroundVideoView.java`.
**Symptom:** `Media#setMuted` only writes the `muted` attribute. That
attribute is the element's `defaultMuted`; the browser copies it into the
`muted` property only when the HTML parser creates the element. Flow creates
the element with `document.createElement` and sets the attribute afterwards,
so the property stays `false`. Measured in UC5 with only `setAutoplay(true)`
and `setMuted(true)`: `hasAttribute('muted') === true`, `muted === false`, and
the video never starts, because autoplay with sound is blocked. For the same
reason `setMuted(false)` does not unmute a playing video, and `isMuted()` does
not notice when the user mutes through the controls.
**Workaround used:** `MissingAPI.setMutedNow(media, muted)` sets the property
as well as the attribute. With it, UC5 autoplays.
**Suggested API:** `setMuted` also sets the `muted` property, and keeps the
attribute for the initial HTML. Ideally the property is synced back, so that
`isMuted()` reflects the user's choice.

## 3. No playback control from the server

**Where it bites:** UC5 (pause button), UC6 (play the picked episode), UC7
(own play/pause, ±10 s, chapter jumps), UC8 (start where the user left off).
**Symptom:** `Media` has no `play()`, `pause()`, `load()` or `currentTime`.
Each is a one-line `executeJs`, but every application has to know that
`play()` returns a promise that rejects when autoplay is blocked, and that
setting `currentTime` before `loadedmetadata` is ignored.
**Workaround used:** `MissingAPI.play`, `pause`, `load`, `seek`, `seekBy` and
`startAt`.
**Suggested API:** `Media#play()` returning a `CompletableFuture<Void>` that
fails when the browser refuses, `Media#pause()`, `Media#load()`,
`Media#setCurrentTime(double)` (applied once metadata is available), and
perhaps `Media#setPlaybackRate(double)` and `Media#setVolume(double)`.

## 4. No media events or playback state on the server

**Where it bites:** UC4 (which source the browser chose), UC6 (`ended` moves
to the next episode), UC7 (play/pause label, position, highlighted chapter),
UC8 (saving the position).
**Symptom:** `Media` tells the server nothing: not that playback started,
paused, ended or moved, and not the duration. Views add raw DOM listeners with
`addEventData("element.currentTime")` and pick their own throttling for
`timeupdate`, which fires four times a second.
**Workaround used:** `MissingAPI.addPlayingListener`, `addPauseListener`,
`addEndedListener`, `addTimeUpdateListener(media, intervalMillis, ...)`,
`addDurationListener` and `addSourceChosenListener`, which feed `ValueSignal`s
in the views.
**Suggested API:** signals, in line with Flow's other browser-state APIs:
`Media#playingSignal()`, `currentTimeSignal()` (throttled), `durationSignal()`,
`endedSignal()`, `currentSrcSignal()`. Or at least typed listeners
(`addPlayListener`, `addPauseListener`, `addEndedListener`,
`addTimeUpdateListener`). UC7 would then be pure binding code.

## 5. Changing the sources of a loaded player does nothing

**Where it bites:** UC1 / `PrivateRecordingView.java`, UC6 /
`PodcastPlaylistView.java`.
**Symptom:** the browser picks a `<source>` once. Removing it and calling
`Media#addSource(...)` on an attached player updates the DOM, but the old
media keeps playing until `load()` is called. The HTML specification says so,
but `addSource` does not mention it, and a playlist or "pick a recording" UI
hits it straight away.
**Workaround used:** UC1 builds a new `Video` for each recording; UC6 calls
`MissingAPI.load(audio)` after swapping the source.
**Suggested API:** call `load()` automatically when the sources of an attached
player change, or offer `Media#setSources(Source...)` that does. A
`Media#setSrc(String | DownloadHandler)` shortcut for the common single-source
case would help too.

## 6. No `Track` component, and no way to choose the showing track

**Where it bites:** UC9 / `SubtitlesView.java`.
**Symptom:** `Media` accepts any child, and its Javadoc says `<track>`
elements can be added, but there is no `Track` component to add. Subtitles are
basic accessibility for video, and WCAG 1.2.2 requires captions for
prerecorded video with audio. There is also no way to choose the showing track
from the server.
**Workaround used:** `MissingAPI.addTextTrack(media, kind, language, label,
DownloadHandler)` appends a raw `<track>` element, with the VTT file served
through a handler. `MissingAPI.showTextTrack(media, language)` sets
`textTrack.mode` with JavaScript. It has to wait for `loadedmetadata`:
Chromium picks a track of its own when the metadata loads and ignores a mode
set before that. Setting `mode` right away left two subtitle languages showing
at once on the first visit.
**Suggested API:** a `Track` component (`kind`, `srclang`, `label`, `default`,
`src` as URL or `DownloadHandler`), plus `Media#setShowingTrack(String
language)` or a signal for it.

## 7. No `playsinline`

**Where it bites:** UC5 / `BackgroundVideoView.java`.
**Symptom:** iOS Safari plays a video full screen, and ignores `autoplay`,
unless the video has `playsinline`. A background video therefore needs
`muted`, `autoplay`, `loop` and `playsinline`, and `Video` has no setter for
the last one. `crossorigin`, `disablepictureinpicture`,
`disableremoteplayback` and `controlslist` have no setter either.
**Workaround used:** `MissingAPI.setPlaysInline(video, true)` sets the
attribute through the element API.
**Suggested API:** `Video#setPlaysInline(boolean)`.

## 8. A handler serves exactly one URL, so HLS and DASH streams need rewriting

**Where it bites:** UC3 / `AdaptiveStreamingView.java`.
**Symptom:** an adaptive stream is a directory of files — a master playlist,
one playlist per rendition, init segments and media segments — that refer to
each other with relative URLs. A `DownloadHandler` is registered for one exact
URL. So `…/index.m3u8` resolves, but the `360p/index.m3u8` and
`segment00.m4s` the browser derives from it are unknown to the resource
registry and return 404.
**Workaround used:** `HlsDownloadHandler` rewrites every URI in the playlists
it serves (lines and `URI="…"` attributes) to `?file=<path>` on its own URL.
It serves the file named by the query parameter after checking it against a
whitelist pattern.
**Suggested API:** let a handler own the path below its URL, for example
`DownloadEvent#getSubPath()` for requests to `<handler-url>/<sub-path>`, so
relative references inside served files keep working. The same would help any
multi-file resource (HTML with images, glTF models, map tiles).

## 9. HLS playback depends on the browser

**Where it bites:** UC3 / `AdaptiveStreamingView.java`.
**Symptom:** Safari and recent Chrome play HLS natively; Firefox does not and
needs `hls.js` on top of Media Source Extensions. `Media` has no server-side
`canPlayType`, so the view asks the browser. Chromium's native HLS player also
fails with `CHUNK_DEMUXER_ERROR_APPEND_FAILED: Parsed buffers not in DTS
sequence` when it switches between renditions encoded as separate MPEG-TS
runs. Fragmented-MP4 renditions from one encode switch cleanly (see
`generate-media.sh`). `hls.js` handled both.
**Workaround used:** `hls-fallback.ts` (bundled through `@NpmPackage` and
`@JsModule`) uses native playback when `canPlayType` says so and attaches
`hls.js` otherwise. It reports which path it took.
**Suggested API:** `Media#canPlayType(String)` returning a
`CompletableFuture<String>`, which would help UC3 and UC4. The fallback itself
is probably not a Flow core concern, but a documented recipe or an `HlsVideo`
add-on would save every application from finding this out.

## 10. No browserless test support for media

**Where it bites:** every view test / `MediaTester.java`.
**Symptom:** the browserless test kit has no tester for `Video` or `Audio`. A
test cannot fire `play`, `timeupdate` or `ended` without a `ComponentTester`
subclass, because `fireDomEvent` is protected. It also cannot see whether the
server asked for `play()` or `load()`: `executeJs` calls are queued before the
response and dropped by the mock round trip.
**Workaround used:** `MediaTester` fires events through `fireDomEvent`. It
reads the queued calls by running
`StateTree#runExecutionsBeforeClientResponse()` itself and then
`UIInternals#dumpPendingJavaScriptInvocations()`, which reaches into framework
internals.
**Suggested API:** `VideoTester` / `AudioTester` in the browserless kit with
`simulatePlay()`, `simulatePause(double position)`,
`simulateTimeUpdate(double)`, `simulateEnded()`,
`simulateMetadata(double duration, String currentSrc)`, and assertions such as
`isPlayRequested()`. Once the playback API from #3 exists, the tester can
observe it directly instead of reading JavaScript strings.
