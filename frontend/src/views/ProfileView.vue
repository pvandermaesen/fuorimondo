<script setup lang="ts">
import { reactive, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useAuthStore } from '../stores/auth';
import { api } from '../api/client';
import type { UpdateProfileRequest, UserResponse, Locale, Civility } from '../api/types';
import { setLocale } from '../i18n';
import FmInput from '../components/FmInput.vue';
import FmSelect from '../components/FmSelect.vue';
import FmButton from '../components/FmButton.vue';
import TierBadge from '../components/TierBadge.vue';

const { t, locale } = useI18n();
const auth = useAuthStore();

const form = reactive<UpdateProfileRequest>({
  firstName: '', lastName: '', civility: 'NONE',
  birthDate: null, phone: '',
  country: '', city: '', locale: 'FR',
});

function hydrate(u: UserResponse | null) {
  if (!u) return;
  form.firstName = u.firstName;
  form.lastName = u.lastName;
  form.civility = u.civility;
  form.birthDate = u.birthDate;
  form.phone = u.phone ?? '';
  form.country = u.country;
  form.city = u.city;
  form.locale = u.locale;
}

hydrate(auth.user);
watch(() => auth.user, (u) => hydrate(u));

const civilityOptions = (['MR', 'MRS', 'OTHER', 'NONE'] as const).map(v => ({ value: v, label: t(`civility.${v}`) }));
const localeOptions = [
  { value: 'FR', label: 'Français' },
  { value: 'IT', label: 'Italiano' },
  { value: 'EN', label: 'English' },
];

async function save() {
  const updated = await api.patch<UserResponse>('/me', form);
  auth.setUser(updated);
  const loc = updated.locale.toLowerCase() as 'fr'|'it'|'en';
  setLocale(loc);
  locale.value = loc;
}
</script>

<template>
  <div class="fm-page" v-if="auth.user">
    <h2 class="text-2xl mb-2">{{ t('profile.title') }}</h2>
    <div class="mb-6">
      <TierBadge :tier="auth.user.tierCode" />
    </div>

    <p v-if="auth.isWaitingList" class="fm-legal-banner border-fm-black bg-fm-stone text-fm-black">{{ t('profile.waitingBanner') }}</p>

    <form @submit.prevent="save" class="space-y-2">
      <FmSelect v-model="form.civility as unknown as string" :label="t('common.civility')" :options="civilityOptions" required @update:modelValue="(v: string) => form.civility = v as Civility" />
      <FmInput v-model="form.firstName" :label="t('common.firstName')" required />
      <FmInput v-model="form.lastName" :label="t('common.lastName')" required />
      <FmInput v-model="form.birthDate" :label="t('common.birthDate')" type="date" />
      <FmInput v-model="form.phone" :label="t('common.phone')" />
      <FmInput v-model="form.country" :label="t('common.country')" required />
      <FmInput v-model="form.city" :label="t('common.city')" required />
      <FmSelect v-model="form.locale as unknown as string" :label="t('common.locale')" :options="localeOptions" required @update:modelValue="(v: string) => form.locale = v as Locale" />
      <FmButton type="submit" block>{{ t('common.save') }}</FmButton>
    </form>
  </div>
</template>
