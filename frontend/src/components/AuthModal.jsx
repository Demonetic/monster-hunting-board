import { useEffect, useMemo, useState } from 'react'
import { getAppearanceOptions, login, register } from '../api/authApi'
import { saveRole, saveToken } from '../api/authStorage'
import panelParchment from '../assets/panel_information.png'
import panelHunt from '../assets/panel_hunt.png'
import buttonLogin from '../assets/button_login.png'
import buttonRegister from '../assets/button_register.png'
import logoRound from '../assets/logo_round.png'
import AppearanceOptionSelector from './AppearanceOptionSelector'
import PassiveSkillSummary from './PassiveSkillSummary'

const initialLoginForm = {
  username: '',
  password: '',
}

const initialRegisterForm = {
  username: '',
  password: '',
  displayName: '',
  city: '',
  appearance: 'MAGE',
}

function AuthModal({ onAuthSuccess, showToast }) {
  const [mode, setMode] = useState('login')
  const [loginForm, setLoginForm] = useState(initialLoginForm)
  const [registerForm, setRegisterForm] = useState(initialRegisterForm)
  const [errorMessage, setErrorMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [appearanceOptions, setAppearanceOptions] = useState([])
  const [appearanceLoading, setAppearanceLoading] = useState(false)
  const [appearancePreview, setAppearancePreview] = useState(initialRegisterForm.appearance)

  const activeForm = mode === 'login' ? loginForm : registerForm
  const submitButtonImage = mode === 'login' ? buttonLogin : buttonRegister
  const panelImage = mode === 'login' ? panelParchment : panelHunt
  const previewedAppearance = useMemo(
    () => appearanceOptions.find((option) => option.appearance === appearancePreview)
      ?? appearanceOptions.find((option) => option.appearance === registerForm.appearance)
      ?? null,
    [appearanceOptions, appearancePreview, registerForm.appearance],
  )

  useEffect(() => {
    let cancelled = false

    const fetchAppearanceOptions = async () => {
      setAppearanceLoading(true)

      try {
        const response = await getAppearanceOptions()
        if (!cancelled) {
          setAppearanceOptions(response.data)
          if (response.data.length > 0) {
            const fallbackAppearance = response.data.some((option) => option.appearance === initialRegisterForm.appearance)
              ? initialRegisterForm.appearance
              : response.data[0].appearance
            setRegisterForm((current) => (
              current.appearance === fallbackAppearance
                ? current
                : { ...current, appearance: fallbackAppearance }
            ))
            setAppearancePreview(fallbackAppearance)
          }
        }
      } catch {
        if (!cancelled) {
          setAppearanceOptions([])
        }
      } finally {
        if (!cancelled) {
          setAppearanceLoading(false)
        }
      }
    }

    fetchAppearanceOptions()

    return () => {
      cancelled = true
    }
  }, [])

  const handleChange = (event) => {
    const { name, value } = event.target

    if (mode === 'login') {
      setLoginForm((current) => ({ ...current, [name]: value }))
      return
    }

    setRegisterForm((current) => ({ ...current, [name]: value }))
  }

  const switchMode = (nextMode) => {
    setMode(nextMode)
    setErrorMessage('')
    setAppearancePreview(registerForm.appearance)
  }

  const handleAppearanceChange = (appearance) => {
    setRegisterForm((current) => ({ ...current, appearance }))
    setAppearancePreview(appearance)
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setErrorMessage('')
    setIsSubmitting(true)

    try {
      const response =
        mode === 'login'
          ? await login(loginForm)
          : await register(registerForm)

      saveToken(response.data.token)
      saveRole(response.data.role ?? 'HUNTER')
      const successMessage =
        mode === 'login' ? 'Logged in successfully' : 'Account created'
      showToast?.(successMessage, 'success')
      onAuthSuccess(successMessage)
    } catch (error) {
      const fallbackMessage = mode === 'login' ? 'Login failed' : 'Registration failed'
      const message = error.response?.data?.message ?? fallbackMessage
      setErrorMessage(message)
      showToast?.(message, 'error')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="auth-modal-overlay">
      <img className="auth-background-logo" src={logoRound} alt="" aria-hidden="true" />

      <section
        className={`auth-modal ${mode === 'register' ? 'is-register' : ''}`.trim()}
        style={{ backgroundImage: `url(${panelImage})` }}
      >
        <div className="auth-modal-copy">
          <h1 className="auth-modal-title">
            {mode === 'login' ? 'Enter The Hunt' : 'Forge A Hunter'}
          </h1>
        </div>

        <div className="auth-modal-switch">
          <button
            type="button"
            className={`auth-switch ${mode === 'login' ? 'is-active' : ''}`}
            onClick={() => switchMode('login')}
          >
            Login
          </button>
          <button
            type="button"
            className={`auth-switch ${mode === 'register' ? 'is-active' : ''}`}
            onClick={() => switchMode('register')}
          >
            Register
          </button>
        </div>

        <form className="auth-form" onSubmit={handleSubmit}>
          <label className="auth-field">
            <span>Username</span>
            <input
              name="username"
              value={activeForm.username}
              onChange={handleChange}
              autoComplete="username"
              required
            />
          </label>

          <label className="auth-field">
            <span>Password</span>
            <input
              type="password"
              name="password"
              value={activeForm.password}
              onChange={handleChange}
              autoComplete={
                mode === 'login' ? 'current-password' : 'new-password'
              }
              required
            />
          </label>

          {mode === 'register' && (
            <>
              <label className="auth-field">
                <span>Display Name</span>
                <input
                  name="displayName"
                  value={registerForm.displayName}
                  onChange={handleChange}
                  autoComplete="nickname"
                  required
                />
              </label>

              <div className="auth-appearance-field" aria-label="Starting appearance">
                <span className="auth-appearance-label">Starting Appearance</span>
                <AppearanceOptionSelector
                  options={appearanceOptions}
                  value={registerForm.appearance}
                  previewValue={appearancePreview}
                  onChange={handleAppearanceChange}
                  onPreviewChange={setAppearancePreview}
                />

                {appearanceLoading && (
                  <p className="auth-appearance-state">Loading appearances...</p>
                )}

                {!appearanceLoading && appearanceOptions.length === 0 && (
                  <p className="auth-appearance-state">Appearances unavailable right now.</p>
                )}

                {previewedAppearance && (
                  <PassiveSkillSummary
                    className="auth-passive-preview"
                    appearanceName={previewedAppearance.displayName}
                    passiveSkillName={previewedAppearance.passiveSkillName}
                    passiveSkillDescription={previewedAppearance.passiveSkillDescription}
                    title="Starting passive"
                  />
                )}
              </div>

              <label className="auth-field">
                <span>City</span>
                <input
                  name="city"
                  value={registerForm.city}
                  onChange={handleChange}
                  autoComplete="address-level2"
                  placeholder="Stockholm"
                />
              </label>

              <p className="weather-source-note auth-weather-source">
                Weather data from Open-Meteo
              </p>
            </>
          )}

          {errorMessage && <p className="auth-error">{errorMessage}</p>}

          <button
            type="submit"
            className={`auth-submit ${mode === 'login' ? 'is-login' : 'is-register'}`.trim()}
            disabled={isSubmitting}
            aria-label={mode === 'login' ? 'Login' : 'Register'}
          >
            <img src={submitButtonImage} alt="" />
          </button>
        </form>
      </section>
    </div>
  )
}

export default AuthModal
