#!/usr/bin/env bash
#
# Regenerates every sample media file used by the multimedia use cases.
# Needs ffmpeg built with libx264, libsvtav1, libmp3lame and
# drawtext, plus jq and Node.js. Narration is spoken by the ElevenLabs API
# through tts.mjs, so ELEVENLABS_API_KEY must be set (responses are cached,
# see tts.mjs); the UC1 recordings, the quarterly review and the product
# trailer are HeyGen recordings instead, see SPRINT_41_SOURCE,
# SPRINT_42_SOURCE, REVIEW_SOURCE and TRAILER_SOURCE below. The generated
# files are committed, so running this is only needed when the samples change.
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

FONT_BOLD=${FONT_BOLD:-/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf}
FF="ffmpeg -hide_banner -loglevel error -y"
# Two-second GOP so seeking and HLS segment boundaries land on keyframes.
X264="-c:v libx264 -preset slow -pix_fmt yuv420p -g 50 -keyint_min 50 -sc_threshold 0"

# ElevenLabs premade voice.
HOST=JBFqnCBsd6RMkjVDRZzb  # George: warm, captivating storyteller

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

# --- The quarterly review (UC2, UC3, UC7, UC8, UC9) ---
# An avatar video recorded with HeyGen in four scenes; the chapter starts in
# Chapters.java follow the scene cuts. Pass the HeyGen export (1080p) as
# REVIEW_SOURCE to re-encode it; without it the committed recording is kept
# and only the subtitles and the HLS ladder are rebuilt from it.
REVIEW_SOURCE=${REVIEW_SOURCE:-}
if [[ -n $REVIEW_SOURCE ]]; then
    $FF -i "$REVIEW_SOURCE" -vf scale=1280:720 $X264 -crf 32 \
        -c:a aac -b:a 64k -ac 1 -movflags +faststart "$WORK/quarterly-review.mp4"
    mv "$WORK/quarterly-review.mp4" "$PRIVATE/quarterly-review.mp4"
fi

# Subtitle cues, timed by hand to the pauses in the narration (UC9).
CUE_TIMES=(
    "0.33 3.88" "4.26 7.39" "7.68 10.49"
    "11.05 14.05" "14.63 18.75" "19.10 22.72"
    "23.25 24.85" "25.41 27.93" "28.33 30.85" "31.11 35.12"
    "36.08 40.24" "40.91 44.20" "44.97 47.48"
)
SUBTITLES_EN=(
    $'Hi, and welcome to the quarterly product review!\nGreat to have you here.'
    $'In the next minute we\'ll walk through the roadmap,\nshow you a quick live demo,'
    $'and answer the question we hear most often.\nLet\'s dive in.'
    $'First, the roadmap.\nThe next release is all about speed.'
    $'Apps start up to twice as fast,\noffline support handles flaky connections gracefully,'
    $'and a brand-new reporting module lets you\nbuild dashboards without writing a single query.'
    "Now for the fun part: the demo."
    $'I\'m opening a customer record\nand changing the shipping address.'
    $'Watch the second window.\nThe moment I hit save,'
    $'every other user sees the new address instantly.\nNo refresh, no extra code.'
    $'Finally, your questions.\nThe big one: when can I try this?'
    $'The beta opens next month,\nand upgrading is a one-line change.'
    $'Thanks so much for watching,\nand see you at the next review!'
)
SUBTITLES_DE=(
    $'Hallo und willkommen zum Quartals-Produktreview!\nSchön, dass Sie dabei sind.'
    $'In der nächsten Minute gehen wir die Roadmap durch,\nzeigen Ihnen eine kurze Live-Demo'
    $'und beantworten die Frage, die wir am häufigsten hören.\nLegen wir los.'
    $'Zuerst die Roadmap.\nIm nächsten Release dreht sich alles um Geschwindigkeit.'
    $'Apps starten bis zu doppelt so schnell,\nder Offline-Support meistert wackelige Verbindungen souverän,'
    $'und mit einem brandneuen Berichtsmodul erstellen Sie\nDashboards, ohne eine einzige Abfrage zu schreiben.'
    "Jetzt kommt der spannende Teil: die Demo."
    $'Ich öffne einen Kundendatensatz\nund ändere die Lieferadresse.'
    $'Achten Sie auf das zweite Fenster.\nSobald ich speichere,'
    $'sehen alle anderen Nutzer sofort die neue Adresse.\nKein Neuladen, kein zusätzlicher Code.'
    $'Zum Schluss Ihre Fragen.\nDie wichtigste: Wann kann ich das ausprobieren?'
    $'Die Beta startet nächsten Monat,\nund das Upgrade ist eine einzeilige Änderung.'
    $'Vielen Dank fürs Zuschauen,\nbis zum nächsten Review!'
)
SUBTITLES_FI=(
    $'Hei ja tervetuloa neljännesvuoden tuotekatsaukseen!\nMukava, että olet mukana.'
    $'Seuraavan minuutin aikana käymme läpi tiekartan,\nnäytämme lyhyen live-demon'
    $'ja vastaamme useimmin kuulemaamme kysymykseen.\nAloitetaan.'
    $'Ensin tiekartta.\nSeuraavassa julkaisussa keskitytään nopeuteen.'
    $'Sovellukset käynnistyvät jopa kaksi kertaa nopeammin,\noffline-tuki selviää katkeilevista yhteyksistä sujuvasti,'
    $'ja upouudella raportointimoduulilla rakennat\nkoontinäyttöjä kirjoittamatta yhtään kyselyä.'
    "Sitten hauskin osuus: demo."
    $'Avaan asiakastietueen\nja muutan toimitusosoitteen.'
    $'Katso toista ikkunaa.\nHeti kun tallennan,'
    $'jokainen muu käyttäjä näkee uuden osoitteen välittömästi.\nEi päivitystä, ei lisäkoodia.'
    $'Lopuksi kysymyksenne.\nTärkein niistä: milloin pääsen kokeilemaan?'
    $'Beta avautuu ensi kuussa,\nja päivitys vaatii vain yhden rivin muutoksen.'
    $'Kiitos katsomisesta,\nnähdään seuraavassa katsauksessa!'
)
for language in en de fi; do
    declare -n cues="SUBTITLES_${language^^}"
    {
        echo "WEBVTT"
        for c in "${!CUE_TIMES[@]}"; do
            read -r start end <<< "${CUE_TIMES[c]}"
            printf '\n%s --> %s\n%s\n' "$(vtt_time "$start")" "$(vtt_time "$end")" "${cues[c]}"
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
$FF -i "${REVIEW_SOURCE:-$PRIVATE/quarterly-review.mp4}" -filter_complex "\
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
# Avatar videos recorded with HeyGen, one presenter and setting each. Pass
# the HeyGen exports (1080p) as SPRINT_41_SOURCE and SPRINT_42_SOURCE to
# re-encode them and take a new poster; otherwise the committed files are
# kept.
SPRINT_41_SOURCE=${SPRINT_41_SOURCE:-}
SPRINT_42_SOURCE=${SPRINT_42_SOURCE:-}
for recording in "sprint-41|$SPRINT_41_SOURCE" "sprint-42|$SPRINT_42_SOURCE"; do
    IFS='|' read -r id source <<< "$recording"
    [[ -n $source ]] || continue
    $FF -i "$source" -vf scale=1280:720 $X264 -crf 32 \
        -c:a aac -b:a 64k -ac 1 -movflags +faststart "$PRIVATE/$id.mp4"
    $FF -ss 1 -i "$PRIVATE/$id.mp4" -frames:v 1 -q:v 4 "$PRIVATE/$id-poster.jpg"
done

# --- The product trailer with two codecs, the codec burnt in (UC4) ---
# An avatar video recorded with HeyGen. Pass the HeyGen export (1080p) as
# TRAILER_SOURCE to re-encode it; without it the committed files are kept.
# Both are 720p; the codecs attributes in FormatFallbackView match the
# profiles and levels below (AV1 Main 3.1, H.264 High 3.1).
TRAILER_SOURCE=${TRAILER_SOURCE:-}
if [[ -n $TRAILER_SOURCE ]]; then
    $FF -i "$TRAILER_SOURCE" -vf "$(label AV1 1280:720)" \
        -c:v libsvtav1 -preset 4 -crf 50 -pix_fmt yuv420p -g 50 \
        -c:a aac -b:a 64k -ac 1 -movflags +faststart "$PUBLIC/trailer-av1.mp4"
    $FF -i "$TRAILER_SOURCE" -vf "$(label H.264 1280:720)" \
        $X264 -crf 30 -level 3.1 \
        -c:a aac -b:a 64k -ac 1 -movflags +faststart "$PUBLIC/trailer.mp4"
fi

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
