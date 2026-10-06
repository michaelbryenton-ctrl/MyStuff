import { motion } from 'framer-motion';
import { Shot, MediaArt, useBeat, EASE } from '../FilmParts';

const ITEMS = [
  { kind: 'cartridge', ext: '.CCC', name: 'CARTRIDGE' },
  { kind: 'cassette', ext: '.CAS', name: 'CASSETTE' },
  { kind: 'disk', ext: '.DSK', name: 'DISK' },
] as const;
export function MediaScene() {
  const beat = useBeat([700, 1550, 2800, 3300, 4000]);
  return <Shot name="Media" field="green" aperture>
    <div className="media-heading"><div className="eyebrow">BRING YOUR OWN</div><h1>ROMs. <motion.span animate={{ opacity: beat >= 1 ? 1 : .15 }}>Games.</motion.span></h1></div>
    <div className="media-stage">{ITEMS.map((item, i) => <motion.div className="media-item" key={item.kind}
      initial={{ rotate: i === 0 ? -12 : 12, scale: .6, opacity: 0 }}
      animate={{ rotate: beat >= 5 ? (i - 1) * 20 : 0, scale: beat >= 5 ? .65 : 1, opacity: 1, x: beat >= 5 ? (1 - i) * 90 : 0 }}
      transition={{ delay: beat >= 5 ? 0 : .15 + i * .7, duration: .55, ease: EASE }}>
      <MediaArt kind={item.kind}/><div className="media-ext">{item.ext}</div><div className="eyebrow">{item.name}</div>
      <motion.div className="media-underline" animate={{ scaleX: beat >= 4 ? 1 : 0 }} transition={{ delay: i * .15, duration: .3 }}/>
    </motion.div>)}</div>
    <motion.p className="media-instruction" animate={{ opacity: beat >= 3 ? 1 : 0 }}>Import firmware. Choose your media.</motion.p>
    <div className="footnote">ROMs and games are not bundled.</div>
    <motion.div className="media-ring" initial={{ scale: 0 }} animate={{ scale: beat >= 5 ? 16 : 0 }} transition={{ duration: 1, ease: EASE }}/>
  </Shot>;
}
