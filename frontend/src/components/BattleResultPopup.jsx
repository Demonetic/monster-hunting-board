import { useEffect } from 'react'
import panelParchment from '../assets/parchment_new.png'
import buttonImage from '../assets/button_new.png'

function BattleResultPopup({ result, onClose }) {
  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      onClose()
    }, 10000)

    return () => {
      window.clearTimeout(timeoutId)
    }
  }, [onClose])

  const title = result.won ? 'VICTORY!' : 'DEFEAT...'

  return (
    <div className="battle-result-overlay" role="presentation">
      <section
        className="battle-result-popup"
        style={{ backgroundImage: `url(${panelParchment})` }}
        aria-live="polite"
      >
        <button
          type="button"
          className="battle-result-close"
          onClick={onClose}
          aria-label="Close battle result"
        >
          X
        </button>

        <h3 className="battle-result-title">{title}</h3>

        <div className="battle-result-stats">
          {result.won ? (
            <>
              <p>
                <span>EXP gained:</span> {result.expChange}
              </p>
              <p>
                <span>Gold gained:</span> {result.goldChange}
              </p>
              <p>
                <span>New Level:</span> {result.newLevel}
              </p>
              <p>
                <span>New HP:</span> {result.newBaseHp}
              </p>
              <p>
                <span>Current HP:</span> {result.newCurrentHp}
              </p>
              <p>
                <span>Damage taken:</span> {result.damageTaken}
              </p>
            </>
          ) : (
            <>
              <p>
                <span>EXP lost:</span> {result.expChange}
              </p>
              <p>
                <span>Gold gained:</span> 0
              </p>
              <p>
                <span>Current EXP:</span> {result.newExp}
              </p>
              <p>
                <span>Level:</span> {result.newLevel}
              </p>
              <p>
                <span>Current HP:</span> {result.newCurrentHp}
              </p>
              <p>
                <span>Damage taken:</span> {result.damageTaken}
              </p>
            </>
          )}
          {(result.expPotionApplied || result.endurancePotionApplied) && (
            <p>
              <span>Potions used:</span>{' '}
              {[result.expPotionApplied ? 'EXP' : null, result.endurancePotionApplied ? 'Endurance' : null]
                .filter(Boolean)
                .join(', ')}
            </p>
          )}
        </div>

        {result.turns?.length > 0 && (
          <div className="battle-result-turns">
            <h4 className="battle-result-turns-title">Battle Log</h4>
            <div className="battle-result-turn-list">
              {result.turns.map((turn) => (
                <p key={turn.turnNumber}>
                  <span>Turn {turn.turnNumber}:</span>{' '}
                  {turn.attacker === 'HUNTER' ? 'Hunter' : 'Monster'} dealt {turn.damage} damage.
                  {' '}Hunter HP: {turn.hunterHpAfterTurn}. Monster HP: {turn.monsterHpAfterTurn}.
                </p>
              ))}
            </div>
          </div>
        )}

        <button
          type="button"
          className="battle-result-dismiss"
          onClick={onClose}
        >
          <img src={buttonImage} alt="" />
          <span>Close</span>
        </button>
      </section>
    </div>
  )
}

export default BattleResultPopup
