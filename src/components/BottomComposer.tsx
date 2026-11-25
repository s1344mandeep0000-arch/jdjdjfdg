import { useEffect, useRef, type ChangeEvent, type KeyboardEvent } from 'react';
import { ArrowUp, AudioLines, Mic, MicOff, Plus, Square, X } from 'lucide-react';
import type { ChatAttachment } from '../types/mod';

interface BottomComposerProps {
  value: string;
  onChange: (value: string) => void;
  onSend: () => void;
  onStop?: () => void;
  onOpenVoiceModal: () => void;
  isGenerating: boolean;
  isListening: boolean;
  onToggleMic: () => void;
  attachments: ChatAttachment[];
  onAddAttachments: (files: FileList) => void;
  onRemoveAttachment: (id: string) => void;
}

export function BottomComposer({
  value,
  onChange,
  onSend,
  onStop,
  onOpenVoiceModal,
  isGenerating,
  isListening,
  onToggleMic,
  attachments,
  onAddAttachments,
  onRemoveAttachment,
}: BottomComposerProps) {
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const hasContent = value.trim().length > 0 || attachments.length > 0;

  useEffect(() => {
    const el = textareaRef.current;
    if (!el) return;
    el.style.height = 'auto';
    el.style.height = `${Math.min(el.scrollHeight, 136)}px`;
  }, [value]);

  const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      if (hasContent && !isGenerating) {
        onSend();
      }
    }
  };

  const handleFileChange = (e: ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      onAddAttachments(e.target.files);
      e.target.value = '';
    }
  };

  return (
    <div className="shrink-0 w-full bg-white px-4 pt-2 pb-[max(1rem,env(safe-area-inset-bottom))]">
      <div className="max-w-2xl mx-auto">
        <div className="bg-white border border-[#E4E4E7] focus-within:border-[#D4D4D8] rounded-[28px] shadow-[0_2px_16px_rgba(0,0,0,0.06)] px-2 py-1.5 transition-all duration-150">
          {attachments.length > 0 && (
            <div className="flex flex-wrap gap-2 px-2 pt-1.5 pb-1">
              {attachments.map((att) => (
                <div
                  key={att.id}
                  className="inline-flex items-center gap-2 bg-[#F4F4F5] rounded-xl px-2.5 py-1.5 text-xs text-[#27272A] max-w-[200px]"
                >
                  {att.previewUrl && (
                    <img
                      src={att.previewUrl}
                      alt={att.name}
                      referrerPolicy="no-referrer"
                      className="w-5 h-5 rounded object-cover shrink-0"
                    />
                  )}
                  <span className="truncate">{att.name}</span>
                  <button
                    type="button"
                    onClick={() => onRemoveAttachment(att.id)}
                    aria-label={`Remove attachment ${att.name}`}
                    className="w-5 h-5 rounded-full hover:bg-[#E4E4E7] flex items-center justify-center text-[#52525B] shrink-0"
                  >
                    <X className="w-3.5 h-3.5" aria-hidden="true" />
                  </button>
                </div>
              ))}
            </div>
          )}

          <div className="flex items-end gap-0.5 sm:gap-1">
            {/* Hidden file input */}
            <input
              ref={fileInputRef}
              type="file"
              multiple
              accept="image/*,.json,.mcpack,.mcaddon,.java,.js,.ts,.txt"
              onChange={handleFileChange}
              className="hidden"
              aria-hidden="true"
            />

            {/* Plus (+) attachment button */}
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              aria-label="Attach reference image or mod file"
              className="w-10 h-10 sm:w-11 sm:h-11 min-w-[40px] min-h-[40px] rounded-full hover:bg-[#F4F4F5] active:scale-95 text-[#3F3F46] flex items-center justify-center transition-colors shrink-0 focus-visible:outline-2 focus-visible:outline-[#3B82F6]"
            >
              <Plus className="w-5 h-5 stroke-[2]" aria-hidden="true" />
            </button>

            {/* Text input */}
            <textarea
              ref={textareaRef}
              rows={1}
              value={value}
              onChange={(e) => onChange(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Describe your Minecraft mod idea..."
              aria-label="Describe your Minecraft mod idea"
              className="flex-1 min-w-0 bg-transparent text-[13px] tracking-[-0.015em] sm:tracking-normal sm:text-[15px] leading-6 text-[#0D0D0D] placeholder:text-[#8E8E93] placeholder:truncate py-2 sm:py-2.5 px-0.5 sm:px-1.5 max-h-[136px] resize-none focus:outline-none clean-scroll"
            />

            {/* Microphone icon button */}
            <button
              type="button"
              onClick={onToggleMic}
              aria-label={isListening ? 'Stop voice dictation' : 'Voice dictation'}
              className={`w-10 h-10 sm:w-11 sm:h-11 min-w-[40px] min-h-[40px] rounded-full flex items-center justify-center transition-all shrink-0 focus-visible:outline-2 focus-visible:outline-[#3B82F6] ${
                isListening
                  ? 'bg-red-50 text-red-600'
                  : 'hover:bg-[#F4F4F5] text-[#3F3F46]'
              }`}
            >
              {isListening ? (
                <MicOff className="w-[18px] h-[18px] sm:w-[19px] sm:h-[19px] stroke-[2]" aria-hidden="true" />
              ) : (
                <Mic className="w-[18px] h-[18px] sm:w-[19px] sm:h-[19px] stroke-[1.9]" aria-hidden="true" />
              )}
            </button>

            {/* Blue AI voice / Send / Stop button */}
            {isGenerating ? (
              <button
                type="button"
                onClick={onStop}
                aria-label="Stop generation"
                className="w-10 h-10 sm:w-11 sm:h-11 min-w-[40px] min-h-[40px] rounded-full bg-[#3B82F6] hover:bg-[#2563EB] active:scale-95 text-white flex items-center justify-center shadow-sm transition-all shrink-0 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#2563EB]"
              >
                <Square className="w-4 h-4 fill-white" aria-hidden="true" />
              </button>
            ) : hasContent ? (
              <button
                type="button"
                onClick={onSend}
                aria-label="Send mod idea"
                className="w-10 h-10 sm:w-11 sm:h-11 min-w-[40px] min-h-[40px] rounded-full bg-[#3B82F6] hover:bg-[#2563EB] active:scale-95 text-white flex items-center justify-center shadow-[0_2px_8px_rgba(59,130,246,0.3)] transition-all duration-150 shrink-0 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#2563EB]"
              >
                <ArrowUp className="w-5 h-5 stroke-[2.25]" aria-hidden="true" />
              </button>
            ) : (
              <button
                type="button"
                onClick={onOpenVoiceModal}
                aria-label="Open AI Voice Studio"
                className="w-10 h-10 sm:w-11 sm:h-11 min-w-[40px] min-h-[40px] rounded-full bg-[#3B82F6] hover:bg-[#2563EB] active:scale-95 text-white flex items-center justify-center shadow-[0_2px_8px_rgba(59,130,246,0.3)] transition-all duration-150 shrink-0 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#2563EB]"
              >
                <AudioLines className="w-5 h-5 stroke-[2.1]" aria-hidden="true" />
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
