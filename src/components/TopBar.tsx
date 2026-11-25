import { Sparkles, SquarePen } from 'lucide-react';

interface TopBarProps {
  onOpenMenu: () => void;
  onOpenAiPro: () => void;
  onNewChat: () => void;
}

export function TopBar({ onOpenMenu, onOpenAiPro, onNewChat }: TopBarProps) {
  return (
    <header className="shrink-0 w-full bg-white/95 backdrop-blur-sm z-20">
      <div className="max-w-3xl mx-auto px-4 pt-3 pb-2 flex items-center justify-between gap-4">
        {/* Top-left circular hamburger menu button */}
        <button
          type="button"
          onClick={onOpenMenu}
          aria-label="Open navigation menu"
          className="w-11 h-11 min-w-[44px] min-h-[44px] rounded-full bg-[#F4F4F5] hover:bg-[#E4E4E7] active:scale-95 text-[#0D0D0D] flex items-center justify-center transition-all duration-150 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#3B82F6]"
        >
          <svg
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            aria-hidden="true"
          >
            <line x1="4" y1="9" x2="20" y2="9" />
            <line x1="4" y1="15" x2="14" y2="15" />
          </svg>
        </button>

        {/* Center blue AI Pro button */}
        <button
          type="button"
          onClick={onOpenAiPro}
          aria-label="AI Pro model settings"
          className="h-10 min-h-[40px] px-4 py-2 rounded-full bg-[#3B82F6] hover:bg-[#2563EB] active:scale-[0.97] text-white text-[14px] font-semibold tracking-tight flex items-center gap-1.5 shadow-[0_2px_10px_rgba(59,130,246,0.28)] transition-all duration-150 whitespace-nowrap shrink-0 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#2563EB]"
        >
          <Sparkles className="w-4 h-4 stroke-[2]" aria-hidden="true" />
          <span>AI Pro</span>
        </button>

        {/* Top-right circular new chat button */}
        <button
          type="button"
          onClick={onNewChat}
          aria-label="Start new chat"
          className="w-11 h-11 min-w-[44px] min-h-[44px] rounded-full bg-[#F4F4F5] hover:bg-[#E4E4E7] active:scale-95 text-[#0D0D0D] flex items-center justify-center transition-all duration-150 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#3B82F6]"
        >
          <SquarePen className="w-[19px] h-[19px] stroke-[1.9]" aria-hidden="true" />
        </button>
      </div>
    </header>
  );
}
