import groupPin from '../assets/pin_group.png'
import glowingPin from '../assets/pin_glowing.png'
import monsterPin from '../assets/pin_monster.png'

const pinImages = {
  group: groupPin,
  solo: glowingPin,
  boss: monsterPin,
}

function HuntPin({ x, y, type, onClick }) {
  const pinImage = pinImages[type] ?? groupPin

  return (
    <button
      type="button"
      className="hunt-pin"
      style={{ left: `${x}%`, top: `${y}%` }}
      onClick={onClick}
      aria-label={`${type} hunt`}
    >
      <img src={pinImage} alt="" />
    </button>
  )
}

export default HuntPin
