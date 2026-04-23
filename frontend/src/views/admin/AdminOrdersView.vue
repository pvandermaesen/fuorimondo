<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { api } from '../../api/client';
import type { AdminOrderResponse, Page } from '../../api/types';
import OrderStatusBadge from '../../components/OrderStatusBadge.vue';

const { t, locale } = useI18n();
const router = useRouter();
const orders = ref<AdminOrderResponse[]>([]);
const loading = ref(true);

function fmtPrice(eur: string): string {
  return new Intl.NumberFormat(locale.value.toLowerCase(), { style: 'currency', currency: 'EUR' }).format(Number(eur));
}
function fmtDate(iso: string): string {
  return new Date(iso).toLocaleString(locale.value.toLowerCase());
}

async function load() {
  loading.value = true;
  try {
    const res = await api.get<Page<AdminOrderResponse>>('/admin/orders?size=100&sort=createdAt,desc');
    orders.value = res.content;
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div class="fm-page">
    <h2 class="text-2xl mb-6 font-serif italic">{{ t('nav.adminOrders') }}</h2>
    <p v-if="loading">{{ t('common.loading') }}</p>
    <table v-else class="w-full text-sm">
      <thead class="text-left text-xs uppercase tracking-widest text-fm-black/60">
        <tr>
          <th class="py-2">Date</th><th>Acheteur</th><th>Produit</th><th>Total</th><th>Statut</th>
        </tr>
      </thead>
      <tbody class="divide-y divide-fm-black/10">
        <tr v-for="o in orders" :key="o.id"
            class="cursor-pointer hover:bg-fm-stone"
            @click="router.push({ name: 'admin-order-detail', params: { id: o.id } })">
          <td class="py-3">{{ fmtDate(o.createdAt) }}</td>
          <td>{{ o.userFirstName }} {{ o.userLastName }}<div class="text-xs text-fm-black/60">{{ o.userEmail }}</div></td>
          <td>{{ o.product.name }}</td>
          <td class="font-logo">{{ fmtPrice(o.totalEur) }}</td>
          <td><OrderStatusBadge :status="o.status" /></td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
