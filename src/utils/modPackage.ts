import type { GeneratedMod, ModFile, PixelArtSpec } from '../types/mod';

export function normalizeModId(input: string): string {
  const cleaned = input
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '');
  if (!cleaned) return 'custom_mod';
  if (/^[0-9]/.test(cleaned)) return `mod_${cleaned}`;
  return cleaned.slice(0, 32);
}

export function deterministicUuid(seed: string): string {
  let h1 = 0x811c9dc5;
  let h2 = 0x9e3779b9;
  for (let i = 0; i < seed.length; i++) {
    const ch = seed.charCodeAt(i);
    h1 = Math.imul(h1 ^ ch, 0x01000193) >>> 0;
    h2 = Math.imul(h2 ^ (ch + i), 0x85ebca6b) >>> 0;
  }
  const hex = (n: number) => n.toString(16).padStart(8, '0');
  const p1 = hex(h1);
  const p2 = hex(h2).slice(0, 4);
  const p3 = '4' + hex(h1 ^ h2).slice(1, 4);
  const p4 = 'a' + hex((h1 >>> 3) ^ h2).slice(1, 4);
  const p5 = (hex(h1 ^ 0x12345678) + hex(h2 ^ 0x87654321)).slice(0, 12);
  return `${p1}-${p2}-${p3}-${p4}-${p5}`;
}

export function hexToRgba(hex: string): [number, number, number, number] {
  const clean = hex.replace('#', '').trim();
  if (/^[0-9a-fA-F]{6}$/.test(clean)) {
    return [
      parseInt(clean.slice(0, 2), 16),
      parseInt(clean.slice(2, 4), 16),
      parseInt(clean.slice(4, 6), 16),
      255,
    ];
  }
  if (/^[0-9a-fA-F]{8}$/.test(clean)) {
    return [
      parseInt(clean.slice(0, 2), 16),
      parseInt(clean.slice(2, 4), 16),
      parseInt(clean.slice(4, 6), 16),
      parseInt(clean.slice(6, 8), 16),
    ];
  }
  return [130, 140, 160, 255];
}

export function buildPaletteMap(spec: PixelArtSpec): Map<string, string> {
  const map = new Map<string, string>();
  for (const entry of spec.palette ?? []) {
    if (entry.key && entry.hex && entry.key !== '.') {
      map.set(entry.key[0], entry.hex);
    }
  }
  return map;
}

export function pixelArtToSvgDataUrl(spec: PixelArtSpec): string {
  const palette = buildPaletteMap(spec);
  const rows = (spec.rows ?? []).slice(0, 16);
  const rects: string[] = [];

  for (let y = 0; y < 16; y++) {
    const row = rows[y] ?? '';
    for (let x = 0; x < 16; x++) {
      const ch = row[x] ?? '.';
      if (ch === '.' || ch === ' ') continue;
      const color = palette.get(ch) ?? '#64748b';
      rects.push(`<rect x="${x}" y="${y}" width="1" height="1" fill="${color}"/>`);
    }
  }

  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16" shape-rendering="crispEdges">${rects.join('')}</svg>`;
  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;
}

function crc32(buf: Uint8Array): number {
  let crc = 0xffffffff;
  for (let i = 0; i < buf.length; i++) {
    crc ^= buf[i];
    for (let bit = 0; bit < 8; bit++) {
      crc = (crc >>> 1) ^ (crc & 1 ? 0xedb88320 : 0);
    }
  }
  return (crc ^ 0xffffffff) >>> 0;
}

function adler32(buf: Uint8Array): number {
  let a = 1;
  let b = 0;
  for (let i = 0; i < buf.length; i++) {
    a = (a + buf[i]) % 65521;
    b = (b + a) % 65521;
  }
  return ((b << 16) | a) >>> 0;
}

function makePngChunk(type: string, data: Uint8Array): Uint8Array {
  const chunk = new Uint8Array(12 + data.length);
  const view = new DataView(chunk.buffer);
  view.setUint32(0, data.length, false);
  for (let i = 0; i < 4; i++) {
    chunk[4 + i] = type.charCodeAt(i);
  }
  chunk.set(data, 8);
  const crc = crc32(chunk.subarray(4, 8 + data.length));
  view.setUint32(8 + data.length, crc, false);
  return chunk;
}

export function pixelArtToPngBytes(spec: PixelArtSpec): Uint8Array {
  const width = 16;
  const height = 16;
  const palette = buildPaletteMap(spec);
  const rows = (spec.rows ?? []).slice(0, height);

  // Raw scanlines: 1 filter byte (0) + 16 * 4 RGBA bytes per row
  const raw = new Uint8Array(height * (1 + width * 4));
  for (let y = 0; y < height; y++) {
    const row = rows[y] ?? '';
    const rowOffset = y * (1 + width * 4);
    raw[rowOffset] = 0;
    for (let x = 0; x < width; x++) {
      const ch = row[x] ?? '.';
      const pxOffset = rowOffset + 1 + x * 4;
      if (ch === '.' || ch === ' ') {
        raw[pxOffset] = 0;
        raw[pxOffset + 1] = 0;
        raw[pxOffset + 2] = 0;
        raw[pxOffset + 3] = 0;
      } else {
        const [r, g, b, a] = hexToRgba(palette.get(ch) ?? '#64748b');
        raw[pxOffset] = r;
        raw[pxOffset + 1] = g;
        raw[pxOffset + 2] = b;
        raw[pxOffset + 3] = a;
      }
    }
  }

  // Uncompressed deflate stream wrapped in zlib header/footer
  const zlib = new Uint8Array(2 + 5 + raw.length + 4);
  zlib[0] = 0x78;
  zlib[1] = 0x01;
  zlib[2] = 0x01; // final block, no compression
  const len = raw.length;
  zlib[3] = len & 0xff;
  zlib[4] = (len >>> 8) & 0xff;
  const nlen = ~len & 0xffff;
  zlib[5] = nlen & 0xff;
  zlib[6] = (nlen >>> 8) & 0xff;
  zlib.set(raw, 7);
  const adler = adler32(raw);
  const zlibView = new DataView(zlib.buffer);
  zlibView.setUint32(7 + raw.length, adler, false);

  const ihdr = new Uint8Array(13);
  const ihdrView = new DataView(ihdr.buffer);
  ihdrView.setUint32(0, width, false);
  ihdrView.setUint32(4, height, false);
  ihdr[8] = 8; // bit depth
  ihdr[9] = 6; // RGBA
  ihdr[10] = 0;
  ihdr[11] = 0;
  ihdr[12] = 0;

  const signature = new Uint8Array([137, 80, 78, 71, 13, 10, 26, 10]);
  const ihdrChunk = makePngChunk('IHDR', ihdr);
  const idatChunk = makePngChunk('IDAT', zlib);
  const iendChunk = makePngChunk('IEND', new Uint8Array(0));

  const out = new Uint8Array(
    signature.length + ihdrChunk.length + idatChunk.length + iendChunk.length
  );
  let offset = 0;
  out.set(signature, offset);
  offset += signature.length;
  out.set(ihdrChunk, offset);
  offset += ihdrChunk.length;
  out.set(idatChunk, offset);
  offset += idatChunk.length;
  out.set(iendChunk, offset);
  return out;
}

export function ensureEditionCompleteness(mod: GeneratedMod): GeneratedMod {
  const modId = normalizeModId(mod.modMeta.modId || mod.modMeta.name);
  const files = [...(mod.files ?? [])];
  const hasPath = (suffix: string) =>
    files.some((f) => f.path.toLowerCase().endsWith(suffix.toLowerCase()));

  if (mod.modMeta.edition === 'bedrock') {
    const bpHeaderUuid = deterministicUuid(`${modId}:bp:header`);
    const bpModuleUuid = deterministicUuid(`${modId}:bp:module`);
    const rpHeaderUuid = deterministicUuid(`${modId}:rp:header`);
    const rpModuleUuid = deterministicUuid(`${modId}:rp:module`);

    if (!hasPath('behavior_pack/manifest.json')) {
      files.unshift({
        path: 'behavior_pack/manifest.json',
        language: 'json',
        description: 'Bedrock Behavior Pack manifest linking Resource Pack and Script APIs.',
        content: JSON.stringify(
          {
            format_version: 2,
            header: {
              name: `${mod.modMeta.name} BP`,
              description: mod.modMeta.description,
              uuid: bpHeaderUuid,
              version: [1, 0, 0],
              min_engine_version: [1, 21, 0],
            },
            modules: [
              {
                type: 'data',
                uuid: bpModuleUuid,
                version: [1, 0, 0],
              },
            ],
            dependencies: [
              {
                uuid: rpHeaderUuid,
                version: [1, 0, 0],
              },
            ],
          },
          null,
          2
        ),
      });
    }

    if (!hasPath('resource_pack/manifest.json')) {
      files.splice(1, 0, {
        path: 'resource_pack/manifest.json',
        language: 'json',
        description: 'Bedrock Resource Pack manifest for textures, models, and localization.',
        content: JSON.stringify(
          {
            format_version: 2,
            header: {
              name: `${mod.modMeta.name} RP`,
              description: mod.modMeta.description,
              uuid: rpHeaderUuid,
              version: [1, 0, 0],
              min_engine_version: [1, 21, 0],
            },
            modules: [
              {
                type: 'resources',
                uuid: rpModuleUuid,
                version: [1, 0, 0],
              },
            ],
          },
          null,
          2
        ),
      });
    }
  } else {
    if (
      mod.modMeta.loader === 'Fabric' &&
      !hasPath('fabric.mod.json')
    ) {
      files.unshift({
        path: 'src/main/resources/fabric.mod.json',
        language: 'json',
        description: 'Fabric mod metadata and entrypoint declaration.',
        content: JSON.stringify(
          {
            schemaVersion: 1,
            id: modId,
            version: mod.modMeta.version || '1.0.0',
            name: mod.modMeta.name,
            description: mod.modMeta.description,
            authors: [mod.modMeta.author || 'Mod Studio User'],
            license: 'MIT',
            environment: '*',
            entrypoints: {
              main: [`com.modstudio.${modId}.ModMain`],
            },
            depends: {
              fabricloader: '>=0.16.0',
              minecraft: `~${mod.modMeta.minecraftVersion || '1.21.4'}`,
              java: '>=21',
              'fabric-api': '*',
            },
          },
          null,
          2
        ),
      });
    } else if (
      (mod.modMeta.loader === 'Forge' || mod.modMeta.loader === 'NeoForge') &&
      !hasPath('mods.toml') &&
      !hasPath('neoforge.mods.toml')
    ) {
      const tomlPath =
        mod.modMeta.loader === 'NeoForge'
          ? 'src/main/resources/META-INF/neoforge.mods.toml'
          : 'src/main/resources/META-INF/mods.toml';
      files.unshift({
        path: tomlPath,
        language: 'toml',
        description: `${mod.modMeta.loader} mod descriptor file.`,
        content: `modLoader="javafml"\nloaderVersion="[4,)"\nlicense="MIT"\n\n[[mods]]\nmodId="${modId}"\nversion="${mod.modMeta.version || '1.0.0'}"\ndisplayName="${mod.modMeta.name}"\ndescription='''${mod.modMeta.description}'''\n`,
      });
    }

    if (!hasPath('en_us.json')) {
      const langEntries: Record<string, string> = {};
      for (const el of mod.elements ?? []) {
        const keyPrefix = el.kind === 'block' ? 'block' : el.kind === 'mob' ? 'entity' : 'item';
        langEntries[`${keyPrefix}.${modId}.${normalizeModId(el.id)}`] = el.displayName;
      }
      files.push({
        path: `src/main/resources/assets/${modId}/lang/en_us.json`,
        language: 'json',
        description: 'English localization strings for all generated blocks, items, and mobs.',
        content: JSON.stringify(langEntries, null, 2),
      });
    }
  }

  const normalizedElements = (mod.elements ?? []).map((el) => {
    const cleanId = normalizeModId(el.id || el.displayName);
    const defaultTexturePath =
      mod.modMeta.edition === 'bedrock'
        ? `resource_pack/textures/${el.kind === 'block' ? 'blocks' : el.kind === 'mob' ? 'entity' : 'items'}/${cleanId}.png`
        : `src/main/resources/assets/${modId}/textures/${el.kind === 'block' ? 'block' : el.kind === 'mob' ? 'entity' : 'item'}/${cleanId}.png`;
    return {
      ...el,
      id: cleanId,
      texturePath: el.texturePath || defaultTexturePath,
    };
  });

  return {
    ...mod,
    modMeta: {
      ...mod.modMeta,
      modId,
    },
    elements: normalizedElements,
    files,
  };
}

export interface ExportArchivePlan {
  archiveFileName: string;
  textFiles: ModFile[];
  binaryTextures: Array<{ path: string; bytes: Uint8Array }>;
}

export function buildExportArchivePlan(rawMod: GeneratedMod): ExportArchivePlan {
  const mod = ensureEditionCompleteness(rawMod);
  const modId = mod.modMeta.modId;
  const archiveFileName =
    mod.modMeta.edition === 'bedrock'
      ? `${modId}-bedrock.mcaddon`
      : `${modId}-${mod.modMeta.loader.toLowerCase()}.zip`;

  const binaryTextures = mod.elements.map((el) => ({
    path: el.texturePath,
    bytes: pixelArtToPngBytes(el.pixelArt),
  }));

  return {
    archiveFileName,
    textFiles: mod.files,
    binaryTextures,
  };
}

export function buildVariationPrompt(mod: GeneratedMod, customTweak?: string): string {
  const tweak = customTweak?.trim();
  const editionLabel =
    mod.modMeta.edition === 'bedrock'
      ? 'Minecraft Bedrock Edition Add-on'
      : `Minecraft Java Edition (${mod.modMeta.loader})`;
  const elementNames = mod.elements.map((e) => e.displayName).join(', ');

  if (tweak) {
    return `Create a secondary variation of the "${mod.modMeta.name}" (${editionLabel}, MC ${mod.modMeta.minecraftVersion}) mod featuring [${elementNames}]. Apply these specific tweaks: ${tweak}. Give the variation a distinct modId, updated 16x16 pixel-art textures, and updated behaviors and files.`;
  }

  return `Create a secondary variation of the "${mod.modMeta.name}" (${editionLabel}, MC ${mod.modMeta.minecraftVersion}) mod featuring [${elementNames}]. Give it an alternate color palette, refreshed 16x16 pixel-art textures, tweaked item/mob behaviors, and a distinct modId and name.`;
}

