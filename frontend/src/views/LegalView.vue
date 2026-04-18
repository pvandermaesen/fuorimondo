<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { api } from '../api/client';
import type { LegalResponse, Locale } from '../api/types';

const { t, locale } = useI18n();
const route = useRoute();

const markdown = ref<string>('');
const loading = ref(true);

const slug = computed(() => String(route.params.slug));
const titleKey = computed(() => `legal.${slug.value}`);

function currentLocale(): Locale {
  return locale.value.toUpperCase() as Locale;
}

async function load() {
  loading.value = true;
  try {
    const res = await api.get<LegalResponse>(`/legal/${slug.value}?locale=${currentLocale()}`);
    markdown.value = res.markdown;
  } catch {
    markdown.value = '';
  } finally {
    loading.value = false;
  }
}

watch([() => slug.value, () => locale.value], load, { immediate: true });
</script>

<template>
  <article class="fm-page prose-like">
    <p class="fm-legal-banner">{{ t('legal.banner') }}</p>
    <h2 v-if="route.params.slug" class="text-2xl mb-4">{{ t(titleKey) }}</h2>
    <p v-if="loading">{{ t('common.loading') }}</p>
    <pre v-else class="whitespace-pre-wrap font-sans text-sm leading-relaxed">{{ markdown }}</pre>
  </article>
</template>
