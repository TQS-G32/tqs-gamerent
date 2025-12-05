import { useState } from 'react';
import './AuthPage.css';
import { LoginForm, RegisterForm } from './AuthForms.jsx';

export default function AuthPage({ onAuth }) {
  const [mode, setMode] = useState('login');
  return (
    <div className="auth-bg">
      <div className="auth-card">
        <div className="auth-header">
          <h2>{mode === 'login' ? 'LOGIN' : 'REGISTER'}</h2>
        </div>
        <div className="auth-toggle-row">
          <button className={mode === 'login' ? 'auth-toggle active' : 'auth-toggle'} onClick={() => setMode('login')}>Login</button>
          <button className={mode === 'register' ? 'auth-toggle active' : 'auth-toggle'} onClick={() => setMode('register')}>Register</button>
        </div>
        <div className="auth-form-wrapper">
          {mode === 'login' ? (
            <LoginForm onLogin={(user) => onAuth && onAuth(user)} />
          ) : (
            <RegisterForm onRegister={(user) => onAuth && onAuth(user)} />
          )}
        </div>
      </div>
    </div>
  );
}
