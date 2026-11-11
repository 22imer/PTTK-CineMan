import hallScene from '../assets/hall-scene.svg';

/** Tranh sảnh rạp pixel art (trang trí). Sinh bởi scripts/gen-hall-scene.mjs. */
export default function HallScene() {
  return <img className="hall-scene" src={hallScene} alt="" width={192} height={108} />;
}
