import { useState } from 'react';
import {
  AlertCircle,
  Bookmark,
  BookmarkCheck,
  Check,
  Download,
  Eye,
  GitBranch,
  Loader2,
  RotateCcw,
  Sparkles,
  X,
} from 'lucide-react';
import type { ChatMessage, GeneratedMod } from '../types/mod';
import { pixelArtToSvgDataUrl } from '../utils/modPackage';

interface ChatThreadProps {
  messages: ChatMessage[];
  savedModIds: Set<string>;
  isGenerating: boolean;
  onPreviewMod: (mod: GeneratedMod) => void;
  onExportMod: (mod: GeneratedMod) => Promise<void>;
  onSaveProject: (mod: GeneratedMod) => void;
  onCreateVariation: (mod: GeneratedMod, customTweak?: string) => void;
  onRetryLast: () => void;
}

export function ChatThread({
  messages,
  savedModIds,
  isGenerating,
  onPreviewMod,
  onExportMod,
  onSaveProject,
  onCreateVariation,
  onRetryLast,
}: ChatThreadProps) {
  const [exportingModId, setExportingModId] = useState<string | null>(null);
  const [variationMsgId, setVariationMsgId] = useState<string | null>(null);
  const [variationTweak, setVariationTweak] = useState('');

  const handleExport = async (mod: GeneratedMod) => {
    setExportingModId(mod.modMeta.modId);
    try {
      await onExportMod(mod);
    } finally {
      setExportingModId(null);
    }
  };

  const handleTriggerVariation = (mod: GeneratedMod, tweak?: string) => {
    setVariationMsgId(null);
    setVariationTweak('');
    onCreateVariation(mod, tweak);
  };

  return (
    <div className="flex-1 overflow-y-auto clean-scroll bg-white">
      <div className="max-w-2xl mx-auto px-4 py-6 space-y-6">
        {messages.map((msg) => {
          if (msg.role === 'user') {
            return (
              <div key={msg.id} className="flex justify-end">
                <div className="max-w-[85%] rounded-3xl bg-[#F4F4F5] px-4 py-3 text-[15px] leading-relaxed text-[#0D0D0D] space-y-2">
                  {msg.attachments && msg.attachments.length > 0 && (
                    <div className="flex flex-wrap gap-2">
                      {msg.attachments.map((att) =>
                        att.previewUrl ? (
                          <img
                            key={att.id}
                            src={att.previewUrl}
                            alt={att.name}
                            referrerPolicy="no-referrer"
                            className="w-20 h-20 rounded-xl object-cover border border-[#E4E4E7]"
                          />
                        ) : (
                          <span
                            key={att.id}
                            className="text-xs text-[#52525B] bg-white px-2.5 py-1 rounded-lg border border-[#E4E4E7]"
                          >
                            {att.name}
                          </span>
                        )
                      )}
                    </div>
                  )}
                  <p className="whitespace-pre-wrap break-words">{msg.text}</p>
                </div>
              </div>
            );
          }

          const mod = msg.mod;
          const isSaved = mod ? savedModIds.has(mod.modMeta.modId) : false;
          const isExporting = mod ? exportingModId === mod.modMeta.modId : false;

          return (
            <div key={msg.id} className="w-full space-y-4">
              {/* Live generation progress */}
              {msg.steps && msg.steps.length > 0 && !mod && !msg.error && (
                <div className="space-y-2.5 py-1">
                  {msg.steps.map((step) => (
                    <div
                      key={step.id}
                      className="flex items-center gap-2.5 text-[14px]"
                    >
                      {step.status === 'done' ? (
                        <Check
                          className="w-4 h-4 text-[#16A34A] shrink-0"
                          aria-hidden="true"
                        />
                      ) : step.status === 'active' ? (
                        <Loader2
                          className="w-4 h-4 text-[#3B82F6] animate-spin shrink-0"
                          aria-hidden="true"
                        />
                      ) : (
                        <span
                          className="w-4 h-4 rounded-full border border-[#D4D4D8] shrink-0"
                          aria-hidden="true"
                        />
                      )}
                      <span
                        className={
                          step.status === 'active'
                            ? 'text-[#0D0D0D] font-medium'
                            : 'text-[#71717A]'
                        }
                      >
                        {step.label}
                      </span>
                    </div>
                  ))}
                </div>
              )}

              {/* Error banner */}
              {msg.error && (
                <div className="rounded-2xl border border-red-200 bg-red-50/70 p-4 text-sm text-red-800 space-y-3">
                  <div className="flex items-start gap-2.5">
                    <AlertCircle className="w-4 h-4 mt-0.5 shrink-0 text-red-600" />
                    <p className="leading-relaxed">{msg.error}</p>
                  </div>
                  <button
                    type="button"
                    onClick={onRetryLast}
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-white border border-red-200 text-xs font-medium text-red-700 hover:bg-red-50 transition-colors"
                  >
                    <RotateCcw className="w-3.5 h-3.5" aria-hidden="true" />
                    <span>Try again</span>
                  </button>
                </div>
              )}

              {/* Generated Mod Summary & Package Card */}
              {mod && (
                <div className="space-y-4">
                  <div className="text-[15px] leading-relaxed text-[#18181B] whitespace-pre-wrap">
                    {mod.summary}
                  </div>

                  <div className="rounded-2xl border border-[#E4E4E7] bg-[#FAFAFA] p-4 space-y-4">
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0">
                        <h3 className="text-[16px] font-semibold text-[#0D0D0D] truncate">
                          {mod.modMeta.name}
                        </h3>
                        <p className="text-xs text-[#71717A] mt-0.5 tabular-nums">
                          {mod.modMeta.edition === 'bedrock'
                            ? 'Bedrock Edition'
                            : 'Java Edition'}{' '}
                          · {mod.modMeta.loader} · MC {mod.modMeta.minecraftVersion}{' '}
                          · {mod.files.length} files
                        </p>
                      </div>
                      <button
                        type="button"
                        onClick={() => onSaveProject(mod)}
                        aria-label={
                          isSaved
                            ? `${mod.modMeta.name} saved to projects`
                            : `Save ${mod.modMeta.name} to projects`
                        }
                        className="w-10 h-10 min-w-[40px] min-h-[40px] rounded-xl border border-[#E4E4E7] bg-white hover:bg-[#F4F4F5] flex items-center justify-center text-[#3F3F46] transition-colors shrink-0"
                      >
                        {isSaved ? (
                          <BookmarkCheck
                            className="w-4 h-4 text-[#3B82F6]"
                            aria-hidden="true"
                          />
                        ) : (
                          <Bookmark className="w-4 h-4" aria-hidden="true" />
                        )}
                      </button>
                    </div>

                    {/* Generated Elements with Real 16x16 Pixel-Art Textures */}
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                      {mod.elements.map((el) => (
                        <div
                          key={el.id}
                          className="flex items-center gap-3 rounded-xl bg-white border border-[#E4E4E7] p-2.5"
                        >
                          <div className="w-11 h-11 rounded-lg bg-[#F4F4F5] border border-[#E4E4E7] flex items-center justify-center shrink-0 overflow-hidden">
                            <img
                              src={pixelArtToSvgDataUrl(el.pixelArt)}
                              alt={`${el.displayName} texture`}
                              referrerPolicy="no-referrer"
                              className="w-8 h-8 pixelated"
                            />
                          </div>
                          <div className="min-w-0 flex-1">
                            <p className="text-sm font-medium text-[#0D0D0D] truncate">
                              {el.displayName}
                            </p>
                            <p className="text-xs text-[#71717A] truncate capitalize">
                              {el.kind} · {el.statsSummary}
                            </p>
                          </div>
                        </div>
                      ))}
                    </div>

                    {/* Action Bar */}
                    <div className="flex flex-wrap items-center gap-2 pt-1">
                      <button
                        type="button"
                        onClick={() => onPreviewMod(mod)}
                        className="min-h-[42px] px-4 py-2 rounded-xl bg-white border border-[#E4E4E7] hover:bg-[#F4F4F5] text-sm font-medium text-[#0D0D0D] inline-flex items-center gap-2 transition-colors whitespace-nowrap shrink-0"
                      >
                        <Eye className="w-4 h-4" aria-hidden="true" />
                        <span>Mod Preview</span>
                      </button>
                      <button
                        type="button"
                        disabled={isGenerating}
                        onClick={() => {
                          if (variationMsgId === msg.id) {
                            setVariationMsgId(null);
                            setVariationTweak('');
                          } else {
                            setVariationMsgId(msg.id);
                            setVariationTweak('');
                          }
                        }}
                        className={`min-h-[42px] px-4 py-2 rounded-xl border text-sm font-medium inline-flex items-center gap-2 transition-colors whitespace-nowrap shrink-0 disabled:opacity-50 ${
                          variationMsgId === msg.id
                            ? 'bg-[#EFF6FF] border-[#BFDBFE] text-[#1D4ED8]'
                            : 'bg-white border-[#E4E4E7] hover:bg-[#F4F4F5] text-[#0D0D0D]'
                        }`}
                      >
                        <GitBranch className="w-4 h-4" aria-hidden="true" />
                        <span>Create Variation</span>
                      </button>
                      <button
                        type="button"
                        onClick={() => handleExport(mod)}
                        disabled={isExporting}
                        className="min-h-[42px] px-4 py-2 rounded-xl bg-[#3B82F6] hover:bg-[#2563EB] disabled:opacity-60 text-sm font-medium text-white inline-flex items-center gap-2 shadow-sm transition-colors whitespace-nowrap shrink-0"
                      >
                        {isExporting ? (
                          <Loader2
                            className="w-4 h-4 animate-spin"
                            aria-hidden="true"
                          />
                        ) : (
                          <Download className="w-4 h-4" aria-hidden="true" />
                        )}
                        <span>
                          {mod.modMeta.edition === 'bedrock'
                            ? 'Export .mcaddon'
                            : 'Export Mod .zip'}
                        </span>
                      </button>
                    </div>

                    {/* Inline Variation Quick-Tweak Panel */}
                    {variationMsgId === msg.id && (
                      <div className="rounded-xl border border-[#E4E4E7] bg-white p-3 space-y-2.5">
                        <div className="flex items-center justify-between gap-2">
                          <span className="text-xs font-semibold text-[#18181B]">
                            Customize variation tweaks (optional)
                          </span>
                          <button
                            type="button"
                            onClick={() => {
                              setVariationMsgId(null);
                              setVariationTweak('');
                            }}
                            aria-label="Close variation options"
                            className="w-7 h-7 rounded-lg hover:bg-[#F4F4F5] flex items-center justify-center text-[#71717A]"
                          >
                            <X className="w-3.5 h-3.5" aria-hidden="true" />
                          </button>
                        </div>

                        <div className="flex flex-wrap gap-1.5">
                          {[
                            'Alternate color palette & glowing textures',
                            'Stronger mob behavior & boss abilities',
                            'Fire & magma elemental twist',
                          ].map((preset) => (
                            <button
                              key={preset}
                              type="button"
                              disabled={isGenerating}
                              onClick={() => handleTriggerVariation(mod, preset)}
                              className="min-h-[32px] px-2.5 py-1 rounded-lg bg-[#F4F4F5] hover:bg-[#EAEAEF] text-xs font-medium text-[#27272A] transition-colors"
                            >
                              {preset}
                            </button>
                          ))}
                        </div>

                        <div className="flex items-center gap-2">
                          <input
                            type="text"
                            value={variationTweak}
                            onChange={(e) => setVariationTweak(e.target.value)}
                            onKeyDown={(e) => {
                              if (e.key === 'Enter' && !isGenerating) {
                                e.preventDefault();
                                handleTriggerVariation(mod, variationTweak);
                              }
                            }}
                            placeholder="e.g. Emerald color scheme and faster dash cooldown..."
                            aria-label="Variation tweak description"
                            className="flex-1 min-w-0 h-10 px-3 rounded-xl border border-[#E4E4E7] bg-[#FAFAFA] text-xs text-[#0D0D0D] placeholder:text-[#8E8E93] focus:outline-none focus:border-[#3B82F6]"
                          />
                          <button
                            type="button"
                            disabled={isGenerating}
                            onClick={() => handleTriggerVariation(mod, variationTweak)}
                            className="min-h-[40px] px-3.5 rounded-xl bg-[#3B82F6] hover:bg-[#2563EB] disabled:opacity-50 text-white text-xs font-medium inline-flex items-center gap-1.5 whitespace-nowrap shrink-0"
                          >
                            <Sparkles className="w-3.5 h-3.5" aria-hidden="true" />
                            <span>Generate</span>
                          </button>
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
