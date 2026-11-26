import { Sliders, Trash2, X } from 'lucide-react';
import type {
  AppSettings,
  EditionPreference,
  JavaModLoader,
  ReasoningDepth,
} from '../types/mod';

interface SettingsModalProps {
  mode: 'ai' | 'app' | null;
  settings: AppSettings;
  onUpdateSettings: (next: AppSettings) => void;
  onClearHistory: () => void;
  onClearProjects: () => void;
  onClose: () => void;
}

export function SettingsModal({
  mode,
  settings,
  onUpdateSettings,
  onClearHistory,
  onClearProjects,
  onClose,
}: SettingsModalProps) {
  if (!mode) return null;

  const setEdition = (editionPreference: EditionPreference) =>
    onUpdateSettings({ ...settings, editionPreference });

  const setLoader = (javaLoader: JavaModLoader) =>
    onUpdateSettings({ ...settings, javaLoader });

  const setReasoning = (reasoningDepth: ReasoningDepth) =>
    onUpdateSettings({ ...settings, reasoningDepth });

  return (
    <div
      className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/40 backdrop-blur-[1px]"
      role="dialog"
      aria-modal="true"
      aria-label={mode === 'ai' ? 'AI Model Settings' : 'Studio Settings'}
    >
      <div className="w-full sm:max-w-md bg-white rounded-t-3xl sm:rounded-3xl border border-[#E4E4E7] shadow-2xl overflow-hidden">
        <div className="px-5 py-4 border-b border-[#E4E4E7] flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Sliders className="w-4 h-4 text-[#3B82F6]" aria-hidden="true" />
            <h2 className="text-base font-semibold text-[#0D0D0D]">
              {mode === 'ai' ? 'AI Pro & Mod Engine' : 'Studio Settings'}
            </h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close settings"
            className="w-10 h-10 min-w-[40px] min-h-[40px] rounded-full hover:bg-[#F4F4F5] flex items-center justify-center text-[#52525B]"
          >
            <X className="w-5 h-5" aria-hidden="true" />
          </button>
        </div>

        <div className="p-5 space-y-5 max-h-[75dvh] overflow-y-auto clean-scroll">
          {mode === 'ai' ? (
            <>
              <div className="space-y-2">
                <label className="block text-xs font-semibold text-[#52525B]">
                  Minecraft Edition Compatibility
                </label>
                <div className="grid grid-cols-3 gap-1.5 p-1 rounded-xl bg-[#F4F4F5]">
                  {(
                    [
                      { id: 'auto', label: 'Auto' },
                      { id: 'java', label: 'Java' },
                      { id: 'bedrock', label: 'Bedrock' },
                    ] as const
                  ).map((opt) => (
                    <button
                      key={opt.id}
                      type="button"
                      onClick={() => setEdition(opt.id)}
                      className={`min-h-[38px] rounded-lg text-xs font-medium transition-colors ${
                        settings.editionPreference === opt.id
                          ? 'bg-white text-[#0D0D0D] shadow-2xs'
                          : 'text-[#71717A] hover:text-[#0D0D0D]'
                      }`}
                    >
                      {opt.label}
                    </button>
                  ))}
                </div>
                <p className="text-xs text-[#71717A]">
                  Auto mode detects Java or Bedrock directly from your prompt.
                </p>
              </div>

              <div className="space-y-2">
                <label className="block text-xs font-semibold text-[#52525B]">
                  Java Mod Loader
                </label>
                <div className="grid grid-cols-3 gap-1.5 p-1 rounded-xl bg-[#F4F4F5]">
                  {(['Fabric', 'Forge', 'NeoForge'] as const).map((loader) => (
                    <button
                      key={loader}
                      type="button"
                      onClick={() => setLoader(loader)}
                      className={`min-h-[38px] rounded-lg text-xs font-medium transition-colors ${
                        settings.javaLoader === loader
                          ? 'bg-white text-[#0D0D0D] shadow-2xs'
                          : 'text-[#71717A] hover:text-[#0D0D0D]'
                      }`}
                    >
                      {loader}
                    </button>
                  ))}
                </div>
              </div>

              <div className="space-y-2">
                <label
                  htmlFor="mc-version-select"
                  className="block text-xs font-semibold text-[#52525B]"
                >
                  Target Minecraft Version
                </label>
                <select
                  id="mc-version-select"
                  value={settings.minecraftVersion}
                  onChange={(e) =>
                    onUpdateSettings({
                      ...settings,
                      minecraftVersion: e.target.value,
                    })
                  }
                  className="w-full h-11 px-3 rounded-xl border border-[#E4E4E7] bg-white text-sm text-[#0D0D0D] focus:outline-none focus:border-[#3B82F6]"
                >
                  <option value="1.21.4">Minecraft 1.21.4</option>
                  <option value="1.21.1">Minecraft 1.21.1</option>
                  <option value="1.20.4">Minecraft 1.20.4</option>
                  <option value="1.20.1">Minecraft 1.20.1</option>
                </select>
              </div>

              <div className="space-y-2">
                <label className="block text-xs font-semibold text-[#52525B]">
                  AI Architecture Depth
                </label>
                <div className="grid grid-cols-3 gap-1.5 p-1 rounded-xl bg-[#F4F4F5]">
                  {(
                    [
                      { id: 'low', label: 'Fast' },
                      { id: 'medium', label: 'Balanced' },
                      { id: 'high', label: 'Deep' },
                    ] as const
                  ).map((d) => (
                    <button
                      key={d.id}
                      type="button"
                      onClick={() => setReasoning(d.id)}
                      className={`min-h-[38px] rounded-lg text-xs font-medium transition-colors ${
                        settings.reasoningDepth === d.id
                          ? 'bg-white text-[#0D0D0D] shadow-2xs'
                          : 'text-[#71717A] hover:text-[#0D0D0D]'
                      }`}
                    >
                      {d.label}
                    </button>
                  ))}
                </div>
              </div>
            </>
          ) : (
            <>
              <label className="flex items-center justify-between gap-3 py-2 cursor-pointer">
                <div>
                  <p className="text-sm font-medium text-[#0D0D0D]">
                    Auto-save generated projects
                  </p>
                  <p className="text-xs text-[#71717A]">
                    Automatically add new mods to Saved Projects
                  </p>
                </div>
                <input
                  type="checkbox"
                  checked={settings.autoSaveProjects}
                  onChange={(e) =>
                    onUpdateSettings({
                      ...settings,
                      autoSaveProjects: e.target.checked,
                    })
                  }
                  className="w-5 h-5 accent-[#3B82F6]"
                />
              </label>

              <label className="flex items-center justify-between gap-3 py-2 cursor-pointer">
                <div>
                  <p className="text-sm font-medium text-[#0D0D0D]">
                    Bedrock Script APIs
                  </p>
                  <p className="text-xs text-[#71717A]">
                    Include @minecraft/server custom behaviors in add-ons
                  </p>
                </div>
                <input
                  type="checkbox"
                  checked={settings.includeBedrockScripts}
                  onChange={(e) =>
                    onUpdateSettings({
                      ...settings,
                      includeBedrockScripts: e.target.checked,
                    })
                  }
                  className="w-5 h-5 accent-[#3B82F6]"
                />
              </label>

              <div className="space-y-1.5 pt-1">
                <label
                  htmlFor="backend-url-input"
                  className="block text-xs font-semibold text-[#52525B]"
                >
                  Cloud Backend URL (Android APK)
                </label>
                <input
                  id="backend-url-input"
                  type="url"
                  value={settings.apiBaseUrl}
                  onChange={(e) =>
                    onUpdateSettings({
                      ...settings,
                      apiBaseUrl: e.target.value,
                    })
                  }
                  placeholder="Default studio backend (leave blank for web)"
                  className="w-full h-10 px-3 rounded-xl border border-[#E4E4E7] bg-[#FAFAFA] text-xs text-[#0D0D0D] placeholder:text-[#8E8E93] focus:outline-none focus:border-[#3B82F6]"
                />
              </div>

              <div className="pt-3 border-t border-[#E4E4E7] space-y-2">
                <button
                  type="button"
                  onClick={onClearHistory}
                  className="w-full min-h-[42px] px-3.5 rounded-xl border border-[#E4E4E7] hover:bg-red-50 hover:border-red-200 text-sm font-medium text-[#3F3F46] hover:text-red-700 flex items-center justify-between transition-colors"
                >
                  <span>Clear Chat History</span>
                  <Trash2 className="w-4 h-4" aria-hidden="true" />
                </button>
                <button
                  type="button"
                  onClick={onClearProjects}
                  className="w-full min-h-[42px] px-3.5 rounded-xl border border-[#E4E4E7] hover:bg-red-50 hover:border-red-200 text-sm font-medium text-[#3F3F46] hover:text-red-700 flex items-center justify-between transition-colors"
                >
                  <span>Clear Saved Projects</span>
                  <Trash2 className="w-4 h-4" aria-hidden="true" />
                </button>
              </div>
            </>
          )}
        </div>

        <div className="px-5 py-3.5 border-t border-[#E4E4E7] bg-[#FAFAFA] flex justify-end">
          <button
            type="button"
            onClick={onClose}
            className="min-h-[40px] px-5 py-2 rounded-xl bg-[#3B82F6] hover:bg-[#2563EB] text-white text-sm font-medium transition-colors"
          >
            Done
          </button>
        </div>
      </div>
    </div>
  );
}
