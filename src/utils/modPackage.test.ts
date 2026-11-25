import test from 'node:test';
import assert from 'node:assert/strict';
import {
  normalizeModId,
  pixelArtToPngBytes,
  pixelArtToSvgDataUrl,
  ensureEditionCompleteness,
  buildExportArchivePlan,
  buildVariationPrompt,
} from './modPackage';
import type { GeneratedMod } from '../types/mod';

const samplePixelArt = {
  palette: [
    { key: 'A', hex: '#7c3aed' },
    { key: 'B', hex: '#c4b5fd' },
  ],
  rows: [
    '................',
    '.......AA.......',
    '......ABBA......',
    '.....ABBBBA.....',
    '......ABBA......',
    '.......AA.......',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
  ],
};

test('normalizeModId produces valid Minecraft namespace', () => {
  assert.equal(normalizeModId('Crystal Golem Mod!'), 'crystal_golem_mod');
  assert.equal(normalizeModId('123 Swords'), 'mod_123_swords');
  assert.equal(normalizeModId('   '), 'custom_mod');
});

test('pixelArtToPngBytes emits valid 16x16 PNG binary', () => {
  const bytes = pixelArtToPngBytes(samplePixelArt);
  assert.equal(bytes[0], 137);
  assert.equal(bytes[1], 80); // P
  assert.equal(bytes[2], 78); // N
  assert.equal(bytes[3], 71); // G
  assert.ok(bytes.length > 50);
});

test('pixelArtToSvgDataUrl encodes crisp pixel rects', () => {
  const url = pixelArtToSvgDataUrl(samplePixelArt);
  assert.ok(url.startsWith('data:image/svg+xml;utf8,'));
  assert.ok(decodeURIComponent(url).includes('#7c3aed'));
});

test('ensureEditionCompleteness adds Bedrock manifests and textures', () => {
  const mod: GeneratedMod = {
    modMeta: {
      name: 'Amethyst Expansion',
      modId: 'Amethyst Expansion',
      version: '1.0.0',
      minecraftVersion: '1.21.4',
      edition: 'bedrock',
      loader: 'Bedrock Add-on',
      description: 'Adds amethyst tools and mobs',
      author: 'Studio',
    },
    summary: 'Adds amethyst content.',
    elements: [
      {
        id: 'amethyst_blade',
        kind: 'weapon',
        displayName: 'Amethyst Blade',
        description: 'A sharp crystal sword.',
        statsSummary: '8 Attack Damage',
        craftingRecipe: '2 Amethyst Shards + 1 Stick',
        behaviorSummary: 'Deals bonus resonance damage.',
        texturePath: '',
        pixelArt: samplePixelArt,
      },
    ],
    files: [],
    installInstructions: ['Import the .mcaddon file into Minecraft Bedrock.'],
  };

  const plan = buildExportArchivePlan(mod);
  assert.equal(plan.archiveFileName, 'amethyst_expansion-bedrock.mcaddon');
  assert.ok(plan.textFiles.some((f) => f.path === 'behavior_pack/manifest.json'));
  assert.ok(plan.textFiles.some((f) => f.path === 'resource_pack/manifest.json'));
  assert.equal(plan.binaryTextures.length, 1);
  assert.equal(
    plan.binaryTextures[0].path,
    'resource_pack/textures/items/amethyst_blade.png'
  );
});

test('ensureEditionCompleteness adds Java Fabric manifest and lang file', () => {
  const mod: GeneratedMod = {
    modMeta: {
      name: 'Skyforge Ore',
      modId: 'skyforge_ore',
      version: '1.0.0',
      minecraftVersion: '1.21.4',
      edition: 'java',
      loader: 'Fabric',
      description: 'Floating sky ore',
      author: 'Studio',
    },
    summary: 'Adds skyforge ore.',
    elements: [
      {
        id: 'sky_ore',
        kind: 'block',
        displayName: 'Skyforge Ore',
        description: 'Glows at high altitude.',
        statsSummary: 'Hardness 4.0',
        craftingRecipe: 'Natural generation above Y=120',
        behaviorSummary: 'Drops raw skyforge.',
        texturePath: '',
        pixelArt: samplePixelArt,
      },
    ],
    files: [],
    installInstructions: ['Build with Gradle and place jar in mods folder.'],
  };

  const completed = ensureEditionCompleteness(mod);
  assert.ok(completed.files.some((f) => f.path.endsWith('fabric.mod.json')));
  assert.ok(completed.files.some((f) => f.path.endsWith('en_us.json')));
});

test('buildVariationPrompt creates default and custom variation instructions', () => {
  const mod: GeneratedMod = {
    modMeta: {
      name: 'Skyforge Ore',
      modId: 'skyforge_ore',
      version: '1.0.0',
      minecraftVersion: '1.21.4',
      edition: 'java',
      loader: 'Fabric',
      description: 'Floating sky ore',
      author: 'Studio',
    },
    summary: 'Adds skyforge ore.',
    elements: [
      {
        id: 'sky_ore',
        kind: 'block',
        displayName: 'Skyforge Ore',
        description: 'Glows at high altitude.',
        statsSummary: 'Hardness 4.0',
        craftingRecipe: 'Natural generation above Y=120',
        behaviorSummary: 'Drops raw skyforge.',
        texturePath: '',
        pixelArt: samplePixelArt,
      },
    ],
    files: [],
    installInstructions: [],
  };

  const defaultPrompt = buildVariationPrompt(mod);
  assert.ok(defaultPrompt.includes('Skyforge Ore'));
  assert.ok(defaultPrompt.includes('alternate color palette'));

  const customPrompt = buildVariationPrompt(mod, 'Emerald green textures');
  assert.ok(customPrompt.includes('Emerald green textures'));
  assert.ok(customPrompt.includes('Skyforge Ore'));
});
