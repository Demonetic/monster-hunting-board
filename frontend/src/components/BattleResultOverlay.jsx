import panelBattleResult from '../assets/panel_battle_result.png'

function BattleResultOverlay({ result, onContinue }) {
  const expLabel = result.expChange >= 0 ? 'EXP earned' : 'EXP lost'
  const goldLabel = result.goldChange >= 0 ? 'Gold earned' : 'Gold lost'
  const itemsGained = result.itemsGained ?? []

  return (
    <div className="battle-result-overlay">
      <div
        className="battle-result-card"
        role="dialog"
        aria-modal="true"
        aria-label="Battle result"
        style={{ backgroundImage: `url(${panelBattleResult})` }}
      >
        <div className="battle-result-card-content">
          <h2 className={`battle-result-heading ${result.won ? 'is-victory' : 'is-defeat'}`.trim()}>
            {result.won ? 'Victory' : 'Defeat'}
          </h2>

          <div className="battle-result-grid">
            <p><span>{expLabel}:</span> {Math.abs(result.expChange)}</p>
            <p><span>{goldLabel}:</span> {Math.abs(result.goldChange)}</p>
            {result.leveledUp && <p><span>Leveled up!</span> Level {result.newLevel}</p>}
            <p><span>HP:</span> {result.newCurrentHp} / {result.newBaseHp}</p>
          </div>

          {itemsGained.length > 0 && (
            <p className="battle-result-loot">
              <span>Items gained:</span> {itemsGained.join(', ')}
            </p>
          )}

          <button type="button" className="battle-continue-button" onClick={onContinue}>
            Continue
          </button>
        </div>
      </div>
    </div>
  )
}

export default BattleResultOverlay
