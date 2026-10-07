import Hls from 'hls.js';

/**
 * Plays the HLS <source> of a <video> element. Safari and recent Chrome play
 * HLS natively; elsewhere hls.js feeds the stream through Media Source
 * Extensions. Returns how the stream is played: 'native', 'hls.js' or
 * 'unsupported'.
 */
function attachHls(video: HTMLVideoElement): string {
  const source = video.querySelector<HTMLSourceElement>('source[type="application/vnd.apple.mpegurl"]');
  if (!source) {
    return 'unsupported';
  }
  if (video.canPlayType('application/vnd.apple.mpegurl')) {
    return 'native';
  }
  if (Hls.isSupported()) {
    const hls = new Hls();
    hls.loadSource(source.src);
    hls.attachMedia(video);
    return 'hls.js';
  }
  return 'unsupported';
}

(window as any).multimediaAttachHls = attachHls;
