export type MinecraftEdition = 'java' | 'bedrock';

export type EditionPreference = 'auto' | 'java' | 'bedrock';

export type JavaModLoader = 'Fabric' | 'Forge' | 'NeoForge';

export type ModLoader = JavaModLoader | 'Bedrock Add-on';

export type ReasoningDepth = 'low' | 'medium' | 'high';

export type ModElementKind =
  | 'block'
  | 'item'
  | 'weapon'
  | 'armor'
  | 'mob'
  | 'structure';

export interface PaletteEntry {
  key: string;
  hex: string;
}

export interface PixelArtSpec {
  palette: PaletteEntry[];
  rows: string[];
}

export interface ModElement {
  id: string;
  kind: ModElementKind;
  displayName: string;
  description: string;
  statsSummary: string;
  craftingRecipe: string;
  behaviorSummary: string;
  texturePath: string;
  pixelArt: PixelArtSpec;
}

export interface ModFile {
  path: string;
  language: string;
  description: string;
  content: string;
}

export interface ModMetadata {
  name: string;
  modId: string;
  version: string;
  minecraftVersion: string;
  edition: MinecraftEdition;
  loader: ModLoader;
  description: string;
  author: string;
}

export interface GeneratedMod {
  modMeta: ModMetadata;
  summary: string;
  elements: ModElement[];
  files: ModFile[];
  installInstructions: string[];
}

export interface ChatAttachment {
  id: string;
  name: string;
  mimeType: string;
  size: number;
  base64Data?: string;
  previewUrl?: string;
  textSnippet?: string;
}

export interface GenerationStep {
  id: string;
  label: string;
  status: 'pending' | 'active' | 'done';
}

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  text: string;
  timestamp: number;
  attachments?: ChatAttachment[];
  steps?: GenerationStep[];
  mod?: GeneratedMod;
  error?: string;
}

export interface ChatSession {
  id: string;
  title: string;
  createdAt: number;
  updatedAt: number;
  messages: ChatMessage[];
}

export interface SavedProject {
  id: string;
  savedAt: number;
  chatId: string;
  mod: GeneratedMod;
}

export interface AppSettings {
  editionPreference: EditionPreference;
  javaLoader: JavaModLoader;
  minecraftVersion: string;
  reasoningDepth: ReasoningDepth;
  autoSaveProjects: boolean;
  includeBedrockScripts: boolean;
  apiBaseUrl: string;
}

export const DEFAULT_APP_SETTINGS: AppSettings = {
  editionPreference: 'auto',
  javaLoader: 'Fabric',
  minecraftVersion: '1.21.4',
  reasoningDepth: 'medium',
  autoSaveProjects: true,
  includeBedrockScripts: true,
  apiBaseUrl: '',
};
