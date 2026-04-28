<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import axios from 'axios'

interface Warehouse {
  _id: string
  name: string
  type: 'external'
  location: string
  storage_type: 'freezer' | 'refrigerated' | 'ambient' | 'controlled'
  capacity: number
  status: 'available' | 'occupied' | 'maintenance'
  company_id?: string | null
}

const warehouses     = ref<Warehouse[]>([])
const loading        = ref(true)
const error          = ref<string | null>(null)
const searchTerm     = ref('')
const statusFilter   = ref('')
const storageTypeFilter = ref('')
const showModal      = ref(false)
const showDeleteModal = ref(false)
const isEditing      = ref(false)
const selectedWarehouseId = ref<string | null>(null)
const lastUpdated    = ref<Date | null>(null)
const isRefreshing   = ref(false)   // pulse indicator
let   pollingInterval: ReturnType<typeof setInterval> | null = null

const warehouseForm = ref<Warehouse>({
  _id: '', name: '', type: 'external', location: '',
  storage_type: 'ambient', capacity: 1000, status: 'available'
})

const api = axios.create({
  baseURL: 'http://localhost:3000/api',
  headers: { 'Content-Type': 'application/json' }
})

// ── Computed ──────────────────────────────────────────────
const filteredWarehouses = computed(() => {
  let f = warehouses.value
  if (searchTerm.value) {
    const t = searchTerm.value.toLowerCase()
    f = f.filter(w => w.name.toLowerCase().includes(t) || w.location.toLowerCase().includes(t))
  }
  if (statusFilter.value)      f = f.filter(w => w.status === statusFilter.value)
  if (storageTypeFilter.value) f = f.filter(w => w.storage_type === storageTypeFilter.value)
  return f
})

const storageTypesCount = computed(() => new Set(warehouses.value.map(w => w.storage_type)).size)

const formattedLastUpdated = computed(() => {
  if (!lastUpdated.value) return '—'
  return lastUpdated.value.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
})

// ── Helpers ───────────────────────────────────────────────
const formatStorageType = (t: string) =>
    ({ freezer: 'Freezer', refrigerated: 'Refrigerated', ambient: 'Ambient', controlled: 'Controlled' }[t] ?? t)

const formatStatus = (s: string) =>
    ({ available: 'Available', occupied: 'Occupied', maintenance: 'Maintenance' }[s] ?? s)

const getStorageTypeIcon = (t: string) =>
    ({ freezer: 'fas fa-snowflake', refrigerated: 'fas fa-temperature-low',
      ambient: 'fas fa-thermometer-half', controlled: 'fas fa-sliders-h' }[t] ?? 'fas fa-warehouse')

// ── Modals ────────────────────────────────────────────────
const openAddModal = () => {
  isEditing.value = false
  selectedWarehouseId.value = null
  warehouseForm.value = { _id: '', name: '', type: 'external', location: '', storage_type: 'ambient', capacity: 1000, status: 'available' }
  showModal.value = true
}
const openEditModal = (w: Warehouse) => {
  isEditing.value = true
  selectedWarehouseId.value = w._id
  warehouseForm.value = { ...w }
  showModal.value = true
}
const openDeleteModal = (id: string) => { selectedWarehouseId.value = id; showDeleteModal.value = true }
const closeModals = () => { showModal.value = false; showDeleteModal.value = false; selectedWarehouseId.value = null }

// ── CRUD ──────────────────────────────────────────────────
const handleSubmit = async () => {
  try {
    loading.value = true; error.value = null
    const token = localStorage.getItem('accessToken')
    if (!token) throw new Error('Authentication required')
    const headers = { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }

    if (isEditing.value && selectedWarehouseId.value) {
      const res = await api.put(`/warehouses/updateExternalDepot/${selectedWarehouseId.value}`, warehouseForm.value, { headers })
      const idx = warehouses.value.findIndex(w => w._id === selectedWarehouseId.value)
      if (idx !== -1) warehouses.value[idx] = res.data
    } else {
      const res = await api.post('/warehouses/addExternalDepot', warehouseForm.value, { headers })
      warehouses.value.unshift(res.data)
    }
    closeModals()
  } catch (err: any) {
    error.value = err.response?.data?.message || err.message || 'Une erreur est survenue'
  } finally { loading.value = false }
}

const handleDelete = async () => {
  try {
    if (!selectedWarehouseId.value) return
    loading.value = true; error.value = null
    const token = localStorage.getItem('accessToken')
    if (!token) throw new Error('Authentication required')
    await api.delete(`/warehouses/deleteExternalDepot/${selectedWarehouseId.value}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    warehouses.value = warehouses.value.filter(w => w._id !== selectedWarehouseId.value)
    closeModals()
  } catch (err: any) {
    error.value = err.response?.data?.message || err.message || 'An error occurred'
  } finally { loading.value = false }
}

// ── Fetch + Polling ───────────────────────────────────────
const fetchWarehouses = async (silent = false) => {
  try {
    if (!silent) loading.value = true
    isRefreshing.value = true
    error.value = null

    const token = localStorage.getItem('accessToken')
    if (!token) throw new Error('Authentication required')

    const response = await api.get('/warehouses/getExternalDepots', {
      headers: { Authorization: `Bearer ${token}` }
    })

    warehouses.value = response.data.data || []
    lastUpdated.value = new Date()
  } catch (err: any) {
    error.value = err.response?.data?.message || err.message || 'Failed to load warehouses'
  } finally {
    loading.value = false
    // Blink the indicator briefly then stop
    setTimeout(() => { isRefreshing.value = false }, 600)
  }
}

const startPolling = () => {
  pollingInterval = setInterval(() => fetchWarehouses(true), 3000)
}

const stopPolling = () => {
  if (pollingInterval) { clearInterval(pollingInterval); pollingInterval = null }
}

onMounted(() => {
  fetchWarehouses()
  startPolling()       // ✅ fix: interval stored properly
})

onUnmounted(() => {
  stopPolling()        // ✅ fix: cleared on component destroy
})
</script>

<template>
  <div class="warehouse-management">

    <!-- Header -->
    <header class="header">
      <div class="header-title">
        <h1>Warehouse Management</h1>
        <!-- 🔴 Live indicator -->
        <div class="live-badge" :class="{ pulsing: isRefreshing }">
          <span class="live-dot"></span>
          LIVE
          <span class="live-time">· mis à jour {{ formattedLastUpdated }}</span>
        </div>
      </div>
      <div class="header-actions">
        <div class="search-bar">
          <i class="fas fa-search"></i>
          <input type="text" v-model="searchTerm" placeholder="Search warehouses...">
        </div>
        <button class="btn-primary" @click="openAddModal">
          <i class="fas fa-plus"></i> Add Warehouse
        </button>
      </div>
    </header>

    <!-- Stats -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon"><i class="fas fa-warehouse"></i></div>
        <div class="stat-content">
          <h3>Total Warehouses</h3>
          <p>{{ filteredWarehouses.length }}</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon"><i class="fas fa-box"></i></div>
        <div class="stat-content">
          <h3>Storage Types</h3>
          <p>{{ storageTypesCount }}</p>
        </div>
      </div>
    </div>

    <!-- Filters -->
    <div class="filters">
      <div class="filter-group">
        <label>Status:</label>
        <select v-model="statusFilter">
          <option value="">All</option>
          <option value="available">Available</option>
          <option value="occupied">Occupied</option>
          <option value="maintenance">Maintenance</option>
        </select>
      </div>
      <div class="filter-group">
        <label>Storage Type:</label>
        <select v-model="storageTypeFilter">
          <option value="">All</option>
          <option value="freezer">Freezer</option>
          <option value="refrigerated">Refrigerated</option>
          <option value="ambient">Ambient</option>
          <option value="controlled">Controlled</option>
        </select>
      </div>
    </div>

    <!-- Alerts -->
    <div v-if="error" class="alert alert-error"><i class="fas fa-exclamation-circle"></i> {{ error }}</div>
    <div v-if="loading && !warehouses.length" class="alert alert-info"><i class="fas fa-spinner fa-spin"></i> Loading warehouses...</div>

    <!-- Grid -->
    <div class="warehouse-grid" v-if="filteredWarehouses.length > 0">
      <div v-for="warehouse in filteredWarehouses" :key="warehouse._id" class="warehouse-card">
        <div class="card-header">
          <div class="warehouse-info">
            <h3>{{ warehouse.name }}</h3>
            <p class="location"><i class="fas fa-map-marker-alt"></i> {{ warehouse.location }}</p>
          </div>
          <div class="card-actions">
            <button class="btn-icon btn-edit" @click="openEditModal(warehouse)"><i class="fas fa-edit"></i></button>
            <button class="btn-icon btn-delete" @click="openDeleteModal(warehouse._id)"><i class="fas fa-trash"></i></button>
          </div>
        </div>
        <div class="card-body">
          <div class="detail-row">
            <span class="detail-label">Storage Type</span>
            <span class="detail-value"><i :class="getStorageTypeIcon(warehouse.storage_type)"></i> {{ formatStorageType(warehouse.storage_type) }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">Status</span>
            <span class="status-badge" :class="warehouse.status">{{ formatStatus(warehouse.status) }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">Capacity</span>
            <span class="detail-value">{{ warehouse.capacity }} kg</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty -->
    <div v-if="!loading && filteredWarehouses.length === 0" class="empty-state">
      <i class="fas fa-warehouse"></i>
      <p>No warehouses found</p>
      <button class="btn-primary" @click="openAddModal"><i class="fas fa-plus"></i> Add Warehouse</button>
    </div>

    <!-- Add/Edit Modal -->
    <div v-if="showModal" class="modal">
      <div class="modal-content">
        <div class="modal-header">
          <h2>{{ isEditing ? 'Edit' : 'Add' }} Warehouse</h2>
          <button class="btn-close" @click="closeModals">×</button>
        </div>
        <form @submit.prevent="handleSubmit">
          <div class="modal-body">
            <div class="form-group"><label>Name</label><input type="text" v-model="warehouseForm.name" required></div>
            <div class="form-group"><label>Location</label><input type="text" v-model="warehouseForm.location" required></div>
            <div class="form-group">
              <label>Storage Type</label>
              <select v-model="warehouseForm.storage_type">
                <option value="freezer">Freezer</option>
                <option value="refrigerated">Refrigerated</option>
                <option value="ambient">Ambient</option>
                <option value="controlled">Controlled</option>
              </select>
            </div>
            <div class="form-group"><label>Capacity (kg)</label><input type="number" v-model.number="warehouseForm.capacity" required min="1"></div>
            <div class="form-group">
              <label>Status</label>
              <select v-model="warehouseForm.status">
                <option value="available">Available</option>
                <option value="occupied">Occupied</option>
                <option value="maintenance">Maintenance</option>
              </select>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn-secondary" @click="closeModals">Cancel</button>
            <button type="submit" class="btn-primary">{{ isEditing ? 'Update' : 'Add' }} Warehouse</button>
          </div>
        </form>
      </div>
    </div>

    <!-- Delete Modal -->
    <div v-if="showDeleteModal" class="modal">
      <div class="modal-content modal-sm">
        <div class="modal-header">
          <h2>Confirm Delete</h2>
          <button class="btn-close" @click="closeModals">×</button>
        </div>
        <div class="modal-body">
          <p>Are you sure you want to delete this warehouse?</p>
          <p v-if="error" class="text-error">{{ error }}</p>
        </div>
        <div class="modal-footer">
          <button class="btn-secondary" @click="closeModals">Cancel</button>
          <button class="btn-danger" @click="handleDelete" :disabled="loading">
            <span v-if="loading">Checking...</span>
            <span v-else>Delete</span>
          </button>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
.warehouse-management { padding: 2rem; max-width: 1400px; margin: 0 auto; }

/* Header */
.header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 2rem; flex-wrap: wrap; gap: 1rem; }
.header-title { display: flex; flex-direction: column; gap: .5rem; }
.header h1 { font-size: 2rem; font-weight: 600; color: #111827; margin: 0; }
.header-actions { display: flex; gap: 1rem; align-items: center; }

/* 🔴 Live badge */
.live-badge {
  display: inline-flex; align-items: center; gap: .4rem;
  background: #f0fdf4; border: 1px solid #bbf7d0;
  color: #15803d; border-radius: 9999px;
  padding: .25rem .75rem; font-size: .75rem; font-weight: 700;
  width: fit-content;
}
.live-dot {
  width: .5rem; height: .5rem; border-radius: 50%;
  background: #16a34a;
  animation: livePulse 2s infinite;
}
.live-badge.pulsing .live-dot { animation: liveBlink .3s ease; }
.live-time { font-weight: 400; color: #4ade80; }

@keyframes livePulse {
  0%, 100% { opacity: 1; box-shadow: 0 0 0 0 rgba(22,163,74,.4); }
  50% { opacity: .6; box-shadow: 0 0 0 4px rgba(22,163,74,0); }
}
@keyframes liveBlink {
  0% { background: #16a34a; } 50% { background: #4ade80; } 100% { background: #16a34a; }
}

/* Search */
.search-bar { position: relative; }
.search-bar i { position: absolute; left: 1rem; top: 50%; transform: translateY(-50%); color: #6b7280; }
.search-bar input { padding: .75rem 1rem .75rem 2.5rem; border: 1px solid #e5e7eb; border-radius: .5rem; width: 300px; background: #fff; color: #111827; }

/* Stats */
.stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 1.5rem; margin-bottom: 2rem; }
.stat-card { background: #fff; padding: 1.5rem; border-radius: .5rem; box-shadow: 0 1px 3px rgba(0,0,0,.1); display: flex; align-items: center; gap: 1rem; }
.stat-icon { width: 3rem; height: 3rem; border-radius: .5rem; background: #dbeafe; color: #2563eb; display: flex; align-items: center; justify-content: center; font-size: 1.25rem; }
.stat-content h3 { font-size: .875rem; color: #6b7280; margin: 0; }
.stat-content p { font-size: 1.5rem; font-weight: 600; color: #111827; margin: .25rem 0 0; }

/* Filters */
.filters { display: flex; gap: 1.5rem; margin-bottom: 2rem; flex-wrap: wrap; }
.filter-group { display: flex; align-items: center; gap: .5rem; }
.filter-group label { font-size: .875rem; color: #6b7280; }
.filter-group select { padding: .5rem; border: 1px solid #e5e7eb; border-radius: .375rem; background: #fff; color: #111827; }

/* Grid */
.warehouse-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(350px, 1fr)); gap: 1.5rem; }
.warehouse-card { background: #fff; border-radius: .5rem; box-shadow: 0 1px 3px rgba(0,0,0,.1); overflow: hidden; transition: box-shadow .2s; }
.warehouse-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,.1); }

.card-header { padding: 1.5rem; border-bottom: 1px solid #e5e7eb; display: flex; justify-content: space-between; align-items: flex-start; }
.warehouse-info h3 { margin: 0; font-size: 1.25rem; color: #111827; }
.location { display: flex; align-items: center; gap: .5rem; color: #6b7280; font-size: .875rem; margin-top: .5rem; }
.card-actions { display: flex; gap: .5rem; }
.btn-icon { width: 2rem; height: 2rem; border-radius: .375rem; display: flex; align-items: center; justify-content: center; border: none; cursor: pointer; transition: all .2s; }
.btn-edit { background: #dbeafe; color: #2563eb; }
.btn-delete { background: #fee2e2; color: #dc2626; }

.card-body { padding: 1.5rem; }
.detail-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
.detail-label { color: #6b7280; font-size: .875rem; }
.detail-value { font-weight: 500; color: #111827; display: flex; align-items: center; gap: .5rem; }

.status-badge { padding: .25rem .75rem; border-radius: 9999px; font-size: .75rem; font-weight: 500; }
.status-badge.available  { background: #d1fae5; color: #16a34a; }
.status-badge.occupied   { background: #fee2e2; color: #dc2626; }
.status-badge.maintenance{ background: #fef3c7; color: #d97706; }

/* Buttons */
.btn-primary   { background: #2563eb; color: #fff; border: none; padding: .75rem 1.5rem; border-radius: .5rem; font-weight: 500; cursor: pointer; display: flex; align-items: center; gap: .5rem; transition: background .2s; }
.btn-primary:hover { background: #1d4ed8; }
.btn-secondary { background: #fff; color: #6b7280; border: 1px solid #e5e7eb; padding: .75rem 1.5rem; border-radius: .5rem; font-weight: 500; cursor: pointer; }
.btn-danger    { background: #dc2626; color: #fff; border: none; padding: .75rem 1.5rem; border-radius: .5rem; font-weight: 500; cursor: pointer; }

/* Modal */
.modal { position: fixed; inset: 0; background: rgba(0,0,0,.5); display: flex; align-items: center; justify-content: center; padding: 1rem; z-index: 1000; }
.modal-content { background: #fff; border-radius: .5rem; width: 100%; max-width: 600px; max-height: 90vh; overflow-y: auto; }
.modal-content.modal-sm { max-width: 400px; }
.modal-header { padding: 1.5rem; border-bottom: 1px solid #e5e7eb; display: flex; justify-content: space-between; align-items: center; }
.modal-header h2 { margin: 0; font-size: 1.25rem; color: #111827; }
.btn-close { background: none; border: none; font-size: 1.5rem; color: #6b7280; cursor: pointer; }
.modal-body { padding: 1.5rem; }
.form-group { margin-bottom: 1.5rem; }
.form-group label { display: block; margin-bottom: .5rem; font-size: .875rem; color: #111827; }
.form-group input, .form-group select { width: 100%; padding: .75rem; border: 1px solid #e5e7eb; border-radius: .375rem; background: #fff; color: #111827; box-sizing: border-box; }
.modal-footer { padding: 1.5rem; border-top: 1px solid #e5e7eb; display: flex; justify-content: flex-end; gap: 1rem; }

/* Empty */
.empty-state { text-align: center; padding: 4rem 2rem; background: #fff; border-radius: .5rem; box-shadow: 0 1px 3px rgba(0,0,0,.1); }
.empty-state i { font-size: 3rem; color: #6b7280; margin-bottom: 1rem; display: block; }
.empty-state p { color: #6b7280; margin-bottom: 1.5rem; }

/* Alerts */
.alert { padding: 1rem; border-radius: .5rem; margin-bottom: 1.5rem; display: flex; align-items: center; gap: .75rem; }
.alert-error { background: #fee2e2; color: #dc2626; }
.alert-info  { background: #dbeafe; color: #2563eb; }
.text-error  { color: #dc2626; margin-top: .5rem; font-size: .875rem; }

@media (max-width: 768px) {
  .header { flex-direction: column; align-items: stretch; }
  .header-actions { flex-direction: column; }
  .search-bar, .search-bar input { width: 100%; }
  .filters { flex-direction: column; }
  .filter-group { width: 100%; }
  .filter-group select { flex: 1; }
  .warehouse-grid { grid-template-columns: 1fr; }
}
</style>