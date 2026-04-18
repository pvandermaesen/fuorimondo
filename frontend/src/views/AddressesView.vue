<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { api } from '../api/client';
import type { AddressResponse, AddressRequest, AddressType } from '../api/types';
import FmInput from '../components/FmInput.vue';
import FmSelect from '../components/FmSelect.vue';
import FmCheckbox from '../components/FmCheckbox.vue';
import FmButton from '../components/FmButton.vue';

const { t } = useI18n();

const addresses = ref<AddressResponse[]>([]);
const editing = ref<string | null>(null); // 'new' or address id
const loading = ref(true);

const form = reactive<AddressRequest>({
  type: 'BILLING', fullName: '', street: '', streetExtra: '',
  postalCode: '', city: '', country: '', isDefault: false,
});

const typeOptions = [
  { value: 'BILLING', label: t('addresses.billing') },
  { value: 'SHIPPING', label: t('addresses.shipping') },
];

async function load() {
  loading.value = true;
  addresses.value = await api.get<AddressResponse[]>('/me/addresses');
  loading.value = false;
}

function startNew() {
  editing.value = 'new';
  form.type = 'BILLING';
  form.fullName = '';
  form.street = '';
  form.streetExtra = '';
  form.postalCode = '';
  form.city = '';
  form.country = '';
  form.isDefault = false;
}

function startEdit(a: AddressResponse) {
  editing.value = a.id;
  form.type = a.type;
  form.fullName = a.fullName;
  form.street = a.street;
  form.streetExtra = a.streetExtra ?? '';
  form.postalCode = a.postalCode;
  form.city = a.city;
  form.country = a.country;
  form.isDefault = a.isDefault;
}

function cancel() { editing.value = null; }

async function save() {
  if (editing.value === 'new') {
    await api.post<AddressResponse>('/me/addresses', form);
  } else if (editing.value) {
    await api.put<AddressResponse>(`/me/addresses/${editing.value}`, form);
  }
  editing.value = null;
  await load();
}

async function remove(id: string) {
  if (!confirm(t('addresses.confirmDelete'))) return;
  await api.delete<void>(`/me/addresses/${id}`);
  await load();
}

onMounted(load);
</script>

<template>
  <div class="fm-page">
    <h2 class="text-2xl mb-6">{{ t('addresses.title') }}</h2>

    <div v-if="editing === null" class="space-y-4">
      <p v-if="!loading && addresses.length === 0" class="text-sm text-fm-black/60">{{ t('addresses.empty') }}</p>
      <article v-for="a in addresses" :key="a.id" class="fm-card">
        <div class="flex items-start justify-between gap-3">
          <div class="text-sm">
            <div class="text-xs uppercase tracking-widest text-fm-black/50 mb-1">
              {{ a.type === 'BILLING' ? t('addresses.billing') : t('addresses.shipping') }}
              <span v-if="a.isDefault" class="ml-2 text-fm-gold">• {{ t('addresses.default') }}</span>
            </div>
            <div class="font-medium">{{ a.fullName }}</div>
            <div>{{ a.street }}<span v-if="a.streetExtra">, {{ a.streetExtra }}</span></div>
            <div>{{ a.postalCode }} {{ a.city }} — {{ a.country }}</div>
          </div>
          <div class="flex flex-col gap-1 shrink-0">
            <button class="text-xs underline" @click="startEdit(a)">{{ t('common.edit') }}</button>
            <button class="text-xs underline text-fm-red" @click="remove(a.id)">{{ t('common.delete') }}</button>
          </div>
        </div>
      </article>
      <FmButton block variant="secondary" @click="startNew">{{ t('addresses.add') }}</FmButton>
    </div>

    <form v-else @submit.prevent="save" class="space-y-1">
      <FmSelect v-model="form.type as unknown as string" :label="t('common.civility')" :options="typeOptions" required @update:modelValue="(v: string) => form.type = v as AddressType" />
      <FmInput v-model="form.fullName" :label="t('addresses.fullName')" required />
      <FmInput v-model="form.street" :label="t('addresses.street')" required />
      <FmInput v-model="form.streetExtra" :label="t('addresses.streetExtra')" />
      <FmInput v-model="form.postalCode" :label="t('addresses.postalCode')" required />
      <FmInput v-model="form.city" :label="t('common.city')" required />
      <FmInput v-model="form.country" :label="t('common.country')" required />
      <FmCheckbox v-model="form.isDefault" :label="t('addresses.setDefault')" />
      <div class="flex gap-3 pt-2">
        <FmButton variant="secondary" type="button" @click="cancel">{{ t('common.cancel') }}</FmButton>
        <FmButton type="submit" block>{{ t('common.save') }}</FmButton>
      </div>
    </form>
  </div>
</template>
