<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';
import { api, uploadMultipart, ApiException } from '../../api/client';
import type { ProductRequest, ProductResponse, TierCode } from '../../api/types';
import FmInput from '../../components/FmInput.vue';
import FmButton from '../../components/FmButton.vue';

const { t } = useI18n();
const route = useRoute();
const router = useRouter();

const product = ref<ProductResponse | null>(null);
const busy = ref(false);
const error = ref<string | null>(null);

const form = ref({
  name: '', description: '',
  priceEur: '0.00', delivery: false,
  weightKg: '', tiers: new Set<TierCode>(),
  saleStartAt: '', saleEndAt: '',
  stock: '',
});

function toLocalDT(iso: string | null) {
  if (!iso) return '';
  const d = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function hydrate(p: ProductResponse) {
  product.value = p;
  form.value = {
    name: p.name, description: p.description || '',
    priceEur: p.priceEur, delivery: p.delivery,
    weightKg: p.weightKg || '', tiers: new Set(p.tiers),
    saleStartAt: toLocalDT(p.saleStartAt),
    saleEndAt: toLocalDT(p.saleEndAt),
    stock: p.stock == null ? '' : String(p.stock),
  };
}

async function load() {
  const p = await api.get<ProductResponse>(`/admin/products/${route.params.id}`);
  hydrate(p);
}

function toggleTier(tc: TierCode) {
  if (form.value.tiers.has(tc)) form.value.tiers.delete(tc);
  else form.value.tiers.add(tc);
}

async function save() {
  error.value = null;
  if (form.value.tiers.size === 0) { error.value = t('admin.products.tiers') + ' ?'; return; }
  busy.value = true;
  try {
    const body: ProductRequest = {
      name: form.value.name,
      description: form.value.description || null,
      priceEur: form.value.priceEur,
      delivery: form.value.delivery,
      weightKg: form.value.weightKg || null,
      tiers: Array.from(form.value.tiers),
      saleStartAt: new Date(form.value.saleStartAt).toISOString(),
      saleEndAt: form.value.saleEndAt ? new Date(form.value.saleEndAt).toISOString() : null,
      stock: form.value.stock === '' ? null : Number(form.value.stock),
    };
    const updated = await api.patch<ProductResponse>(`/admin/products/${route.params.id}`, body);
    hydrate(updated);
  } catch (e) {
    error.value = e instanceof ApiException ? (e.payload?.message || t('common.error')) : t('common.error');
  } finally {
    busy.value = false;
  }
}

async function remove() {
  if (!window.confirm(t('admin.products.confirmDelete'))) return;
  await api.delete(`/admin/products/${route.params.id}`);
  router.push({ name: 'admin-products' });
}

async function onPhotoChange(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0];
  if (!file) return;
  const updated = await uploadMultipart<ProductResponse>(`/admin/products/${route.params.id}/photo`, file);
  hydrate(updated);
}

async function removePhoto() {
  const updated = await api.delete<ProductResponse>(`/admin/products/${route.params.id}/photo`);
  hydrate(updated);
}

onMounted(load);
</script>

<template>
  <div v-if="product" class="fm-page max-w-lg">
    <h2 class="text-2xl mb-6">{{ t('admin.products.edit') }}</h2>

    <section class="mb-8">
      <h3 class="text-sm uppercase tracking-widest text-fm-black/60 mb-2">{{ t('admin.products.photo') }}</h3>
      <div v-if="product.photoFilename" class="mb-3">
        <img :src="`/api/admin/products/${product.id}/photo`" alt="" class="max-h-60 rounded" />
      </div>
      <p v-else class="text-sm text-fm-black/60 mb-3">{{ t('admin.products.noPhoto') }}</p>
      <input type="file" accept="image/jpeg,image/png,image/webp" @change="onPhotoChange" />
      <FmButton v-if="product.photoFilename" variant="ghost" @click="removePhoto" class="ml-2">
        {{ t('admin.products.removePhoto') }}
      </FmButton>
    </section>

    <form @submit.prevent="save" class="space-y-4">
      <FmInput v-model="form.name" :label="t('admin.products.name')" required maxlength="200" />
      <label class="block text-sm">
        {{ t('admin.products.description') }}
        <textarea v-model="form.description" rows="4" maxlength="4000"
                  class="mt-1 w-full border border-fm-black/20 rounded p-2" />
      </label>
      <FmInput v-model="form.priceEur" :label="t('admin.products.price')" type="text" required />

      <label class="flex items-center gap-2">
        <input type="checkbox" v-model="form.delivery" />
        {{ t('admin.products.delivery') }}
      </label>

      <FmInput v-model="form.weightKg" :label="t('admin.products.weight')" type="text" />

      <fieldset>
        <legend class="text-sm mb-2">{{ t('admin.products.tiers') }}</legend>
        <label v-for="tc in ['TIER_1','TIER_2','TIER_3'] as const" :key="tc" class="mr-4 inline-flex items-center gap-1">
          <input type="checkbox" :checked="form.tiers.has(tc)" @change="toggleTier(tc)" />
          {{ t(`tiers.${tc}`) }}
        </label>
      </fieldset>

      <FmInput v-model="form.saleStartAt" :label="t('admin.products.saleStart')" type="datetime-local" required />
      <FmInput v-model="form.saleEndAt" :label="t('admin.products.saleEnd')" type="datetime-local" />
      <FmInput v-model="form.stock" :label="t('admin.products.stock')" type="number" />

      <p v-if="error" class="text-sm text-fm-red">{{ error }}</p>
      <div class="flex gap-2">
        <FmButton type="submit" variant="primary" :disabled="busy">{{ t('admin.products.save') }}</FmButton>
        <FmButton type="button" variant="ghost" @click="remove">{{ t('admin.products.deleteProduct') }}</FmButton>
      </div>
    </form>
  </div>
</template>
