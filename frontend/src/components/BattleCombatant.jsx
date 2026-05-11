import FloatingCombatText from './FloatingCombatText'

function getHpTone(currentHp, maxHp) {
  const safeMaxHp = Math.max(maxHp ?? 0, 1)
  const ratio = Math.max(0, Math.min(1, (currentHp ?? 0) / safeMaxHp))

  if (ratio <= 0.3) {
    return 'danger'
  }

  if (ratio <= 0.6) {
    return 'warning'
  }

  return 'safe'
}

function BattleCombatant({
  side,
  className = '',
  style,
  name,
  image,
  currentHp,
  maxHp,
  isActing,
  isDamaged,
  floatingTexts = [],
}) {
  const safeMaxHp = Math.max(maxHp ?? 0, 1)
  const hpPercent = Math.max(0, Math.min(100, ((currentHp ?? 0) / safeMaxHp) * 100))
  const hpTone = getHpTone(currentHp, maxHp)

  return (
    <div
      className={[
        'battle-combatant',
        `battle-combatant-${side}`,
        className,
        isActing ? 'is-acting' : '',
        isDamaged ? 'is-damaged' : '',
      ].filter(Boolean).join(' ')}
      style={style}
    >
      <div className="battle-combatant-ui">
        <p className="battle-combatant-name">{name}</p>
        <div className="battle-combatant-hp">
          <div className="battle-combatant-hp-track" aria-hidden="true">
            <div
              className={`battle-combatant-hp-fill is-${hpTone}`}
              style={{ width: `${hpPercent}%` }}
            />
          </div>
          <span className="battle-combatant-hp-value">{currentHp} / {maxHp}</span>
        </div>
      </div>

      {floatingTexts.map((entry, index) => (
        <FloatingCombatText
          key={entry.id}
          side={side}
          text={entry.text}
          variant={entry.variant}
          stackIndex={index}
        />
      ))}

      <div className="battle-combatant-sprite">
        <img src={image} alt={name} />
      </div>
    </div>
  )
}

export default BattleCombatant
