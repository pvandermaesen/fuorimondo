<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import { api } from '../../api/client';
import type { AdminUserResponse, Page } from '../../api/types';
import FmInput from '../../components/FmInput.vue';
import FmSelect from '../../components/FmSelect.vue';
import FmButton from '../../components/FmButton.vue';
import TierBadge from '../../components/TierBadge.vue';

const { t } = useI18n();
const router = useRouter();

const users = ref<AdminUserResponse[]>([]);
const q = ref<string>('');
const status = ref<string>('');
const loading = ref(true);

const statusOptions = [
  { value: '', label: '—' },
  { value: 'WAITING_LIST', label: t('status.WAITING_LIST') },
  { value: 'ALLOCATAIRE_PENDING', label: t('status.ALLOCATAIRE_PENDING') },
  { value: 'ALLOCATAIRE', label: t('status.ALLOCATAIRE') },
  { value: 'SUSPENDED', label: t('status.SUSPENDED') },
];

async function load() {
  loading.value = true;
  const params = new URLSearchParams();
  if (q.value) params.set('q', q.value);
  if (status.value) params.set('status', status.value);
  params.set('size', '50');
  const res = await api.get<Page<AdminUserResponse>>(`/admin/users?${params.toString()}`);
  users.value = res.content;
  loading.value = false;
}

let searchTimer: ReturnType<typeof setTimeout> | null = null;
watch([q, status], () => {
  if (searchTimer) clearTimeout(searchTimer);
  searchTimer = setTimeout(load, 250);
});

onMounted(load);
</script>

<template>
  <div class="fm-page">
    <div class="flex items-center justify-between mb-6">
      <h2 class="text-2xl">{{ t('admin.usersList') }}</h2>
      <FmButton variant="primary" @click="router.push({ name: 'admin-users-create' })" data-testid="admin-create-btn">
        + {{ t('admin.createAllocataire') }}
      </FmButton>
    </div>

    <div class="space-y-0">
      <FmInput v-model="q" :label="t('admin.search')" data-testid="admin-search" />
      <FmSelect v-model="status" :label="t('admin.filterStatus')" :options="statusOptions" />
    </div>

    <p v-if="loading" class="text-sm">{{ t('common.loading') }}</p>
    <ul v-else class="divide-y divide-fm-black/10">
      <li v-for="u in users" :key="u.id" class="py-3">
        <button class="w-full text-left flex justify-between items-start gap-3" @click="router.push({ name: 'admin-user-detail', params: { id: u.id } })">
          <div>
            <div class="font-medium">{{ u.firstName }} {{ u.lastName }}</div>
            <div class="text-xs text-fm-black/60">{{ u.email }}</div>
            <div class="text-xs mt-1 flex items-center gap-3">
              <span>{{ t(`status.${u.status}`) }}</span>
              <TierBadge :tier="u.tierCode" />
            </div>
          </div>
          <span class="text-xs text-fm-black/40">→</span>
        </button>
      </li>
    </ul>
  </div>
</template>
