<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { api } from '../../api/client';
import type { AdminUserResponse, UpdateUserByAdminRequest, TierCode, UserStatus } from '../../api/types';
import FmSelect from '../../components/FmSelect.vue';
import FmInput from '../../components/FmInput.vue';
import FmButton from '../../components/FmButton.vue';
import TierBadge from '../../components/TierBadge.vue';

const { t, locale } = useI18n();
const route = useRoute();

const user = ref<AdminUserResponse | null>(null);
const busy = ref(false);
const copied = ref(false);

const form = ref<UpdateUserByAdminRequest>({ status: undefined, tierCode: undefined, adminNotes: '' });

const statusOpts = (['WAITING_LIST', 'ALLOCATAIRE_PENDING', 'ALLOCATAIRE', 'SUSPENDED'] as const).map(v => ({ value: v, label: t(`status.${v}`) }));
const tierOpts = (['TIER_1', 'TIER_2', 'TIER_3'] as const).map(v => ({ value: v, label: t(`tiers.${v}`) }));

const codeState = computed(() => {
  const u = user.value;
  if (!u || !u.invitationCode) return null;
  if (u.invitationCodeUsedAt) return 'used' as const;
  if (u.invitationCodeExpiresAt && new Date(u.invitationCodeExpiresAt).getTime() < Date.now()) return 'expired' as const;
  return 'valid' as const;
});

async function copyCode() {
  const code = user.value?.invitationCode;
  if (!code) return;
  try {
    await navigator.clipboard.writeText(code);
    copied.value = true;
    setTimeout(() => { copied.value = false; }, 1500);
  } catch { /* clipboard unavailable */ }
}

function formatDate(iso: string | null | undefined): string {
  if (!iso) return '';
  return new Date(iso).toLocaleDateString(locale.value.toLowerCase(), {
    day: '2-digit', month: 'long', year: 'numeric',
  });
}

async function load() {
  const u = await api.get<AdminUserResponse>(`/admin/users/${route.params.id}`);
  user.value = u;
  form.value = { status: u.status, tierCode: u.tierCode ?? undefined, adminNotes: u.adminNotes ?? '' };
}

async function save() {
  busy.value = true;
  try {
    user.value = await api.patch<AdminUserResponse>(`/admin/users/${route.params.id}`, form.value);
  } finally { busy.value = false; }
}

async function regenerate() {
  if (!confirm(t('admin.confirmRegenerateCode'))) return;
  busy.value = true;
  try {
    await api.post<{ code: string }>(`/admin/users/${route.params.id}/regenerate-code`);
    await load();
  } finally { busy.value = false; }
}

onMounted(load);
</script>

<template>
  <div class="fm-page max-w-md" v-if="user">
    <h2 class="text-2xl mb-1">{{ user.firstName }} {{ user.lastName }}</h2>
    <p class="text-sm text-fm-black/60 mb-4">{{ user.email }}</p>
    <div class="mb-6 flex gap-4 items-center text-xs">
      <span>{{ t(`status.${user.status}`) }}</span>
      <TierBadge :tier="user.tierCode" />
    </div>

    <section class="fm-card text-center space-y-2 mb-6" data-testid="current-code">
      <p class="text-xs uppercase tracking-widest text-fm-black/60">{{ t('admin.currentCode') }}</p>
      <template v-if="user.invitationCode">
        <div class="flex items-center justify-center gap-3">
          <p class="font-logo text-3xl tracking-[0.3em]"
             :class="{ 'line-through text-fm-black/40': codeState !== 'valid' }"
             data-testid="current-code-value">{{ user.invitationCode }}</p>
          <button type="button"
                  class="text-xs underline text-fm-black/60 hover:text-fm-black"
                  @click="copyCode"
                  data-testid="copy-code">
            {{ copied ? t('admin.codeCopied') : t('admin.codeCopy') }}
          </button>
        </div>
        <p v-if="codeState === 'valid'" class="text-xs text-fm-black/60">
          {{ t('admin.codeValidUntil') }} {{ formatDate(user.invitationCodeExpiresAt) }}
        </p>
        <p v-else-if="codeState === 'used'" class="text-xs text-fm-red">
          {{ t('admin.codeUsed') }} — {{ formatDate(user.invitationCodeUsedAt) }}
        </p>
        <p v-else class="text-xs text-fm-red">
          {{ t('admin.codeExpired') }} — {{ formatDate(user.invitationCodeExpiresAt) }}
        </p>
      </template>
      <p v-else class="text-sm text-fm-black/60">{{ t('admin.codeNone') }}</p>
    </section>

    <form @submit.prevent="save" class="space-y-1">
      <FmSelect v-model="form.status as unknown as string" :label="t('admin.status')" :options="statusOpts" @update:modelValue="(v: string) => form.status = v as UserStatus" />
      <FmSelect v-model="form.tierCode as unknown as string" :label="t('admin.tier')" :options="tierOpts" @update:modelValue="(v: string) => form.tierCode = v as TierCode" />
      <FmInput v-model="form.adminNotes as unknown as string" :label="t('admin.adminNotes')" />
      <div class="pt-3 flex gap-3">
        <FmButton type="submit" :disabled="busy">{{ t('admin.updateUser') }}</FmButton>
        <FmButton type="button" variant="secondary" :disabled="busy" @click="regenerate" data-testid="regenerate-code">{{ t('admin.regenerateCode') }}</FmButton>
      </div>
    </form>
  </div>
</template>
