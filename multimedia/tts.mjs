// Speaks text with the ElevenLabs text-to-speech API, for generate-media.sh.
//
//   node tts.mjs <voice-id> <out.mp3> <segment>...
//
// The segments are spoken as one text. Writes the speech to <out.mp3> and
// prints a JSON array with the [start, end] time in seconds of each segment,
// which generate-media.sh turns into subtitle cues. Needs ELEVENLABS_API_KEY.
// Responses are cached in TTS_CACHE (default ~/.cache/multimedia-tts), so
// regenerating the media does not call the API again for unchanged text.
import { createHash } from 'node:crypto';
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { homedir } from 'node:os';
import { join } from 'node:path';

const MODEL = 'eleven_multilingual_v2';
const [voice, out, ...segments] = process.argv.slice(2);
if (!voice || !out || segments.length === 0) {
  console.error('usage: node tts.mjs <voice-id> <out.mp3> <segment>...');
  process.exit(2);
}
const text = segments.join(' ');

const cacheDir = process.env.TTS_CACHE ?? join(homedir(), '.cache', 'multimedia-tts');
mkdirSync(cacheDir, { recursive: true });
const cacheFile = join(cacheDir,
  createHash('sha256').update(`${MODEL}\n${voice}\n${text}`).digest('hex') + '.json');

let response;
if (existsSync(cacheFile)) {
  response = JSON.parse(readFileSync(cacheFile, 'utf8'));
} else {
  const key = process.env.ELEVENLABS_API_KEY;
  if (!key) {
    console.error('ELEVENLABS_API_KEY is not set');
    process.exit(1);
  }
  const res = await fetch(
    `https://api.elevenlabs.io/v1/text-to-speech/${voice}/with-timestamps?output_format=mp3_44100_128`,
    {
      method: 'POST',
      headers: { 'xi-api-key': key, 'Content-Type': 'application/json' },
      body: JSON.stringify({ text, model_id: MODEL }),
    });
  if (!res.ok) {
    console.error(`ElevenLabs answered ${res.status}: ${await res.text()}`);
    process.exit(1);
  }
  response = await res.json();
  writeFileSync(cacheFile, JSON.stringify(response));
}

writeFileSync(out, Buffer.from(response.audio_base64, 'base64'));

// Map each segment to the time of its first and last character.
const { character_start_times_seconds: starts, character_end_times_seconds: ends } =
  response.alignment;
const times = [];
let offset = 0;
for (const segment of segments) {
  const first = offset;
  const last = offset + segment.length - 1;
  times.push([starts[first], ends[last]]);
  offset += segment.length + 1; // the joining space
}
console.log(JSON.stringify(times));
