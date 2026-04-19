<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { api, ApiException } from '../api/client';
import type { PublicProductResponse, OrderResponse } from '../api/types';
import FmButton from '../components/FmButton.vue';
import TierBadge from '../components/TierBadge.vue';

const { t, locale } = useI18n();
const route = useRoute();
const router = useRouter();

const product = ref<PublicProductResponse | null>(null);
const pendingOrder = ref<OrderResponse | null>(null);
const loading = ref(true);
const error = ref<string | null>(null);

const disabled = computed(() => {
  const p = product.value;
  if (!p) return true;
  if (p.stockRemaining !== null && p.stockRemaining <= 0) return true;
  return false;
});

function fmtPrice(eur: string): string {
  return new Intl.NumberFormat(locale.value.toLowerCase(), { style: 'currency', currency: 'EUR' }).format(Number(eur));
}
function fmtDate(iso: string): string {
  return new Date(iso).toLocaleDateString(locale.value.toLowerCase(), { day: '2-digit', month: 'long', year: 'numeric' });
}
function photoUrl(id: string): string {
  return `/api/products/${id}/photo`;
}

async function load() {
  loading.value = true; error.value = null;
  try {
    product.value = await api.get<PublicProductResponse>(`/products/${route.params.id}`);
    // Check for an existing pending payment by this user on this product
    const myOrders = await api.get<{ content: OrderResponse[] }>(`/orders?size=10&sort=createdAt,desc`);
    const existing = myOrders.content.find(o => o.status === 'PENDING_PAYMENT' && o.product.id === product.value!.id);
    pendingOrder.value = existing ?? null;
  } catch (e) {
    if (e instanceof ApiException && e.status === 404) error.value = t('errors.tierMismatch');
    else error.value = t('common.error');
  } finally {
    loading.value = false;
  }
}

function resumePayment() {
  if (pendingOrder.value?.mollieCheckoutUrl) {
    window.location.href = pendingOrder.value.mollieCheckoutUrl;
  }
}

onMounted(load);
</script>

<template>
  <div class="fm-page max-w-2xl">
    <p v-if="loading" class="text-sm">{{ t('common.loading') }}</p>
    <div v-else-if="error" class="text-sm text-fm-red">{{ error }}</div>
    <div v-else-if="product" class="space-y-6">
      <div class="aspect-[4/3] bg-fm-stone">
        <img v-if="product.photoFilename" :src="photoUrl(product.id)" :alt="product.name" class="w-full h-full object-cover" />
      </div>
      <h1 class="font-serif italic text-3xl">{{ product.name }}</h1>
      <p class="font-logo text-2xl">{{ fmtPrice(product.priceEur) }}</p>
      <div class="flex gap-2 flex-wrap">
        <TierBadge v-for="tier in product.tiers" :key="tier" :tier="tier" />
      </div>
      <p v-if="product.description" class="font-serif text-fm-black/80 whitespace-pre-line">{{ product.description }}</p>
      <div class="text-sm text-fm-black/60 space-y-1">
        <p v-if="product.saleEndAt">{{ t('shop.availableUntil', { date: fmtDate(product.saleEndAt) }) }}</p>
        <p v-if="product.delivery && product.weightKg">{{ t('shop.delivery') }} — {{ t('shop.weight', { kg: product.weightKg }) }}</p>
        <p v-else-if="product.delivery">{{ t('shop.delivery') }}</p>
        <p v-else>{{ t('shop.digital') }}</p>
        <p v-if="product.stockRemaining !== null && product.stockRemaining <= 3">{{ t('shop.limitedStock', { n: product.stockRemaining }) }}</p>
      </div>
      <div class="pt-4">
        <FmButton v-if="pendingOrder" block variant="secondary" @click="resumePayment" data-testid="resume-payment">
          {{ t('shop.resumePayment') }}
        </FmButton>
        <FmButton v-else block variant="primary" :disabled="disabled"
                  @click="router.push({ name: 'shop-checkout', params: { productId: product.id } })"
                  data-testid="buy-cta">
          {{ disabled ? t('shop.outOfStock') : t('shop.buyCta', { price: fmtPrice(product.priceEur) }) }}
        </FmButton>
      </div>
    </div>
  </div>
</template>
