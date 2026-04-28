<template>
  <div class="edit-page">

    <!-- ── HEADER ── -->
    <div class="edit-header">
      <div class="edit-header-left">
        <router-link to="/Profile" class="back-btn">
          <i class="fas fa-arrow-left"></i>
        </router-link>
        <div class="edit-title-wrap">
          <h1 class="edit-title">Modifier le profil</h1>
          <p class="edit-sub">Mettez à jour vos informations personnelles</p>
        </div>
      </div>
      <div class="edit-avatar-wrap">
        <div class="edit-avatar">{{ initials }}</div>
        <div class="edit-online"></div>
      </div>
    </div>

    <!-- ── ALERTS ── -->
    <transition name="fade">
      <div v-if="successMsg" class="alert success">
        <i class="fas fa-check-circle"></i> {{ successMsg }}
      </div>
    </transition>
    <transition name="fade">
      <div v-if="errorMsg" class="alert error">
        <i class="fas fa-exclamation-circle"></i> {{ errorMsg }}
      </div>
    </transition>

    <!-- ── GRID ── -->
    <div class="edit-grid">

      <!-- Informations personnelles -->
      <div class="edit-card">
        <div class="edit-card-head">
          <h2><i class="fas fa-user"></i> Informations personnelles</h2>
        </div>
        <div class="edit-card-body">

          <div class="field-group">
            <label class="field-label">
              <i class="fas fa-user-circle"></i> Nom d'utilisateur
            </label>
            <div class="field-input-wrap">
              <input
                  v-model="form.username"
                  type="text"
                  class="field-input"
                  placeholder="Entrez votre nom d'utilisateur"
                  :class="{ error: errors.username }"
              />
              <i class="fas fa-pencil-alt field-icon"></i>
            </div>
            <span v-if="errors.username" class="field-error">{{ errors.username }}</span>
          </div>

          <div class="field-group">
            <label class="field-label">
              <i class="fas fa-id-badge"></i> Rôle
            </label>
            <div class="field-input-wrap">
              <input
                  :value="form.role"
                  type="text"
                  class="field-input readonly"
                  readonly
              />
              <i class="fas fa-lock field-icon"></i>
            </div>
            <span class="field-hint">Le rôle ne peut pas être modifié.</span>
          </div>

        </div>
      </div>

      <!-- Sécurité -->
      <div class="edit-card">
        <div class="edit-card-head">
          <h2><i class="fas fa-lock"></i> Changer le mot de passe</h2>
        </div>
        <div class="edit-card-body">

          <div class="field-group">
            <label class="field-label">
              <i class="fas fa-key"></i> Mot de passe actuel
            </label>
            <div class="field-input-wrap">
              <input
                  v-model="form.currentPassword"
                  :type="showCurrent ? 'text' : 'password'"
                  class="field-input"
                  placeholder="••••••••"
                  :class="{ error: errors.currentPassword }"
              />
              <i
                  class="field-icon toggle-eye"
                  :class="showCurrent ? 'fas fa-eye-slash' : 'fas fa-eye'"
                  @click="showCurrent = !showCurrent"
              ></i>
            </div>
            <span v-if="errors.currentPassword" class="field-error">{{ errors.currentPassword }}</span>
          </div>

          <div class="field-group">
            <label class="field-label">
              <i class="fas fa-lock"></i> Nouveau mot de passe
            </label>
            <div class="field-input-wrap">
              <input
                  v-model="form.newPassword"
                  :type="showNew ? 'text' : 'password'"
                  class="field-input"
                  placeholder="Minimum 6 caractères"
                  :class="{ error: errors.newPassword }"
              />
              <i
                  class="field-icon toggle-eye"
                  :class="showNew ? 'fas fa-eye-slash' : 'fas fa-eye'"
                  @click="showNew = !showNew"
              ></i>
            </div>
            <span v-if="errors.newPassword" class="field-error">{{ errors.newPassword }}</span>

            <!-- Strength bar -->
            <div v-if="form.newPassword" class="strength-wrap">
              <div class="strength-bar">
                <div class="strength-fill" :class="strengthClass" :style="{ width: strengthPct + '%' }"></div>
              </div>
              <span class="strength-label" :class="strengthClass">{{ strengthLabel }}</span>
            </div>
          </div>

          <div class="field-group">
            <label class="field-label">
              <i class="fas fa-check-double"></i> Confirmer le nouveau mot de passe
            </label>
            <div class="field-input-wrap">
              <input
                  v-model="form.confirmPassword"
                  :type="showConfirm ? 'text' : 'password'"
                  class="field-input"
                  placeholder="Répétez le mot de passe"
                  :class="{ error: errors.confirmPassword }"
              />
              <i
                  class="field-icon toggle-eye"
                  :class="showConfirm ? 'fas fa-eye-slash' : 'fas fa-eye'"
                  @click="showConfirm = !showConfirm"
              ></i>
            </div>
            <span v-if="errors.confirmPassword" class="field-error">{{ errors.confirmPassword }}</span>
          </div>

        </div>
      </div>

    </div>

    <!-- ── ACTIONS ── -->
    <div class="edit-actions">
      <router-link to="/Profile" class="btn-cancel">
        <i class="fas fa-times"></i> Annuler
      </router-link>
      <button class="btn-save" :disabled="saving" @click="handleSave">
        <i v-if="saving" class="fas fa-spinner fa-spin"></i>
        <i v-else class="fas fa-save"></i>
        {{ saving ? 'Enregistrement...' : 'Enregistrer les modifications' }}
      </button>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const username = ref(localStorage.getItem('username') || 'Utilisateur')

const form = ref({
  username: username.value,
  role: 'CHEF_ENTREPOT',
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const errors       = ref<Record<string, string>>({})
const successMsg   = ref('')
const errorMsg     = ref('')
const saving       = ref(false)
const showCurrent  = ref(false)
const showNew      = ref(false)
const showConfirm  = ref(false)

const initials = computed(() => {
  const u = form.value.username
  return u.length >= 2 ? u.slice(0, 2).toUpperCase() : u.toUpperCase()
})

/* ── Password strength ── */
const strengthPct = computed(() => {
  const p = form.value.newPassword
  if (!p) return 0
  let s = 0
  if (p.length >= 6)  s += 25
  if (p.length >= 10) s += 25
  if (/[A-Z]/.test(p) && /[a-z]/.test(p)) s += 25
  if (/[0-9!@#$%^&*]/.test(p)) s += 25
  return s
})
const strengthClass = computed(() => {
  const v = strengthPct.value
  if (v <= 25) return 'weak'
  if (v <= 50) return 'fair'
  if (v <= 75) return 'good'
  return 'strong'
})
const strengthLabel = computed(() => {
  const map: Record<string, string> = { weak: 'Faible', fair: 'Moyen', good: 'Bon', strong: 'Fort' }
  return map[strengthClass.value]
})

/* ── Validation ── */
function validate() {
  errors.value = {}
  if (!form.value.username.trim()) {
    errors.value.username = "Le nom d'utilisateur est requis."
  }
  const changingPwd = form.value.currentPassword || form.value.newPassword || form.value.confirmPassword
  if (changingPwd) {
    if (!form.value.currentPassword) {
      errors.value.currentPassword = 'Entrez votre mot de passe actuel.'
    }
    if (!form.value.newPassword || form.value.newPassword.length < 6) {
      errors.value.newPassword = 'Le nouveau mot de passe doit contenir au moins 6 caractères.'
    }
    if (form.value.newPassword !== form.value.confirmPassword) {
      errors.value.confirmPassword = 'Les mots de passe ne correspondent pas.'
    }
  }
  return Object.keys(errors.value).length === 0
}

/* ── Save ── */
async function handleSave() {
  successMsg.value = ''
  errorMsg.value   = ''
  if (!validate()) return

  saving.value = true
  try {
    // ── Update username ──
    if (form.value.username !== username.value) {
      const res = await fetch('http://localhost:8080/api/utilisateurs/username', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: form.value.username }),
      })
      if (!res.ok) throw new Error('Erreur lors de la mise à jour du nom.')
      localStorage.setItem('username', form.value.username)
      username.value = form.value.username
    }

    // ── Update password ──
    const changingPwd = form.value.currentPassword && form.value.newPassword
    if (changingPwd) {
      const res = await fetch('http://localhost:8080/api/utilisateurs/password', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          currentPassword: form.value.currentPassword,
          newPassword:     form.value.newPassword,
        }),
      })
      if (!res.ok) {
        const data = await res.json().catch(() => ({}))
        throw new Error(data.message || 'Mot de passe actuel incorrect.')
      }
      form.value.currentPassword = ''
      form.value.newPassword     = ''
      form.value.confirmPassword = ''
    }

    successMsg.value = 'Profil mis à jour avec succès !'
    setTimeout(() => router.push('/Profile'), 1500)
  } catch (e: any) {
    errorMsg.value = e.message || 'Une erreur est survenue.'
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&family=DM+Mono:wght@500&display=swap');

.edit-page {
  padding: 2rem;
  max-width: 900px;
  margin: 0 auto;
  min-height: 100vh;
  background: #f0f4f8;
  font-family: 'DM Sans', sans-serif;
}

/* ── HEADER ── */
.edit-header {
  background: linear-gradient(135deg, #1e3a8a 0%, #2563eb 100%);
  border-radius: 1.25rem;
  padding: 2rem 2.5rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.5rem;
  color: white;
  position: relative;
  overflow: hidden;
}
.edit-header::before {
  content: '';
  position: absolute;
  right: -3rem; top: -3rem;
  width: 12rem; height: 12rem;
  border-radius: 50%;
  background: rgba(255,255,255,.07);
}

.edit-header-left {
  display: flex;
  align-items: center;
  gap: 1.25rem;
}

.back-btn {
  width: 2.75rem; height: 2.75rem;
  border-radius: .75rem;
  background: rgba(255,255,255,.15);
  border: 1.5px solid rgba(255,255,255,.3);
  display: flex; align-items: center; justify-content: center;
  color: white; font-size: 1rem;
  text-decoration: none;
  transition: background .2s, transform .2s;
  flex-shrink: 0; z-index: 1;
}
.back-btn:hover {
  background: rgba(255,255,255,.25);
  transform: translateX(-2px);
}

.edit-title { font-size: 1.5rem; font-weight: 700; margin: 0 0 .2rem; letter-spacing: -.03em; }
.edit-sub   { margin: 0; font-size: .82rem; color: rgba(255,255,255,.7); }

.edit-avatar-wrap { position: relative; flex-shrink: 0; z-index: 1; }
.edit-avatar {
  width: 4.5rem; height: 4.5rem;
  border-radius: 1rem;
  background: rgba(255,255,255,.2);
  border: 2px solid rgba(255,255,255,.4);
  display: flex; align-items: center; justify-content: center;
  font-size: 1.5rem; font-weight: 700; color: white;
  letter-spacing: -.05em;
}
.edit-online {
  position: absolute; bottom: -.2rem; right: -.2rem;
  width: 1rem; height: 1rem;
  background: #22c55e; border-radius: 50%;
  border: 2px solid white;
}

/* ── ALERTS ── */
.alert {
  display: flex; align-items: center; gap: .6rem;
  padding: .9rem 1.25rem;
  border-radius: .75rem;
  font-size: .875rem; font-weight: 600;
  margin-bottom: 1rem;
}
.alert.success { background: #dcfce7; color: #15803d; border: 1px solid #bbf7d0; }
.alert.error   { background: #fee2e2; color: #dc2626; border: 1px solid #fecaca; }

.fade-enter-active, .fade-leave-active { transition: opacity .3s, transform .3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateY(-6px); }

/* ── GRID ── */
.edit-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.25rem;
  margin-bottom: 1.25rem;
}

/* ── CARDS ── */
.edit-card {
  background: white;
  border-radius: 1rem;
  box-shadow: 0 1px 4px rgba(0,0,0,.06);
  overflow: hidden;
}
.edit-card-head {
  padding: 1rem 1.25rem;
  border-bottom: 1px solid #f1f5f9;
  background: #fafbfc;
}
.edit-card-head h2 {
  margin: 0; font-size: .95rem; font-weight: 700; color: #0f172a;
  display: flex; align-items: center; gap: .5rem;
}
.edit-card-head h2 i { color: #64748b; }
.edit-card-body {
  padding: 1.25rem;
  display: flex; flex-direction: column; gap: 1rem;
}

/* ── FIELDS ── */
.field-group { display: flex; flex-direction: column; gap: .4rem; }

.field-label {
  font-size: .82rem; font-weight: 600; color: #475569;
  display: flex; align-items: center; gap: .4rem;
}
.field-label i { color: #94a3b8; font-size: .75rem; }

.field-input-wrap { position: relative; }

.field-input {
  width: 100%;
  padding: .7rem 2.5rem .7rem .9rem;
  border: 1.5px solid #e2e8f0;
  border-radius: .6rem;
  font-size: .875rem;
  font-family: 'DM Sans', sans-serif;
  color: #0f172a;
  background: white;
  transition: border-color .2s, box-shadow .2s;
  box-sizing: border-box;
  outline: none;
}
.field-input::placeholder { color: #cbd5e1; }
.field-input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37,99,235,.12);
}
.field-input.error {
  border-color: #f87171;
  box-shadow: 0 0 0 3px rgba(248,113,113,.12);
}
.field-input.readonly {
  background: #f8fafc;
  color: #94a3b8;
  cursor: not-allowed;
}

.field-icon {
  position: absolute;
  right: .85rem; top: 50%;
  transform: translateY(-50%);
  color: #cbd5e1;
  font-size: .8rem;
  pointer-events: none;
}
.toggle-eye {
  pointer-events: all;
  cursor: pointer;
  transition: color .2s;
}
.toggle-eye:hover { color: #2563eb; }

.field-error { font-size: .75rem; color: #dc2626; font-weight: 500; }
.field-hint  { font-size: .75rem; color: #94a3b8; }

/* ── STRENGTH BAR ── */
.strength-wrap {
  display: flex; align-items: center; gap: .75rem;
  margin-top: .35rem;
}
.strength-bar {
  flex: 1; height: .35rem;
  background: #e2e8f0; border-radius: 9999px;
  overflow: hidden;
}
.strength-fill {
  height: 100%; border-radius: 9999px;
  transition: width .4s ease, background .4s;
}
.strength-fill.weak   { background: #f87171; }
.strength-fill.fair   { background: #fb923c; }
.strength-fill.good   { background: #facc15; }
.strength-fill.strong { background: #4ade80; }
.strength-label {
  font-size: .72rem; font-weight: 700;
  min-width: 3rem; text-align: right;
}
.strength-label.weak   { color: #dc2626; }
.strength-label.fair   { color: #ea580c; }
.strength-label.good   { color: #ca8a04; }
.strength-label.strong { color: #16a34a; }

/* ── ACTIONS ── */
.edit-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: .75rem;
}
.btn-cancel {
  display: inline-flex; align-items: center; gap: .5rem;
  padding: .75rem 1.5rem;
  background: white; color: #475569;
  border: 1.5px solid #e2e8f0;
  border-radius: .75rem; font-size: .875rem; font-weight: 600;
  text-decoration: none;
  transition: background .2s, border-color .2s;
  font-family: 'DM Sans', sans-serif;
}
.btn-cancel:hover { background: #f8fafc; border-color: #cbd5e1; }

.btn-save {
  display: inline-flex; align-items: center; gap: .5rem;
  padding: .75rem 1.75rem;
  background: linear-gradient(135deg, #1e3a8a, #2563eb);
  color: white;
  border: none; border-radius: .75rem;
  font-size: .875rem; font-weight: 700;
  cursor: pointer;
  font-family: 'DM Sans', sans-serif;
  transition: opacity .2s, transform .2s, box-shadow .2s;
}
.btn-save:hover:not(:disabled) {
  opacity: .92;
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(37,99,235,.3);
}
.btn-save:disabled { opacity: .6; cursor: not-allowed; }

/* ── RESPONSIVE ── */
@media (max-width: 768px) {
  .edit-page  { padding: 1rem; }
  .edit-grid  { grid-template-columns: 1fr; }
  .edit-header { padding: 1.5rem; }
}
@media (max-width: 480px) {
  .edit-actions { flex-direction: column; }
  .btn-cancel, .btn-save { width: 100%; justify-content: center; }
}
</style>