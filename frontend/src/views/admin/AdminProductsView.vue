<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import { api } from '../../api/client';
import type { ProductResponse } from '../../api/types';
import FmButton from '../../components/FmButton.vue';

const { t } = useI18n();
const router = useRouter();

const products = ref<ProductResponse[]>([]);
const loading = ref(true);
const showExpired = ref(false);

async function load() {
  loading.value = true;
  products.value = await api.get<ProductResponse[]>('/admin/products');
  loading.value = false;
}

function isExpired(p: ProductResponse): boolean {
  return p.saleEndAt != null && new Date(p.saleEndAt).getTime() < Date.now();
}

const expiredCount = computed(() => products.value.filter(isExpired).length);
const visibleProducts = computed(() =>
  products.value.filter(p => showExpired.value ? isExpired(p) : !isExpired(p))
);

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
    <p v-else-if="visibleProducts.length === 0" class="text-sm text-fm-black/60">{{ t('admin.products.empty') }}</p>
    <div v-else class="grid grid-cols-2 gap-3">
      <button v-for="p in visibleProducts" :key="p.id"
              class="text-left border border-fm-black/10 rounded-lg overflow-hidden bg-fm-white hover:border-fm-black/30 transition-colors flex flex-col"
              :class="{ 'opacity-60': isExpired(p) }"
              @click="router.push({ name: 'admin-product-detail', params: { id: p.id } })">
        <div class="aspect-square bg-fm-stone flex items-center justify-center overflow-hidden relative">
          <img v-if="p.photoFilename" :src="`/api/admin/products/${p.id}/photo`" alt="" class="w-full h-full object-cover" />
          <span v-else class="text-xs text-fm-black/40">{{ t('admin.products.noPhoto') }}</span>
          <span v-if="isExpired(p)" class="absolute top-2 left-2 text-[10px] uppercase tracking-widest bg-fm-black text-fm-white px-2 py-0.5 rounded">
            {{ t('admin.products.expiredBadge') }}
          </span>
        </div>
        <div class="p-3 space-y-1">
          <div class="font-medium text-sm leading-tight">{{ p.name }}</div>
          <div class="text-xs text-fm-black/60">{{ fmtPrice(p.priceEur) }} · {{ p.tiers.join(', ') }}</div>
          <div class="text-xs text-fm-black/60">{{ t('admin.products.stock') }} : {{ fmtStock(p.stock) }}</div>
          <div class="text-xs text-fm-black/50">{{ fmtDate(p.saleStartAt) }}</div>
        </div>
      </button>
    </div>

    <div v-if="!loading && expiredCount > 0" class="mt-6 text-center">
      <FmButton variant="ghost" @click="showExpired = !showExpired">
        {{ showExpired ? t('admin.products.hideExpired') : t('admin.products.showExpired') }} ({{ expiredCount }})
      </FmButton>
    </div>
  </div>
</template>
