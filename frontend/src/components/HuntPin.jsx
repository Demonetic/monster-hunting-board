import { getBeastDisplayName, getHuntPinImage, getHuntPinType } from '../assets/beastVisuals'

function HuntPin({ hunt, x, y, onClick }) {
  const pinImage = getHuntPinImage(hunt)
  const pinType = getHuntPinType(hunt)
  const beastLabel = getBeastDisplayName(hunt?.beasts?.[0] ?? hunt?.primaryBeast ?? hunt?.beast ?? hunt?.primaryBeastType ?? hunt?.beastType ?? null)

  return (
    <button
      type="button"
      className="hunt-pin"
      style={{ left: `${x}%`, top: `${y}%` }}
      onClick={onClick}
      aria-label={`${pinType} ${String(beastLabel).toLowerCase()} hunt`}
    >
      <img src={pinImage} alt="" />
    </button>
  )
}

export default HuntPin
