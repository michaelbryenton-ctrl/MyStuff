import { motion } from 'framer-motion';
import { Shot, GamepadArt, useBeat, EASE } from '../FilmParts';

export function ControlsScene() {
  const beat = useBeat([800, 1800, 3000, 4000, 4200, 5000]);
  return <Shot name="Controls" field="dark" aperture>
    <motion.h1 className="controls-title" animate={{ y: beat >= 6 ? '-8vh' : 0 }}>Two ways to play.</motion.h1>
    <div className="control-stage">
      <div className="touch-demo">
        <div className="eyebrow">TOUCH</div>
        <div className="touch-box"><div className="pad-cross"/>
          <svg viewBox="0 0 300 210"><motion.path d="M75 147L121 112L219 66" stroke="#9fe759" strokeWidth="3" fill="none" strokeDasharray="5 8" initial={{pathLength:0}} animate={{pathLength:beat >= 1 ? 1 : 0}} transition={{duration:1}}/></svg>
          <motion.i className="demo-dot" initial={{ left: '25%', top: '70%' }} animate={{ left: beat >= 1 ? '73%' : '25%', top: beat >= 1 ? '31%' : '70%' }} transition={{ duration: 1, ease: EASE }}/>
        </div>
        <motion.p animate={{ opacity: beat >= 2 ? 1 : 0 }}>Position stays.</motion.p>
      </div>
      <div className="gamepad-demo">
        <div className="eyebrow">USB GAMEPAD</div>
        <motion.div initial={{scale:.85}} animate={{scale:1}} transition={{duration:.7,ease:EASE}}><GamepadArt active={beat >= 3}/></motion.div>
        <motion.p animate={{opacity:beat >= 3 ? 1 : 0}}>Left stick / D-pad <span className="green-text">· A fires.</span></motion.p>
      </div>
    </div>
    <div className="footnote">Android-recognized controllers · Right / Left port selection</div>
    <motion.div className="final-aperture" initial={{ scale: 0 }} animate={{ scale: beat >= 6 ? 18 : 0 }} transition={{ duration: 1, ease: EASE }}/>
  </Shot>;
}
