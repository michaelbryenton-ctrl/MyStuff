import { useEffect, useRef, type ComponentType } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { VideoCanvas, VideoPausedContext, useVideoPlayer } from '@/lib/video';
import logo from '@assets/xroar_1791246786368.png';
import { SpiritScene } from './video_scenes/SpiritScene';
import { AndroidScene } from './video_scenes/AndroidScene';
import { MediaScene } from './video_scenes/MediaScene';
import { ControlsScene } from './video_scenes/ControlsScene';
import { IdentityScene } from './video_scenes/IdentityScene';
import './film.css';

export const SCENE_DURATIONS = {
  spirit: 4000,
  android: 6000,
  media: 5000,
  controls: 6000,
  identity: 4000,
};
const VIDEO_ASPECT_RATIO = '16:9' as const;
const SCENES: Record<string, ComponentType> = {
  spirit: SpiritScene, android: AndroidScene, media: MediaScene,
  controls: ControlsScene, identity: IdentityScene,
};
const LOGO_POSES = [
  { left: '50%', top: '23%', width: '12vmin' },
  { left: '92%', top: '9%', width: '5vmin' },
  { left: '8%', top: '10%', width: '5vmin' },
  { left: '92%', top: '9%', width: '5vmin' },
  { left: '50%', top: '20%', width: '12vmin' },
];
const SCENE_START_SEC: Record<string, number> = {};
let cumulativeMs = 0;
for (const [key, ms] of Object.entries(SCENE_DURATIONS)) {
  SCENE_START_SEC[key] = cumulativeMs / 1000;
  cumulativeMs += ms;
}

export default function VideoTemplate({
  durations = SCENE_DURATIONS, loop = true, paused = false, muted = false, onSceneChange,
}: {
  durations?: Record<string, number>; loop?: boolean; paused?: boolean;
  muted?: boolean; onSceneChange?: (key: string) => void;
} = {}) {
  const { currentSceneKey } = useVideoPlayer({ durations, loop, paused });
  const baseKey = currentSceneKey.replace(/_r[12]$/, '');
  const sceneIndex = Object.keys(SCENE_DURATIONS).indexOf(baseKey);
  const Scene = SCENES[baseKey];
  const audioRef = useRef<HTMLAudioElement>(null);
  const lastSceneKey = useRef<string | null>(null);
  useEffect(() => { onSceneChange?.(currentSceneKey); }, [currentSceneKey, onSceneChange]);
  useEffect(() => {
    const audio = audioRef.current;
    if (!audio) return;
    audio.volume = .45;
    if (paused) { audio.pause(); return; }
    if (lastSceneKey.current !== currentSceneKey) {
      lastSceneKey.current = currentSceneKey;
      const target = SCENE_START_SEC[baseKey] ?? 0;
      if (Math.abs(audio.currentTime - target) > .18) audio.currentTime = target;
    }
    audio.play().catch(() => {}); // A user gesture can be required in iframe previews.
  }, [currentSceneKey, baseKey, muted, paused]);
  return (
    <VideoPausedContext.Provider value={paused}>
      <VideoCanvas aspectRatio={VIDEO_ASPECT_RATIO} style={{ backgroundColor: '#141817' }}>
        <motion.div className="film-camera" initial={false}
          animate={{ rotate: [0, .35, 0, -.3, 0][sceneIndex] }}
          transition={{ duration: 1 }}>
          <AnimatePresence mode="sync">
            {Scene && <Scene key={currentSceneKey} />}
          </AnimatePresence>
        </motion.div>
        <motion.img src={logo} alt="XRoar pixel icon" className="pixel-logo" initial={false}
          style={{ backgroundColor: sceneIndex === 4 ? '#141817' : 'transparent',
            padding: sceneIndex === 4 ? '1.3vmin' : 0, borderRadius: sceneIndex === 4 ? '2vmin' : 0 }}
          animate={LOGO_POSES[sceneIndex]} transition={{ duration: .65, ease: [.22, 1, .36, 1] }} />
        <audio ref={audioRef} src={`${import.meta.env.BASE_URL}audio/bg_music.mp3`} autoPlay preload="auto" muted={muted}/>
      </VideoCanvas>
    </VideoPausedContext.Provider>
  );
}
