import { getAppearanceCharacterImage } from '../constants/appearanceVisuals'

function AppearanceOptionSelector({
  options,
  value,
  previewValue,
  onChange,
  onPreviewChange,
  className = '',
}) {
  return (
    <div className={`appearance-option-selector ${className}`.trim()} role="group" aria-label="Appearance options">
      {options.map((option) => {
        const isSelected = option.appearance === value
        const isPreviewed = option.appearance === previewValue

        return (
          <button
            key={option.appearance}
            type="button"
            className={`appearance-option-card ${isSelected ? 'is-selected' : ''} ${isPreviewed ? 'is-previewed' : ''}`.trim()}
            onClick={() => onChange(option.appearance)}
            onMouseEnter={() => onPreviewChange(option.appearance)}
            onFocus={() => onPreviewChange(option.appearance)}
            onMouseLeave={() => onPreviewChange(value)}
            onBlur={() => onPreviewChange(value)}
            aria-pressed={isSelected}
          >
            <span className="appearance-option-card-image-wrap">
              <img
                className="appearance-option-card-image"
                src={getAppearanceCharacterImage(option.appearance)}
                alt=""
              />
            </span>
            <span className="appearance-option-card-name">{option.displayName}</span>
          </button>
        )
      })}
    </div>
  )
}

export default AppearanceOptionSelector
