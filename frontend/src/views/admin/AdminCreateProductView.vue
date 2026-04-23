<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import { postMultipart, ApiException } from '../../api/client';
import type { ProductRequest, ProductResponse, TierCode } from '../../api/types';
import FmInput from '../../components/FmInput.vue';
import FmButton from '../../components/FmButton.vue';

const { t } = useI18n();
const router = useRouter();

const form = reactive<{
  name: string; description: string;
  priceEur: string; delivery: boolean;
  weightKg: string; tiers: Set<TierCode>;
  saleStartAt: string; saleEndAt: string;
  stock: string;
}>({
  name: '', description: '',
  priceEur: '0.00', delivery: false,
  weightKg: '', tiers: new Set<TierCode>(),
  saleStartAt: '', saleEndAt: '',
  stock: '',
});

const error = ref<string | null>(null);
const busy = ref(false);
const photoFile = ref<File | null>(null);

function toggleTier(tc: TierCode) {
  if (form.tiers.has(tc)) form.tiers.delete(tc);
  else form.tiers.add(tc);
}

function onPhotoChange(e: Event) {
  const files = (e.target as HTMLInputElement).files;
  photoFile.value = files && files.length > 0 ? files[0] : null;
}

async function submit() {
  error.value = null;
  if (form.tiers.size === 0) { error.value = t('admin.products.tiers') + ' ?'; return; }
  if (!form.saleStartAt) { error.value = t('admin.products.saleStart') + ' ?'; return; }
  busy.value = true;
  try {
    const body: ProductRequest = {
      name: form.name,
      description: form.description || null,
      priceEur: form.priceEur,
      delivery: form.delivery,
      weightKg: form.weightKg || null,
      tiers: Array.from(form.tiers),
      saleStartAt: new Date(form.saleStartAt).toISOString(),
      saleEndAt: form.saleEndAt ? new Date(form.saleEndAt).toISOString() : null,
      stock: form.stock === '' ? null : Number(form.stock),
    };
    const fd = new FormData();
    fd.append('product', new Blob([JSON.stringify(body)], { type: 'application/json' }));
    if (photoFile.value) fd.append('photo', photoFile.value);
    const created = await postMultipart<ProductResponse>('/admin/products', fd);
    router.push({ name: 'admin-product-detail', params: { id: created.id } });
  } catch (e) {
    error.value = e instanceof ApiException ? (e.payload?.message || t('common.error')) : t('common.error');
  } finally {
    busy.value = false;
  }
}
</script>

<template>
  <div class="fm-page max-w-lg">
    <h2 class="text-2xl mb-6">{{ t('admin.products.create') }}</h2>

    <form @submit.prevent="submit" class="space-y-4">
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

      <label class="block text-sm">
        {{ t('admin.products.photo') }}
        <input type="file" accept="image/jpeg,image/png,image/webp" @change="onPhotoChange" class="mt-1 block" />
      </label>

      <p v-if="error" class="text-sm text-fm-red">{{ error }}</p>
      <FmButton type="submit" variant="primary" block :disabled="busy">
        {{ t('admin.products.save') }}
      </FmButton>
    </form>
  </div>
</template>
