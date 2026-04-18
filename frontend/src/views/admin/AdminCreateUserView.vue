<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { api, ApiException } from '../../api/client';
import type { CreateAllocataireRequest, CreateAllocataireResponse, Civility, TierCode, Locale } from '../../api/types';
import FmInput from '../../components/FmInput.vue';
import FmSelect from '../../components/FmSelect.vue';
import FmButton from '../../components/FmButton.vue';

const { t } = useI18n();
const router = useRouter();

const form = reactive<CreateAllocataireRequest>({
  email: '', firstName: '', lastName: '',
  civility: 'NONE', birthDate: null, phone: '',
  country: '', city: '',
  tierCode: 'TIER_3', locale: 'FR', adminNotes: '',
});

const busy = ref(false);
const error = ref<string | null>(null);
const result = ref<CreateAllocataireResponse | null>(null);

const civilityOpts = (['MR', 'MRS', 'OTHER', 'NONE'] as const).map(v => ({ value: v, label: t(`civility.${v}`) }));
const tierOpts = (['TIER_1', 'TIER_2', 'TIER_3'] as const).map(v => ({ value: v, label: t(`tiers.${v}`) }));
const localeOpts = [
  { value: 'FR', label: 'Français' },
  { value: 'IT', label: 'Italiano' },
  { value: 'EN', label: 'English' },
];

async function submit() {
  error.value = null;
  busy.value = true;
  try {
    result.value = await api.post<CreateAllocataireResponse>('/admin/users', form);
  } catch (err) {
    if (err instanceof ApiException && err.status === 409) error.value = t('register.emailExists');
    else error.value = t('common.error');
  } finally { busy.value = false; }
}

function copyCode() {
  if (result.value) navigator.clipboard.writeText(result.value.code).catch(() => {});
}
</script>

<template>
  <div class="fm-page max-w-md">
    <h2 class="text-2xl mb-6">{{ t('admin.createAllocataire') }}</h2>

    <section v-if="result" class="fm-card border-fm-gold bg-fm-stone text-center space-y-3">
      <p class="text-xs uppercase tracking-widest text-fm-black/60">{{ t('admin.codeGenerated') }}</p>
      <p class="font-logo text-3xl tracking-[0.3em]" data-testid="generated-code">{{ result.code }}</p>
      <p class="text-sm">{{ result.user.email }}</p>
      <div class="flex gap-3 justify-center">
        <FmButton variant="secondary" @click="copyCode">{{ t('admin.codeCopy') }}</FmButton>
        <FmButton @click="router.push({ name: 'admin-user-detail', params: { id: result.user.id } })">{{ t('common.continue') }}</FmButton>
      </div>
    </section>

    <form v-else @submit.prevent="submit" class="space-y-1">
      <FmInput v-model="form.email" :label="t('common.email')" type="email" required data-testid="new-email" />
      <FmSelect v-model="form.civility as unknown as string" :label="t('common.civility')" :options="civilityOpts" required @update:modelValue="(v: string) => form.civility = v as Civility" />
      <FmInput v-model="form.firstName" :label="t('common.firstName')" required data-testid="new-first" />
      <FmInput v-model="form.lastName" :label="t('common.lastName')" required data-testid="new-last" />
      <FmInput v-model="form.birthDate" :label="t('common.birthDate')" type="date" />
      <FmInput v-model="form.phone" :label="t('common.phone')" />
      <FmInput v-model="form.country" :label="t('common.country')" required />
      <FmInput v-model="form.city" :label="t('common.city')" required />
      <FmSelect v-model="form.tierCode as unknown as string" :label="t('admin.tier')" :options="tierOpts" required @update:modelValue="(v: string) => form.tierCode = v as TierCode" />
      <FmSelect v-model="form.locale as unknown as string" :label="t('common.locale')" :options="localeOpts" required @update:modelValue="(v: string) => form.locale = v as Locale" />
      <FmInput v-model="form.adminNotes" :label="t('admin.adminNotes')" />
      <p v-if="error" class="text-sm text-fm-red my-3">{{ error }}</p>
      <div class="pt-3">
        <FmButton type="submit" block :disabled="busy" data-testid="admin-create-submit">{{ t('admin.createAllocataire') }}</FmButton>
      </div>
    </form>
  </div>
</template>
