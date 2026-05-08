import { useState } from 'react'
import { login, register } from '../api/authApi'
import { saveRole, saveToken } from '../api/authStorage'
import panelParchment from '../assets/panel_information.png'
import buttonLogin from '../assets/button_login.png'
import buttonRegister from '../assets/button_register.png'

const initialLoginForm = {
  username: '',
  password: '',
}

const initialRegisterForm = {
  username: '',
  password: '',
  displayName: '',
  appearance: 'MAGE',
}

const appearanceOptions = ['BARD', 'MAGE', 'RANGER', 'KNIGHT', 'PALADIN', 'HUNTER']

function AuthModal({ onAuthSuccess, showToast }) {
  const [mode, setMode] = useState('login')
  const [loginForm, setLoginForm] = useState(initialLoginForm)
  const [registerForm, setRegisterForm] = useState(initialRegisterForm)
  const [errorMessage, setErrorMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const activeForm = mode === 'login' ? loginForm : registerForm
  const submitButtonImage = mode === 'login' ? buttonLogin : buttonRegister

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
      <section
        className={`auth-modal ${mode === 'register' ? 'is-register' : ''}`.trim()}
        style={{ backgroundImage: `url(${panelParchment})` }}
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

              <label className="auth-field">
                <span>Appearance</span>
                <select
                  name="appearance"
                  value={registerForm.appearance}
                  onChange={handleChange}
                >
                  {appearanceOptions.map((appearance) => (
                    <option key={appearance} value={appearance}>
                      {appearance}
                    </option>
                  ))}
                </select>
              </label>
            </>
          )}

          {errorMessage && <p className="auth-error">{errorMessage}</p>}

          <button
            type="submit"
            className="auth-submit"
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
