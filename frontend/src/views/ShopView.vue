<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { api } from '../api/client';
import type { PublicProductResponse } from '../api/types';
import TierBadge from '../components/TierBadge.vue';

const { t, locale } = useI18n();
const router = useRouter();
const products = ref<PublicProductResponse[]>([]);
const loading = ref(true);

function fmtPrice(eur: string): string {
  return new Intl.NumberFormat(locale.value.toLowerCase(), {
    style: 'currency', currency: 'EUR',
  }).format(Number(eur));
}

async function load() {
  loading.value = true;
  try {
    products.value = await api.get<PublicProductResponse[]>('/products');
  } finally {
    loading.value = false;
  }
}

function photoUrl(id: string): string {
  return `/api/admin/products/${id}/photo`;
}

onMounted(load);
</script>

<template>
  <div class="fm-page">
    <h2 class="text-2xl mb-6 font-serif italic">{{ t('shop.title') }}</h2>
    <p v-if="loading" class="text-sm">{{ t('common.loading') }}</p>
    <p v-else-if="products.length === 0" class="text-sm text-fm-black/60">{{ t('shop.empty') }}</p>
    <div v-else class="grid grid-cols-1 sm:grid-cols-2 desk:grid-cols-3 gap-6">
      <button
        v-for="p in products" :key="p.id"
        class="text-left bg-fm-white border border-fm-black/10 rounded overflow-hidden hover:border-fm-black/40 transition"
        @click="router.push({ name: 'shop-product', params: { id: p.id } })"
        :data-testid="`product-card-${p.id}`"
      >
        <div class="aspect-[4/3] bg-fm-stone flex items-center justify-center">
          <img v-if="p.photoFilename" :src="photoUrl(p.id)" :alt="p.name" class="w-full h-full object-cover" />
          <span v-else class="text-xs text-fm-black/30">—</span>
        </div>
        <div class="p-4 space-y-2">
          <h3 class="font-serif italic text-lg">{{ p.name }}</h3>
          <p class="font-logo text-xl">{{ fmtPrice(p.priceEur) }}</p>
          <div class="flex flex-wrap gap-1">
            <TierBadge v-for="tier in p.tiers" :key="tier" :tier="tier" />
          </div>
          <div class="text-xs text-fm-black/60 space-y-0.5">
            <p v-if="p.stockRemaining !== null && p.stockRemaining <= 3">{{ t('shop.limitedStock', { n: p.stockRemaining }) }}</p>
            <p v-if="p.delivery">{{ t('shop.delivery') }}</p>
            <p v-else>{{ t('shop.digital') }}</p>
          </div>
        </div>
      </button>
    </div>
  </div>
</template>
