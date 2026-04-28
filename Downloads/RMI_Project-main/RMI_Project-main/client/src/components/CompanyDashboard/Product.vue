<template>
  <div class="ps-container">

    <!-- ══════════════════════════════════════════
         HEADER STATS
    ══════════════════════════════════════════ -->
    <div class="ps-stats">
      <div class="ps-stat-card blue">
        <div class="ps-stat-icon"><i class="fas fa-box"></i></div>
        <div>
          <div class="ps-stat-label">Total Produits</div>
          <div class="ps-stat-val">{{ produits.length }}</div>
        </div>
      </div>
      <div class="ps-stat-card purple">
        <div class="ps-stat-icon"><i class="fas fa-tags"></i></div>
        <div>
          <div class="ps-stat-label">Catégories</div>
          <div class="ps-stat-val">{{ nbCategories }}</div>
        </div>
      </div>
      <div class="ps-stat-card green">
        <div class="ps-stat-icon"><i class="fas fa-coins"></i></div>
        <div>
          <div class="ps-stat-label">Prix Moyen (TND)</div>
          <div class="ps-stat-val">{{ prixMoyen }}</div>
        </div>
      </div>
      <div class="ps-stat-card teal">
        <div class="ps-stat-icon"><i class="fas fa-filter"></i></div>
        <div>
          <div class="ps-stat-label">Filtrés</div>
          <div class="ps-stat-val">{{ filteredProduits.length }}</div>
        </div>
      </div>
    </div>

    <!-- ══════════════════════════════════════════
         TOOLBAR
    ══════════════════════════════════════════ -->
    <div class="ps-toolbar">
      <div class="ps-search">
        <i class="fas fa-search"></i>
        <input v-model="search" placeholder="Rechercher un produit…" />
      </div>
      <div class="ps-filter-group">
        <select v-model="filterCategorie" class="ps-select-filter">
          <option value="">Toutes catégories</option>
          <option v-for="cat in categories" :key="cat" :value="cat">{{ cat }}</option>
        </select>
      </div>
      <div class="ps-toolbar-btns">
        <button class="ps-btn purple" @click="openModal">
          <i class="fas fa-plus"></i> Nouveau Produit
        </button>
        <button class="ps-btn teal" @click="fetchProduits" :disabled="loading">
          <i class="fas fa-sync-alt" :class="{ 'fa-spin': loading }"></i>
        </button>
      </div>
    </div>

    <!-- ══════════════════════════════════════════
         ERROR / LOADING
    ══════════════════════════════════════════ -->
    <div v-if="error" class="ps-msg error">
      <i class="fas fa-exclamation-circle"></i> {{ error }}
      <button @click="error = null">×</button>
    </div>
    <div v-if="loading && !produits.length" class="ps-loading">
      <i class="fas fa-spinner fa-spin"></i> Chargement des produits…
    </div>

    <!-- ══════════════════════════════════════════
         PRODUCTS TABLE VIEW
    ══════════════════════════════════════════ -->
    <div v-if="!loading || produits.length" class="ps-table-wrapper">
      <div v-if="filteredProduits.length === 0 && !loading" class="ps-empty-state">
        <i class="fas fa-box-open"></i>
        <p>Aucun produit trouvé</p>
        <button class="ps-btn purple" @click="openModal">
          <i class="fas fa-plus"></i> Ajouter un produit
        </button>
      </div>

      <table v-else class="ps-table">
        <thead>
        <tr>
          <th @click="sortBy('id')" class="sortable">
            ID <i :class="sortIcon('id')"></i>
          </th>
          <th @click="sortBy('nom')" class="sortable">
            Nom <i :class="sortIcon('nom')"></i>
          </th>
          <th @click="sortBy('categorie')" class="sortable">
            Catégorie <i :class="sortIcon('categorie')"></i>
          </th>
          <th>Description</th>
          <th @click="sortBy('prixUnitaire')" class="sortable right">
            Prix (TND) <i :class="sortIcon('prixUnitaire')"></i>
          </th>
          <th class="center">Actions</th>
        </tr>
        </thead>
        <tbody>
        <tr
            v-for="p in filteredProduits"
            :key="p.id"
            class="ps-row"
        >
          <td>
            <span class="ps-chip gray-chip">{{ p.id }}</span>
          </td>
          <td>
            <div class="ps-prod-cell">
              <div class="ps-prod-icon" :style="{ background: colorForId(p.id) }">
                {{ p.nom.charAt(0).toUpperCase() }}
              </div>
              <div>
                <span class="ps-prod-name">{{ p.nom }}</span>
              </div>
            </div>
          </td>
          <td>
              <span class="ps-cat-badge" :style="catStyle(p.categorie)">
                {{ p.categorie || '—' }}
              </span>
          </td>
          <td class="ps-desc">{{ p.description || '—' }}</td>
          <td class="right">
              <span class="ps-price-badge">
                {{ p.prixUnitaire.toFixed(3) }}
              </span>
          </td>
          <td class="center">
            <div class="ps-actions">
              <button class="ps-icon-btn blue-btn" @click="openEdit(p)" title="Modifier">
                <i class="fas fa-edit"></i>
              </button>
              <button class="ps-icon-btn red-btn" @click="confirmerSuppression(p)" title="Supprimer">
                <i class="fas fa-trash-alt"></i>
              </button>
            </div>
          </td>
        </tr>
        </tbody>
      </table>
    </div>

    <!-- ══════════════════════════════════════════
         PAGINATION
    ══════════════════════════════════════════ -->
    <div v-if="totalPages > 1" class="ps-pagination">
      <button :disabled="page === 1" @click="page--" class="ps-pg-btn">
        <i class="fas fa-chevron-left"></i>
      </button>
      <span class="ps-pg-info">Page {{ page }} / {{ totalPages }}</span>
      <button :disabled="page === totalPages" @click="page++" class="ps-pg-btn">
        <i class="fas fa-chevron-right"></i>
      </button>
    </div>

    <!-- ════════════════════════════════════════════════════════════
         MODAL : AJOUTER / MODIFIER PRODUIT
    ════════════════════════════════════════════════════════════ -->
    <transition name="fade">
      <div v-if="showModal" class="ps-overlay" @click.self="closeModal">
        <div class="ps-modal">
          <div class="ps-modal-head" :class="editMode ? 'blue-head' : 'purple-head'">
            <span>
              <i :class="editMode ? 'fas fa-edit' : 'fas fa-plus-circle'"></i>
              {{ editMode ? 'Modifier le Produit' : 'Nouveau Produit' }}
            </span>
            <button @click="closeModal">×</button>
          </div>
          <div class="ps-modal-body">

            <div class="ps-field">
              <label>ID Produit *</label>
              <input
                  v-model="form.id"
                  placeholder="ex: P005"
                  maxlength="20"
                  :disabled="editMode"
                  :class="{ 'disabled-field': editMode }"
              />
              <small>Identifiant unique (varchar 20) — non modifiable après création</small>
            </div>

            <div class="ps-field">
              <label>Nom *</label>
              <input v-model="form.nom" placeholder="Nom du produit" maxlength="100" />
            </div>

            <div class="ps-field">
              <label>Catégorie</label>
              <div class="ps-cat-picker">
                <div
                    v-for="cat in categorieOptions"
                    :key="cat.value"
                    class="ps-cat-opt"
                    :class="{ selected: form.categorie === cat.value }"
                    @click="form.categorie = cat.value"
                >
                  <i :class="cat.icon"></i>
                  <span>{{ cat.label }}</span>
                </div>
              </div>
            </div>

            <div class="ps-field">
              <label>Description</label>
              <textarea
                  v-model="form.description"
                  placeholder="Description optionnelle…"
                  rows="2"
                  maxlength="500"
              ></textarea>
            </div>

            <div class="ps-field">
              <label>Prix unitaire (TND) *</label>
              <div class="ps-price-input">
                <span class="ps-currency">TND</span>
                <input
                    type="number"
                    v-model.number="form.prixUnitaire"
                    min="0"
                    step="0.001"
                    placeholder="0.000"
                />
              </div>
            </div>

          </div>
          <div class="ps-modal-foot">
            <button class="ps-btn gray" @click="closeModal">Annuler</button>
            <button
                :class="editMode ? 'ps-btn blue' : 'ps-btn purple'"
                @click="sauvegarder"
                :disabled="submitting || !form.id || !form.nom"
            >
              <i v-if="submitting" class="fas fa-spinner fa-spin"></i>
              <span v-else>
                <i :class="editMode ? 'fas fa-save' : 'fas fa-check'"></i>
                {{ editMode ? 'Enregistrer' : 'Créer' }}
              </span>
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- ════════════════════════════════════════════════════════════
         MODAL : CONFIRMER SUPPRESSION
    ════════════════════════════════════════════════════════════ -->
    <transition name="fade">
      <div v-if="produitASupprimer" class="ps-overlay" @click.self="produitASupprimer = null">
        <div class="ps-modal" style="max-width:400px">
          <div class="ps-modal-head red-head">
            <span><i class="fas fa-exclamation-triangle"></i> Confirmer la suppression</span>
            <button @click="produitASupprimer = null">×</button>
          </div>
          <div class="ps-modal-body ps-confirm-body">
            <div class="ps-confirm-icon">
              <i class="fas fa-trash-alt"></i>
            </div>
            <p>Voulez-vous supprimer le produit :</p>
            <div class="ps-confirm-target">
              <strong>{{ produitASupprimer.nom }}</strong>
              <span class="ps-chip gray-chip">{{ produitASupprimer.id }}</span>
            </div>
            <p class="ps-warn-text">Cette action est irréversible.</p>
          </div>
          <div class="ps-modal-foot">
            <button class="ps-btn gray" @click="produitASupprimer = null">Annuler</button>
            <button class="ps-btn red" @click="supprimerProduit" :disabled="submitting">
              <i v-if="submitting" class="fas fa-spinner fa-spin"></i>
              <span v-else><i class="fas fa-trash-alt"></i> Supprimer</span>
            </button>
          </div>
        </div>
      </div>
    </transition>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'

// ─────────────────────────────────────────────────────────────────
// TYPE — correspond exactement à la table `produits` MySQL
// et à common.Produit du backend Java
// ─────────────────────────────────────────────────────────────────
interface Produit {
  id: string
  nom: string
  categorie?: string
  description?: string
  prixUnitaire: number
}

// ─────────────────────────────────────────────────────────────────
// API — HttpBridge.java sur :8080
//   GET  /api/produits              → List<Produit>
//   POST /api/produits              → void  (body: Produit)
//   PUT  /api/produits/:id          → void  (body: Produit partiel)  ← optionnel si implémenté
// ─────────────────────────────────────────────────────────────────
const BASE = 'http://localhost:8080/api'

async function get<T>(path: string): Promise<T> {
  const r = await fetch(BASE + path)
  if (!r.ok) throw new Error(await r.text().catch(() => `HTTP ${r.status}`))
  return r.json()
}

async function post<T>(path: string, body: object): Promise<T> {
  const r = await fetch(BASE + path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  })
  if (!r.ok) throw new Error(await r.text().catch(() => `HTTP ${r.status}`))
  return r.json()
}

async function put<T>(path: string, body: object): Promise<T> {
  const r = await fetch(BASE + path, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  })
  if (!r.ok) throw new Error(await r.text().catch(() => `HTTP ${r.status}`))
  return r.json()
}

async function del(path: string): Promise<void> {
  const r = await fetch(BASE + path, { method: 'DELETE' })
  if (!r.ok) throw new Error(await r.text().catch(() => `HTTP ${r.status}`))
}

// ─────────────────────────────────────────────────────────────────
// STATE
// ─────────────────────────────────────────────────────────────────
const produits        = ref<Produit[]>([])
const loading         = ref(false)
const submitting      = ref(false)
const error           = ref<string | null>(null)
const search          = ref('')
const filterCategorie = ref('')
const sortKey         = ref<keyof Produit>('nom')
const sortDir         = ref<1 | -1>(1)
const page            = ref(1)
const PAGE_SIZE       = 15

const showModal        = ref(false)
const editMode         = ref(false)
const produitASupprimer = ref<Produit | null>(null)

const form = ref<Produit>({ id: '', nom: '', categorie: '', description: '', prixUnitaire: 0 })

// ─────────────────────────────────────────────────────────────────
// CATEGORIES
// ─────────────────────────────────────────────────────────────────
const categorieOptions = [
  { value: 'Electronique',     label: 'Électronique',   icon: 'fas fa-microchip' },
  { value: 'Alimentaire',      label: 'Alimentaire',    icon: 'fas fa-utensils'  },
  { value: 'Textile',          label: 'Textile',        icon: 'fas fa-tshirt'    },
  { value: 'Mobilier',         label: 'Mobilier',       icon: 'fas fa-couch'     },
  { value: 'Outillage',        label: 'Outillage',      icon: 'fas fa-tools'     },
  { value: 'Médical',          label: 'Médical',        icon: 'fas fa-heartbeat' },
  { value: 'Cosmétique',       label: 'Cosmétique',     icon: 'fas fa-spa'       },
  { value: 'Automobile',       label: 'Automobile',     icon: 'fas fa-car'       },
  { value: 'Informatique',     label: 'Informatique',   icon: 'fas fa-laptop'    },
  { value: 'Autre',            label: 'Autre',          icon: 'fas fa-box'       },
]

// ─────────────────────────────────────────────────────────────────
// COMPUTED
// ─────────────────────────────────────────────────────────────────
const categories = computed(() => {
  const set = new Set(produits.value.map(p => p.categorie).filter(Boolean) as string[])
  return [...set].sort()
})

const nbCategories = computed(() => categories.value.length)

const prixMoyen = computed(() => {
  if (!produits.value.length) return '0.000'
  const avg = produits.value.reduce((s, p) => s + p.prixUnitaire, 0) / produits.value.length
  return avg.toFixed(3)
})

const sortedProduits = computed(() => {
  return [...produits.value].sort((a, b) => {
    const av = a[sortKey.value] ?? ''
    const bv = b[sortKey.value] ?? ''
    if (av < bv) return -sortDir.value
    if (av > bv) return  sortDir.value
    return 0
  })
})

const filteredProduits = computed(() => {
  let list = sortedProduits.value
  if (search.value) {
    const t = search.value.toLowerCase()
    list = list.filter(p =>
        p.id.toLowerCase().includes(t) ||
        p.nom.toLowerCase().includes(t) ||
        (p.description ?? '').toLowerCase().includes(t) ||
        (p.categorie  ?? '').toLowerCase().includes(t)
    )
  }
  if (filterCategorie.value) {
    list = list.filter(p => p.categorie === filterCategorie.value)
  }
  const start = (page.value - 1) * PAGE_SIZE
  return list.slice(start, start + PAGE_SIZE)
})

const totalPages = computed(() => {
  let list = sortedProduits.value
  if (search.value) {
    const t = search.value.toLowerCase()
    list = list.filter(p =>
        p.id.toLowerCase().includes(t) || p.nom.toLowerCase().includes(t)
    )
  }
  if (filterCategorie.value) list = list.filter(p => p.categorie === filterCategorie.value)
  return Math.max(1, Math.ceil(list.length / PAGE_SIZE))
})

// ─────────────────────────────────────────────────────────────────
// UTILITAIRES
// ─────────────────────────────────────────────────────────────────
const COLORS = ['#3b82f6','#8b5cf6','#10b981','#f59e0b','#ef4444','#06b6d4','#84cc16','#f43f5e','#a855f7','#14b8a6']
function colorForId(id: string): string {
  let h = 0; for (const c of id) h += c.charCodeAt(0)
  return COLORS[h % COLORS.length]
}

const CAT_COLORS: Record<string, { bg: string; color: string }> = {
  'Electronique':  { bg:'#dbeafe', color:'#1d4ed8' },
  'Alimentaire':   { bg:'#fef3c7', color:'#92400e' },
  'Textile':       { bg:'#fce7f3', color:'#9d174d' },
  'Mobilier':      { bg:'#ede9fe', color:'#5b21b6' },
  'Outillage':     { bg:'#e0f2fe', color:'#075985' },
  'Médical':       { bg:'#fee2e2', color:'#991b1b' },
  'Cosmétique':    { bg:'#dcfce7', color:'#166534' },
  'Automobile':    { bg:'#f1f5f9', color:'#334155' },
  'Informatique':  { bg:'#ccfbf1', color:'#0f766e' },
  'Autre':         { bg:'#f1f5f9', color:'#64748b' },
}
function catStyle(cat?: string) {
  const c = CAT_COLORS[cat ?? ''] ?? { bg: '#f1f5f9', color: '#64748b' }
  return { background: c.bg, color: c.color }
}

function sortBy(key: keyof Produit) {
  if (sortKey.value === key) sortDir.value = sortDir.value === 1 ? -1 : 1
  else { sortKey.value = key; sortDir.value = 1 }
  page.value = 1
}
function sortIcon(key: string): string {
  if (sortKey.value !== key) return 'fas fa-sort text-muted'
  return sortDir.value === 1 ? 'fas fa-sort-up' : 'fas fa-sort-down'
}

// ─────────────────────────────────────────────────────────────────
// FETCH
// ─────────────────────────────────────────────────────────────────
async function fetchProduits() {
  loading.value = true; error.value = null
  try {
    // GET /api/produits → retourne List<Produit> sérialisé par HttpBridge
    // Chaque objet: { id, nom, categorie, description, prixUnitaire }
    produits.value = await get<Produit[]>('/produits')
  } catch (e: any) {
    error.value = e.message ?? 'Erreur de connexion au serveur'
  } finally {
    loading.value = false
  }
}

// ─────────────────────────────────────────────────────────────────
// MODALS
// ─────────────────────────────────────────────────────────────────
function openModal() {
  form.value = { id: '', nom: '', categorie: '', description: '', prixUnitaire: 0 }
  editMode.value = false
  showModal.value = true
}
function openEdit(p: Produit) {
  form.value = { ...p }
  editMode.value = true
  showModal.value = true
}
function closeModal() { showModal.value = false }

function confirmerSuppression(p: Produit) { produitASupprimer.value = p }

// ─────────────────────────────────────────────────────────────────
// CRUD
// ─────────────────────────────────────────────────────────────────
async function sauvegarder() {
  if (!form.value.id || !form.value.nom) return
  submitting.value = true
  try {
    if (editMode.value) {
      // PUT /api/produits/:id  (si implémenté dans HttpBridge)
      // Sinon, utiliser POST /api/produits avec les nouvelles valeurs
      await put(`/produits/${form.value.id}`, {
        nom:          form.value.nom,
        categorie:    form.value.categorie,
        description:  form.value.description,
        prixUnitaire: form.value.prixUnitaire
      })
    } else {
      // POST /api/produits  → appelle GestionStocksImpl.ajouterProduit()
      await post('/produits', {
        id:           form.value.id,
        nom:          form.value.nom,
        categorie:    form.value.categorie ?? '',
        description:  form.value.description ?? '',
        prixUnitaire: form.value.prixUnitaire
      })
    }
    closeModal()
    await fetchProduits()
  } catch (e: any) {
    error.value = e.message ?? 'Erreur lors de la sauvegarde'
  } finally {
    submitting.value = false
  }
}

async function supprimerProduit() {
  if (!produitASupprimer.value) return
  submitting.value = true
  try {
    // DELETE /api/produits/:id  (à ajouter dans HttpBridge si non présent)
    await del(`/produits/${produitASupprimer.value.id}`)
    produitASupprimer.value = null
    await fetchProduits()
  } catch (e: any) {
    error.value = e.message ?? 'Erreur lors de la suppression'
    produitASupprimer.value = null
  } finally {
    submitting.value = false
  }
}

onMounted(fetchProduits)
</script>

<style scoped>
/* ══════════════════════════════════════════
   VARIABLES — même palette que GestionStocks
══════════════════════════════════════════ */
.ps-container {
  --blue:   #2563eb; --blue-l:   #dbeafe;
  --purple: #7c3aed; --purple-l: #ede9fe;
  --green:  #16a34a; --green-l:  #dcfce7;
  --red:    #dc2626; --red-l:    #fee2e2;
  --orange: #d97706; --orange-l: #fef3c7;
  --teal:   #0d9488; --teal-l:   #ccfbf1;
  --gray:   #64748b; --gray-l:   #f1f5f9;
  --dark:   #0f172a;
  --radius: .75rem;
  --shadow: 0 2px 8px rgba(0,0,0,.08);

  padding: 1.75rem;
  max-width: 1440px;
  margin: 0 auto;
  background: #f8fafc;
  min-height: 100vh;
  font-family: 'Segoe UI', system-ui, sans-serif;
}

/* ══ STATS ══ */
.ps-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px,1fr));
  gap: 1rem;
  margin-bottom: 1.5rem;
}
.ps-stat-card {
  background: white; border-radius: var(--radius);
  padding: 1.25rem; box-shadow: var(--shadow);
  display: flex; align-items: center; gap: 1rem;
  border-left: 4px solid transparent;
}
.ps-stat-card.blue   { border-color: var(--blue);   }
.ps-stat-card.purple { border-color: var(--purple);  }
.ps-stat-card.green  { border-color: var(--green);   }
.ps-stat-card.teal   { border-color: var(--teal);    }

.ps-stat-icon {
  width:2.75rem; height:2.75rem; border-radius:.6rem;
  display:flex; align-items:center; justify-content:center;
  font-size:1.1rem; flex-shrink:0;
}
.blue   .ps-stat-icon { background:var(--blue-l);   color:var(--blue);   }
.purple .ps-stat-icon { background:var(--purple-l);  color:var(--purple); }
.green  .ps-stat-icon { background:var(--green-l);   color:var(--green);  }
.teal   .ps-stat-icon { background:var(--teal-l);    color:var(--teal);   }
.ps-stat-label { font-size:.75rem; color:var(--gray); margin-bottom:.25rem; font-weight:500; }
.ps-stat-val   { font-size:1.6rem; font-weight:700; color:var(--dark); line-height:1; }

/* ══ TOOLBAR ══ */
.ps-toolbar {
  display:flex; gap:.75rem; align-items:center;
  margin-bottom:1.25rem; flex-wrap:wrap;
}
.ps-search {
  flex:1; min-width:220px; position:relative;
}
.ps-search i { position:absolute; left:.85rem; top:50%; transform:translateY(-50%); color:var(--gray); font-size:.9rem; }
.ps-search input {
  width:100%; padding:.65rem 1rem .65rem 2.25rem;
  border:1px solid #e2e8f0; border-radius:.6rem;
  font-size:.875rem; background:white; box-sizing:border-box;
  transition:border-color .2s;
}
.ps-search input:focus { outline:none; border-color:var(--blue); }

.ps-filter-group select.ps-select-filter {
  padding:.65rem 2rem .65rem .85rem;
  border:1px solid #e2e8f0; border-radius:.6rem;
  font-size:.85rem; background:white; cursor:pointer;
  appearance:none;
  background-image:url("data:image/svg+xml;charset=UTF-8,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%2364748b' stroke-width='2'%3e%3cpolyline points='6 9 12 15 18 9'%3e%3c/polyline%3e%3c/svg%3e");
  background-repeat:no-repeat; background-position:right .6rem center; background-size:1em;
}

.ps-toolbar-btns { display:flex; gap:.6rem; }

/* ══ BUTTONS ══ */
.ps-btn {
  padding:.6rem 1rem; border:none; border-radius:.6rem;
  font-size:.85rem; font-weight:600; cursor:pointer;
  display:inline-flex; align-items:center; gap:.4rem;
  transition:filter .15s; white-space:nowrap;
}
.ps-btn:hover:not(:disabled) { filter:brightness(.92); }
.ps-btn:disabled { opacity:.55; cursor:not-allowed; }
.ps-btn.blue   { background:var(--blue);   color:white; }
.ps-btn.purple { background:var(--purple); color:white; }
.ps-btn.red    { background:var(--red);    color:white; }
.ps-btn.teal   { background:var(--teal);   color:white; }
.ps-btn.gray   { background:#e2e8f0; color:var(--dark); }

/* ══ MESSAGES ══ */
.ps-msg {
  display:flex; align-items:center; gap:.75rem;
  padding:.85rem 1rem; border-radius:.6rem; margin-bottom:1rem; font-size:.875rem;
}
.ps-msg button { margin-left:auto; background:none; border:none; cursor:pointer; font-size:1rem; }
.ps-msg.error  { background:var(--red-l); color:var(--red); border-left:4px solid var(--red); }
.ps-loading {
  display:flex; align-items:center; justify-content:center; gap:.75rem;
  padding:2.5rem; color:var(--gray); font-size:.95rem;
}

/* ══ TABLE ══ */
.ps-table-wrapper {
  background:white; border-radius:var(--radius); box-shadow:var(--shadow);
  overflow:hidden;
}
.ps-table {
  width:100%; border-collapse:collapse; font-size:.875rem;
}
.ps-table thead { position:sticky; top:0; z-index:2; }
.ps-table th {
  text-align:left; padding:.75rem 1rem;
  background:#f8fafc; color:var(--gray);
  font-size:.72rem; text-transform:uppercase; letter-spacing:.05em;
  border-bottom:2px solid #e2e8f0; user-select:none;
}
.ps-table th.sortable { cursor:pointer; }
.ps-table th.sortable:hover { color:var(--dark); }
.ps-table th.right { text-align:right; }
.ps-table th.center { text-align:center; }
.ps-table td {
  padding:.75rem 1rem;
  border-bottom:1px solid #f1f5f9;
  vertical-align:middle;
}
.ps-table tr:last-child td { border-bottom:none; }
.ps-row:hover td { background:#f8fafc; }
.right  { text-align:right; }
.center { text-align:center; }
.text-muted { opacity:.4; }

/* ── Product cell ── */
.ps-prod-cell {
  display:flex; align-items:center; gap:.75rem;
}
.ps-prod-icon {
  width:2.2rem; height:2.2rem; border-radius:.5rem;
  display:flex; align-items:center; justify-content:center;
  font-size:.95rem; font-weight:700; color:white; flex-shrink:0;
}
.ps-prod-name  { font-weight:600; color:var(--dark); }

/* ── Chips & badges ── */
.ps-chip { padding:.15rem .5rem; border-radius:9999px; font-size:.72rem; font-weight:600; }
.gray-chip { background:var(--gray-l); color:var(--gray); }

.ps-cat-badge {
  padding:.2rem .65rem; border-radius:9999px;
  font-size:.75rem; font-weight:600; white-space:nowrap;
}

.ps-price-badge {
  font-size:.875rem; font-weight:700; color:var(--blue);
  background:var(--blue-l); padding:.2rem .6rem; border-radius:9999px;
}

.ps-desc {
  color:var(--gray); font-size:.8rem; font-style:italic;
  max-width:200px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;
}

/* ── Actions ── */
.ps-actions { display:flex; gap:.4rem; justify-content:center; }
.ps-icon-btn {
  width:2rem; height:2rem; border:none; border-radius:.45rem;
  display:flex; align-items:center; justify-content:center;
  font-size:.85rem; cursor:pointer; transition:filter .15s;
}
.ps-icon-btn:hover { filter:brightness(.88); }
.blue-btn { background:var(--blue-l); color:var(--blue); }
.red-btn  { background:var(--red-l);  color:var(--red);  }

/* ── Empty state ── */
.ps-empty-state {
  display:flex; flex-direction:column; align-items:center; justify-content:center;
  padding:3.5rem 2rem; color:var(--gray); text-align:center; gap:.75rem;
}
.ps-empty-state i { font-size:2.5rem; color:#cbd5e1; }
.ps-empty-state p { margin:0; font-size:1rem; }

/* ══ PAGINATION ══ */
.ps-pagination {
  display:flex; align-items:center; justify-content:center; gap:1rem;
  margin-top:1.25rem;
}
.ps-pg-btn {
  width:2.2rem; height:2.2rem; border:1px solid #e2e8f0;
  border-radius:.5rem; background:white; cursor:pointer; font-size:.9rem; color:var(--gray);
  display:flex; align-items:center; justify-content:center; transition:all .15s;
}
.ps-pg-btn:hover:not(:disabled) { border-color:var(--blue); color:var(--blue); }
.ps-pg-btn:disabled { opacity:.4; cursor:not-allowed; }
.ps-pg-info { font-size:.85rem; color:var(--gray); font-weight:600; }

/* ══ MODALS ══ */
.ps-overlay {
  position:fixed; inset:0; background:rgba(15,23,42,.45);
  display:flex; align-items:center; justify-content:center;
  z-index:1000; padding:1rem; backdrop-filter:blur(2px);
}
.ps-modal {
  background:white; border-radius:var(--radius);
  width:100%; max-width:520px;
  box-shadow:0 20px 40px rgba(0,0,0,.15);
  overflow:hidden; display:flex; flex-direction:column;
  max-height:90vh;
}
.ps-modal-head {
  padding:1rem 1.25rem; border-bottom:1px solid #e2e8f0;
  display:flex; justify-content:space-between; align-items:center;
  background:#f8fafc; font-size:.95rem; font-weight:700; color:var(--dark);
}
.ps-modal-head.blue-head   { background:var(--blue-l);   color:var(--blue); }
.ps-modal-head.purple-head { background:var(--purple-l); color:var(--purple); }
.ps-modal-head.red-head    { background:var(--red-l);    color:var(--red); }
.ps-modal-head button { background:none; border:none; font-size:1.4rem; line-height:1; cursor:pointer; color:inherit; }

.ps-modal-body { padding:1.25rem; overflow-y:auto; flex:1; }
.ps-modal-foot {
  padding:.9rem 1.25rem; border-top:1px solid #e2e8f0;
  display:flex; justify-content:flex-end; gap:.6rem;
}

/* ── Fields ── */
.ps-field { margin-bottom:1rem; }
.ps-field label { display:block; font-size:.82rem; font-weight:600; color:var(--dark); margin-bottom:.35rem; }
.ps-field small { font-size:.72rem; color:var(--gray); margin-top:.2rem; display:block; }
.ps-field input,
.ps-field textarea {
  width:100%; padding:.6rem .75rem; box-sizing:border-box;
  border:1.5px solid #e2e8f0; border-radius:.5rem;
  font-size:.875rem; background:white; transition:border-color .2s;
  font-family:inherit; resize:vertical;
}
.ps-field input:focus, .ps-field textarea:focus { outline:none; border-color:var(--blue); }
.ps-field input.disabled-field { background:#f8fafc; color:var(--gray); cursor:not-allowed; }

/* ── Category picker ── */
.ps-cat-picker {
  display:grid; grid-template-columns:repeat(5,1fr); gap:.4rem;
}
.ps-cat-opt {
  display:flex; flex-direction:column; align-items:center; gap:.25rem;
  padding:.45rem .25rem; border-radius:.5rem; cursor:pointer;
  border:1.5px solid #e2e8f0; font-size:.7rem; color:var(--gray);
  transition:all .15s; text-align:center;
}
.ps-cat-opt i { font-size:.95rem; }
.ps-cat-opt:hover { border-color:var(--purple); color:var(--purple); background:var(--purple-l); }
.ps-cat-opt.selected { border-color:var(--purple); color:var(--purple); background:var(--purple-l); font-weight:600; }

/* ── Price input ── */
.ps-price-input {
  display:flex; align-items:center; overflow:hidden;
  border:1.5px solid #e2e8f0; border-radius:.5rem;
}
.ps-currency {
  padding:.6rem .75rem; background:#f8fafc; color:var(--gray);
  font-size:.8rem; font-weight:600; border-right:1.5px solid #e2e8f0; white-space:nowrap;
}
.ps-price-input input {
  border:none; border-radius:0; flex:1;
  padding:.6rem .75rem; font-size:.875rem; box-sizing:border-box;
}
.ps-price-input input:focus { outline:none; }

/* ── Confirm modal ── */
.ps-confirm-body { text-align:center; padding:1.5rem 1.25rem; }
.ps-confirm-icon {
  width:3.5rem; height:3.5rem; background:var(--red-l); color:var(--red);
  border-radius:50%; display:flex; align-items:center; justify-content:center;
  font-size:1.5rem; margin:0 auto 1rem;
}
.ps-confirm-body p { margin:.25rem 0; font-size:.9rem; color:var(--gray); }
.ps-confirm-target {
  display:flex; align-items:center; justify-content:center; gap:.5rem;
  margin:.75rem 0; font-size:1rem;
}
.ps-warn-text { color:var(--red) !important; font-size:.8rem !important; font-weight:600; }

/* ══ ANIMATIONS ══ */
.fade-enter-active, .fade-leave-active { transition:opacity .18s; }
.fade-enter-from,  .fade-leave-to      { opacity:0; }

/* ══ RESPONSIVE ══ */
@media (max-width:768px) {
  .ps-container { padding:1rem; }
  .ps-stats { grid-template-columns:1fr 1fr; }
  .ps-toolbar { flex-direction:column; align-items:stretch; }
  .ps-toolbar-btns { justify-content:flex-end; }
  .ps-cat-picker { grid-template-columns:repeat(3,1fr); }
  .ps-table { font-size:.78rem; }
  .ps-table td, .ps-table th { padding:.5rem .6rem; }
  .ps-desc { max-width:100px; }
}
@media (max-width:480px) {
  .ps-stats { grid-template-columns:1fr; }
  .ps-cat-picker { grid-template-columns:repeat(2,1fr); }
}
</style>