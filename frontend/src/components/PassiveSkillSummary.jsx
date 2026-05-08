function PassiveSkillSummary({
  appearanceName,
  passiveSkillName,
  passiveSkillDescription,
  className = '',
  title = 'Passive skill',
}) {
  if (!passiveSkillName && !passiveSkillDescription) {
    return null
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
