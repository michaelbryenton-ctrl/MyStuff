import { motion } from 'framer-motion';
import { Shot, useBeat, EASE } from '../FilmParts';

export function SpiritScene() {
  const beat = useBeat([650, 1250, 1600, 2300, 2900, 3250]);
  return <Shot name="Spirit" field="dark">
    <div className="eyebrow top-label">XROAR / ANDROID PROJECT</div>
    <div className="pixel-grid">{Array.from({length: 40}, (_, i) => <motion.i key={i}
      initial={{ opacity: .06, scale: .3 }} animate={{ opacity: beat >= 2 ? [.1, .6, .1][i % 3] : .08, scale: 1 }}
      transition={{ delay: i * .018, duration: .4 }}/>)}</div>
    <motion.div className="spirit-title" initial={{ scale: 1.15 }} animate={{ scale: beat >= 6 ? 1.35 : 1, y: beat >= 6 ? '-8vh' : 0 }}
      transition={{ duration: .55, ease: EASE }}>
      <div>1980s</div>
      <motion.div initial={{ opacity: 0, y: 30 }} animate={{ opacity: beat >= 1 ? 1 : 0, y: 0 }}>spirit<span className="orange">.</span></motion.div>
    </motion.div>
    <motion.p className="spirit-caption" animate={{ opacity: beat >= 3 && beat < 6 ? 1 : 0 }}>A new place to play.</motion.p>
    <motion.div className="green-aperture" initial={{ scale: 0 }} animate={{ scale: beat >= 6 ? 12 : beat >= 5 ? 1.5 : beat >= 4 ? 1 : 0 }}
      transition={{ duration: .7, ease: EASE }}/>
    <div className="footnote">ANIMATED PROJECT SHOWCASE</div>
  </Shot>;
}
