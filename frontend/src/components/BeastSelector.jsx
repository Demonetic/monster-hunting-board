function BeastSelector({
  options,
  selectedIds,
  onToggle,
  disabled = false,
  className = '',
}) {
  return (
    <div className={`beast-selector ${className}`.trim()} role="listbox" aria-multiselectable="true">
      {options.map((option) => {
        const selected = selectedIds.includes(option.id)

        return (
          <button
            key={option.id}
            type="button"
            className={`beast-selector-option ${selected ? 'is-selected' : ''}`.trim()}
            onClick={() => onToggle(option.id)}
            disabled={disabled}
            aria-pressed={selected}
          >
            <span className="beast-selector-label">{option.label}</span>
          </button>
        )
      })}
    </div>
  )
}

export default BeastSelector
