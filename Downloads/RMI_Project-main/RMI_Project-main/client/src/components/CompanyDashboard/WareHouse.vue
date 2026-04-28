<template>
  <div class="wh-container">

    <!-- ── STATS ── -->
    <div class="wh-stats">
      <div class="wh-stat blue">
        <div class="wh-stat-icon"><i class="fas fa-warehouse"></i></div>
        <div>
          <div class="wh-stat-label">Entrepôts</div>
          <div class="wh-stat-val">{{ entrepots.length }}</div>
        </div>
      </div>
      <div class="wh-stat green">
        <div class="wh-stat-icon"><i class="fas fa-cubes"></i></div>
        <div>
          <div class="wh-stat-label">Total Produits</div>
          <div class="wh-stat-val">{{ totalProduits }}</div>
        </div>
      </div>
      <div class="wh-stat purple">
        <div class="wh-stat-icon"><i class="fas fa-filter"></i></div>
        <div>
          <div class="wh-stat-label">Filtrés</div>
          <div class="wh-stat-val">{{ filteredEntrepots.length }}</div>
        </div>
      </div>
    </div>

    <!-- ── TOOLBAR ── -->
    <div class="wh-toolbar">
      <div class="wh-search">
        <i class="fas fa-search"></i>
        <input v-model="search" placeholder="Rechercher un entrepôt…" />
      </div>
      <button class="wh-btn blue" @click="openAdd">
        <i class="fas fa-plus"></i> Nouvel Entrepôt
      </button>
      <button class="wh-btn teal" @click="fetchAll" :disabled="loading">
        <i class="fas fa-sync-alt" :class="{ 'fa-spin': loading }"></i>
      </button>
    </div>

    <!-- ── ERROR ── -->
    <div v-if="error" class="wh-error">
      <i class="fas fa-exclamation-circle"></i> {{ error }}
      <button @click="error = null">×</button>
    </div>

    <!-- ── LOADING ── -->
    <div v-if="loading && !entrepots.length" class="wh-loading">
      <i class="fas fa-spinner fa-spin"></i> Chargement…
    </div>

    <!-- ── GRID ── -->
    <div class="wh-grid" v-if="!loading || entrepots.length">

      <div v-if="!filteredEntrepots.length && !loading" class="wh-empty">
        <i class="fas fa-warehouse"></i>
        <p>Aucun entrepôt trouvé</p>
        <button class="wh-btn blue" @click="openAdd">
          <i class="fas fa-plus"></i> Ajouter
        </button>
      </div>

      <div
          v-for="e in filteredEntrepots"
          :key="e.id"
          class="wh-card"
      >
        <!-- card header -->
        <div class="wh-card-head">
          <div class="wh-card-icon"><i class="fas fa-warehouse"></i></div>
          <div class="wh-card-info">
            <h3>{{ e.nom }}</h3>
            <span class="wh-ville"><i class="fas fa-map-marker-alt"></i> {{ e.ville || '—' }}</span>
          </div>
          <div class="wh-card-actions">
            <button class="wh-icon-btn edit-btn" @click="openEdit(e)" title="Modifier">
              <i class="fas fa-edit"></i>
            </button>
            <button class="wh-icon-btn del-btn" @click="openDelete(e)" title="Supprimer">
              <i class="fas fa-trash-alt"></i>
            </button>
          </div>
        </div>

        <!-- stocks section -->
        <div class="wh-card-body">
          <div class="wh-section-head">
            <h4><i class="fas fa-boxes"></i> Stocks</h4>
            <div class="wh-section-btns">
              <button class="wh-btn-sm green" @click="openEntree(e)">
                <i class="fas fa-arrow-down"></i> Entrée
              </button>
              <button class="wh-btn-sm red" @click="openSortie(e)">
                <i class="fas fa-arrow-up"></i> Sortie
              </button>
              <button class="wh-btn-sm purple" @click="openTransfert(e)">
                <i class="fas fa-exchange-alt"></i> Transfert
              </button>
            </div>
          </div>

          <div v-if="e.loadingStocks" class="wh-stocks-loading">
            <i class="fas fa-spinner fa-spin"></i>
          </div>

          <div v-else-if="e.stocks && e.stocks.length" class="wh-stocks-list">
            <div v-for="s in e.stocks" :key="s.idProduit" class="wh-stock-row">
              <span class="wh-prod-name">{{ getProduitNom(s.idProduit) }}</span>
              <span class="wh-qty-badge" :class="s.quantite <= 5 ? 'low' : ''">
                {{ s.quantite }}
              </span>
            </div>
          </div>

          <div v-else class="wh-no-stock">
            <i class="fas fa-box-open"></i> Aucun stock
          </div>
        </div>
      </div>
    </div>

    <!-- ══════════════ MODAL : AJOUTER / MODIFIER ENTREPÔT ══════════════ -->
    <transition name="fade">
      <div v-if="showModal" class="wh-overlay" @click.self="closeModal">
        <div class="wh-modal">
          <div class="wh-modal-head" :class="editMode ? 'blue-head' : 'green-head'">
            <span>
              <i :class="editMode ? 'fas fa-edit' : 'fas fa-plus-circle'"></i>
              {{ editMode ? 'Modifier Entrepôt' : 'Nouvel Entrepôt' }}
            </span>
            <button @click="closeModal">×</button>
          </div>
          <div class="wh-modal-body">
            <div class="wh-field">
              <label>ID *</label>
              <input v-model="form.id" placeholder="ex: E01" :disabled="editMode" :class="{ disabled: editMode }" maxlength="10" />
              <small>Identifiant unique non modifiable après création</small>
            </div>
            <div class="wh-field">
              <label>Nom *</label>
              <input v-model="form.nom" placeholder="Nom de l'entrepôt" maxlength="100" />
            </div>
            <div class="wh-field">
              <label>Ville</label>
              <input v-model="form.ville" placeholder="Ville / Localisation" maxlength="100" />
            </div>
          </div>
          <div class="wh-modal-foot">
            <button class="wh-btn gray" @click="closeModal">Annuler</button>
            <button
                :class="editMode ? 'wh-btn blue' : 'wh-btn green'"
                @click="saveEntrepot"
                :disabled="submitting || !form.id || !form.nom"
            >
              <i v-if="submitting" class="fas fa-spinner fa-spin"></i>
              <span v-else>{{ editMode ? 'Enregistrer' : 'Créer' }}</span>
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- ══════════════ MODAL : SUPPRIMER ══════════════ -->
    <transition name="fade">
      <div v-if="showDeleteModal" class="wh-overlay" @click.self="showDeleteModal = false">
        <div class="wh-modal" style="max-width:400px">
          <div class="wh-modal-head red-head">
            <span><i class="fas fa-exclamation-triangle"></i> Confirmer suppression</span>
            <button @click="showDeleteModal = false">×</button>
          </div>
          <div class="wh-modal-body wh-confirm-body">
            <div class="wh-confirm-icon"><i class="fas fa-trash-alt"></i></div>
            <p>Supprimer l'entrepôt :</p>
            <strong>{{ toDelete?.nom }}</strong>
            <p class="wh-warn">Cette action est irréversible.</p>
          </div>
          <div class="wh-modal-foot">
            <button class="wh-btn gray" @click="showDeleteModal = false">Annuler</button>
            <button class="wh-btn red" @click="deleteEntrepot" :disabled="submitting">
              <i v-if="submitting" class="fas fa-spinner fa-spin"></i>
              <span v-else>Supprimer</span>
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- ══════════════ MODAL : ENTRÉE STOCK ══════════════ -->
    <transition name="fade">
      <div v-if="showEntreeModal" class="wh-overlay" @click.self="showEntreeModal = false">
        <div class="wh-modal">
          <div class="wh-modal-head green-head">
            <span><i class="fas fa-arrow-down"></i> Entrée Stock — {{ mvtEntrepot?.nom }}</span>
            <button @click="showEntreeModal = false">×</button>
          </div>
          <div class="wh-modal-body">
            <div class="wh-field">
              <label>Produit *</label>
              <select v-model="mvtForm.idProduit">
                <option value="">— Sélectionner —</option>
                <option v-for="p in produits" :key="p.id" :value="p.id">{{ p.nom }} ({{ p.id }})</option>
              </select>
            </div>
            <div class="wh-field">
              <label>Quantité *</label>
              <input type="number" v-model.number="mvtForm.quantite" min="1" />
            </div>
            <div class="wh-field">
              <label>Note</label>
              <input v-model="mvtForm.note" placeholder="Optionnel" />
            </div>
          </div>
          <div class="wh-modal-foot">
            <button class="wh-btn gray" @click="showEntreeModal = false">Annuler</button>
            <button class="wh-btn green" @click="submitEntree" :disabled="submitting || !mvtForm.idProduit || mvtForm.quantite <= 0">
              <i v-if="submitting" class="fas fa-spinner fa-spin"></i>
              <span v-else><i class="fas fa-check"></i> Valider</span>
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- ══════════════ MODAL : SORTIE STOCK ══════════════ -->
    <transition name="fade">
      <div v-if="showSortieModal" class="wh-overlay" @click.self="showSortieModal = false">
        <div class="wh-modal">
          <div class="wh-modal-head red-head">
            <span><i class="fas fa-arrow-up"></i> Sortie Stock — {{ mvtEntrepot?.nom }}</span>
            <button @click="showSortieModal = false">×</button>
          </div>
          <div class="wh-modal-body">
            <div class="wh-field">
              <label>Produit *</label>
              <select v-model="mvtForm.idProduit">
                <option value="">— Sélectionner —</option>
                <option v-for="p in produits" :key="p.id" :value="p.id">{{ p.nom }} ({{ p.id }})</option>
              </select>
            </div>
            <div class="wh-field">
              <label>Quantité *</label>
              <input type="number" v-model.number="mvtForm.quantite" min="1" />
            </div>
            <div class="wh-field">
              <label>Note</label>
              <input v-model="mvtForm.note" placeholder="Optionnel" />
            </div>
          </div>
          <div class="wh-modal-foot">
            <button class="wh-btn gray" @click="showSortieModal = false">Annuler</button>
            <button class="wh-btn red" @click="submitSortie" :disabled="submitting || !mvtForm.idProduit || mvtForm.quantite <= 0">
              <i v-if="submitting" class="fas fa-spinner fa-spin"></i>
              <span v-else><i class="fas fa-check"></i> Valider</span>
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- ══════════════ MODAL : TRANSFERT ══════════════ -->
    <transition name="fade">
      <div v-if="showTransfertModal" class="wh-overlay" @click.self="showTransfertModal = false">
        <div class="wh-modal">
          <div class="wh-modal-head purple-head">
            <span><i class="fas fa-exchange-alt"></i> Transfert — depuis {{ mvtEntrepot?.nom }}</span>
            <button @click="showTransfertModal = false">×</button>
          </div>
          <div class="wh-modal-body">
            <div class="wh-field">
              <label>Entrepôt destination *</label>
              <select v-model="mvtForm.idDest">
                <option value="">— Sélectionner —</option>
                <option
                    v-for="e in entrepots.filter(x => x.id !== mvtEntrepot?.id)"
                    :key="e.id" :value="e.id"
                >{{ e.nom }}</option>
              </select>
            </div>
            <div class="wh-field">
              <label>Produit *</label>
              <select v-model="mvtForm.idProduit">
                <option value="">— Sélectionner —</option>
                <option v-for="p in produits" :key="p.id" :value="p.id">{{ p.nom }} ({{ p.id }})</option>
              </select>
            </div>
            <div class="wh-field">
              <label>Quantité *</label>
              <input type="number" v-model.number="mvtForm.quantite" min="1" />
            </div>
          </div>
          <div class="wh-modal-foot">
            <button class="wh-btn gray" @click="showTransfertModal = false">Annuler</button>
            <button
                class="wh-btn purple"
                @click="submitTransfert"
                :disabled="submitting || !mvtForm.idProduit || !mvtForm.idDest || mvtForm.quantite <= 0"
            >
              <i v-if="submitting" class="fas fa-spinner fa-spin"></i>
              <span v-else><i class="fas fa-check"></i> Transférer</span>
            </button>
          </div>
        </div>
      </div>
    </transition>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'

// ─── TYPES ───────────────────────────────────────────────────────
interface Entrepot {
  id: string
  nom: string
  ville?: string
  stocks?: StockItem[]
  loadingStocks?: boolean
}

interface StockItem {
  idProduit: string
  quantite: number
}

interface Produit {
  id: string
  nom: string
  categorie?: string
  prixUnitaire: number
}

// ─── API BASE ─────────────────────────────────────────────────────
const BASE = 'http://localhost:8080/api'

async function apiFetch(path: string, options?: RequestInit) {
  const r = await fetch(BASE + path, options)
  if (!r.ok) throw new Error(await r.text().catch(() => `HTTP ${r.status}`))
  return r.json()
}

// ─── STATE ────────────────────────────────────────────────────────
const entrepots  = ref<Entrepot[]>([])
const produits   = ref<Produit[]>([])
const loading    = ref(false)
const submitting = ref(false)
const error      = ref<string | null>(null)
const search     = ref('')

// modals
const showModal        = ref(false)
const showDeleteModal  = ref(false)
const showEntreeModal  = ref(false)
const showSortieModal  = ref(false)
const showTransfertModal = ref(false)
const editMode         = ref(false)
const toDelete         = ref<Entrepot | null>(null)
const mvtEntrepot      = ref<Entrepot | null>(null)

const form = ref({ id: '', nom: '', ville: '' })
const mvtForm = ref({ idProduit: '', quantite: 1, note: '', idDest: '' })

// ─── COMPUTED ─────────────────────────────────────────────────────
const filteredEntrepots = computed(() => {
  if (!search.value) return entrepots.value
  const t = search.value.toLowerCase()
  return entrepots.value.filter(e =>
      e.nom.toLowerCase().includes(t) ||
      e.id.toLowerCase().includes(t) ||
      (e.ville ?? '').toLowerCase().includes(t)
  )
})

const totalProduits = computed(() =>
    entrepots.value.reduce((sum, e) =>
        sum + (e.stocks?.reduce((s, st) => s + st.quantite, 0) ?? 0), 0)
)

// ─── HELPERS ──────────────────────────────────────────────────────
function getProduitNom(id: string): string {
  return produits.value.find(p => p.id === id)?.nom ?? id
}

function resetMvt() {
  mvtForm.value = { idProduit: '', quantite: 1, note: '', idDest: '' }
}

function closeModal() {
  showModal.value = false
}

// ─── FETCH ────────────────────────────────────────────────────────
async function fetchProduits() {
  try {
    produits.value = await apiFetch('/produits')
  } catch (e: any) {
    console.error('Erreur produits:', e.message)
  }
}

async function fetchStocks(e: Entrepot) {
  e.loadingStocks = true
  try {
    const data = await apiFetch(`/stocks/entrepot?id=${e.id}`)
    e.stocks = data.stocks ?? []
  } catch {
    e.stocks = []
  } finally {
    e.loadingStocks = false
  }
}

async function fetchAll() {
  loading.value = true
  error.value = null
  try {
    // GET /api/entrepots → [{id, nom}]
    const raw: {id: string, nom: string}[] = await apiFetch('/entrepots')

    // Reconstruire en extrayant la ville du format "Nom (Ville)"
    entrepots.value = raw.map(r => {
      const match = r.nom.match(/^(.*?)\s*\(([^)]+)\)$/)
      return {
        id: r.id,
        nom: match ? match[1].trim() : r.nom,
        ville: match ? match[2].trim() : '',
        stocks: [],
        loadingStocks: false
      }
    })

    // Charger les stocks de chaque entrepôt en parallèle
    await Promise.all(entrepots.value.map(e => fetchStocks(e)))
  } catch (e: any) {
    error.value = e.message ?? 'Erreur de chargement'
  } finally {
    loading.value = false
  }
}

// ─── CRUD ENTREPÔT ────────────────────────────────────────────────
function openAdd() {
  form.value = { id: '', nom: '', ville: '' }
  editMode.value = false
  showModal.value = true
}

function openEdit(e: Entrepot) {
  form.value = { id: e.id, nom: e.nom, ville: e.ville ?? '' }
  editMode.value = true
  showModal.value = true
}

function openDelete(e: Entrepot) {
  toDelete.value = e
  showDeleteModal.value = true
}

async function saveEntrepot() {
  if (!form.value.id || !form.value.nom) return
  submitting.value = true
  try {
    if (!editMode.value) {
      // POST /api/entrepots
      await apiFetch('/entrepots', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ id: form.value.id, nom: form.value.nom, ville: form.value.ville })
      })
    }
    // Note: pas d'endpoint PUT entrepôt dans HttpBridge — à ajouter si besoin
    closeModal()
    await fetchAll()
  } catch (e: any) {
    error.value = e.message
  } finally {
    submitting.value = false
  }
}

async function deleteEntrepot() {
  if (!toDelete.value) return
  submitting.value = true
  try {
    // Note: pas d'endpoint DELETE entrepôt — à ajouter dans HttpBridge si besoin
    // Pour l'instant on simule localement
    entrepots.value = entrepots.value.filter(e => e.id !== toDelete.value!.id)
    showDeleteModal.value = false
  } catch (e: any) {
    error.value = e.message
  } finally {
    submitting.value = false
  }
}

// ─── MOUVEMENTS ───────────────────────────────────────────────────
function openEntree(e: Entrepot) {
  mvtEntrepot.value = e
  resetMvt()
  showEntreeModal.value = true
}

function openSortie(e: Entrepot) {
  mvtEntrepot.value = e
  resetMvt()
  showSortieModal.value = true
}

function openTransfert(e: Entrepot) {
  mvtEntrepot.value = e
  resetMvt()
  showTransfertModal.value = true
}

async function submitEntree() {
  if (!mvtEntrepot.value) return
  submitting.value = true
  try {
    await apiFetch('/stocks/entree', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        idEntrepot: mvtEntrepot.value.id,
        idProduit: mvtForm.value.idProduit,
        quantite: mvtForm.value.quantite,
        note: mvtForm.value.note
      })
    })
    showEntreeModal.value = false
    await fetchStocks(mvtEntrepot.value)
  } catch (e: any) {
    error.value = e.message
  } finally {
    submitting.value = false
  }
}

async function submitSortie() {
  if (!mvtEntrepot.value) return
  submitting.value = true
  try {
    await apiFetch('/stocks/sortie', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        idEntrepot: mvtEntrepot.value.id,
        idProduit: mvtForm.value.idProduit,
        quantite: mvtForm.value.quantite,
        note: mvtForm.value.note
      })
    })
    showSortieModal.value = false
    await fetchStocks(mvtEntrepot.value)
  } catch (e: any) {
    error.value = e.message
  } finally {
    submitting.value = false
  }
}

async function submitTransfert() {
  if (!mvtEntrepot.value) return
  submitting.value = true
  try {
    await apiFetch('/stocks/transfert', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        idSrc: mvtEntrepot.value.id,
        idDest: mvtForm.value.idDest,
        idProduit: mvtForm.value.idProduit,
        quantite: mvtForm.value.quantite
      })
    })
    showTransfertModal.value = false
    await fetchAll()
  } catch (e: any) {
    error.value = e.message
  } finally {
    submitting.value = false
  }
}

// ─── INIT ─────────────────────────────────────────────────────────
onMounted(async () => {
  await Promise.all([fetchAll(), fetchProduits()])
})
</script>

<style scoped>
.wh-container {
  --blue:   #2563eb; --blue-l:   #dbeafe;
  --green:  #16a34a; --green-l:  #dcfce7;
  --red:    #dc2626; --red-l:    #fee2e2;
  --purple: #7c3aed; --purple-l: #ede9fe;
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

/* ── STATS ── */
.wh-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
  margin-bottom: 1.5rem;
}
.wh-stat {
  background: white; border-radius: var(--radius);
  padding: 1.25rem; box-shadow: var(--shadow);
  display: flex; align-items: center; gap: 1rem;
  border-left: 4px solid transparent;
}
.wh-stat.blue   { border-color: var(--blue); }
.wh-stat.green  { border-color: var(--green); }
.wh-stat.purple { border-color: var(--purple); }

.wh-stat-icon {
  width: 2.75rem; height: 2.75rem; border-radius: .6rem;
  display: flex; align-items: center; justify-content: center;
  font-size: 1.1rem;
}
.blue  .wh-stat-icon { background: var(--blue-l);   color: var(--blue); }
.green .wh-stat-icon { background: var(--green-l);  color: var(--green); }
.purple .wh-stat-icon { background: var(--purple-l); color: var(--purple); }

.wh-stat-label { font-size: .75rem; color: var(--gray); font-weight: 500; margin-bottom: .25rem; }
.wh-stat-val   { font-size: 1.6rem; font-weight: 700; color: var(--dark); line-height: 1; }

/* ── TOOLBAR ── */
.wh-toolbar {
  display: flex; gap: .75rem; align-items: center;
  margin-bottom: 1.25rem; flex-wrap: wrap;
}
.wh-search {
  flex: 1; min-width: 220px; position: relative;
}
.wh-search i { position: absolute; left: .85rem; top: 50%; transform: translateY(-50%); color: var(--gray); }
.wh-search input {
  width: 100%; padding: .65rem 1rem .65rem 2.25rem;
  border: 1px solid #e2e8f0; border-radius: .6rem;
  font-size: .875rem; background: white; box-sizing: border-box;
  transition: border-color .2s;
}
.wh-search input:focus { outline: none; border-color: var(--blue); }

/* ── BUTTONS ── */
.wh-btn {
  padding: .6rem 1rem; border: none; border-radius: .6rem;
  font-size: .85rem; font-weight: 600; cursor: pointer;
  display: inline-flex; align-items: center; gap: .4rem;
  transition: filter .15s; white-space: nowrap;
}
.wh-btn:hover:not(:disabled) { filter: brightness(.92); }
.wh-btn:disabled { opacity: .55; cursor: not-allowed; }
.wh-btn.blue   { background: var(--blue);   color: white; }
.wh-btn.green  { background: var(--green);  color: white; }
.wh-btn.red    { background: var(--red);    color: white; }
.wh-btn.purple { background: var(--purple); color: white; }
.wh-btn.teal   { background: var(--teal);   color: white; }
.wh-btn.gray   { background: #e2e8f0; color: var(--dark); }

.wh-btn-sm {
  padding: .35rem .7rem; border: none; border-radius: .45rem;
  font-size: .75rem; font-weight: 600; cursor: pointer;
  display: inline-flex; align-items: center; gap: .3rem;
  transition: filter .15s;
}
.wh-btn-sm:hover { filter: brightness(.9); }
.wh-btn-sm.green  { background: var(--green-l);  color: var(--green); }
.wh-btn-sm.red    { background: var(--red-l);    color: var(--red); }
.wh-btn-sm.purple { background: var(--purple-l); color: var(--purple); }

/* ── ERROR / LOADING ── */
.wh-error {
  display: flex; align-items: center; gap: .75rem;
  padding: .85rem 1rem; border-radius: .6rem; margin-bottom: 1rem;
  background: var(--red-l); color: var(--red);
  border-left: 4px solid var(--red); font-size: .875rem;
}
.wh-error button { margin-left: auto; background: none; border: none; cursor: pointer; font-size: 1rem; color: var(--red); }
.wh-loading {
  display: flex; align-items: center; justify-content: center;
  gap: .75rem; padding: 2.5rem; color: var(--gray);
}

/* ── GRID ── */
.wh-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 1.25rem;
}

.wh-empty {
  grid-column: 1 / -1;
  display: flex; flex-direction: column; align-items: center;
  padding: 3rem; background: white; border-radius: var(--radius);
  box-shadow: var(--shadow); color: var(--gray); gap: .75rem;
}
.wh-empty i { font-size: 2.5rem; color: #cbd5e1; }

/* ── CARD ── */
.wh-card {
  background: white; border-radius: var(--radius);
  box-shadow: var(--shadow); overflow: hidden;
  transition: transform .2s, box-shadow .2s;
}
.wh-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0,0,0,.1);
}

.wh-card-head {
  display: flex; align-items: center; gap: 1rem;
  padding: 1.25rem; background: #f8fafc;
  border-bottom: 1px solid #f1f5f9;
}
.wh-card-icon {
  width: 2.75rem; height: 2.75rem; border-radius: .6rem;
  background: var(--blue-l); color: var(--blue);
  display: flex; align-items: center; justify-content: center;
  font-size: 1.1rem; flex-shrink: 0;
}
.wh-card-info { flex: 1; min-width: 0; }
.wh-card-info h3 { margin: 0 0 .25rem; font-size: 1rem; font-weight: 700; color: var(--dark); }
.wh-ville { font-size: .78rem; color: var(--gray); display: flex; align-items: center; gap: .3rem; }

.wh-card-actions { display: flex; gap: .4rem; }
.wh-icon-btn {
  width: 2rem; height: 2rem; border: none; border-radius: .45rem;
  display: flex; align-items: center; justify-content: center;
  font-size: .85rem; cursor: pointer; transition: filter .15s;
}
.wh-icon-btn:hover { filter: brightness(.88); }
.edit-btn { background: var(--blue-l); color: var(--blue); }
.del-btn  { background: var(--red-l);  color: var(--red); }

/* ── CARD BODY ── */
.wh-card-body { padding: 1.25rem; }

.wh-section-head {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 1rem; flex-wrap: wrap; gap: .5rem;
}
.wh-section-head h4 {
  margin: 0; font-size: .875rem; font-weight: 700;
  color: var(--dark); display: flex; align-items: center; gap: .4rem;
}
.wh-section-btns { display: flex; gap: .4rem; flex-wrap: wrap; }

.wh-stocks-loading {
  text-align: center; padding: 1rem; color: var(--gray);
}
.wh-no-stock {
  display: flex; align-items: center; justify-content: center;
  gap: .5rem; padding: 1rem; color: var(--gray);
  background: #f8fafc; border-radius: .5rem; font-size: .85rem;
}

.wh-stocks-list { display: flex; flex-direction: column; gap: .4rem; }
.wh-stock-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: .5rem .75rem; background: #f8fafc; border-radius: .45rem;
}
.wh-prod-name { font-size: .85rem; color: var(--dark); font-weight: 500; }
.wh-qty-badge {
  padding: .15rem .55rem; border-radius: 9999px;
  font-size: .75rem; font-weight: 700;
  background: var(--blue-l); color: var(--blue);
}
.wh-qty-badge.low { background: var(--red-l); color: var(--red); }

/* ── MODALS ── */
.wh-overlay {
  position: fixed; inset: 0; background: rgba(15,23,42,.45);
  display: flex; align-items: center; justify-content: center;
  z-index: 1000; padding: 1rem; backdrop-filter: blur(2px);
}
.wh-modal {
  background: white; border-radius: var(--radius);
  width: 100%; max-width: 480px; max-height: 90vh;
  box-shadow: 0 20px 40px rgba(0,0,0,.15);
  overflow: hidden; display: flex; flex-direction: column;
}
.wh-modal-head {
  padding: 1rem 1.25rem; border-bottom: 1px solid #e2e8f0;
  display: flex; justify-content: space-between; align-items: center;
  font-size: .95rem; font-weight: 700;
}
.wh-modal-head button { background: none; border: none; font-size: 1.4rem; cursor: pointer; color: inherit; }
.blue-head   { background: var(--blue-l);   color: var(--blue); }
.green-head  { background: var(--green-l);  color: var(--green); }
.red-head    { background: var(--red-l);    color: var(--red); }
.purple-head { background: var(--purple-l); color: var(--purple); }

.wh-modal-body { padding: 1.25rem; overflow-y: auto; flex: 1; }
.wh-modal-foot {
  padding: .9rem 1.25rem; border-top: 1px solid #e2e8f0;
  display: flex; justify-content: flex-end; gap: .6rem;
}

.wh-field { margin-bottom: 1rem; }
.wh-field label { display: block; font-size: .82rem; font-weight: 600; color: var(--dark); margin-bottom: .35rem; }
.wh-field small { font-size: .72rem; color: var(--gray); margin-top: .2rem; display: block; }
.wh-field input,
.wh-field select {
  width: 100%; padding: .6rem .75rem; box-sizing: border-box;
  border: 1.5px solid #e2e8f0; border-radius: .5rem;
  font-size: .875rem; background: white; transition: border-color .2s;
  font-family: inherit;
}
.wh-field input:focus,
.wh-field select:focus { outline: none; border-color: var(--blue); }
.wh-field input.disabled { background: #f8fafc; color: var(--gray); cursor: not-allowed; }

.wh-confirm-body { text-align: center; padding: 1.5rem; }
.wh-confirm-icon {
  width: 3.5rem; height: 3.5rem; background: var(--red-l); color: var(--red);
  border-radius: 50%; display: flex; align-items: center; justify-content: center;
  font-size: 1.5rem; margin: 0 auto 1rem;
}
.wh-confirm-body p { color: var(--gray); font-size: .9rem; margin: .25rem 0; }
.wh-warn { color: var(--red) !important; font-weight: 600; font-size: .8rem !important; }

/* ── ANIMATIONS ── */
.fade-enter-active, .fade-leave-active { transition: opacity .18s; }
.fade-enter-from,  .fade-leave-to      { opacity: 0; }

/* ── RESPONSIVE ── */
@media (max-width: 768px) {
  .wh-container { padding: 1rem; }
  .wh-stats { grid-template-columns: 1fr 1fr; }
  .wh-grid  { grid-template-columns: 1fr; }
  .wh-toolbar { flex-direction: column; align-items: stretch; }
}
@media (max-width: 480px) {
  .wh-stats { grid-template-columns: 1fr; }
}
</style>