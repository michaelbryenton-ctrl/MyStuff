import { motion } from 'framer-motion';
import { Shot, useBeat, EASE } from '../FilmParts';

export function IdentityScene() {
  const beat = useBeat([650, 1200, 1700, 2200, 3300]);
  return <Shot name="Identity" field="signal" aperture>
    <motion.div className="identity-lockup" initial={{ clipPath: 'inset(0 0 100% 0)' }} animate={{ clipPath: 'inset(0 0 0% 0)', scale: beat >= 5 ? .93 : 1 }}
      transition={{ duration: .7, ease: EASE }}>
      <h1>XRoar</h1>
      <motion.p animate={{ opacity: beat >= 1 ? 1 : 0 }}>CoCo in your hands.</motion.p>
      <motion.div className="eyebrow" animate={{ opacity: beat >= 2 ? 1 : 0 }}>NATIVE ANDROID</motion.div>
      <motion.div className="machine-family" animate={{ opacity: beat >= 3 ? 1 : 0 }}>Dragon · CoCo · MC-10</motion.div>
    </motion.div>
    <div className="identity-foot"><span>Bring your own ROMs.</span><span>ANIMATED PROJECT SHOWCASE</span></div>
    <motion.div className="loop-shutter" initial={{ scaleY: 0 }} animate={{ scaleY: beat >= 5 ? 1 : 0 }} transition={{ duration: .7, ease: EASE }}/>
  </Shot>;
}
