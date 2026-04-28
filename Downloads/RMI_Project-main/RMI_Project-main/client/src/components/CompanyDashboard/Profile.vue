<template>
  <div class="profile-page">

    <!-- ── HEADER ── -->
    <div class="prof-header">
      <div class="prof-avatar-wrap">
        <div class="prof-avatar">
          {{ initials }}
        </div>
        <div class="prof-online"></div>
      </div>
      <div class="prof-header-info">
        <h1 class="prof-name">{{ username }}</h1>
        <span class="prof-role"><i class="fas fa-shield-alt"></i> {{ role }}</span>
        <p class="prof-joined"><i class="fas fa-calendar-alt"></i> Connecté depuis cette session</p>
      </div>
      <router-link to="/Edit" class="prof-edit-btn">
        <i class="fas fa-edit"></i> Modifier le profil
      </router-link>
    </div>

    <!-- ── STATS ── -->
    <div class="prof-stats">
      <div class="prof-stat-card">
        <div class="prof-stat-icon blue"><i class="fas fa-box"></i></div>
        <div>
          <div class="prof-stat-val">{{ stats.nbProduits }}</div>
          <div class="prof-stat-label">Produits</div>
        </div>
      </div>
      <div class="prof-stat-card">
        <div class="prof-stat-icon green"><i class="fas fa-warehouse"></i></div>
        <div>
          <div class="prof-stat-val">{{ stats.nbEntrepots }}</div>
          <div class="prof-stat-label">Entrepôts</div>
        </div>
      </div>
      <div class="prof-stat-card">
        <div class="prof-stat-icon orange"><i class="fas fa-cubes"></i></div>
        <div>
          <div class="prof-stat-val">{{ stats.stockTotal }}</div>
          <div class="prof-stat-label">Unités en stock</div>
        </div>
      </div>
      <div class="prof-stat-card">
        <div class="prof-stat-icon red"><i class="fas fa-bell"></i></div>
        <div>
          <div class="prof-stat-val">{{ stats.alertes }}</div>
          <div class="prof-stat-label">Alertes stock bas</div>
        </div>
      </div>
    </div>

    <!-- ── MAIN ── -->
    <div class="prof-grid">

      <!-- Infos personnelles -->
      <div class="prof-card">
        <div class="prof-card-head">
          <h2><i class="fas fa-user"></i> Informations personnelles</h2>
        </div>
        <div class="prof-card-body">
          <div class="prof-info-row">
            <span class="prof-info-label"><i class="fas fa-user-circle"></i> Nom d'utilisateur</span>
            <span class="prof-info-val">{{ username }}</span>
          </div>
          <div class="prof-info-row">
            <span class="prof-info-label"><i class="fas fa-id-badge"></i> Rôle</span>
            <span class="prof-info-val">
              <span class="role-badge">{{ role }}</span>
            </span>
          </div>
          <div class="prof-info-row">
            <span class="prof-info-label"><i class="fas fa-toggle-on"></i> Statut</span>
            <span class="prof-info-val">
              <span class="status-badge active">
                <span class="status-dot"></span> Connecté
              </span>
            </span>
          </div>
        </div>
      </div>

      <!-- Sécurité -->
      <div class="prof-card">
        <div class="prof-card-head">
          <h2><i class="fas fa-lock"></i> Sécurité</h2>
        </div>
        <div class="prof-card-body">
          <div class="prof-info-row">
            <span class="prof-info-label"><i class="fas fa-key"></i> Mot de passe</span>
            <span class="prof-info-val">••••••••</span>
          </div>
          <div class="prof-info-row">
            <span class="prof-info-label"><i class="fas fa-sign-in-alt"></i> Dernière connexion</span>
            <span class="prof-info-val">{{ sessionStart }}</span>
          </div>
          <div class="prof-security-actions">
            <router-link to="/Edit" class="sec-btn">
              <i class="fas fa-key"></i> Changer le mot de passe
            </router-link>
          </div>
        </div>
      </div>

      <!-- Activité récente -->
      <div class="prof-card span2">
        <div class="prof-card-head">
          <h2><i class="fas fa-history"></i> Activité récente</h2>
          <select v-model="selectedEntrepot" @change="fetchHistorique" class="act-select">
            <option value="">— Sélectionner un entrepôt —</option>
            <option v-for="e in entrepots" :key="e.id" :value="e.id">{{ e.nom }}</option>
          </select>
        </div>
        <div class="prof-card-body">
          <div v-if="loadingMvt" class="prof-loading">
            <i class="fas fa-spinner fa-spin"></i>
          </div>
          <div v-else-if="!mouvements.length" class="prof-empty">
            <i class="fas fa-inbox"></i>
            <p>Sélectionnez un entrepôt pour voir l'activité</p>
          </div>
          <div v-else class="act-list">
            <div v-for="(m, i) in mouvements.slice(0, 8)" :key="i" class="act-item">
              <div class="act-icon" :class="m.type.toLowerCase()">
                <i :class="mvtIcon(m.type)"></i>
              </div>
              <div class="act-info">
                <div class="act-title">
                  <strong>{{ m.type }}</strong> —
                  {{ getProduitNom(m.idProduit) }}
                </div>
                <div class="act-sub">{{ m.note || 'Aucune note' }}</div>
              </div>
              <div class="act-right">
                <div class="act-qty" :class="m.type.toLowerCase()">
                  {{ m.type === 'SORTIE' ? '-' : '+' }}{{ m.quantite }}
                </div>
                <div class="act-date">{{ formatDate(m.date) }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'

interface Produit  { id: string; nom: string; categorie?: string; prixUnitaire: number }
interface Entrepot { id: string; nom: string }
interface Mouvement { idProduit: string; idEntrepot: string; type: string; quantite: number; note: string; date: string }

const BASE = 'http://localhost:8080/api'
async function api<T>(path: string): Promise<T> {
  const r = await fetch(BASE + path)
  if (!r.ok) throw new Error(`HTTP ${r.status}`)
  return r.json()
}

const username        = ref(localStorage.getItem('username') || 'Utilisateur')
const role            = ref('CHEF_ENTREPOT')
const sessionStart    = ref(new Date().toLocaleString('fr-FR'))
const produits        = ref<Produit[]>([])
const entrepots       = ref<Entrepot[]>([])
const mouvements      = ref<Mouvement[]>([])
const selectedEntrepot = ref('')
const loadingMvt      = ref(false)
const stats           = ref({ nbProduits: 0, nbEntrepots: 0, stockTotal: 0, alertes: 0 })

const initials = computed(() => {
  const u = username.value
  return u.length >= 2 ? u.slice(0, 2).toUpperCase() : u.toUpperCase()
})

function getProduitNom(id: string) {
  return produits.value.find(p => p.id === id)?.nom ?? id
}
function formatDate(d: string) {
  if (!d) return '—'
  try { return new Date(d).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' }) }
  catch { return d }
}
function mvtIcon(type: string) {
  const icons: Record<string, string> = {
    ENTREE: 'fas fa-arrow-down', SORTIE: 'fas fa-arrow-up', TRANSFERT: 'fas fa-exchange-alt'
  }
  return icons[type] ?? 'fas fa-circle'
}

async function fetchHistorique() {
  if (!selectedEntrepot.value) { mouvements.value = []; return }
  loadingMvt.value = true
  try {
    const data = await api<Mouvement[]>(`/stocks/historique?idEntrepot=${selectedEntrepot.value}`)
    mouvements.value = Array.isArray(data) ? data.slice().reverse() : []
  } catch { mouvements.value = [] }
  finally { loadingMvt.value = false }
}

onMounted(async () => {
  try {
    const [prods, entrs] = await Promise.all([
      api<Produit[]>('/produits'),
      api<{id:string,nom:string}[]>('/entrepots')
    ])
    produits.value = prods
    entrepots.value = entrs.map(r => {
      const match = r.nom.match(/^(.*?)\s*\(([^)]+)\)$/)
      return { id: r.id, nom: match ? match[1].trim() : r.nom }
    })

    // Stocks total
    let total = 0
    await Promise.all(entrepots.value.map(async e => {
      try {
        const d = await api<{stocks:{quantite:number}[]}>(`/stocks/entrepot?id=${e.id}`)
        total += (d.stocks ?? []).reduce((s, st) => s + st.quantite, 0)
      } catch {}
    }))

    // Alertes
    const alertData = await api<{alertes:string[]}>('/stocks/alertes?seuil=10')

    stats.value = {
      nbProduits:  prods.length,
      nbEntrepots: entrepots.value.length,
      stockTotal:  total,
      alertes:     alertData.alertes?.length ?? 0
    }
  } catch (e) { console.error(e) }
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&family=DM+Mono:wght@500&display=swap');

.profile-page {
  padding: 2rem;
  max-width: 1100px;
  margin: 0 auto;
  min-height: 100vh;
  background: #f0f4f8;
  font-family: 'DM Sans', sans-serif;
}

/* ── HEADER ── */
.prof-header {
  background: linear-gradient(135deg, #1e3a8a 0%, #2563eb 100%);
  border-radius: 1.25rem;
  padding: 2rem 2.5rem;
  display: flex;
  align-items: center;
  gap: 2rem;
  margin-bottom: 1.5rem;
  color: white;
  position: relative;
  overflow: hidden;
  flex-wrap: wrap;
}
.prof-header::before {
  content: '';
  position: absolute;
  right: -3rem; top: -3rem;
  width: 12rem; height: 12rem;
  border-radius: 50%;
  background: rgba(255,255,255,.07);
}
.prof-header::after {
  content: '';
  position: absolute;
  right: 5rem; bottom: -5rem;
  width: 18rem; height: 18rem;
  border-radius: 50%;
  background: rgba(255,255,255,.05);
}

.prof-avatar-wrap { position: relative; flex-shrink: 0; }
.prof-avatar {
  width: 5rem; height: 5rem;
  border-radius: 1rem;
  background: rgba(255,255,255,.2);
  border: 2px solid rgba(255,255,255,.4);
  display: flex; align-items: center; justify-content: center;
  font-size: 1.75rem; font-weight: 700; color: white;
  letter-spacing: -.05em;
}
.prof-online {
  position: absolute; bottom: -.2rem; right: -.2rem;
  width: 1rem; height: 1rem;
  background: #22c55e; border-radius: 50%;
  border: 2px solid white;
}

.prof-header-info { flex: 1; min-width: 0; }
.prof-name {
  font-size: 1.75rem; font-weight: 700; margin: 0 0 .4rem;
  letter-spacing: -.03em;
}
.prof-role {
  display: inline-flex; align-items: center; gap: .4rem;
  background: rgba(255,255,255,.15);
  padding: .25rem .75rem; border-radius: 9999px;
  font-size: .8rem; font-weight: 600;
  margin-bottom: .5rem;
}
.prof-joined {
  font-size: .8rem; color: rgba(255,255,255,.7);
  margin: 0; display: flex; align-items: center; gap: .4rem;
}

.prof-edit-btn {
  display: inline-flex; align-items: center; gap: .5rem;
  padding: .75rem 1.5rem;
  background: white; color: #2563eb;
  border-radius: .75rem; font-weight: 700;
  font-size: .875rem; text-decoration: none;
  transition: transform .2s, box-shadow .2s;
  white-space: nowrap; z-index: 1;
}
.prof-edit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0,0,0,.15);
}

/* ── STATS ── */
.prof-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
  margin-bottom: 1.5rem;
}
.prof-stat-card {
  background: white; border-radius: 1rem;
  padding: 1.25rem 1.5rem;
  box-shadow: 0 1px 4px rgba(0,0,0,.06);
  display: flex; align-items: center; gap: 1rem;
  transition: transform .2s;
}
.prof-stat-card:hover { transform: translateY(-2px); }
.prof-stat-icon {
  width: 2.75rem; height: 2.75rem; border-radius: .75rem;
  display: flex; align-items: center; justify-content: center;
  font-size: 1.1rem; flex-shrink: 0;
}
.prof-stat-icon.blue   { background: #dbeafe; color: #2563eb; }
.prof-stat-icon.green  { background: #dcfce7; color: #16a34a; }
.prof-stat-icon.orange { background: #fef3c7; color: #d97706; }
.prof-stat-icon.red    { background: #fee2e2; color: #dc2626; }
.prof-stat-val   { font-size: 1.6rem; font-weight: 700; color: #0f172a; font-family: 'DM Mono', monospace; line-height: 1; }
.prof-stat-label { font-size: .75rem; color: #64748b; font-weight: 500; margin-top: .2rem; }

/* ── GRID ── */
.prof-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.25rem;
}
.span2 { grid-column: span 2; }

.prof-card {
  background: white; border-radius: 1rem;
  box-shadow: 0 1px 4px rgba(0,0,0,.06);
  overflow: hidden;
}
.prof-card-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid #f1f5f9;
  background: #fafbfc;
  flex-wrap: wrap; gap: .5rem;
}
.prof-card-head h2 {
  margin: 0; font-size: .95rem; font-weight: 700; color: #0f172a;
  display: flex; align-items: center; gap: .5rem;
}
.prof-card-head h2 i { color: #64748b; }
.prof-card-body { padding: 1.25rem; }

/* ── INFO ROWS ── */
.prof-info-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: .85rem 0;
  border-bottom: 1px solid #f8fafc;
}
.prof-info-row:last-child { border-bottom: none; }
.prof-info-label {
  font-size: .85rem; color: #64748b; font-weight: 500;
  display: flex; align-items: center; gap: .5rem;
}
.prof-info-val { font-size: .875rem; color: #0f172a; font-weight: 600; }

.role-badge {
  background: #dbeafe; color: #1d4ed8;
  padding: .2rem .75rem; border-radius: 9999px;
  font-size: .75rem; font-weight: 700;
}
.status-badge.active {
  display: inline-flex; align-items: center; gap: .4rem;
  background: #dcfce7; color: #16a34a;
  padding: .2rem .75rem; border-radius: 9999px;
  font-size: .75rem; font-weight: 700;
}
.status-dot {
  width: .45rem; height: .45rem;
  border-radius: 50%; background: #16a34a;
  animation: pulse 2s infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; } 50% { opacity: .4; }
}

.prof-security-actions { margin-top: 1rem; }
.sec-btn {
  display: inline-flex; align-items: center; gap: .5rem;
  padding: .6rem 1.2rem;
  background: #0f172a; color: white;
  border-radius: .6rem; font-size: .85rem; font-weight: 600;
  text-decoration: none; transition: background .2s;
}
.sec-btn:hover { background: #1e293b; }

/* ── ACTIVITÉ ── */
.act-select {
  padding: .4rem .75rem; border: 1px solid #e2e8f0;
  border-radius: .5rem; font-size: .82rem; background: white;
  cursor: pointer; font-family: inherit;
}
.act-select:focus { outline: none; border-color: #2563eb; }

.prof-loading {
  display: flex; align-items: center; justify-content: center;
  padding: 2rem; color: #94a3b8; font-size: 1.5rem;
}
.prof-empty {
  display: flex; flex-direction: column; align-items: center;
  padding: 2rem; color: #94a3b8; gap: .5rem;
}
.prof-empty i { font-size: 2rem; }
.prof-empty p { margin: 0; font-size: .875rem; }

.act-list { display: flex; flex-direction: column; gap: .6rem; }
.act-item {
  display: flex; align-items: center; gap: 1rem;
  padding: .85rem 1rem;
  background: #f8fafc; border-radius: .75rem;
  transition: background .15s;
}
.act-item:hover { background: #f1f5f9; }

.act-icon {
  width: 2.5rem; height: 2.5rem; border-radius: .6rem;
  display: flex; align-items: center; justify-content: center;
  font-size: .9rem; flex-shrink: 0;
}
.act-icon.entree    { background: #dcfce7; color: #16a34a; }
.act-icon.sortie    { background: #fee2e2; color: #dc2626; }
.act-icon.transfert { background: #ede9fe; color: #7c3aed; }

.act-info { flex: 1; min-width: 0; }
.act-title { font-size: .85rem; font-weight: 600; color: #0f172a; }
.act-sub   { font-size: .75rem; color: #94a3b8; margin-top: .15rem; }

.act-right { text-align: right; flex-shrink: 0; }
.act-qty {
  font-size: .9rem; font-weight: 700;
  font-family: 'DM Mono', monospace;
}
.act-qty.entree    { color: #16a34a; }
.act-qty.sortie    { color: #dc2626; }
.act-qty.transfert { color: #7c3aed; }
.act-date { font-size: .72rem; color: #94a3b8; margin-top: .15rem; }

/* ── RESPONSIVE ── */
@media (max-width: 768px) {
  .profile-page { padding: 1rem; }
  .prof-grid { grid-template-columns: 1fr; }
  .span2 { grid-column: span 1; }
  .prof-stats { grid-template-columns: 1fr 1fr; }
  .prof-header { padding: 1.5rem; }
}
@media (max-width: 480px) {
  .prof-stats { grid-template-columns: 1fr; }
  .prof-name { font-size: 1.25rem; }
}
</style>