import { createI18n } from 'vue-i18n';
import fr from './fr';
import it from './it';
import en from './en';

const STORAGE_KEY = 'fm.locale';
const fallback = 'fr' as const;

function detectInitialLocale(): 'fr' | 'it' | 'en' {
  const saved = localStorage.getItem(STORAGE_KEY);
  if (saved === 'fr' || saved === 'it' || saved === 'en') return saved;
  const nav = navigator.language.slice(0, 2).toLowerCase();
  if (nav === 'it' || nav === 'en') return nav;
  return fallback;
}

export const i18n = createI18n({
  legacy: false,
  locale: detectInitialLocale(),
  fallbackLocale: fallback,
  messages: { fr, it, en },
});

export function setLocale(loc: 'fr' | 'it' | 'en') {
  i18n.global.locale.value = loc;
  localStorage.setItem(STORAGE_KEY, loc);
  document.documentElement.lang = loc;
}
