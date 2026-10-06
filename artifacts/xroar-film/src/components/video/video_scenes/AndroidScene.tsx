import { motion } from 'framer-motion';
import { Shot, Receiver, useBeat, EASE } from '../FilmParts';

export function AndroidScene() {
  const beat = useBeat([400, 800, 1100, 2200, 3200, 4100, 4900]);
  return <Shot name="Android" field="cream" aperture>
    <div className="android-copy">
      <div className="eyebrow">CLASSIC COMPUTING / NATIVE ANDROID</div>
      <motion.h1 initial={{ opacity: 0, y: 30 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: .25, duration: .55, ease: EASE }}>CoCo.<br/><span className="sub-title">In your<br/>hands.</span></motion.h1>
      <div className="feature-rule"/>
      <motion.p className="body-copy" animate={{ opacity: beat >= 4 ? 1 : 0 }}>The retro display.</motion.p>
      <motion.p className="body-copy" animate={{ opacity: beat >= 5 ? 1 : 0 }}>Controls below the picture.</motion.p>
    </div>
    <motion.div className="phone" initial={{ scale: 1.5, rotate: -5 }} animate={{ scale: beat >= 7 ? 1.15 : 1, rotate: 0 }}
      transition={{ duration: .85, ease: EASE }}>
      <div className="phone-camera"/><div className="phone-status">XRoar / CoCo</div>
      <Receiver typing={beat >= 3}/>
      <div className="phone-keys"><span>Keyboard</span><span>Enter</span><span>Space</span><span>Library</span></div>
      <div className="phone-keys second"><span>Port: Right</span><span>Input: Touch</span><span>Exit app</span></div>
      <div className="phone-pad"><small>Touch or drag — position stays</small><div className="pad-cross"/><i className="phone-touch"/></div>
      <div className="phone-credit">Illustrated app screen</div>
    </motion.div>
    <motion.div className="android-green-cover" initial={{ scaleX: 0 }} animate={{ scaleX: beat >= 7 ? 1 : 0 }} transition={{ duration: 1.1, ease: EASE }}/>
  </Shot>;
}
