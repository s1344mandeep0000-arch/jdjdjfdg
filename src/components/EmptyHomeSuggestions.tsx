import { Box, Layers, Pickaxe } from 'lucide-react';

interface SuggestionOption {
  id: string;
  label: string;
  prompt: string;
  icon: 'mod' | 'addon' | 'design';
}

const SUGGESTIONS: SuggestionOption[] = [
  {
    id: 'create-mod',
    label: 'Create a Minecraft Mod',
    prompt:
      'Create a Minecraft Java mod that adds Glowing Crystal Ore, a Crystal Greatsword with dash ability, Crystal Armor, and a friendly Crystal Golem mob.',
    icon: 'mod',
  },
  {
    id: 'generate-addon',
    label: 'Generate Minecraft Add-on',
    prompt:
      'Generate a Minecraft Bedrock Edition add-on featuring a Solar Forge block, Sunfire Staff item, and a Desert Guardian mob with custom scripts.',
    icon: 'addon',
  },
  {
    id: 'design-elements',
    label: 'Design Blocks, Items & Mobs',
    prompt:
      'Design a Deep Cavern expansion with Bioluminescent Shroomlight blocks, Echo Pickaxe, Cave Lantern item, and a Lurking Glow-Beetle mob.',
    icon: 'design',
  },
];

interface EmptyHomeSuggestionsProps {
  onSelectSuggestion: (prompt: string) => void;
}

export function EmptyHomeSuggestions({
  onSelectSuggestion,
}: EmptyHomeSuggestionsProps) {
  return (
    <div className="flex-1 flex flex-col items-center justify-center px-6 py-8 bg-white">
      <div className="w-full max-w-sm flex flex-col items-center gap-3">
        {SUGGESTIONS.map((item) => {
          const Icon =
            item.icon === 'mod'
              ? Box
              : item.icon === 'addon'
              ? Layers
              : Pickaxe;
          return (
            <button
              key={item.id}
              type="button"
              onClick={() => onSelectSuggestion(item.prompt)}
              className="w-full min-h-[48px] px-4 py-3 rounded-2xl hover:bg-[#F4F4F5] active:scale-[0.99] flex items-center justify-center gap-3 text-[#71717A] hover:text-[#18181B] transition-all duration-150 focus-visible:outline-2 focus-visible:outline-[#3B82F6]"
            >
              <Icon
                className="w-[19px] h-[19px] stroke-[1.75] text-[#71717A] shrink-0"
                aria-hidden="true"
              />
              <span className="text-[15px] font-medium whitespace-nowrap">
                {item.label}
              </span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
