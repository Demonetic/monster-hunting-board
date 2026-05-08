import { getPinImage } from '../assets/beastVisuals'

function HuntPin({ x, y, type, beastType, onClick }) {
  const pinImage = getPinImage(type, beastType)

  return (
    <button
      type="button"
      className="hunt-pin"
      style={{ left: `${x}%`, top: `${y}%` }}
      onClick={onClick}
      aria-label={`${type} ${beastType?.toLowerCase() ?? 'hunt'} hunt`}
    >
      <img src={pinImage} alt="" />
    </button>
  )
}

export default HuntPin
