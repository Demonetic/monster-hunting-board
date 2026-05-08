function FloatingCombatText({ side, text, variant, stackIndex = 0 }) {
  return (
    <div
      className={[
        'floating-combat-text',
        `floating-combat-text-${side}`,
        `is-${variant}`,
      ].join(' ')}
      style={{ '--stack-offset': `${stackIndex * 30}px` }}
    >
      {text}
    </div>
  )
}

export default FloatingCombatText
