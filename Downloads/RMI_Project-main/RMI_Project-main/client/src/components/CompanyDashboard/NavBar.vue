<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

interface CompanyInfo {
  companyName: string
  Logo: string
}

const router = useRouter()
const notifications = ref(3)
const isProfileDropdownOpen = ref(false)
const companyInfo = ref<CompanyInfo>({
  companyName: localStorage.getItem('username') || 'Utilisateur',
  Logo: ''
})

// Plus besoin d'appel API pour les infos de base
// On utilise ce qui est stocké au moment du login
const fetchCompanyInfo = () => {
  const username = localStorage.getItem('username')
  if (username) {
    companyInfo.value.companyName = username
  }
}

const toggleProfileDropdown = () => {
  isProfileDropdownOpen.value = !isProfileDropdownOpen.value
}

const goToProfile = () => {
  router.push('/profile')
  isProfileDropdownOpen.value = false
}

const goToEdit = () => {
  router.push('/Edit')
  isProfileDropdownOpen.value = false
}

const logout = async () => {
  try {
    const username = localStorage.getItem('username')
    if (username) {
      // Appel logout vers HttpBridge Java
      await fetch('http://localhost:8080/api/auth/logout', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username })
      })
    }
  } catch (error) {
    console.error('Erreur logout:', error)
  } finally {
    localStorage.removeItem('username')
    localStorage.removeItem('isLoggedIn')
    router.push('/Login')
    isProfileDropdownOpen.value = false
  }
}

const handleClickOutside = (event: MouseEvent) => {
  const profileMenu = document.querySelector('.profile-menu')
  if (profileMenu && !profileMenu.contains(event.target as Node)) {
    isProfileDropdownOpen.value = false
  }
}

onMounted(() => {
  fetchCompanyInfo()
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>