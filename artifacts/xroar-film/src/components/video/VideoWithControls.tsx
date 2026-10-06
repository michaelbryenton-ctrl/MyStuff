import { useCallback, useEffect, useRef, useState } from 'react';
import { Play, Pause, Repeat, Volume2, VolumeX, ChevronDown, ChevronUp } from 'lucide-react';
import VideoTemplate, { SCENE_DURATIONS } from './VideoTemplate';
import { useSceneControls } from './useSceneControls';

const SCENE_DETAILS: Record<string, {title: string; filePath: string}> = {
  spirit: {title:'1980s spirit',filePath:'src/components/video/video_scenes/SpiritScene.tsx'},
  android: {title:'CoCo on Android',filePath:'src/components/video/video_scenes/AndroidScene.tsx'},
  media: {title:'ROMs and games',filePath:'src/components/video/video_scenes/MediaScene.tsx'},
  controls: {title:'Touch and gamepad',filePath:'src/components/video/video_scenes/ControlsScene.tsx'},
  identity: {title:'XRoar',filePath:'src/components/video/video_scenes/IdentityScene.tsx'},
};
const formatTime = (ms: number) => `0:${Math.floor(ms / 1000).toString().padStart(2,'0')}`;

function PlaybackStatus({ controls, onJump }: {
  controls: ReturnType<typeof useSceneControls>; onJump: (index: number) => void;
}) {
  const {sceneKeys, activeIndex, activeDuration, activeStartTime, totalDuration, tick, paused} = controls;
  const [elapsed, setElapsed] = useState(0);
  const base = useRef(0);
  useEffect(() => { setElapsed(0); base.current = 0; }, [tick]);
  useEffect(() => {
    if (paused) return;
    const start = performance.now();
    const id = window.setInterval(() => setElapsed(base.current + performance.now() - start), 60);
    return () => { window.clearInterval(id); base.current += performance.now() - start; };
  }, [tick, paused]);
  return <>
    <div className="flex flex-1 gap-2">
      {sceneKeys.map((key, i) => <button key={key} aria-label={`Jump to scene ${i+1}`}
        title={SCENE_DETAILS[key].title} aria-current={i === activeIndex ? 'true' : undefined}
        onClick={() => onJump(i)} className="relative h-3 flex-1 overflow-hidden rounded-full bg-white/20">
        <span className="absolute inset-y-0 left-0 bg-white/90"
          style={{width:`${i === activeIndex ? Math.min(1, elapsed / activeDuration)*100 : 0}%`}}/>
      </button>)}
    </div>
    <span className="text-xl text-white/70">{activeIndex + 1}/{sceneKeys.length}</span>
    <span className="min-w-[11ch] text-right font-mono text-xl text-white/80" role="timer">
      {formatTime(Math.min(totalDuration, activeStartTime + Math.min(elapsed,activeDuration)))} / {formatTime(totalDuration)}
    </span>
  </>;
}

export default function VideoWithControls() {
  const controls = useSceneControls(SCENE_DURATIONS);
  const {sceneKeys, mountKey, durations, paused, locked, onSceneChange, jumpTo, togglePause, toggleLock} = controls;
  const [muted, setMuted] = useState(false);
  const [collapsed, setCollapsed] = useState(false);
  const [hovering, setHovering] = useState(false);
  const [tapPinned, setTapPinned] = useState(false);
  const sensor = useRef<HTMLDivElement>(null);
  const isIframed = window.self !== window.top;
  const handleJump = useCallback((index: number) => {
    jumpTo(index);
    const detail = SCENE_DETAILS[sceneKeys[index]];
    window.parent.postMessage({type:'REPLIT_VIDEO_SCENE_SELECTED', payload:{
      sceneIndex:index, sceneCount:sceneKeys.length, sceneTitle:detail.title,
      filePath:detail.filePath, lineNumber:1,
    }}, '*');
  }, [jumpTo,sceneKeys]);
  useEffect(() => {
    if (!paused) return;
    const frozen = document.getAnimations().filter(a => a.playState === 'running');
    frozen.forEach(a => a.pause());
    return () => frozen.forEach(a => a.play());
  }, [paused]);
  useEffect(() => {
    if (!(collapsed && tapPinned)) return;
    const outside = (e: PointerEvent) => {
      if (e.pointerType !== 'mouse' && !sensor.current?.contains(e.target as Node)) setTapPinned(false);
    };
    document.addEventListener('pointerdown', outside);
    return () => document.removeEventListener('pointerdown', outside);
  }, [collapsed,tapPinned]);
  if (!isIframed) return <VideoTemplate />;
  const visible = !collapsed || hovering || tapPinned;
  const toggleCollapse = () => {
    if (!collapsed) {setHovering(false);setTapPinned(false);}
    setCollapsed(!collapsed);
  };
  const buttonClass = 'flex h-14 w-14 shrink-0 items-center justify-center rounded-lg text-white/70 hover:bg-white/10 hover:text-white';
  return <div className="relative h-screen w-full overflow-hidden">
    <VideoTemplate key={mountKey} durations={durations} loop paused={paused} muted={muted} onSceneChange={onSceneChange}/>
    <div ref={sensor} className="absolute inset-x-0 bottom-0 z-50 flex flex-col justify-end" style={{height:'25%'}}
      onPointerEnter={e => {if(e.pointerType === 'mouse') setHovering(true);}}
      onPointerLeave={e => {if(e.pointerType === 'mouse') setHovering(false);}}
      onPointerDown={e => {if(e.pointerType !== 'mouse' && collapsed) setTapPinned(true);}}>
      <div className="flex-1" aria-hidden="true"/>
      <div aria-hidden={!visible} className={`flex items-center gap-3 bg-black/70 px-5 py-4 backdrop-blur-sm transition-all ${visible ? 'translate-y-0 opacity-100' : 'pointer-events-none translate-y-full opacity-0'}`}>
        <button className={buttonClass} onClick={togglePause} aria-label={paused ? 'Play' : 'Pause'}>{paused ? <Play size={32}/> : <Pause size={32}/>}</button>
        <button className={`${buttonClass} ${locked ? 'bg-white/15' : ''}`} onClick={toggleLock} aria-pressed={locked} aria-label="Loop current scene"><Repeat size={32}/></button>
        <button className={buttonClass} onClick={() => setMuted(!muted)} aria-label={muted ? 'Unmute music' : 'Mute music'}>{muted ? <VolumeX size={32}/> : <Volume2 size={32}/>}</button>
        <div className="w-px self-stretch bg-white/15"/>
        <PlaybackStatus controls={controls} onJump={handleJump}/>
        <button className={buttonClass} onClick={toggleCollapse} aria-label={collapsed ? 'Show controls' : 'Hide controls'} aria-expanded={!collapsed}>{collapsed ? <ChevronUp size={40}/> : <ChevronDown size={40}/>}</button>
      </div>
    </div>
  </div>;
}
