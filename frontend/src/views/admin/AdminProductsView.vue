<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import { api } from '../../api/client';
import type { ProductResponse } from '../../api/types';
import FmButton from '../../components/FmButton.vue';

const { t } = useI18n();
const router = useRouter();

const products = ref<ProductResponse[]>([]);
const loading = ref(true);

async function load() {
  loading.value = true;
  products.value = await api.get<ProductResponse[]>('/admin/products');
  loading.value = false;
}

function fmtPrice(v: string) { return `${Number(v).toFixed(2)} €`; }
function fmtDate(v: string | null) { return v ? new Date(v).toLocaleString('fr-FR') : '—'; }
function fmtStock(v: number | null) { return v == null ? '∞' : String(v); }

onMounted(load);
</script>

<template>
  <div class="fm-page">
    <div class="flex items-center justify-between mb-6">
      <h2 class="text-2xl">{{ t('admin.products.list') }}</h2>
      <FmButton variant="primary" @click="router.push({ name: 'admin-products-create' })" data-testid="admin-product-create-btn">
        + {{ t('admin.products.create') }}
      </FmButton>
    </div>

    <p v-if="loading" class="text-sm">{{ t('common.loading') }}</p>
    <p v-else-if="products.length === 0" class="text-sm text-fm-black/60">{{ t('admin.products.empty') }}</p>
    <ul v-else class="divide-y divide-fm-black/10">
      <li v-for="p in products" :key="p.id" class="py-3">
        <button class="w-full text-left flex justify-between items-start gap-3"
                @click="router.push({ name: 'admin-product-detail', params: { id: p.id } })">
          <div>
            <div class="font-medium">{{ p.name }}</div>
            <div class="text-xs text-fm-black/60">{{ fmtPrice(p.priceEur) }} · {{ p.tiers.join(', ') }}</div>
            <div class="text-xs text-fm-black/60">{{ t('admin.products.saleStart') }} : {{ fmtDate(p.saleStartAt) }} · {{ t('admin.products.stock') }} : {{ fmtStock(p.stock) }}</div>
          </div>
          <span class="text-xs text-fm-black/40">→</span>
        </button>
      </li>
    </ul>
  </div>
</template>
