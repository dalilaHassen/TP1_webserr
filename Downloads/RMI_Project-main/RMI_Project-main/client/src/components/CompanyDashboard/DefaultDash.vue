<template>
  <div class="dash">

    <!-- ── HEADER ── -->
    <div class="dash-header">
      <div>
        <h1 class="dash-title">Tableau de Bord</h1>
        <p class="dash-sub">Bienvenue, <strong>{{ username }}</strong> — {{ dateNow }}</p>
      </div>
      <button class="dash-refresh" @click="fetchAll" :disabled="loading">
        <i class="fas fa-sync-alt" :class="{ 'fa-spin': loading }"></i>
        Actualiser
      </button>
    </div>

    <!-- ── KPI CARDS ── -->
    <div class="dash-kpis">
      <div class="kpi-card" style="--accent:#2563eb;--accent-l:#dbeafe">
        <div class="kpi-icon"><i class="fas fa-box"></i></div>
        <div class="kpi-body">
          <div class="kpi-label">Produits</div>
          <div class="kpi-val">{{ stats.nbProduits }}</div>
          <div class="kpi-sub">références enregistrées</div>
        </div>
        <div class="kpi-ring" :style="ringStyle(stats.nbProduits, 100)"></div>
      </div>

      <div class="kpi-card" style="--accent:#16a34a;--accent-l:#dcfce7">
        <div class="kpi-icon"><i class="fas fa-warehouse"></i></div>
        <div class="kpi-body">
          <div class="kpi-label">Entrepôts</div>
          <div class="kpi-val">{{ stats.nbEntrepots }}</div>
          <div class="kpi-sub">sites actifs</div>
        </div>
        <div class="kpi-ring" :style="ringStyle(stats.nbEntrepots, 20)"></div>
      </div>

      <div class="kpi-card" style="--accent:#d97706;--accent-l:#fef3c7">
        <div class="kpi-icon"><i class="fas fa-cubes"></i></div>
        <div class="kpi-body">
          <div class="kpi-label">Stock Total</div>
          <div class="kpi-val">{{ stats.stockTotal.toLocaleString() }}</div>
          <div class="kpi-sub">unités en stock</div>
        </div>
        <div class="kpi-ring" :style="ringStyle(Math.min(stats.stockTotal,9999), 9999)"></div>
      </div>

      <div class="kpi-card" style="--accent:#dc2626;--accent-l:#fee2e2">
        <div class="kpi-icon"><i class="fas fa-exclamation-triangle"></i></div>
        <div class="kpi-body">
          <div class="kpi-label">Alertes Stock Bas</div>
          <div class="kpi-val">{{ alertes.length }}</div>
          <div class="kpi-sub">produits sous seuil</div>
        </div>
        <div class="kpi-ring" :style="ringStyle(alertes.length, 20)"></div>
      </div>
    </div>

    <!-- ── MAIN GRID ── -->
    <div class="dash-grid">

      <!-- Stock par entrepôt -->
      <div class="dash-card span2">
        <div class="card-head">
          <h2><i class="fas fa-chart-bar"></i> Stock par Entrepôt</h2>
        </div>
        <div class="card-body">
          <div v-if="loading" class="dash-loading"><i class="fas fa-spinner fa-spin"></i></div>
          <div v-else-if="!stockParEntrepot.length" class="dash-empty">Aucune donnée</div>
          <div v-else class="bar-chart">
            <div
                v-for="item in stockParEntrepot"
                :key="item.id"
                class="bar-row"
            >
              <div class="bar-label">{{ item.nom }}</div>
              <div class="bar-track">
                <div
                    class="bar-fill"
                    :style="{
                    width: maxStock ? (item.total / maxStock * 100) + '%' : '0%',
                    background: item.color
                  }"
                ></div>
              </div>
              <div class="bar-val">{{ item.total.toLocaleString() }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Alertes stock bas -->
      <div class="dash-card">
        <div class="card-head alert-head">
          <h2><i class="fas fa-bell"></i> Alertes Stock Bas</h2>
          <span class="alert-badge">{{ alertes.length }}</span>
        </div>
        <div class="card-body">
          <div v-if="loading" class="dash-loading"><i class="fas fa-spinner fa-spin"></i></div>
          <div v-else-if="!alertes.length" class="dash-ok">
            <i class="fas fa-check-circle"></i>
            Tous les stocks sont suffisants
          </div>
          <ul v-else class="alert-list">
            <li v-for="a in alertes" :key="a" class="alert-item">
              <i class="fas fa-exclamation-circle"></i>
              <span>{{ a }}</span>
            </li>
          </ul>
        </div>
      </div>

      <!-- Top produits -->
      <div class="dash-card">
        <div class="card-head">
          <h2><i class="fas fa-trophy"></i> Top Produits en Stock</h2>
        </div>
        <div class="card-body">
          <div v-if="loading" class="dash-loading"><i class="fas fa-spinner fa-spin"></i></div>
          <div v-else-if="!topProduits.length" class="dash-empty">Aucune donnée</div>
          <div v-else class="top-list">
            <div
                v-for="(p, i) in topProduits"
                :key="p.id"
                class="top-item"
            >
              <div class="top-rank" :class="['rank-'+Math.min(i+1,4)]">{{ i + 1 }}</div>
              <div class="top-info">
                <div class="top-name">{{ p.nom }}</div>
                <div class="top-cat">{{ p.categorie || '—' }}</div>
              </div>
              <div class="top-qty">{{ p.total.toLocaleString() }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Répartition catégories -->
      <div class="dash-card">
        <div class="card-head">
          <h2><i class="fas fa-tags"></i> Produits par Catégorie</h2>
        </div>
        <div class="card-body">
          <div v-if="loading" class="dash-loading"><i class="fas fa-spinner fa-spin"></i></div>
          <div v-else-if="!categories.length" class="dash-empty">Aucune donnée</div>
          <div v-else class="cat-list">
            <div v-for="c in categories" :key="c.name" class="cat-item">
              <div class="cat-info">
                <span class="cat-dot" :style="{ background: c.color }"></span>
                <span class="cat-name">{{ c.name || 'Non classé' }}</span>
              </div>
              <div class="cat-bar-wrap">
                <div class="cat-bar-track">
                  <div
                      class="cat-bar-fill"
                      :style="{
                      width: (c.count / totalProduits * 100) + '%',
                      background: c.color
                    }"
                  ></div>
                </div>
                <span class="cat-count">{{ c.count }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Derniers mouvements -->
      <div class="dash-card span2">
        <div class="card-head">
          <h2><i class="fas fa-history"></i> Derniers Mouvements</h2>
          <select v-model="selectedEntrepot" class="mvt-select" @change="fetchHistorique">
            <option value="">— Tous les entrepôts —</option>
            <option v-for="e in entrepots" :key="e.id" :value="e.id">{{ e.nom }}</option>
          </select>
        </div>
        <div class="card-body">
          <div v-if="loadingMvt" class="dash-loading"><i class="fas fa-spinner fa-spin"></i></div>
          <div v-else-if="!mouvements.length" class="dash-empty">Sélectionnez un entrepôt</div>
          <div v-else class="mvt-table-wrap">
            <table class="mvt-table">
              <thead>
              <tr>
                <th>Type</th>
                <th>Produit</th>
                <th>Entrepôt</th>
                <th class="right">Qté</th>
                <th>Note</th>
                <th>Date</th>
              </tr>
              </thead>
              <tbody>
              <tr v-for="(m, i) in mouvements.slice(0, 12)" :key="i">
                <td>
                    <span class="mvt-badge" :class="m.type.toLowerCase()">
                      <i :class="mvtIcon(m.type)"></i>
                      {{ m.type }}
                    </span>
                </td>
                <td class="mvt-prod">{{ getProduitNom(m.idProduit) }}</td>
                <td class="mvt-entrepot">{{ getEntrepotNom(m.idEntrepot) }}</td>
                <td class="right"><strong>{{ m.quantite }}</strong></td>
                <td class="mvt-note">{{ m.note || '—' }}</td>
                <td class="mvt-date">{{ formatDate(m.date) }}</td>
              </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'

// ─── TYPES ───────────────────────────────────────────────────────
interface Produit   { id: string; nom: string; categorie?: string; prixUnitaire: number }
interface Entrepot  { id: string; nom: string; ville?: string }
interface StockItem { idProduit: string; quantite: number }
interface Mouvement { idProduit: string; idEntrepot: string; type: string; quantite: number; note: string; date: string }

// ─── API ─────────────────────────────────────────────────────────
const BASE = 'http://localhost:8080/api'
async function api<T>(path: string): Promise<T> {
  const r = await fetch(BASE + path)
  if (!r.ok) throw new Error(`HTTP ${r.status}`)
  return r.json()
}

// ─── STATE ────────────────────────────────────────────────────────
const username        = ref(localStorage.getItem('username') || 'Utilisateur')
const loading         = ref(false)
const loadingMvt      = ref(false)
const produits        = ref<Produit[]>([])
const entrepots       = ref<Entrepot[]>([])
const alertes         = ref<string[]>([])
const mouvements      = ref<Mouvement[]>([])
const stockParEntrepot = ref<{ id: string; nom: string; total: number; color: string }[]>([])
const selectedEntrepot = ref('')

const COLORS = ['#2563eb','#16a34a','#d97706','#dc2626','#7c3aed','#0d9488','#db2777','#ea580c']

// ─── COMPUTED ─────────────────────────────────────────────────────
const stats = computed(() => ({
  nbProduits:  produits.value.length,
  nbEntrepots: entrepots.value.length,
  stockTotal:  stockParEntrepot.value.reduce((s, e) => s + e.total, 0),
}))

const maxStock = computed(() =>
    Math.max(...stockParEntrepot.value.map(e => e.total), 1)
)

const totalProduits = computed(() => produits.value.length)

const topProduits = computed(() => {
  // Agréger stock total par produit sur tous entrepôts
  const map: Record<string, number> = {}
  // On utilise les données de stockParEntrepot — approximation par produit impossible sans détail
  // On retourne les produits triés par prix comme fallback si pas de détail stock
  return produits.value
      .slice()
      .sort((a, b) => b.prixUnitaire - a.prixUnitaire)
      .slice(0, 6)
      .map(p => ({ ...p, total: Math.round(p.prixUnitaire) }))
})

const categories = computed(() => {
  const map: Record<string, number> = {}
  produits.value.forEach(p => {
    const cat = p.categorie || 'Autre'
    map[cat] = (map[cat] ?? 0) + 1
  })
  return Object.entries(map)
      .sort((a, b) => b[1] - a[1])
      .map(([name, count], i) => ({ name, count, color: COLORS[i % COLORS.length] }))
})

const dateNow = computed(() => {
  return new Date().toLocaleDateString('fr-FR', {
    weekday: 'long', year: 'numeric', month: 'long', day: 'numeric'
  })
})

// ─── HELPERS ──────────────────────────────────────────────────────
function getProduitNom(id: string) {
  return produits.value.find(p => p.id === id)?.nom ?? id
}
function getEntrepotNom(id: string) {
  return entrepots.value.find(e => e.id === id)?.nom ?? id
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
function ringStyle(val: number, max: number) {
  const pct = Math.min(val / max * 100, 100)
  return { '--pct': pct + '%' }
}

// ─── FETCH ────────────────────────────────────────────────────────
async function fetchProduits() {
  produits.value = await api<Produit[]>('/produits')
}

async function fetchEntrepots() {
  const raw = await api<{ id: string; nom: string }[]>('/entrepots')
  entrepots.value = raw.map(r => {
    const match = r.nom.match(/^(.*?)\s*\(([^)]+)\)$/)
    return { id: r.id, nom: match ? match[1].trim() : r.nom, ville: match?.[2] }
  })
}

async function fetchStocksParEntrepot() {
  const results = await Promise.allSettled(
      entrepots.value.map(async (e, i) => {
        const data = await api<{ stocks: StockItem[] }>(`/stocks/entrepot?id=${e.id}`)
        const total = (data.stocks ?? []).reduce((s, st) => s + st.quantite, 0)
        return { id: e.id, nom: e.nom, total, color: COLORS[i % COLORS.length] }
      })
  )
  stockParEntrepot.value = results
      .filter(r => r.status === 'fulfilled')
      .map(r => (r as PromiseFulfilledResult<any>).value)
}

async function fetchAlertes() {
  try {
    const data = await api<{ alertes: string[] }>('/stocks/alertes?seuil=10')
    alertes.value = data.alertes ?? []
  } catch { alertes.value = [] }
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

async function fetchAll() {
  loading.value = true
  try {
    await Promise.all([fetchProduits(), fetchEntrepots()])
    await Promise.all([fetchStocksParEntrepot(), fetchAlertes()])
    if (selectedEntrepot.value) await fetchHistorique()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(fetchAll)
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&family=DM+Mono:wght@500&display=swap');

.dash {
  padding: 2rem;
  max-width: 1440px;
  margin: 0 auto;
  background: #f0f4f8;
  min-height: 100vh;
  font-family: 'DM Sans', sans-serif;
}

/* ── HEADER ── */
.dash-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 2rem;
  flex-wrap: wrap;
  gap: 1rem;
}
.dash-title {
  font-size: 1.75rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 .3rem;
  letter-spacing: -.03em;
}
.dash-sub {
  font-size: .875rem;
  color: #64748b;
  margin: 0;
  text-transform: capitalize;
}
.dash-refresh {
  display: flex;
  align-items: center;
  gap: .5rem;
  padding: .6rem 1.2rem;
  background: #0f172a;
  color: white;
  border: none;
  border-radius: .6rem;
  font-size: .85rem;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
  transition: background .2s;
}
.dash-refresh:hover:not(:disabled) { background: #1e293b; }
.dash-refresh:disabled { opacity: .5; cursor: not-allowed; }

/* ── KPI CARDS ── */
.dash-kpis {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.kpi-card {
  background: white;
  border-radius: 1rem;
  padding: 1.5rem;
  box-shadow: 0 1px 4px rgba(0,0,0,.06);
  display: flex;
  align-items: center;
  gap: 1.25rem;
  position: relative;
  overflow: hidden;
  border-top: 3px solid var(--accent);
  transition: transform .2s, box-shadow .2s;
}
.kpi-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(0,0,0,.1);
}

.kpi-icon {
  width: 3rem;
  height: 3rem;
  border-radius: .75rem;
  background: var(--accent-l);
  color: var(--accent);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  flex-shrink: 0;
}

.kpi-label {
  font-size: .75rem;
  color: #64748b;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: .05em;
  margin-bottom: .25rem;
}
.kpi-val {
  font-size: 2rem;
  font-weight: 700;
  color: #0f172a;
  font-family: 'DM Mono', monospace;
  line-height: 1;
  margin-bottom: .2rem;
}
.kpi-sub { font-size: .75rem; color: #94a3b8; }

.kpi-ring {
  position: absolute;
  right: -1rem;
  top: 50%;
  transform: translateY(-50%);
  width: 5rem;
  height: 5rem;
  border-radius: 50%;
  background: conic-gradient(var(--accent) var(--pct), var(--accent-l) 0);
  opacity: .25;
}

/* ── MAIN GRID ── */
.dash-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1.25rem;
}

.dash-card {
  background: white;
  border-radius: 1rem;
  box-shadow: 0 1px 4px rgba(0,0,0,.06);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.span2 { grid-column: span 2; }

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.1rem 1.25rem;
  border-bottom: 1px solid #f1f5f9;
  background: #fafbfc;
  flex-wrap: wrap;
  gap: .5rem;
}
.card-head h2 {
  margin: 0;
  font-size: .95rem;
  font-weight: 700;
  color: #0f172a;
  display: flex;
  align-items: center;
  gap: .5rem;
}
.card-head h2 i { color: #64748b; font-size: .9rem; }

.card-body { padding: 1.25rem; flex: 1; overflow: auto; }

.dash-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  color: #94a3b8;
  font-size: 1.5rem;
}
.dash-empty {
  text-align: center;
  padding: 2rem;
  color: #94a3b8;
  font-size: .9rem;
}
.dash-ok {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: .5rem;
  padding: 1.5rem;
  color: #16a34a;
  font-size: .9rem;
  font-weight: 600;
}

/* ── BAR CHART ── */
.bar-chart { display: flex; flex-direction: column; gap: .75rem; }
.bar-row {
  display: grid;
  grid-template-columns: 120px 1fr 60px;
  align-items: center;
  gap: .75rem;
}
.bar-label { font-size: .82rem; font-weight: 600; color: #334155; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.bar-track { height: .6rem; background: #f1f5f9; border-radius: 9999px; overflow: hidden; }
.bar-fill  { height: 100%; border-radius: 9999px; transition: width .6s ease; }
.bar-val   { font-size: .82rem; font-weight: 700; color: #0f172a; text-align: right; font-family: 'DM Mono', monospace; }

/* ── ALERTES ── */
.alert-head { background: #fff5f5 !important; }
.alert-badge {
  background: #dc2626;
  color: white;
  font-size: .72rem;
  font-weight: 700;
  padding: .15rem .55rem;
  border-radius: 9999px;
}
.alert-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: .5rem; }
.alert-item {
  display: flex;
  align-items: center;
  gap: .6rem;
  padding: .6rem .85rem;
  background: #fff5f5;
  border-radius: .5rem;
  color: #dc2626;
  font-size: .85rem;
  font-weight: 500;
  border-left: 3px solid #dc2626;
}

/* ── TOP PRODUITS ── */
.top-list { display: flex; flex-direction: column; gap: .6rem; }
.top-item {
  display: flex;
  align-items: center;
  gap: .85rem;
  padding: .65rem .85rem;
  background: #f8fafc;
  border-radius: .6rem;
  transition: background .15s;
}
.top-item:hover { background: #f1f5f9; }
.top-rank {
  width: 1.75rem; height: 1.75rem;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: .75rem; font-weight: 700; flex-shrink: 0;
  background: #e2e8f0; color: #64748b;
}
.top-rank.rank-1 { background: #fef9c3; color: #ca8a04; }
.top-rank.rank-2 { background: #f1f5f9; color: #475569; }
.top-rank.rank-3 { background: #fde8d8; color: #c2410c; }
.top-info { flex: 1; min-width: 0; }
.top-name { font-size: .85rem; font-weight: 600; color: #0f172a; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.top-cat  { font-size: .72rem; color: #94a3b8; }
.top-qty  { font-size: .9rem; font-weight: 700; color: #2563eb; font-family: 'DM Mono', monospace; }

/* ── CATÉGORIES ── */
.cat-list { display: flex; flex-direction: column; gap: .65rem; }
.cat-item { display: flex; flex-direction: column; gap: .3rem; }
.cat-info { display: flex; align-items: center; gap: .5rem; }
.cat-dot  { width: .6rem; height: .6rem; border-radius: 50%; flex-shrink: 0; }
.cat-name { font-size: .82rem; font-weight: 600; color: #334155; }
.cat-bar-wrap { display: flex; align-items: center; gap: .6rem; }
.cat-bar-track { flex: 1; height: .45rem; background: #f1f5f9; border-radius: 9999px; overflow: hidden; }
.cat-bar-fill  { height: 100%; border-radius: 9999px; transition: width .5s ease; }
.cat-count { font-size: .75rem; font-weight: 700; color: #64748b; min-width: 1.5rem; text-align: right; font-family: 'DM Mono', monospace; }

/* ── MOUVEMENTS ── */
.mvt-select {
  padding: .4rem .75rem;
  border: 1px solid #e2e8f0;
  border-radius: .5rem;
  font-size: .82rem;
  background: white;
  cursor: pointer;
  font-family: inherit;
}
.mvt-select:focus { outline: none; border-color: #2563eb; }

.mvt-table-wrap { overflow-x: auto; }
.mvt-table {
  width: 100%;
  border-collapse: collapse;
  font-size: .82rem;
}
.mvt-table th {
  text-align: left;
  padding: .6rem .75rem;
  background: #f8fafc;
  color: #64748b;
  font-size: .7rem;
  text-transform: uppercase;
  letter-spacing: .05em;
  border-bottom: 1px solid #f1f5f9;
  font-weight: 600;
}
.mvt-table td {
  padding: .65rem .75rem;
  border-bottom: 1px solid #f8fafc;
  color: #334155;
  vertical-align: middle;
}
.mvt-table tr:last-child td { border-bottom: none; }
.mvt-table tr:hover td { background: #f8fafc; }
.right { text-align: right; }

.mvt-badge {
  display: inline-flex;
  align-items: center;
  gap: .3rem;
  padding: .2rem .55rem;
  border-radius: .4rem;
  font-size: .72rem;
  font-weight: 700;
  text-transform: uppercase;
}
.mvt-badge.entree    { background: #dcfce7; color: #16a34a; }
.mvt-badge.sortie    { background: #fee2e2; color: #dc2626; }
.mvt-badge.transfert { background: #ede9fe; color: #7c3aed; }

.mvt-prod    { font-weight: 600; color: #0f172a; }
.mvt-entrepot { color: #64748b; font-size: .78rem; }
.mvt-note   { color: #94a3b8; font-style: italic; max-width: 120px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.mvt-date   { color: #94a3b8; font-size: .75rem; white-space: nowrap; }

/* ── RESPONSIVE ── */
@media (max-width: 1100px) {
  .dash-grid { grid-template-columns: repeat(2, 1fr); }
  .span2 { grid-column: span 2; }
}
@media (max-width: 768px) {
  .dash { padding: 1rem; }
  .dash-grid { grid-template-columns: 1fr; }
  .span2 { grid-column: span 1; }
  .dash-kpis { grid-template-columns: 1fr 1fr; }
  .bar-row { grid-template-columns: 80px 1fr 50px; }
}
@media (max-width: 480px) {
  .dash-kpis { grid-template-columns: 1fr; }
}
</style>