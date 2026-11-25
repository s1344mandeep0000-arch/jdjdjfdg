import { useState } from 'react';
import {
  Download,
  Eye,
  FolderKanban,
  MessageSquare,
  Settings,
  Sliders,
  SquarePen,
  Trash2,
  X,
} from 'lucide-react';
import type { ChatSession, GeneratedMod, SavedProject } from '../types/mod';

interface NavigationDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  sessions: ChatSession[];
  activeSessionId: string | null;
  savedProjects: SavedProject[];
  onNewChat: () => void;
  onSelectSession: (sessionId: string) => void;
  onDeleteSession: (sessionId: string) => void;
  onPreviewProject: (mod: GeneratedMod) => void;
  onExportProject: (mod: GeneratedMod) => void;
  onDeleteProject: (projectId: string) => void;
  onOpenAiSettings: () => void;
  onOpenAppSettings: () => void;
}

export function NavigationDrawer({
  isOpen,
  onClose,
  sessions,
  activeSessionId,
  savedProjects,
  onNewChat,
  onSelectSession,
  onDeleteSession,
  onPreviewProject,
  onExportProject,
  onDeleteProject,
  onOpenAiSettings,
  onOpenAppSettings,
}: NavigationDrawerProps) {
  const [activeTab, setActiveTab] = useState<'history' | 'projects'>('history');
  const [search, setSearch] = useState('');

  if (!isOpen) return null;

  const query = search.trim().toLowerCase();
  const filteredSessions = sessions.filter((s) =>
    s.title.toLowerCase().includes(query)
  );
  const filteredProjects = savedProjects.filter((p) =>
    p.mod.modMeta.name.toLowerCase().includes(query)
  );

  return (
    <div
      className="fixed inset-0 z-50 flex"
      role="dialog"
      aria-modal="true"
      aria-label="Studio Navigation"
    >
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black/35 backdrop-blur-[1px]"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* Drawer Panel */}
      <aside className="relative z-10 w-[85vw] max-w-[320px] h-full bg-white border-r border-[#E4E4E7] flex flex-col shadow-xl">
        {/* Header */}
        <div className="px-4 pt-4 pb-3 flex items-center justify-between gap-2 border-b border-[#F4F4F5]">
          <span className="text-[15px] font-semibold text-[#0D0D0D] truncate">
            Minecraft AI Studio
          </span>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close navigation menu"
            className="w-10 h-10 min-w-[40px] min-h-[40px] rounded-full hover:bg-[#F4F4F5] flex items-center justify-center text-[#52525B]"
          >
            <X className="w-5 h-5" aria-hidden="true" />
          </button>
        </div>

        {/* New Chat + Search */}
        <div className="p-3 space-y-2.5">
          <button
            type="button"
            onClick={() => {
              onNewChat();
              onClose();
            }}
            className="w-full min-h-[44px] px-3.5 py-2.5 rounded-xl bg-[#F4F4F5] hover:bg-[#EAEAEF] text-sm font-medium text-[#0D0D0D] flex items-center gap-2.5 transition-colors"
          >
            <SquarePen className="w-4 h-4 text-[#3B82F6]" aria-hidden="true" />
            <span>New Chat</span>
          </button>

          <input
            type="search"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search chats or projects..."
            aria-label="Search chats or projects"
            className="w-full h-10 px-3 rounded-xl bg-[#FAFAFA] border border-[#E4E4E7] text-sm text-[#0D0D0D] placeholder:text-[#8E8E93] focus:outline-none focus:border-[#3B82F6]"
          />

          {/* Segmented Tab Switcher */}
          <div className="grid grid-cols-2 gap-1 p-1 rounded-xl bg-[#F4F4F5]">
            <button
              type="button"
              onClick={() => setActiveTab('history')}
              className={`min-h-[36px] rounded-lg text-xs font-medium flex items-center justify-center gap-1.5 transition-colors ${
                activeTab === 'history'
                  ? 'bg-white text-[#0D0D0D] shadow-xs'
                  : 'text-[#71717A] hover:text-[#0D0D0D]'
              }`}
            >
              <MessageSquare className="w-3.5 h-3.5" aria-hidden="true" />
              <span>Chat History</span>
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('projects')}
              className={`min-h-[36px] rounded-lg text-xs font-medium flex items-center justify-center gap-1.5 transition-colors ${
                activeTab === 'projects'
                  ? 'bg-white text-[#0D0D0D] shadow-xs'
                  : 'text-[#71717A] hover:text-[#0D0D0D]'
              }`}
            >
              <FolderKanban className="w-3.5 h-3.5" aria-hidden="true" />
              <span>Saved Projects</span>
            </button>
          </div>
        </div>

        {/* Scrollable List */}
        <div className="flex-1 overflow-y-auto clean-scroll px-3 pb-3 space-y-1">
          {activeTab === 'history' ? (
            filteredSessions.length === 0 ? (
              <p className="text-xs text-[#71717A] text-center py-8 px-4">
                No chat history yet. Describe a mod idea to get started.
              </p>
            ) : (
              filteredSessions.map((session) => {
                const isActive = session.id === activeSessionId;
                return (
                  <div
                    key={session.id}
                    className={`group flex items-center gap-1 rounded-xl px-2.5 py-1.5 transition-colors ${
                      isActive ? 'bg-[#EFF6FF]' : 'hover:bg-[#F4F4F5]'
                    }`}
                  >
                    <button
                      type="button"
                      onClick={() => {
                        onSelectSession(session.id);
                        onClose();
                      }}
                      className="flex-1 min-w-0 text-left py-1.5"
                    >
                      <p
                        className={`text-sm truncate ${
                          isActive
                            ? 'font-semibold text-[#1D4ED8]'
                            : 'font-medium text-[#18181B]'
                        }`}
                      >
                        {session.title}
                      </p>
                      <p className="text-[11px] text-[#71717A] tabular-nums">
                        {new Date(session.updatedAt).toLocaleDateString()} ·{' '}
                        {session.messages.length} messages
                      </p>
                    </button>
                    <button
                      type="button"
                      onClick={() => onDeleteSession(session.id)}
                      aria-label={`Delete chat '${session.title}'`}
                      className="w-9 h-9 min-w-[36px] min-h-[36px] rounded-lg hover:bg-white text-[#71717A] hover:text-red-600 flex items-center justify-center shrink-0"
                    >
                      <Trash2 className="w-4 h-4" aria-hidden="true" />
                    </button>
                  </div>
                );
              })
            )
          ) : filteredProjects.length === 0 ? (
            <p className="text-xs text-[#71717A] text-center py-8 px-4">
              No saved mod projects yet. Generated mods are saved here automatically.
            </p>
          ) : (
            filteredProjects.map((proj) => (
              <div
                key={proj.id}
                className="rounded-xl border border-[#E4E4E7] bg-[#FAFAFA] p-2.5 space-y-2"
              >
                <div className="flex items-start justify-between gap-2">
                  <div className="min-w-0">
                    <p className="text-sm font-semibold text-[#0D0D0D] truncate">
                      {proj.mod.modMeta.name}
                    </p>
                    <p className="text-[11px] text-[#71717A] tabular-nums">
                      {proj.mod.modMeta.edition === 'bedrock'
                        ? 'Bedrock Add-on'
                        : `Java ${proj.mod.modMeta.loader}`}{' '}
                      · MC {proj.mod.modMeta.minecraftVersion}
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={() => onDeleteProject(proj.id)}
                    aria-label={`Delete project '${proj.mod.modMeta.name}'`}
                    className="w-8 h-8 rounded-lg hover:bg-white text-[#71717A] hover:text-red-600 flex items-center justify-center shrink-0"
                  >
                    <Trash2 className="w-3.5 h-3.5" aria-hidden="true" />
                  </button>
                </div>
                <div className="flex items-center gap-1.5">
                  <button
                    type="button"
                    onClick={() => {
                      onPreviewProject(proj.mod);
                      onClose();
                    }}
                    className="flex-1 min-h-[36px] px-2.5 rounded-lg bg-white border border-[#E4E4E7] hover:bg-[#F4F4F5] text-xs font-medium text-[#0D0D0D] flex items-center justify-center gap-1.5"
                  >
                    <Eye className="w-3.5 h-3.5" aria-hidden="true" />
                    <span>Preview</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => onExportProject(proj.mod)}
                    className="flex-1 min-h-[36px] px-2.5 rounded-lg bg-[#3B82F6] hover:bg-[#2563EB] text-xs font-medium text-white flex items-center justify-center gap-1.5"
                  >
                    <Download className="w-3.5 h-3.5" aria-hidden="true" />
                    <span>Export</span>
                  </button>
                </div>
              </div>
            ))
          )}
        </div>

        {/* Footer Settings Links */}
        <div className="p-3 border-t border-[#E4E4E7] space-y-1">
          <button
            type="button"
            onClick={() => {
              onClose();
              onOpenAiSettings();
            }}
            className="w-full min-h-[44px] px-3 rounded-xl hover:bg-[#F4F4F5] text-sm font-medium text-[#18181B] flex items-center gap-2.5 transition-colors"
          >
            <Sliders className="w-4 h-4 text-[#3B82F6]" aria-hidden="true" />
            <span>AI Model Settings</span>
          </button>
          <button
            type="button"
            onClick={() => {
              onClose();
              onOpenAppSettings();
            }}
            className="w-full min-h-[44px] px-3 rounded-xl hover:bg-[#F4F4F5] text-sm font-medium text-[#18181B] flex items-center gap-2.5 transition-colors"
          >
            <Settings className="w-4 h-4 text-[#52525B]" aria-hidden="true" />
            <span>Settings</span>
          </button>
        </div>
      </aside>
    </div>
  );
}
