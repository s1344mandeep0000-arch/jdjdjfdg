import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { TopBar } from './components/TopBar';
import { EmptyHomeSuggestions } from './components/EmptyHomeSuggestions';
import { ChatThread } from './components/ChatThread';
import { BottomComposer } from './components/BottomComposer';
import { NavigationDrawer } from './components/NavigationDrawer';
import { ModPreviewModal } from './components/ModPreviewModal';
import { SettingsModal } from './components/SettingsModal';
import { VoiceModal } from './components/VoiceModal';
import type {
  AppSettings,
  ChatAttachment,
  ChatMessage,
  ChatSession,
  GeneratedMod,
  GenerationStep,
  SavedProject,
} from './types/mod';
import { exportModArchive } from './utils/exportZip';
import { buildVariationPrompt } from './utils/modPackage';
import {
  loadAppSettings,
  loadSavedProjects,
  loadSessions,
  saveAppSettings,
  saveSavedProjects,
  saveSessions,
} from './utils/storage';

const INITIAL_STEPS: GenerationStep[] = [
  { id: 'analyze', label: 'Analyzing mod concept & Minecraft compatibility...', status: 'active' },
  { id: 'design', label: 'Designing blocks, items, mobs & crafting recipes...', status: 'pending' },
  { id: 'textures', label: 'Generating 16×16 pixel textures & 3D JSON models...', status: 'pending' },
  { id: 'files', label: 'Compiling Minecraft project manifests & source files...', status: 'pending' },
];

function createId(prefix: string): string {
  return `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
}

function advanceSteps(steps: GenerationStep[], activeStepId: string, label?: string): GenerationStep[] {
  const activeIdx = steps.findIndex((s) => s.id === activeStepId);
  if (activeIdx === -1) return steps;
  return steps.map((step, idx) => {
    if (idx < activeIdx) return { ...step, status: 'done' };
    if (idx === activeIdx) {
      return { ...step, label: label || step.label, status: 'active' };
    }
    return { ...step, status: 'pending' };
  });
}

export default function App() {
  const [sessions, setSessions] = useState<ChatSession[]>(() => loadSessions());
  const [activeSessionId, setActiveSessionId] = useState<string | null>(null);
  const [savedProjects, setSavedProjects] = useState<SavedProject[]>(() => loadSavedProjects());
  const [settings, setSettings] = useState<AppSettings>(() => loadAppSettings());

  const [inputText, setInputText] = useState('');
  const [attachments, setAttachments] = useState<ChatAttachment[]>([]);
  const [isGenerating, setIsGenerating] = useState(false);
  const [isListening, setIsListening] = useState(false);

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [previewMod, setPreviewMod] = useState<GeneratedMod | null>(null);
  const [settingsMode, setSettingsMode] = useState<'ai' | 'app' | null>(null);
  const [voiceModalOpen, setVoiceModalOpen] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [viewportHeight, setViewportHeight] = useState<number | null>(null);

  const abortRef = useRef<AbortController | null>(null);
  const recognitionRef = useRef<{ stop: () => void } | null>(null);

  // Sync storage
  useEffect(() => {
    saveSessions(sessions);
  }, [sessions]);

  useEffect(() => {
    saveSavedProjects(savedProjects);
  }, [savedProjects]);

  useEffect(() => {
    saveAppSettings(settings);
  }, [settings]);

  // Adjust layout automatically when mobile/Android keyboard opens via visualViewport
  useEffect(() => {
    const vv = window.visualViewport;
    if (!vv) return;
    const updateHeight = () => {
      setViewportHeight(vv.height);
    };
    updateHeight();
    vv.addEventListener('resize', updateHeight);
    return () => vv.removeEventListener('resize', updateHeight);
  }, []);

  const showToast = useCallback((msg: string) => {
    setToastMessage(msg);
    setTimeout(() => {
      setToastMessage((prev) => (prev === msg ? null : prev));
    }, 2800);
  }, []);

  const activeSession = useMemo(
    () => sessions.find((s) => s.id === activeSessionId) ?? null,
    [sessions, activeSessionId]
  );

  const savedModIds = useMemo(
    () => new Set(savedProjects.map((p) => p.mod.modMeta.modId)),
    [savedProjects]
  );

  const handleNewChat = useCallback(() => {
    abortRef.current?.abort();
    setIsGenerating(false);
    setActiveSessionId(null);
    setInputText('');
    setAttachments([]);
  }, []);

  const upsertSavedProject = useCallback(
    (mod: GeneratedMod, chatId: string, explicit = false) => {
      setSavedProjects((prev) => {
        const existingIndex = prev.findIndex(
          (p) => p.mod.modMeta.modId === mod.modMeta.modId
        );
        const entry: SavedProject = {
          id: existingIndex >= 0 ? prev[existingIndex].id : createId('proj'),
          savedAt: Date.now(),
          chatId,
          mod,
        };
        if (existingIndex >= 0) {
          const next = [...prev];
          next[existingIndex] = entry;
          return next;
        }
        return [entry, ...prev];
      });
      if (explicit) {
        showToast(`Saved "${mod.modMeta.name}" to Saved Projects`);
      }
    },
    [showToast]
  );

  const runGeneration = useCallback(
    async (
      promptText: string,
      promptAttachments: ChatAttachment[],
      targetSessionId?: string
    ) => {
      const trimmed = promptText.trim();
      if (!trimmed && promptAttachments.length === 0) return;

      const userMsg: ChatMessage = {
        id: createId('msg'),
        role: 'user',
        text: trimmed || 'Analyze attached reference and create a Minecraft mod.',
        timestamp: Date.now(),
        attachments: promptAttachments.length > 0 ? promptAttachments : undefined,
      };

      const assistantMsgId = createId('msg');
      const assistantPlaceholder: ChatMessage = {
        id: assistantMsgId,
        role: 'assistant',
        text: '',
        timestamp: Date.now(),
        steps: INITIAL_STEPS.map((s) => ({ ...s })),
      };

      let sessionId = targetSessionId ?? activeSessionId;
      let historyForApi: Array<{ role: 'user' | 'assistant'; text: string }> = [];

      if (!sessionId) {
        sessionId = createId('chat');
        const title =
          userMsg.text.length > 42
            ? `${userMsg.text.slice(0, 42)}...`
            : userMsg.text;
        const newSession: ChatSession = {
          id: sessionId,
          title,
          createdAt: Date.now(),
          updatedAt: Date.now(),
          messages: [userMsg, assistantPlaceholder],
        };
        setSessions((prev) => [newSession, ...prev]);
        setActiveSessionId(sessionId);
        historyForApi = [{ role: 'user', text: userMsg.text }];
      } else {
        const existing = sessions.find((s) => s.id === sessionId);
        const priorMessages = existing ? existing.messages : [];
        historyForApi = [
          ...priorMessages
            .filter((m) => !m.error && (m.text || m.mod))
            .map((m) => ({
              role: m.role,
              text: m.mod
                ? `Generated mod "${m.mod.modMeta.name}" (${m.mod.modMeta.edition}): ${m.mod.summary}`
                : m.text,
            })),
          { role: 'user' as const, text: userMsg.text },
        ];

        setSessions((prev) =>
          prev.map((s) =>
            s.id === sessionId
              ? {
                  ...s,
                  updatedAt: Date.now(),
                  messages: [...s.messages, userMsg, assistantPlaceholder],
                }
              : s
          )
        );
      }

      setInputText('');
      setAttachments([]);
      setIsGenerating(true);

      const controller = new AbortController();
      abortRef.current = controller;

      const updateAssistantMsg = (updater: (msg: ChatMessage) => ChatMessage) => {
        setSessions((prev) =>
          prev.map((s) =>
            s.id === sessionId
              ? {
                  ...s,
                  updatedAt: Date.now(),
                  messages: s.messages.map((m) =>
                    m.id === assistantMsgId ? updater(m) : m
                  ),
                }
              : s
          )
        );
      };

      try {
        const response = await fetch('/api/mods/generate', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            messages: historyForApi,
            attachments: promptAttachments,
            settings,
          }),
          signal: controller.signal,
        });

        if (!response.ok || !response.body) {
          throw new Error(`Mod generation request failed (${response.status}).`);
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        while (true) {
          const { done, value } = await reader.read();
          if (done) break;
          buffer += decoder.decode(value, { stream: true });

          const events = buffer.split('\n\n');
          buffer = events.pop() ?? '';

          for (const rawEvent of events) {
            const lines = rawEvent.split('\n');
            let eventType = 'message';
            let dataStr = '';
            for (const line of lines) {
              if (line.startsWith('event:')) {
                eventType = line.slice(6).trim();
              } else if (line.startsWith('data:')) {
                dataStr += line.slice(5).trim();
              }
            }
            if (!dataStr) continue;

            const payload = JSON.parse(dataStr);
            if (eventType === 'step') {
              updateAssistantMsg((m) => ({
                ...m,
                steps: advanceSteps(
                  m.steps ?? INITIAL_STEPS,
                  payload.stepId,
                  payload.label
                ),
              }));
            } else if (eventType === 'complete' && payload.mod) {
              const generatedMod = payload.mod as GeneratedMod;
              updateAssistantMsg((m) => ({
                ...m,
                text: generatedMod.summary,
                mod: generatedMod,
                steps: (m.steps ?? []).map((s) => ({ ...s, status: 'done' })),
              }));
              if (settings.autoSaveProjects) {
                upsertSavedProject(generatedMod, sessionId!, false);
              }
            } else if (eventType === 'error') {
              throw new Error(
                payload.message || 'AI encountered an error generating this mod.'
              );
            }
          }
        }
      } catch (err) {
        if ((err as Error).name === 'AbortError') {
          updateAssistantMsg((m) => ({
            ...m,
            error: 'Generation stopped.',
          }));
        } else {
          updateAssistantMsg((m) => ({
            ...m,
            error:
              err instanceof Error
                ? err.message
                : 'Could not generate mod. Please try again.',
          }));
        }
      } finally {
        setIsGenerating(false);
        abortRef.current = null;
      }
    },
    [activeSessionId, sessions, settings, upsertSavedProject]
  );

  const handleSend = () => {
    runGeneration(inputText, attachments);
  };

  const handleAddAttachments = async (fileList: FileList) => {
    const next: ChatAttachment[] = [];
    for (const file of Array.from(fileList)) {
      const id = createId('att');
      if (file.type.startsWith('image/')) {
        const dataUrl = await new Promise<string>((resolve) => {
          const reader = new FileReader();
          reader.onload = () => resolve(String(reader.result || ''));
          reader.readAsDataURL(file);
        });
        const base64Data = dataUrl.split(',')[1] || '';
        next.push({
          id,
          name: file.name,
          mimeType: file.type,
          size: file.size,
          base64Data,
          previewUrl: dataUrl,
        });
      } else {
        const textSnippet = await file.text();
        next.push({
          id,
          name: file.name,
          mimeType: file.type || 'text/plain',
          size: file.size,
          textSnippet,
        });
      }
    }
    setAttachments((prev) => [...prev, ...next]);
  };

  const handleToggleMic = () => {
    if (isListening) {
      recognitionRef.current?.stop();
      setIsListening(false);
      return;
    }

    const win = window as unknown as {
      SpeechRecognition?: new () => {
        continuous: boolean;
        interimResults: boolean;
        lang: string;
        onresult: ((ev: { results: ArrayLike<ArrayLike<{ transcript: string }>> }) => void) | null;
        onerror: (() => void) | null;
        onend: (() => void) | null;
        start: () => void;
        stop: () => void;
      };
      webkitSpeechRecognition?: new () => {
        continuous: boolean;
        interimResults: boolean;
        lang: string;
        onresult: ((ev: { results: ArrayLike<ArrayLike<{ transcript: string }>> }) => void) | null;
        onerror: (() => void) | null;
        onend: (() => void) | null;
        start: () => void;
        stop: () => void;
      };
    };
    const SpeechCtor = win.SpeechRecognition || win.webkitSpeechRecognition;
    if (!SpeechCtor) {
      setVoiceModalOpen(true);
      return;
    }

    const rec = new SpeechCtor();
    rec.continuous = false;
    rec.interimResults = true;
    rec.lang = 'en-US';
    rec.onresult = (event) => {
      let text = '';
      for (let i = 0; i < event.results.length; i++) {
        text += event.results[i][0]?.transcript ?? '';
      }
      setInputText(text.trim());
    };
    rec.onerror = () => setIsListening(false);
    rec.onend = () => setIsListening(false);
    recognitionRef.current = rec;
    try {
      rec.start();
      setIsListening(true);
    } catch {
      setIsListening(false);
    }
  };

  const handleExportMod = async (mod: GeneratedMod) => {
    const filename = await exportModArchive(mod);
    showToast(`Exported ${filename}`);
  };

  const handleRetryLast = () => {
    if (!activeSession) return;
    const lastUser = [...activeSession.messages]
      .reverse()
      .find((m) => m.role === 'user');
    if (lastUser) {
      runGeneration(lastUser.text, lastUser.attachments ?? [], activeSession.id);
    }
  };

  return (
    <div
      style={viewportHeight ? { height: `${viewportHeight}px` } : undefined}
      className="h-dvh w-full bg-white text-[#0D0D0D] flex flex-col overflow-hidden"
    >
      <TopBar
        onOpenMenu={() => setDrawerOpen(true)}
        onOpenAiPro={() => setSettingsMode('ai')}
        onNewChat={handleNewChat}
      />

      <main className="flex-1 flex flex-col min-h-0 overflow-hidden bg-white">
        {!activeSession || activeSession.messages.length === 0 ? (
          <EmptyHomeSuggestions
            onSelectSuggestion={(prompt) => runGeneration(prompt, [])}
          />
        ) : (
          <ChatThread
            messages={activeSession.messages}
            savedModIds={savedModIds}
            isGenerating={isGenerating}
            onPreviewMod={(mod) => setPreviewMod(mod)}
            onExportMod={handleExportMod}
            onSaveProject={(mod) =>
              upsertSavedProject(mod, activeSession.id, true)
            }
            onCreateVariation={(mod, customTweak) =>
              runGeneration(
                buildVariationPrompt(mod, customTweak),
                [],
                activeSession.id
              )
            }
            onRetryLast={handleRetryLast}
          />
        )}
      </main>

      <BottomComposer
        value={inputText}
        onChange={setInputText}
        onSend={handleSend}
        onStop={() => abortRef.current?.abort()}
        onOpenVoiceModal={() => setVoiceModalOpen(true)}
        isGenerating={isGenerating}
        isListening={isListening}
        onToggleMic={handleToggleMic}
        attachments={attachments}
        onAddAttachments={handleAddAttachments}
        onRemoveAttachment={(id) =>
          setAttachments((prev) => prev.filter((a) => a.id !== id))
        }
      />

      {/* Slide-out Drawer */}
      <NavigationDrawer
        isOpen={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        sessions={sessions}
        activeSessionId={activeSessionId}
        savedProjects={savedProjects}
        onNewChat={handleNewChat}
        onSelectSession={(id) => setActiveSessionId(id)}
        onDeleteSession={(id) => {
          setSessions((prev) => prev.filter((s) => s.id !== id));
          if (activeSessionId === id) setActiveSessionId(null);
        }}
        onPreviewProject={(mod) => setPreviewMod(mod)}
        onExportProject={handleExportMod}
        onDeleteProject={(id) =>
          setSavedProjects((prev) => prev.filter((p) => p.id !== id))
        }
        onOpenAiSettings={() => setSettingsMode('ai')}
        onOpenAppSettings={() => setSettingsMode('app')}
      />

      {/* Mod Preview & File Inspector Modal */}
      <ModPreviewModal
        mod={previewMod}
        onClose={() => setPreviewMod(null)}
        onExportMod={handleExportMod}
      />

      {/* AI Model Settings & App Settings Modal */}
      <SettingsModal
        mode={settingsMode}
        settings={settings}
        onUpdateSettings={setSettings}
        onClearHistory={() => {
          setSessions([]);
          setActiveSessionId(null);
          showToast('Chat history cleared');
        }}
        onClearProjects={() => {
          setSavedProjects([]);
          showToast('Saved projects cleared');
        }}
        onClose={() => setSettingsMode(null)}
      />

      {/* Voice Idea Modal */}
      <VoiceModal
        isOpen={voiceModalOpen}
        onClose={() => setVoiceModalOpen(false)}
        onSubmitIdea={(text) => runGeneration(text, [])}
      />

      {/* Transient Confirmation Toast */}
      {toastMessage && (
        <div
          role="status"
          aria-live="polite"
          className="fixed top-16 left-1/2 -translate-x-1/2 z-50 px-4 py-2 rounded-full bg-[#18181B] text-white text-xs font-medium shadow-lg whitespace-nowrap"
        >
          {toastMessage}
        </div>
      )}
    </div>
  );
}
