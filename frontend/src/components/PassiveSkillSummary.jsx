function PassiveSkillSummary({
  appearanceName,
  passiveSkillName,
  passiveSkillDescription,
  className = '',
  title = 'Passive skill',
  compact = false,
}) {
  if (!passiveSkillName && !passiveSkillDescription) {
    return null
  }

  if (compact) {
    return (
      <section className={`passive-skill-summary is-compact ${className}`.trim()}>
        <p className="passive-skill-summary-inline">
          <span className="passive-skill-summary-label">{title}:</span>{' '}
          {appearanceName && <strong className="passive-skill-summary-appearance">{appearanceName}</strong>}
          {appearanceName && passiveSkillName && <span className="passive-skill-summary-separator"> - </span>}
          {passiveSkillName && <strong className="passive-skill-summary-name">{passiveSkillName}</strong>}
        </p>
        {passiveSkillDescription && (
          <p className="passive-skill-summary-description">{passiveSkillDescription}</p>
        )}
      </section>
    )
  }

  return (
    <section className={`passive-skill-summary ${className}`.trim()}>
      <span className="passive-skill-summary-label">{title}</span>
      {appearanceName && <strong className="passive-skill-summary-appearance">{appearanceName}</strong>}
      {passiveSkillName && <strong className="passive-skill-summary-name">{passiveSkillName}</strong>}
      {passiveSkillDescription && (
        <p className="passive-skill-summary-description">{passiveSkillDescription}</p>
      )}
    </section>
  )
}

export default PassiveSkillSummary
