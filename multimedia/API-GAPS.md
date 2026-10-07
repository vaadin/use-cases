# Video & Audio — gaps observed while building the use cases

Friction points hit while building the nine use cases on the components from
vaadin/flow#25900:

- `Media` (abstract base of `Video` and `Audio`): `addSource(String, type)` /
  `addSource(DownloadHandler, type)`, `getSources()`, and the `controls`,
  `autoplay`, `loop`, `muted` and `preload` attributes. It accepts only
  `Source` children.
- `Video#setPoster(String | DownloadHandler)`.
- `Source` with `src` (URL or `DownloadHandler`) and `type`.

The components cover the declarative part well: UC1 (private recording with a
poster), UC4 (format fallback) and most of UC5 (background video) need nothing
else. Everything that involves *playing* media from the server, *observing*
playback, or serving media that is not a single small file needs a workaround.
Those workarounds live in [`MissingAPI.java`](src/main/java/com/example/MissingAPI.java),
[`RangeDownloadHandler.java`](src/main/java/com/example/RangeDownloadHandler.java)
and [`HlsDownloadHandler.java`](src/main/java/com/example/HlsDownloadHandler.java).

The measurements quoted below come from headless Chromium 153 against the
running app. Statements about iOS and Safari come from Apple's documentation
and were not tested.

## Built-in download handlers ignore `Range`, so served media cannot be seeked

**Where it bit us:** UC2 / `SeekableStreamView.java` (side by side), and UC7,
UC8, UC9, which all serve the 60-second recording.
**Symptom:** `DownloadHandler.fromInputStream`, `forFile`, `forClassResource`
and `forServletResource` always answer `200` with the whole body and no
`Accept-Ranges` header. The browser then reports `video.seekable` as empty:
dragging the scrubber to 0:45 in the left-hand UC2 player snaps back to
~0:01.5, while the same file served with range support seeks to 0:46.5.
`preload="metadata"` also cannot save bandwidth, because there is no way to
fetch only the header. Apple's Safari documentation requires byte-range
support for video, so there such a response is not expected to play at all
(not tested here). Static files are fine: `media/trailer.mp4` answers a
`Range` request with `206`, and Flow's own `ResponseWriter` implements `Range`
for the static resources it serves. Only the handler path lacks it.
**Workaround used:** `RangeDownloadHandler`, a ~100-line handler that parses a
single `bytes=` range and answers `206` with `Content-Range`, or `416`.
**Suggested API:** honour `Range` in the built-in handlers whenever the length
is known (`forFile`, `forClassResource`, and `fromInputStream` when the
`DownloadResponse` has a content length), reusing `ResponseWriter`'s range
logic. For custom sources, a seekable variant of `DownloadResponse` (for
example one taking a `SeekableByteChannel` or a `(offset, length) ->
InputStream` function) would let applications stream from object storage
without implementing HTTP ranges themselves.

## `setMuted(true)` does not mute a player created from the server, so autoplay is blocked

**Where it bit us:** UC5 / `BackgroundVideoView.java`.
**Symptom:** `Media#setMuted` only writes the `muted` *attribute*. That
attribute is the element's `defaultMuted`; the browser copies it into the
`muted` *property* only when the parser creates the element. Flow creates the
element with `document.createElement` and sets the attribute afterwards, so
the property stays `false`. Measured in UC5 with only `setAutoplay(true)` +
`setMuted(true)`: `hasAttribute('muted') === true`, `muted === false`, and the
video never starts, because autoplay with sound is blocked. For the same
reason, calling `setMuted(false)` later does not unmute a playing video, and
`isMuted()` does not notice when the user mutes via the controls.
**Workaround used:** `MissingAPI.setMutedNow(media, muted)` sets the property
as well as the attribute. With it, UC5 autoplays.
**Suggested API:** make `setMuted` set the `muted` property too (and keep the
attribute for the initial HTML). Consider syncing the property back so that
`isMuted()` reflects the user's choice, the same way `volume` would need to.

## No playback control from the server

**Where it bit us:** UC5 (pause button), UC6 (play the picked episode), UC7
(own play/pause, ±10 s, chapter jumps), UC8 (start where the user left off).
**Symptom:** there is no `play()`, `pause()`, `load()` or `currentTime` on
`Media`. Every one of these is a one-line `executeJs`, but each application
has to know that `play()` returns a promise that rejects when autoplay is
blocked, and that setting `currentTime` before `loadedmetadata` is ignored.
**Workaround used:** `MissingAPI.play/pause/load/seek/seekBy/startAt`.
**Suggested API:** `Media#play()` returning a `CompletableFuture<Void>` that
fails when the browser refuses, `Media#pause()`, `Media#load()`,
`Media#setCurrentTime(double)` (applied once metadata is available), and
perhaps `Media#setPlaybackRate(double)` and `Media#setVolume(double)`.

## No media events or playback state on the server

**Where it bit us:** UC4 (which source the browser chose), UC6 (`ended` moves
to the next episode), UC7 (play/pause label, position, highlighted chapter),
UC8 (saving the position).
**Symptom:** nothing tells the server that playback started, paused, ended or
moved, or what the duration is. Views wire raw DOM listeners with
`addEventData("element.currentTime")` and pick their own throttling for
`timeupdate`, which fires four times a second.
**Workaround used:** `MissingAPI.addPlayingListener`, `addPauseListener`,
`addEndedListener`, `addTimeUpdateListener(media, intervalMillis, ...)`,
`addDurationListener` and `addSourceChosenListener`, feeding `ValueSignal`s in
the views.
**Suggested API:** signals, in line with the rest of Flow's browser-state APIs:
`Media#playingSignal()`, `currentTimeSignal()` (throttled), `durationSignal()`,
`endedSignal()`, `currentSrcSignal()`. Or at least typed listeners
(`addPlayListener`, `addPauseListener`, `addEndedListener`,
`addTimeUpdateListener`). UC7 then becomes pure binding code.

## Changing the sources of a player that is already loaded does nothing

**Where it bit us:** UC1 / `PrivateRecordingView.java`, UC6 /
`PodcastPlaylistView.java`.
**Symptom:** the browser picks a `<source>` once. Removing it and calling
`addSource(...)` on an attached player updates the DOM, but the old media
keeps playing until `load()` is called. That behaviour is in the HTML
specification, but `addSource` does not mention it, and a playlist or
"pick a recording" UI hits it straight away.
**Workaround used:** UC1 builds a new `Video` for each recording; UC6 calls
`MissingAPI.load(audio)` after swapping the source.
**Suggested API:** call `load()` automatically when the sources of an attached
player change, or offer `Media#setSources(Source...)` that does. A
`Media#setSrc(String | DownloadHandler)` shortcut for the common single-source
case would help too.

## No `<track>`: subtitles, captions and chapters cannot be added

**Where it bit us:** UC9 / `SubtitlesView.java`.
**Symptom:** `Media` implements `HasComponentsOfType<Source>`, so it accepts
only `Source` children and there is no `Track` component. Subtitles are
basic accessibility for video, and captions are required by WCAG 1.2.2 for
prerecorded video with audio. There is also no way to choose the showing track
from the server.
**Workaround used:** `MissingAPI.addTextTrack(media, kind, language, label,
DownloadHandler)` appends a raw `<track>` element (with the VTT file served
through a handler), and `MissingAPI.showTextTrack(media, language)` sets
`textTrack.mode` with JavaScript.
**Suggested API:** a `Track` component (`kind`, `srclang`, `label`, `default`,
`src` as URL or `DownloadHandler`), accepted by `Media` next to `Source`,
plus `Media#setShowingTrack(String language)` or a signal for it.

## No `playsinline`

**Where it bit us:** UC5 / `BackgroundVideoView.java`.
**Symptom:** iOS Safari plays a video full screen, and ignores `autoplay`,
unless the video has `playsinline`. A background or hero video therefore
needs `muted` + `autoplay` + `loop` + `playsinline`, and the last one has no
setter.
**Workaround used:** `MissingAPI.setPlaysInline(video, true)`, which sets the
attribute through the element API.
**Suggested API:** `Video#setPlaysInline(boolean)`. Related attributes that
also have no setter: `crossorigin`, `disablepictureinpicture`,
`disableremoteplayback`, `controlslist`.

## A handler serves exactly one URL, so HLS and DASH streams need rewriting

**Where it bit us:** UC3 / `AdaptiveStreamingView.java`.
**Symptom:** an adaptive stream is a directory of files — a master playlist,
one playlist per rendition, init segments and media segments — that refer to
each other with relative URLs. A `DownloadHandler` is registered for one exact
URL, so `…/index.m3u8` resolves, but the `360p/index.m3u8` and
`segment00.m4s` the browser derives from it are unknown to the resource
registry and return 404.
**Workaround used:** `HlsDownloadHandler` rewrites every URI in the
playlists it serves (lines and `URI="…"` attributes) to `?file=<path>` on its
own URL, and serves the file named by the query parameter after checking it
against a whitelist pattern.
**Suggested API:** let a handler own the path below its URL, for example a
`DownloadEvent#getSubPath()` for requests to `<handler-url>/<sub-path>`, so
relative references inside served files keep working. The same would help
any multi-file resource (HTML with images, glTF models, map tiles).

## HLS playback depends on the browser

**Where it bit us:** UC3 / `AdaptiveStreamingView.java`.
**Symptom:** Safari and recent Chrome play HLS natively; Firefox does not and
needs `hls.js` on top of Media Source Extensions. The server cannot tell which
case applies (there is no server-side `canPlayType`), so the view asks the
browser. We also found that Chromium's native HLS player fails with
`CHUNK_DEMUXER_ERROR_APPEND_FAILED: Parsed buffers not in DTS sequence` when it
switches between renditions that were encoded as separate MPEG-TS runs;
fragmented-MP4 renditions produced by one encode switch cleanly (see
`generate-media.sh`). `hls.js` handled both.
**Workaround used:** `hls-fallback.ts` (bundled through `@NpmPackage` +
`@JsModule`) uses native playback when `canPlayType` says so and attaches
`hls.js` otherwise; it reports which path it took.
**Suggested API:** probably not a Flow core concern — but a documented recipe,
or an add-on `HlsVideo` that does the fallback, would save every application
from finding this out. A server-side `Media#canPlayType(String)` returning a
`CompletableFuture<String>` would help both UC3 and UC4.

## No test support for media

**Where it bit us:** all view tests, via
[`MediaTester.java`](src/test/java/com/example/MediaTester.java).
**Symptom:** the browserless test kit has no tester or simulator for `Video`
or `Audio`. A test cannot fire `play`, `timeupdate` or `ended` without a
`ComponentTester` subclass (`fireDomEvent` is protected), and it cannot see
whether the server asked for `play()` or `load()`: `executeJs` calls are
queued before the response and dropped by the mock round trip.
`MediaTester` fires events through `fireDomEvent` and reads the queued calls by
running `StateTree#runExecutionsBeforeClientResponse()` itself and then
`UIInternals#dumpPendingJavaScriptInvocations()`, which reaches into
framework internals.
**Suggested API:** `VideoTester` / `AudioTester` in the browserless kit with
`simulatePlay()`, `simulatePause(double position)`,
`simulateTimeUpdate(double)`, `simulateEnded()`, `simulateMetadata(double
duration, String currentSrc)`, and assertions such as `isPlayRequested()`.
Once the playback API from the sections above exists, the tester can
observe it directly instead of looking at JavaScript strings.
