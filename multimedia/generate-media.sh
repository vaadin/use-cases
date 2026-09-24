#!/usr/bin/env bash
#
# Regenerates every sample media file used by the multimedia use cases.
# Needs ffmpeg built with libx264, libvpx-vp9, libopus, libmp3lame and
# drawtext, plus jq and Node.js. Narration is spoken by the ElevenLabs API
# through tts.mjs, so ELEVENLABS_API_KEY must be set (responses are cached,
# see tts.mjs). The generated files are committed, so running this is only
# needed when the samples change.
#
# Private media (served through DownloadHandlers) goes to
# src/main/resources/media; public media (served as static files) goes to
# src/main/resources/META-INF/resources/media.
set -euo pipefail

cd "$(dirname "$0")"
PRIVATE=src/main/resources/media
PUBLIC=src/main/resources/META-INF/resources/media
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT
mkdir -p "$PRIVATE/hls" "$PRIVATE/subtitles" "$PUBLIC/podcast"

FONT=${FONT:-/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf}
FONT_BOLD=${FONT_BOLD:-/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf}
FF="ffmpeg -hide_banner -loglevel error -y"
# Two-second GOP so seeking and HLS segment boundaries land on keyframes.
X264="-c:v libx264 -preset slow -pix_fmt yuv420p -g 50 -keyint_min 50 -sc_threshold 0"

# ElevenLabs premade voices.
NARRATOR=Xb7hH8MSUJpSbSDYk0k2   # Alice: clear, engaging educator
PRESENTER=iP95p4xoKVk53GoZ742B  # Chris: charming, down-to-earth
HOST=JBFqnCBsd6RMkjVDRZzb       # George: warm, captivating storyteller

# say <voice> <seconds> <out.wav> <segment>...: speech padded to a fixed
# length. Prints the [start, end] times of the segments as JSON.
say() {
    local voice=$1 seconds=$2 out=$3
    shift 3
    local times spoken
    times=$(node tts.mjs "$voice" "$out.mp3" "$@")
    spoken=$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$out.mp3")
    if awk -v s="$spoken" -v max="$seconds" 'BEGIN { exit !(s > max) }'; then
        echo "Narration is ${spoken}s, longer than its ${seconds}s slot: $*" >&2
        exit 1
    fi
    $FF -i "$out.mp3" -af "apad=whole_dur=$seconds,aresample=48000" -ac 1 \
        -t "$seconds" "$out"
    echo "$times"
}

# vtt_time <seconds>: formats a cue time as mm:ss.mmm.
vtt_time() {
    awk -v t="$1" 'BEGIN { m = int(t / 60); printf "%02d:%06.3f", m, t - m * 60 }'
}

# slide <color> <title> <subtitle> <seconds> <out.mp4> [size]: a titled
# colour card with a running timecode, silent.
slide() {
    local size=${6:-640x360}
    $FF -f lavfi -i "color=c=$1:s=$size:r=25:d=$4" -vf "\
drawtext=fontfile=$FONT_BOLD:text='$2':fontcolor=white:fontsize=h/9:x=(w-tw)/2:y=h*0.36,\
drawtext=fontfile=$FONT:text='$3':fontcolor=white@0.85:fontsize=h/20:x=(w-tw)/2:y=h*0.52,\
drawtext=fontfile=$FONT:text='%{pts\:hms}':fontcolor=white@0.7:fontsize=h/22:x=w-tw-h/20:y=h-th-h/20" \
        $X264 -t "$4" "$5"
}

# --- The quarterly review: four 15 s chapters (UC2, UC3, UC7, UC8, UC9) ---
CHAPTERS=("Welcome|#1d4ed8" "Roadmap|#047857" "Demo|#b45309" "Questions|#7c3aed")
# Two subtitle cues per chapter; the English cues are also the narration.
SUBTITLES_EN=(
    "Welcome to the quarterly product review."
    $'Today we look at what shipped, what is next,\nand your questions.'
    "The next release focuses on faster startup,"
    $'better offline support,\nand a new reporting module.'
    $'In the demo we open a customer record,\nchange the address,'
    "and every user sees the update at once."
    "Finally, questions and answers."
    $'Thank you for joining,\nand see you at the next review.'
)
SUBTITLES_DE=(
    "Willkommen zum Quartals-Produktreview."
    $'Heute sehen wir, was ausgeliefert wurde, was als Nächstes kommt,\nund beantworten Ihre Fragen.'
    "Das nächste Release bringt einen schnelleren Start,"
    $'besseren Offline-Support\nund ein neues Berichtsmodul.'
    $'In der Demo öffnen wir einen Kundendatensatz,\nändern die Adresse,'
    "und alle Nutzer sehen die Änderung sofort."
    "Zum Schluss Fragen und Antworten."
    $'Danke fürs Dabeisein,\nbis zum nächsten Review.'
)
SUBTITLES_FI=(
    "Tervetuloa neljännesvuoden tuotekatsaukseen."
    $'Katsomme, mitä julkaistiin, mitä on tulossa,\nja vastaamme kysymyksiinne.'
    "Seuraava julkaisu tuo nopeamman käynnistyksen,"
    $'paremman offline-tuen\nja uuden raportointimoduulin.'
    $'Demossa avaamme asiakastietueen,\nmuutamme osoitteen,'
    "ja jokainen käyttäjä näkee muutoksen heti."
    "Lopuksi kysymykset ja vastaukset."
    $'Kiitos osallistumisesta,\nnähdään seuraavassa katsauksessa.'
)
: > "$WORK/review-parts.txt"
CUE_TIMES=()
i=0
for chapter in "${CHAPTERS[@]}"; do
    IFS='|' read -r title color <<< "$chapter"
    slide "$color" "$title" "Quarterly product review" 15 "$WORK/v$i.mp4"
    first=${SUBTITLES_EN[2 * i]}
    second=${SUBTITLES_EN[2 * i + 1]}
    times=$(say "$NARRATOR" 15 "$WORK/a$i.wav" "${first//$'\n'/ }" "${second//$'\n'/ }")
    while read -r start end; do
        CUE_TIMES+=("$(vtt_time "$(awk -v t="$start" -v o=$((15 * i)) 'BEGIN { print t + o }')") --> $(vtt_time "$(awk -v t="$end" -v o=$((15 * i)) 'BEGIN { print t + o }')")")
    done < <(jq -r '.[] | "\(.[0]) \(.[1])"' <<< "$times")
    $FF -i "$WORK/v$i.mp4" -i "$WORK/a$i.wav" -c:v copy -c:a aac -b:a 64k \
        "$WORK/part$i.mp4"
    echo "file '$WORK/part$i.mp4'" >> "$WORK/review-parts.txt"
    i=$((i + 1))
done
$FF -f concat -safe 0 -i "$WORK/review-parts.txt" -c copy -movflags +faststart \
    "$PRIVATE/quarterly-review.mp4"

# Subtitles timed to the narration (UC9).
for language in en de fi; do
    declare -n cues="SUBTITLES_${language^^}"
    {
        echo "WEBVTT"
        for c in "${!CUE_TIMES[@]}"; do
            printf '\n%s\n%s\n' "${CUE_TIMES[c]}" "${cues[c]}"
        done
    } > "$PRIVATE/subtitles/$language.vtt"
    unset -n cues
done

# HLS ladder of the same review; the rendition is burnt into the picture so
# a quality switch is visible. All renditions come from one encode so their
# timestamps line up and the player can switch between them mid-stream.
label() {
    echo "scale=$2,drawtext=fontfile=$FONT_BOLD:text='$1':fontcolor=black:box=1:boxcolor=white@0.8:boxborderw=6:fontsize=h/18:x=h/20:y=h/20"
}
rm -rf "$PRIVATE/hls" && mkdir -p "$PRIVATE/hls"
$FF -i "$PRIVATE/quarterly-review.mp4" -filter_complex "\
[0:v]split=3[s0][s1][s2];\
[s0]$(label 240p 426x240)[v0];\
[s1]$(label 360p 640x360)[v1];\
[s2]$(label 720p 1280x720)[v2]" \
    -map "[v0]" -map 0:a -map "[v1]" -map 0:a -map "[v2]" -map 0:a \
    $X264 -crf 30 -c:a aac -b:a 64k \
    -maxrate:v:0 300k -bufsize:v:0 300k \
    -maxrate:v:1 700k -bufsize:v:1 700k \
    -maxrate:v:2 1500k -bufsize:v:2 1500k \
    -f hls -hls_time 6 -hls_playlist_type vod -master_pl_name index.m3u8 \
    -hls_segment_type fmp4 -hls_fmp4_init_filename init.mp4 \
    -hls_segment_filename "$PRIVATE/hls/%v/segment%02d.m4s" \
    -var_stream_map "v:0,a:0,name:240p v:1,a:1,name:360p v:2,a:2,name:720p" \
    "$PRIVATE/hls/%v/index.m3u8"

# --- Per-user recordings with posters (UC1) ---
for recording in "sprint-41|#0f766e|Sprint 41 review|Sprint forty-one review. Search is now twice as fast, and the export button is back." \
                 "sprint-42|#be123c|Sprint 42 review|Sprint forty-two review. Dark mode has shipped, and the mobile layout was reworked."; do
    IFS='|' read -r id color title text <<< "$recording"
    slide "$color" "$title" "Team recording" 10 "$WORK/$id.mp4"
    say "$PRESENTER" 10 "$WORK/$id.wav" "$text" > /dev/null
    $FF -i "$WORK/$id.mp4" -i "$WORK/$id.wav" -c:v copy -c:a aac -b:a 64k \
        -movflags +faststart "$PRIVATE/$id.mp4"
    $FF -ss 1 -i "$PRIVATE/$id.mp4" -frames:v 1 -q:v 4 "$PRIVATE/$id-poster.jpg"
done

# --- Same clip in two formats, the format burnt in (UC4) ---
$FF -f lavfi -i "gradients=s=640x360:r=25:d=10:speed=0.02" \
    -f lavfi -i "sine=frequency=330:duration=10" -vf "\
drawtext=fontfile=$FONT_BOLD:text='WebM · VP9':fontcolor=white:fontsize=48:x=(w-tw)/2:y=(h-th)/2" \
    -c:v libvpx-vp9 -b:v 0 -crf 40 -row-mt 1 -c:a libopus -b:a 32k -t 10 \
    "$PUBLIC/trailer.webm"
$FF -f lavfi -i "gradients=s=640x360:r=25:d=10:speed=0.02" \
    -f lavfi -i "sine=frequency=330:duration=10" -vf "\
drawtext=fontfile=$FONT_BOLD:text='MP4 · H.264':fontcolor=white:fontsize=48:x=(w-tw)/2:y=(h-th)/2" \
    $X264 -crf 30 -c:a aac -b:a 48k -t 10 -movflags +faststart \
    "$PUBLIC/trailer.mp4"

# --- Silent background loop (UC5) ---
$FF -f lavfi -i "gradients=s=960x540:r=25:d=8:speed=0.01:c0=0x1d4ed8:c1=0x7c3aed:c2=0x0f766e:nb_colors=3" \
    $X264 -crf 32 -an -t 8 -movflags +faststart "$PUBLIC/background.mp4"

# --- Podcast episodes (UC6) ---
EPISODES=(
    "episode-1|Welcome to Shipping Notes. Today: why our team moved to a four-day week, and what it did to our release cadence. Spoiler: we ship more often now, not less."
    "episode-2|This is Shipping Notes, episode two. We turn on a screen reader, close our eyes, and try to book a meeting room with our own app. It did not go well, and that is the point."
    "episode-3|Shipping Notes, episode three. Moving a ten-year-old application to the cloud: what we kept, what we rewrote, and the one database migration we would never do again."
)
for episode in "${EPISODES[@]}"; do
    IFS='|' read -r id text <<< "$episode"
    node tts.mjs "$HOST" "$WORK/$id.mp3" "$text" > /dev/null
    $FF -i "$WORK/$id.mp3" -ac 1 -c:a libmp3lame -b:a 48k "$PUBLIC/podcast/$id.mp3"
done

du -sh "$PRIVATE" "$PUBLIC"
