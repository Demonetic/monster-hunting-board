import { useMemo, useState } from 'react'
import panelImage from '../assets/parchment_new.png'
import buttonImage from '../assets/button_new.png'
import dragonIcon from '../assets/icon_dragon.png'
import phoenixIcon from '../assets/icon_phoenix.png'
import griffinIcon from '../assets/icon_griffin.png'
import chimeraIcon from '../assets/icon_chimera.png'
import basiliskIcon from '../assets/icon_basilisk.png'

const beastIcons = {
  DRAGON: dragonIcon,
  PHOENIX: phoenixIcon,
  GRIFFIN: griffinIcon,
  CHIMERA: chimeraIcon,
  BASILISK: basiliskIcon,
}

function matchesFilter(hunt, filter) {
  if (filter === 'GROUP') {
    return hunt.type === 'HUNT'
  }

  if (filter === 'SOLO') {
    return hunt.type === 'SOLO_HUNT'
  }

  if (filter === 'BOSS') {
    return hunt.difficulty === 'BOSS'
  }

  return true
}

function HuntsPanel({ hunts, onClose, onSelectHunt }) {
  const [filter, setFilter] = useState('ALL')

  const filteredHunts = useMemo(
    () => hunts.filter((hunt) => matchesFilter(hunt, filter)),
    [hunts, filter],
  )

  return (
    <div className="game-overlay-layer">
      <section
        className="hunts-panel"
        style={{ backgroundImage: `url(${panelImage})` }}
        role="dialog"
        aria-modal="true"
        aria-label="Hunts panel"
      >
        <button type="button" className="hunts-panel-close" onClick={onClose}>
          <img src={buttonImage} alt="" />
          <span>Close</span>
        </button>

        <div className="hunts-panel-header">
          <h2 className="hunts-panel-title">Hunts</h2>

          <div className="hunts-filter-row">
            {['ALL', 'GROUP', 'SOLO', 'BOSS'].map((filterOption) => (
              <button
                key={filterOption}
                type="button"
                className={`hunts-filter-button ${filter === filterOption ? 'is-active' : ''}`}
                onClick={() => setFilter(filterOption)}
              >
                {filterOption === 'ALL'
                  ? 'All'
                  : filterOption === 'GROUP'
                    ? 'Group'
                    : filterOption === 'SOLO'
                      ? 'Solo'
                      : 'Boss'}
              </button>
            ))}
          </div>
        </div>

        <div className="hunts-list">
          {filteredHunts.map((hunt) => {
            const firstBeast = hunt.beasts?.[0]
            const beastIcon = firstBeast ? beastIcons[firstBeast.type] : null

            return (
              <div key={hunt.id} className="hunt-list-row">
                <button
                  type="button"
                  className="hunt-list-item"
                  onClick={() => onSelectHunt(hunt)}
                >
                  <div className="hunt-list-copy">
                    <h3>{hunt.title}</h3>
                    <p>{hunt.type === 'HUNT' ? 'Group Hunt' : 'Solo Hunt'}</p>
                    <p>Difficulty: {hunt.difficulty}</p>
                    <p>
                      Party: {hunt.currentPartySize}
                      {hunt.maxPartySize ? ` / ${hunt.maxPartySize}` : ''}
                    </p>
                    <p>Beast: {firstBeast?.type ?? 'Unknown'}</p>
                  </div>

                  {beastIcon && (
                    <img
                      className="hunt-list-icon"
                      src={beastIcon}
                      alt={firstBeast.type}
                    />
                  )}
                </button>
              </div>
            )
          })}

          {filteredHunts.length === 0 && (
            <p className="hunts-empty">No hunts found for this filter.</p>
          )}
        </div>
      </section>
    </div>
  )
}

export default HuntsPanel
