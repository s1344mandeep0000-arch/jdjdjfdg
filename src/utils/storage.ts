import {
  DEFAULT_APP_SETTINGS,
  type AppSettings,
  type ChatSession,
  type SavedProject,
} from '../types/mod';

const SESSIONS_KEY = 'mc_ai_mod_studio_sessions_v1';
const PROJECTS_KEY = 'mc_ai_mod_studio_projects_v1';
const SETTINGS_KEY = 'mc_ai_mod_studio_settings_v1';

export function loadSessions(): ChatSession[] {
  try {
    const raw = localStorage.getItem(SESSIONS_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export function saveSessions(sessions: ChatSession[]): void {
  try {
    localStorage.setItem(SESSIONS_KEY, JSON.stringify(sessions.slice(0, 40)));
  } catch {
    // Ignore storage quota issues gracefully
  }
}

export function loadSavedProjects(): SavedProject[] {
  try {
    const raw = localStorage.getItem(PROJECTS_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export function saveSavedProjects(projects: SavedProject[]): void {
  try {
    localStorage.setItem(PROJECTS_KEY, JSON.stringify(projects.slice(0, 50)));
  } catch {
    // Ignore storage quota issues gracefully
  }
}

export function loadAppSettings(): AppSettings {
  try {
    const raw = localStorage.getItem(SETTINGS_KEY);
    if (!raw) return DEFAULT_APP_SETTINGS;
    return { ...DEFAULT_APP_SETTINGS, ...JSON.parse(raw) };
  } catch {
    return DEFAULT_APP_SETTINGS;
  }
}

export function saveAppSettings(settings: AppSettings): void {
  try {
    localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings));
  } catch {
    // Ignore storage quota issues gracefully
  }
}
