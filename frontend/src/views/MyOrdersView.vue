<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { api } from '../api/client';
import type { OrderResponse, Page } from '../api/types';
import OrderStatusBadge from '../components/OrderStatusBadge.vue';

const { t, locale } = useI18n();
const router = useRouter();
const orders = ref<OrderResponse[]>([]);
const loading = ref(true);

function fmtPrice(eur: string): string {
  return new Intl.NumberFormat(locale.value.toLowerCase(), { style: 'currency', currency: 'EUR' }).format(Number(eur));
}
function fmtDate(iso: string): string {
  return new Date(iso).toLocaleDateString(locale.value.toLowerCase(), { day: '2-digit', month: 'short', year: 'numeric' });
}

async function load() {
  loading.value = true;
  try {
    const res = await api.get<Page<OrderResponse>>('/orders?size=50&sort=createdAt,desc');
    orders.value = res.content;
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div class="fm-page">
    <h2 class="text-2xl mb-6 font-serif italic">{{ t('order.title') }}</h2>
    <p v-if="loading">{{ t('common.loading') }}</p>
    <p v-else-if="orders.length === 0" class="text-sm text-fm-black/60">{{ t('order.empty') }}</p>
    <ul v-else class="divide-y divide-fm-black/10">
      <li v-for="o in orders" :key="o.id" class="py-3">
        <button class="w-full text-left flex justify-between items-start gap-3"
                @click="router.push({ name: 'order-detail', params: { id: o.id } })">
          <div>
            <div class="font-medium">{{ o.product.name }}</div>
            <div class="text-xs text-fm-black/60">{{ fmtDate(o.createdAt) }} — {{ fmtPrice(o.totalEur) }}</div>
          </div>
          <OrderStatusBadge :status="o.status" />
        </button>
      </li>
    </ul>
  </div>
</template>
