export type ThemeMode = "light" | "dark" | "system";

const KEY = "dad_theme";

export function getStoredTheme(): ThemeMode {
  const v = localStorage.getItem(KEY) as ThemeMode | null;
  return v ?? "system";
}

export function applyTheme(mode: ThemeMode) {
  const root = document.documentElement;
  const systemDark = window.matchMedia?.("(prefers-color-scheme: dark)")?.matches ?? false;

  const dark = mode === "dark" || (mode === "system" && systemDark);
  root.classList.toggle("dark", dark);
  localStorage.setItem(KEY, mode);
}