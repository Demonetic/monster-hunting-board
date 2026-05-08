function HPBar({ name, currentHp, maxHp, side }) {
  const safeMaxHp = Math.max(1, maxHp || 1)
  const safeCurrentHp = Math.max(0, currentHp || 0)
  const ratio = safeCurrentHp / safeMaxHp
  const percentage = Math.max(0, Math.min(100, ratio * 100))
  const toneClass = ratio <= 0.25 ? 'is-danger' : ratio <= 0.5 ? 'is-warning' : 'is-safe'

  return (
    <div className={`battle-hp-panel battle-hp-panel-${side}`.trim()}>
      <div className="battle-hp-header">
        <strong>{name}</strong>
        <span>{safeCurrentHp} / {safeMaxHp}</span>
      </div>

      <div className="battle-hp-track" aria-hidden="true">
        <div
          className={`battle-hp-fill ${toneClass}`.trim()}
          style={{ width: `${percentage}%` }}
        />
      </div>
    </div>
  )
}

export default HPBar
