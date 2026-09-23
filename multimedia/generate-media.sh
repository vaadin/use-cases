#!/usr/bin/env bash
#
# Regenerates every sample media file used by the multimedia use cases.
# Needs ffmpeg built with libx264, libvpx-vp9, libopus, libmp3lame, drawtext
# and flite. The generated files are committed, so running this is only
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
mkdir -p "$PRIVATE/hls" "$PUBLIC/podcast"

FONT=${FONT:-/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf}
FONT_BOLD=${FONT_BOLD:-/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf}
FF="ffmpeg -hide_banner -loglevel error -y"
# Two-second GOP so seeking and HLS segment boundaries land on keyframes.
X264="-c:v libx264 -preset slow -pix_fmt yuv420p -g 50 -keyint_min 50 -sc_threshold 0"

# say <text> <seconds> <out.wav>: synthesised speech padded to a fixed length.
say() {
    $FF -f lavfi -i "flite=text='$1':voice=slt" \
        -af "apad=whole_dur=$2,aresample=48000" -ac 1 -t "$2" "$3"
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
CHAPTERS=(
    "Welcome|#1d4ed8|Welcome to the quarterly product review. Today we look at what shipped, what is next, and your questions."
    "Roadmap|#047857|The next release focuses on faster start up, better offline support, and a new reporting module."
    "Demo|#b45309|In the demo we open a customer record, change the address, and every user sees the update at once."
    "Questions|#7c3aed|Finally, questions and answers. Thank you for joining, and see you at the next review."
)
: > "$WORK/review-parts.txt"
i=0
for chapter in "${CHAPTERS[@]}"; do
    IFS='|' read -r title color text <<< "$chapter"
    slide "$color" "$title" "Quarterly product review" 15 "$WORK/v$i.mp4"
    say "$text" 15 "$WORK/a$i.wav"
    $FF -i "$WORK/v$i.mp4" -i "$WORK/a$i.wav" -c:v copy -c:a aac -b:a 48k \
        "$WORK/part$i.mp4"
    echo "file '$WORK/part$i.mp4'" >> "$WORK/review-parts.txt"
    i=$((i + 1))
done
$FF -f concat -safe 0 -i "$WORK/review-parts.txt" -c copy -movflags +faststart \
    "$PRIVATE/quarterly-review.mp4"

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
    $X264 -crf 30 -c:a aac -b:a 48k \
    -maxrate:v:0 300k -bufsize:v:0 300k \
    -maxrate:v:1 700k -bufsize:v:1 700k \
    -maxrate:v:2 1500k -bufsize:v:2 1500k \
    -f hls -hls_time 6 -hls_playlist_type vod -master_pl_name index.m3u8 \
    -hls_segment_type fmp4 -hls_fmp4_init_filename init.mp4 \
    -hls_segment_filename "$PRIVATE/hls/%v/segment%02d.m4s" \
    -var_stream_map "v:0,a:0,name:240p v:1,a:1,name:360p v:2,a:2,name:720p" \
    "$PRIVATE/hls/%v/index.m3u8"

# --- Per-user recordings with posters (UC1) ---
for recording in "sprint-41|#0f766e|Sprint 41 review|Search got twice as fast and the export button is back." \
                 "sprint-42|#be123c|Sprint 42 review|Dark mode shipped and the mobile layout was reworked."; do
    IFS='|' read -r id color title text <<< "$recording"
    slide "$color" "$title" "Team recording" 10 "$WORK/$id.mp4"
    say "$text" 10 "$WORK/$id.wav"
    $FF -i "$WORK/$id.mp4" -i "$WORK/$id.wav" -c:v copy -c:a aac -b:a 48k \
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
    "episode-1|Episode one. Why we moved our team to four day weeks, and what happened to our release cadence."
    "episode-2|Episode two. A beginner friendly tour of accessibility testing with a screen reader."
    "episode-3|Episode three. Lessons from migrating a ten year old application to the cloud."
)
for episode in "${EPISODES[@]}"; do
    IFS='|' read -r id text <<< "$episode"
    $FF -f lavfi -i "sine=frequency=660:duration=0.4" -f lavfi \
        -i "flite=text='$text':voice=kal16" -filter_complex \
        "[0]aresample=22050[a0];[1]aresample=22050[a1];[a0][a1]concat=n=2:v=0:a=1" \
        -ac 1 -c:a libmp3lame -b:a 32k "$PUBLIC/podcast/$id.mp3"
done

du -sh "$PRIVATE" "$PUBLIC"
