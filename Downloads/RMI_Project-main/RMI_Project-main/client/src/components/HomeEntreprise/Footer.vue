<template>
  <div class="auth-container">
    <div class="auth-card">
      <div class="auth-header">
        <div class="brand">
          <span class="brand-icon">📦</span>
          <h1 class="brand-name">LogistiCo</h1>
        </div>
        <p class="welcome-text">Welcome back! Please login to your account.</p>
      </div>

      <form @submit.prevent="handleLogin" class="auth-form">
        <div class="form-group">
          <label for="identifier">
            <span class="input-icon">📧</span>
            Nom d'utilisateur
          </label>
          <input
              v-model="loginData.identifier"
              id="identifier"
              type="text"
              placeholder="Entrez votre nom d'utilisateur"
              required
          />
        </div>

        <div class="form-group">
          <label for="password">
            <span class="input-icon">🔑</span>
            Mot de passe
          </label>
          <div class="password-field">
            <input
                v-model="loginData.password"
                id="password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="Entrez votre mot de passe"
                required
            />
            <button type="button" class="toggle-password" @click="showPassword = !showPassword">
              <span class="password-icon">{{ showPassword ? '🔓' : '🔒' }}</span>
            </button>
          </div>
        </div>

        <!-- Message d'erreur -->
        <div v-if="errorMessage" class="error-banner">
          ⚠️ {{ errorMessage }}
        </div>

        <button type="submit" class="submit-button" :disabled="isLoading">
          <span class="button-content">
            <span class="button-icon">{{ isLoading ? '⌛' : '➜' }}</span>
            <span>{{ isLoading ? 'Connexion...' : 'Se connecter' }}</span>
          </span>
        </button>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const loginData    = ref({ identifier: '', password: '' })
const isLoading    = ref(false)
const showPassword = ref(false)
const errorMessage = ref('')

const handleLogin = async () => {
  if (!loginData.value.identifier || !loginData.value.password) {
    errorMessage.value = 'Veuillez remplir tous les champs.'
    return
  }

  isLoading.value    = true
  errorMessage.value = ''

  try {
    // ── Appel vers HttpBridge Java sur :8080 ──────────────────
    // POST /api/auth/login  →  { "success": true }  ou  { "success": false }
    const response = await fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: loginData.value.identifier,
        password: loginData.value.password
      })
    })

    const data = await response.json()

    if (!response.ok || !data.success) {
      // HttpBridge renvoie 401 + { "success": false } si mauvais identifiants
      errorMessage.value = 'Identifiants incorrects. Veuillez réessayer.'
      return
    }

    // ── Connexion réussie ─────────────────────────────────────
    // HttpBridge ne retourne pas de token JWT — on stocke juste le username
    // pour savoir que l'utilisateur est connecté.
    localStorage.setItem('username', loginData.value.identifier)
    localStorage.setItem('isLoggedIn', 'true')

    // Redirection vers le dashboard principal
    router.push('/dashCompany')

  } catch (error) {
    // Connexion refusée = serveur Java non démarré
    console.error('Login error:', error)
    errorMessage.value =
        'Impossible de contacter le serveur. Vérifiez que le serveur Java est démarré sur le port 8080.'
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');

.auth-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(145deg, #e0e7ff 0%, #e6e6fa 100%);
  padding: 1.5rem;
  font-family: 'Inter', sans-serif;
}

.auth-card {
  width: 80%;
  max-width: 460px;
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 0 8px 24px rgba(0,0,0,.1);
  padding: 2.5rem;
  border: 1px solid rgba(255,255,255,.2);
  transition: transform .3s ease, box-shadow .3s ease;
}
.auth-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 32px rgba(0,0,0,.15);
}

.auth-header { text-align: center; margin-bottom: 2.5rem; }
.brand { display:flex; align-items:center; justify-content:center; gap:.75rem; margin-bottom:1rem; }
.brand-icon { font-size:2.75rem; transition:transform .3s ease; }
.brand-icon:hover { transform: scale(1.1); }
.brand-name { font-size:2.25rem; font-weight:700; color:#1e3a8a; margin:0; letter-spacing:-.025em; }
.welcome-text { color:#64748b; font-size:1rem; font-weight:400; margin:0; line-height:1.5; }

.auth-form { display:flex; flex-direction:column; gap:1.75rem; }
.form-group { display:flex; flex-direction:column; gap:.5rem; }
.form-group label { display:flex; align-items:center; gap:.5rem; color:#1e3a8a; font-size:.9rem; font-weight:500; }
.input-icon { font-size:1.125rem; color:#3b82f6; }
.form-group input {
  width:90%; padding:.875rem 1.25rem;
  border:1px solid #e5e7eb; border-radius:10px;
  font-size:1rem; color:#1e3a8a; background:#f9fafb;
  transition:all .3s ease;
}
.form-group input:focus {
  outline:none; border-color:#3b82f6;
  box-shadow:0 0 0 4px rgba(59,130,246,.2); background:#fff;
}

.password-field { position:relative; }
.toggle-password {
  position:absolute; right:1rem; top:50%; transform:translateY(-50%);
  background:none; border:none; padding:0; cursor:pointer; color:#64748b;
}
.toggle-password:hover { color:#3b82f6; }

/* ── Bannière d'erreur ── */
.error-banner {
  background:#fee2e2; color:#b91c1c;
  border:1px solid #fca5a5; border-radius:8px;
  padding:.75rem 1rem; font-size:.875rem; font-weight:500;
}

.submit-button {
  width:100%; padding:1rem;
  background:linear-gradient(90deg,#3b82f6 0%,#2563eb 100%);
  color:white; border:none; border-radius:10px;
  font-size:1rem; font-weight:600; cursor:pointer;
  transition:all .3s ease;
}
.submit-button:hover:not(:disabled) {
  background:linear-gradient(90deg,#2563eb 0%,#1e40af 100%);
  transform:translateY(-2px);
  box-shadow:0 4px 12px rgba(59,130,246,.3);
}
.submit-button:disabled { opacity:.6; cursor:not-allowed; }
.button-content { display:flex; align-items:center; justify-content:center; gap:.75rem; }

@media (max-width:480px) {
  .auth-card { padding:1.75rem; border-radius:16px; }
  .brand-name { font-size:1.875rem; }
  .brand-icon { font-size:2.25rem; }
  .submit-button { padding:.875rem; }
}
</style>