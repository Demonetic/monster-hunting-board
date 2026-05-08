function BattleSprite({ side, name, image, isActing, isDamaged }) {
  return (
    <div
      className={[
        'battle-sprite',
        `battle-sprite-${side}`,
        isActing ? 'is-acting' : '',
        isDamaged ? 'is-damaged' : '',
      ].filter(Boolean).join(' ')}
    >
      <img src={image} alt={name} />
    </div>
  )
}

export default BattleSprite
