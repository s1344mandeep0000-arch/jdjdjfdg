import { useState } from 'react';
import {
  Check,
  Copy,
  Download,
  FileCode2,
  Layers,
  ListChecks,
  X,
} from 'lucide-react';
import type { GeneratedMod } from '../types/mod';
import { pixelArtToSvgDataUrl } from '../utils/modPackage';
import {
  downloadElementPng,
  downloadSingleTextFile,
} from '../utils/exportZip';

interface ModPreviewModalProps {
  mod: GeneratedMod | null;
  onClose: () => void;
  onExportMod: (mod: GeneratedMod) => Promise<void>;
}

export function ModPreviewModal({
  mod,
  onClose,
  onExportMod,
}: ModPreviewModalProps) {
  const [tab, setTab] = useState<'elements' | 'files' | 'install'>('elements');
  const [selectedFileIndex, setSelectedFileIndex] = useState(0);
  const [copiedPath, setCopiedPath] = useState<string | null>(null);
  const [isExporting, setIsExporting] = useState(false);

  if (!mod) return null;

  const activeFile = mod.files[selectedFileIndex] ?? mod.files[0];

  const handleCopy = async () => {
    if (!activeFile) return;
    await navigator.clipboard.writeText(activeFile.content);
    setCopiedPath(activeFile.path);
    setTimeout(() => setCopiedPath(null), 1500);
  };

  const handleExportAll = async () => {
    setIsExporting(true);
    try {
      await onExportMod(mod);
    } finally {
      setIsExporting(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/40 backdrop-blur-[1px]"
      role="dialog"
      aria-modal="true"
      aria-label={`Mod Preview for ${mod.modMeta.name}`}
    >
      <div className="w-full sm:max-w-3xl h-[92dvh] sm:h-[85dvh] bg-white rounded-t-3xl sm:rounded-3xl border border-[#E4E4E7] shadow-2xl flex flex-col overflow-hidden">
        {/* Header */}
        <div className="px-5 py-4 border-b border-[#E4E4E7] flex items-center justify-between gap-3">
          <div className="min-w-0">
            <h2 className="text-lg font-semibold text-[#0D0D0D] truncate">
              {mod.modMeta.name}
            </h2>
            <p className="text-xs text-[#71717A] truncate tabular-nums">
              {mod.modMeta.edition === 'bedrock'
                ? 'Minecraft Bedrock Edition Add-on'
                : `Minecraft Java Edition · ${mod.modMeta.loader}`}{' '}
              · v{mod.modMeta.version} · MC {mod.modMeta.minecraftVersion}
            </p>
          </div>
          <div className="flex items-center gap-2 shrink-0">
            <button
              type="button"
              onClick={handleExportAll}
              disabled={isExporting}
              className="min-h-[40px] px-4 py-2 rounded-xl bg-[#3B82F6] hover:bg-[#2563EB] disabled:opacity-60 text-white text-xs sm:text-sm font-medium inline-flex items-center gap-1.5 whitespace-nowrap"
            >
              <Download className="w-4 h-4" aria-hidden="true" />
              <span>
                {isExporting
                  ? 'Packaging...'
                  : mod.modMeta.edition === 'bedrock'
                  ? 'Export .mcaddon'
                  : 'Export .zip'}
              </span>
            </button>
            <button
              type="button"
              onClick={onClose}
              aria-label="Close mod preview"
              className="w-10 h-10 min-w-[40px] min-h-[40px] rounded-full hover:bg-[#F4F4F5] flex items-center justify-center text-[#52525B]"
            >
              <X className="w-5 h-5" aria-hidden="true" />
            </button>
          </div>
        </div>

        {/* Tabs */}
        <div className="px-5 py-2.5 border-b border-[#F4F4F5] bg-[#FAFAFA] flex items-center gap-2 overflow-x-auto">
          <button
            type="button"
            onClick={() => setTab('elements')}
            className={`min-h-[38px] px-3.5 py-1.5 rounded-xl text-xs font-medium inline-flex items-center gap-1.5 whitespace-nowrap shrink-0 transition-colors ${
              tab === 'elements'
                ? 'bg-white text-[#0D0D0D] border border-[#E4E4E7] shadow-2xs'
                : 'text-[#71717A] hover:text-[#0D0D0D]'
            }`}
          >
            <Layers className="w-3.5 h-3.5" aria-hidden="true" />
            <span>Elements & Textures ({mod.elements.length})</span>
          </button>
          <button
            type="button"
            onClick={() => setTab('files')}
            className={`min-h-[38px] px-3.5 py-1.5 rounded-xl text-xs font-medium inline-flex items-center gap-1.5 whitespace-nowrap shrink-0 transition-colors ${
              tab === 'files'
                ? 'bg-white text-[#0D0D0D] border border-[#E4E4E7] shadow-2xs'
                : 'text-[#71717A] hover:text-[#0D0D0D]'
            }`}
          >
            <FileCode2 className="w-3.5 h-3.5" aria-hidden="true" />
            <span>Generated Files ({mod.files.length})</span>
          </button>
          <button
            type="button"
            onClick={() => setTab('install')}
            className={`min-h-[38px] px-3.5 py-1.5 rounded-xl text-xs font-medium inline-flex items-center gap-1.5 whitespace-nowrap shrink-0 transition-colors ${
              tab === 'install'
                ? 'bg-white text-[#0D0D0D] border border-[#E4E4E7] shadow-2xs'
                : 'text-[#71717A] hover:text-[#0D0D0D]'
            }`}
          >
            <ListChecks className="w-3.5 h-3.5" aria-hidden="true" />
            <span>Install Guide</span>
          </button>
        </div>

        {/* Body */}
        <div className="flex-1 overflow-y-auto clean-scroll p-5">
          {tab === 'elements' && (
            <div className="space-y-4">
              {mod.elements.map((el) => {
                const texUrl = pixelArtToSvgDataUrl(el.pixelArt);
                return (
                  <div
                    key={el.id}
                    className="rounded-2xl border border-[#E4E4E7] p-4 flex flex-col sm:flex-row gap-4 bg-white"
                  >
                    <div className="flex sm:flex-col items-center gap-3 shrink-0">
                      <div className="w-24 h-24 rounded-2xl bg-[#F4F4F5] border border-[#E4E4E7] flex items-center justify-center">
                        <img
                          src={texUrl}
                          alt={`${el.displayName} 16x16 texture`}
                          referrerPolicy="no-referrer"
                          className="w-16 h-16 pixelated"
                        />
                      </div>
                      <button
                        type="button"
                        onClick={() => downloadElementPng(mod, el.id)}
                        className="min-h-[36px] px-3 py-1.5 rounded-lg border border-[#E4E4E7] hover:bg-[#F4F4F5] text-xs font-medium text-[#18181B] inline-flex items-center gap-1.5 whitespace-nowrap"
                      >
                        <Download className="w-3.5 h-3.5" aria-hidden="true" />
                        <span>PNG</span>
                      </button>
                    </div>

                    <div className="flex-1 min-w-0 space-y-2">
                      <div>
                        <h3 className="text-base font-semibold text-[#0D0D0D]">
                          {el.displayName}
                        </h3>
                        <p className="text-xs text-[#71717A] capitalize">
                          {el.kind} · {el.id} · {el.statsSummary}
                        </p>
                      </div>
                      <p className="text-sm text-[#3F3F46] leading-relaxed">
                        {el.description}
                      </p>
                      <div className="pt-1 space-y-1 text-xs text-[#52525B]">
                        <p>
                          <span className="font-semibold text-[#18181B]">
                            Recipe / Spawn:
                          </span>{' '}
                          {el.craftingRecipe}
                        </p>
                        <p>
                          <span className="font-semibold text-[#18181B]">
                            Behavior:
                          </span>{' '}
                          {el.behaviorSummary}
                        </p>
                        <p className="font-mono text-[11px] text-[#71717A] truncate">
                          {el.texturePath}
                        </p>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {tab === 'files' && (
            <div className="flex flex-col sm:flex-row gap-4 h-full min-h-[380px]">
              {/* File List */}
              <div className="sm:w-64 shrink-0 space-y-1 max-h-52 sm:max-h-full overflow-y-auto clean-scroll">
                {mod.files.map((file, idx) => {
                  const isActive = idx === selectedFileIndex;
                  return (
                    <button
                      key={file.path}
                      type="button"
                      onClick={() => setSelectedFileIndex(idx)}
                      className={`w-full text-left px-3 py-2 rounded-xl text-xs transition-colors ${
                        isActive
                          ? 'bg-[#EFF6FF] text-[#1D4ED8] font-medium'
                          : 'hover:bg-[#F4F4F5] text-[#3F3F46]'
                      }`}
                    >
                      <p className="font-mono truncate">{file.path}</p>
                    </button>
                  );
                })}
              </div>

              {/* Code Viewer */}
              {activeFile && (
                <div className="flex-1 min-w-0 rounded-2xl border border-[#E4E4E7] bg-[#FAFAFA] flex flex-col overflow-hidden">
                  <div className="px-3.5 py-2.5 border-b border-[#E4E4E7] bg-white flex items-center justify-between gap-2">
                    <span className="font-mono text-xs text-[#18181B] truncate">
                      {activeFile.path}
                    </span>
                    <div className="flex items-center gap-1.5 shrink-0">
                      <button
                        type="button"
                        onClick={handleCopy}
                        className="min-h-[32px] px-2.5 rounded-lg border border-[#E4E4E7] hover:bg-[#F4F4F5] text-xs font-medium text-[#18181B] inline-flex items-center gap-1"
                      >
                        {copiedPath === activeFile.path ? (
                          <Check
                            className="w-3.5 h-3.5 text-[#16A34A]"
                            aria-hidden="true"
                          />
                        ) : (
                          <Copy className="w-3.5 h-3.5" aria-hidden="true" />
                        )}
                        <span>
                          {copiedPath === activeFile.path ? 'Copied' : 'Copy'}
                        </span>
                      </button>
                      <button
                        type="button"
                        onClick={() =>
                          downloadSingleTextFile(
                            activeFile.path,
                            activeFile.content
                          )
                        }
                        className="min-h-[32px] px-2.5 rounded-lg border border-[#E4E4E7] hover:bg-[#F4F4F5] text-xs font-medium text-[#18181B] inline-flex items-center gap-1"
                      >
                        <Download className="w-3.5 h-3.5" aria-hidden="true" />
                        <span>Download</span>
                      </button>
                    </div>
                  </div>
                  <pre className="flex-1 p-4 overflow-auto clean-scroll font-mono text-xs leading-relaxed text-[#18181B]">
                    <code>{activeFile.content}</code>
                  </pre>
                </div>
              )}
            </div>
          )}

          {tab === 'install' && (
            <div className="space-y-4">
              <div className="rounded-2xl border border-[#E4E4E7] p-4 space-y-3">
                <h3 className="text-sm font-semibold text-[#0D0D0D]">
                  How to install {mod.modMeta.name}
                </h3>
                <ol className="space-y-2.5 list-decimal list-inside text-sm text-[#3F3F46] leading-relaxed">
                  {mod.installInstructions.map((step, i) => (
                    <li key={i}>{step}</li>
                  ))}
                </ol>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
