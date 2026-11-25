import JSZip from 'jszip';
import type { GeneratedMod } from '../types/mod';
import { buildExportArchivePlan, pixelArtToPngBytes, normalizeModId } from './modPackage';

export function triggerBlobDownload(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  setTimeout(() => URL.revokeObjectURL(url), 1500);
}

export async function exportModArchive(mod: GeneratedMod): Promise<string> {
  const plan = buildExportArchivePlan(mod);
  const zip = new JSZip();

  for (const file of plan.textFiles) {
    zip.file(file.path, file.content);
  }

  for (const tex of plan.binaryTextures) {
    zip.file(tex.path, tex.bytes);
  }

  const readmeContent = [
    `# ${mod.modMeta.name}`,
    `Edition: ${mod.modMeta.edition === 'bedrock' ? 'Minecraft Bedrock Edition' : 'Minecraft Java Edition'} (${mod.modMeta.loader})`,
    `Target Minecraft Version: ${mod.modMeta.minecraftVersion}`,
    '',
    mod.summary,
    '',
    '## Installation Steps',
    ...mod.installInstructions.map((step, idx) => `${idx + 1}. ${step}`),
  ].join('\n');

  zip.file('README.md', readmeContent);

  const blob = await zip.generateAsync({ type: 'blob' });
  triggerBlobDownload(blob, plan.archiveFileName);
  return plan.archiveFileName;
}

export function downloadSingleTextFile(filename: string, content: string): void {
  const cleanName = filename.split('/').pop() || 'mod-file.txt';
  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
  triggerBlobDownload(blob, cleanName);
}

export function downloadElementPng(mod: GeneratedMod, elementId: string): void {
  const el = mod.elements.find((e) => e.id === elementId);
  if (!el) return;
  const bytes = pixelArtToPngBytes(el.pixelArt);
  const copy = new Uint8Array(bytes.byteLength);
  copy.set(bytes);
  const blob = new Blob([copy.buffer], { type: 'image/png' });
  triggerBlobDownload(blob, `${normalizeModId(el.id)}.png`);
}
