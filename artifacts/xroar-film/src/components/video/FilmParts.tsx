import { useState, type PropsWithChildren } from 'react';
import { motion } from 'framer-motion';
import { useSceneTimer } from '@/lib/video';

export const EASE = [.22, 1, .36, 1] as const;
export function useBeat(times: number[]) {
  const [beat, setBeat] = useState(0);
  useSceneTimer(times.map((time, i) => ({ time, callback: () => setBeat(i + 1) })));
  return beat;
}
export function Shot({ children, name, field, aperture = false }: PropsWithChildren<{
  name: string; field: string; aperture?: boolean;
}>) {
  return <motion.section data-scene={name} className={`shot ${field}`}
    initial={aperture ? { clipPath: 'circle(12% at 65% 50%)', scale: 1.12 } : { opacity: 1, scale: 1.06 }}
    animate={{ clipPath: 'circle(150% at 65% 50%)', scale: 1, opacity: 1 }}
    exit={{ scale: 1.2, opacity: 0, transition: { duration: .45 } }}
    transition={{ duration: .7, ease: EASE }}>
    {children}
  </motion.section>;
}
export function Receiver({ typing = false }: { typing?: boolean }) {
  return <div className="receiver">
    <div className="receiver-screen">
      <div className="basic-head">COLOR BASIC</div>
      <div>OK</div>
      {typing && <div className="basic-code">10 PRINT "HELLO"<br/>20 GOTO 10<br/>RUN<br/>HELLO</div>}
      <span className="cursor">▉</span>
    </div>
    <div className="receiver-side">
      <div className="speaker" />
      <div className="dial" /><div className="dial small" />
      <i className="power" />
    </div>
    <div className="receiver-brand">TRS-80 · COLOR VIDEO RECEIVER</div>
  </div>;
}
export function MediaArt({ kind }: { kind: 'cassette' | 'disk' | 'cartridge' }) {
  return <svg viewBox="0 0 240 180" fill="none" aria-hidden="true">
    {kind === 'cassette' ? <>
      <rect x="8" y="22" width="224" height="140" rx="12" fill="#f4efdd" stroke="#141817" strokeWidth="6"/>
      <rect x="28" y="40" width="184" height="78" rx="10" fill="#141817"/>
      {[72,168].map(x => <g key={x} className="reel" style={{ transformOrigin: `${x}px 79px` }}>
        <circle cx={x} cy="79" r="25" stroke="#9fe759" strokeWidth="7"/>
        <path d={`M${x-10} 79h20M${x} 69v20`} stroke="#f4efdd" strokeWidth="5"/>
      </g>)}
      <path d="M66 152l12-21h84l12 21" fill="#ff6942" stroke="#141817" strokeWidth="5"/>
    </> : kind === 'disk' ? <>
      <path d="M32 8h152l26 26v138H32Z" fill="#141817"/>
      <rect x="64" y="10" width="107" height="58" fill="#b8c1b5"/>
      <rect x="125" y="17" width="27" height="43" fill="#141817"/>
      <rect x="56" y="91" width="128" height="68" rx="4" fill="#f4efdd"/>
      <path d="M69 106h88M69 121h73M69 136h53" stroke="#ff6942" strokeWidth="6"/>
    </> : <>
      <path d="M43 26h154v112h-23v31H65v-31H43Z" fill="#141817"/>
      <rect x="58" y="43" width="124" height="72" rx="5" fill="#ff6942"/>
      <path d="M75 63h89M75 80h72M75 98h51" stroke="#f4efdd" strokeWidth="7"/>
      {[80,96,112,128,144,160].map(x => <path key={x} d={`M${x} 146v21`} stroke="#d2b667" strokeWidth="8"/>)}
    </>}
  </svg>;
}
export function GamepadArt({ active }: { active: boolean }) {
  return <svg viewBox="0 0 500 300" className="gamepad-art" aria-hidden="true">
    <path className="controller-cable" d="M250 62V5" stroke="#f4efdd" strokeWidth="7" fill="none"/>
    <path d="M142 65C91 65 78 91 62 145L32 237C17 282 54 297 82 271L148 218H350L417 272C447 299 483 280 468 237L437 144C421 91 407 65 357 65Z"
      fill="#f4efdd" stroke="#4b5550" strokeWidth="8"/>
    <path d="M117 101v65M84 134h65" stroke="#141817" strokeWidth="22"/>
    <circle cx="167" cy="199" r="32" fill="#141817"/>
    <circle cx="167" cy="199" r="20" fill="#9fe759" className="physical-stick"/>
    <circle cx="320" cy="199" r="30" fill="#141817"/><circle cx="320" cy="199" r="18" fill="#657060"/>
    <circle cx="385" cy="150" r="23" fill={active ? '#9fe759' : '#ff6942'} className={active ? 'fire-lit' : ''}/>
    <text x="385" y="158" textAnchor="middle" fill="#141817" fontSize="25" fontWeight="700">A</text>
    <circle cx="422" cy="112" r="13" fill="#141817"/>
    <circle cx="385" cy="79" r="13" fill="#141817"/><circle cx="350" cy="112" r="13" fill="#141817"/>
    <path d="M223 143h16M262 143h16" stroke="#141817" strokeWidth="8"/>
  </svg>;
}
