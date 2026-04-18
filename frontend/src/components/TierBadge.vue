<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import type { TierCode } from '../api/types';

defineProps<{ tier: TierCode | null | undefined; }>();
const { t } = useI18n();

const colorFor: Record<TierCode, string> = {
  TIER_1: 'bg-tier-1',
  TIER_2: 'bg-tier-2',
  TIER_3: 'bg-tier-3',
};
</script>

<template>
  <span v-if="tier" class="inline-flex items-center gap-2 text-xs uppercase tracking-widest">
    <span :class="['h-3 w-3 rounded-full', colorFor[tier]]" :aria-label="tier" />
    <span>{{ t(`tiers.${tier}`) }}</span>
  </span>
  <span v-else class="text-xs text-fm-black/50">—</span>
</template>
