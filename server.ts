import express from 'express';
import path from 'path';
import { fileURLToPath } from 'url';
import dotenv from 'dotenv';
import { GoogleGenAI, ThinkingLevel, Type } from '@google/genai';
import { createServer as createViteServer } from 'vite';
import type { AppSettings, ChatAttachment, GeneratedMod } from './src/types/mod';
import { ensureEditionCompleteness } from './src/utils/modPackage';

dotenv.config();

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const PORT = Number(process.env.PORT || 3000);

function getGeminiClient(): GoogleGenAI {
  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey) {
    throw new Error('GEMINI_API_KEY is not configured in environment secrets.');
  }
  return new GoogleGenAI({
    apiKey,
    httpOptions: {
      headers: {
        'User-Agent': 'aistudio-build',
      },
    },
  });
}

function mapThinkingLevel(depth: AppSettings['reasoningDepth']): ThinkingLevel {
  switch (depth) {
    case 'low':
      return ThinkingLevel.LOW;
    case 'high':
      return ThinkingLevel.HIGH;
    case 'medium':
    default:
      return ThinkingLevel.MEDIUM;
  }
}

const MOD_RESPONSE_SCHEMA = {
  type: Type.OBJECT,
  properties: {
    modMeta: {
      type: Type.OBJECT,
      properties: {
        name: { type: Type.STRING },
        modId: { type: Type.STRING },
        version: { type: Type.STRING },
        minecraftVersion: { type: Type.STRING },
        edition: { type: Type.STRING, description: "Either 'java' or 'bedrock'" },
        loader: {
          type: Type.STRING,
          description: "One of 'Fabric', 'Forge', 'NeoForge', or 'Bedrock Add-on'",
        },
        description: { type: Type.STRING },
        author: { type: Type.STRING },
      },
      required: ['name', 'modId', 'version', 'minecraftVersion', 'edition', 'loader', 'description'],
    },
    summary: {
      type: Type.STRING,
      description: 'Friendly concise overview explaining the generated mod, gameplay loop, and how items/mobs work together.',
    },
    elements: {
      type: Type.ARRAY,
      items: {
        type: Type.OBJECT,
        properties: {
          id: { type: Type.STRING },
          kind: {
            type: Type.STRING,
            description: "One of 'block', 'item', 'weapon', 'armor', 'mob', or 'structure'",
          },
          displayName: { type: Type.STRING },
          description: { type: Type.STRING },
          statsSummary: { type: Type.STRING },
          craftingRecipe: { type: Type.STRING },
          behaviorSummary: { type: Type.STRING },
          texturePath: { type: Type.STRING },
          pixelArt: {
            type: Type.OBJECT,
            properties: {
              palette: {
                type: Type.ARRAY,
                items: {
                  type: Type.OBJECT,
                  properties: {
                    key: { type: Type.STRING, description: 'Single character key such as A, B, C' },
                    hex: { type: Type.STRING, description: '6-digit hex color like #7c3aed' },
                  },
                  required: ['key', 'hex'],
                },
              },
              rows: {
                type: Type.ARRAY,
                items: { type: Type.STRING },
                description: 'Exactly 16 strings of length 16 using palette keys and "." for transparent pixels.',
              },
            },
            required: ['palette', 'rows'],
          },
        },
        required: [
          'id',
          'kind',
          'displayName',
          'description',
          'statsSummary',
          'craftingRecipe',
          'behaviorSummary',
          'texturePath',
          'pixelArt',
        ],
      },
    },
    files: {
      type: Type.ARRAY,
      items: {
        type: Type.OBJECT,
        properties: {
          path: { type: Type.STRING },
          language: { type: Type.STRING },
          description: { type: Type.STRING },
          content: { type: Type.STRING },
        },
        required: ['path', 'language', 'description', 'content'],
      },
    },
    installInstructions: {
      type: Type.ARRAY,
      items: { type: Type.STRING },
    },
  },
  required: ['modMeta', 'summary', 'elements', 'files', 'installInstructions'],
};

interface GenerateRequestBody {
  messages: Array<{ role: 'user' | 'assistant'; text: string }>;
  attachments?: ChatAttachment[];
  settings?: Partial<AppSettings>;
}

async function startServer() {
  const app = express();
  app.use(express.json({ limit: '12mb' }));

  app.use('/api', (req, res, next) => {
    const origin = req.headers.origin;
    if (origin) {
      res.setHeader('Access-Control-Allow-Origin', origin);
      res.setHeader('Vary', 'Origin');
    } else {
      res.setHeader('Access-Control-Allow-Origin', '*');
    }
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');
    if (req.method === 'OPTIONS') {
      res.status(204).end();
      return;
    }
    next();
  });

  app.get('/api/health', (_req, res) => {
    res.json({ ok: true, service: 'minecraft-ai-mod-maker-studio' });
  });

  app.post('/api/mods/generate', async (req, res) => {
    res.setHeader('Content-Type', 'text/event-stream');
    res.setHeader('Cache-Control', 'no-cache, no-transform');
    res.setHeader('Connection', 'keep-alive');
    res.flushHeaders?.();

    const sendEvent = (event: string, data: unknown) => {
      res.write(`event: ${event}\ndata: ${JSON.stringify(data)}\n\n`);
    };

    try {
      const body = req.body as GenerateRequestBody;
      const messages = Array.isArray(body?.messages) ? body.messages : [];
      if (messages.length === 0) {
        sendEvent('error', { message: 'Please describe your Minecraft mod idea.' });
        res.end();
        return;
      }

      const settings: AppSettings = {
        editionPreference: body.settings?.editionPreference ?? 'auto',
        javaLoader: body.settings?.javaLoader ?? 'Fabric',
        minecraftVersion: body.settings?.minecraftVersion ?? '1.21.4',
        reasoningDepth: body.settings?.reasoningDepth ?? 'medium',
        autoSaveProjects: body.settings?.autoSaveProjects ?? true,
        includeBedrockScripts: body.settings?.includeBedrockScripts ?? true,
        apiBaseUrl: body.settings?.apiBaseUrl ?? '',
      };

      sendEvent('step', {
        stepId: 'analyze',
        label: 'Analyzing mod concept & Minecraft compatibility...',
      });

      const ai = getGeminiClient();

      const editionInstruction =
        settings.editionPreference === 'bedrock'
          ? `Generate a Minecraft Bedrock Edition Add-on (edition: "bedrock", loader: "Bedrock Add-on") compatible with Minecraft Bedrock ${settings.minecraftVersion}. Include Behavior Pack (behavior_pack/manifest.json, blocks/*.json, items/*.json, entities/*.json${settings.includeBedrockScripts ? ', scripts/main.js using @minecraft/server' : ''}) and Resource Pack (resource_pack/manifest.json, textures/terrain_texture.json, textures/item_texture.json, texts/en_US.lang).`
          : settings.editionPreference === 'java'
          ? `Generate a Minecraft Java Edition Mod (edition: "java", loader: "${settings.javaLoader}") compatible with Minecraft Java ${settings.minecraftVersion}. Include proper loader config (${settings.javaLoader === 'Fabric' ? 'src/main/resources/fabric.mod.json' : 'src/main/resources/META-INF/mods.toml'}), Java source files (ModMain.java, registry classes for blocks/items/entities), JSON blockstates/models, recipes, and en_us.json.`
          : `Automatically infer whether Minecraft Java Edition (${settings.javaLoader}) or Minecraft Bedrock Edition (Add-on) best fits the user's request (default to Java ${settings.javaLoader} if unspecified, or Bedrock Add-on if they mention Bedrock, Pocket Edition, Add-on, MCPE, or .mcaddon). Target Minecraft ${settings.minecraftVersion}.`;

      const systemInstruction = `You are Minecraft AI Mod Maker Studio, an expert Minecraft Java Edition & Bedrock Edition mod architect, pixel texture artist, and gameplay designer.

Your task is to turn any natural-language Minecraft mod idea into a complete, coherent, playable mod specification with real source files, JSON models/definitions, crafting recipes, and 16x16 pixel-art textures.

Compatibility Rules:
${editionInstruction}

Output Quality Rules:
1. Automatically design all relevant elements the user's idea implies (blocks, items, weapons, armor, mobs, and/or structures) without forcing the user to pick categories.
2. Create 2 to 6 distinct, well-balanced elements per mod.
3. For EVERY element, generate an expressive, recognizable 16x16 pixelArt grid (16 rows of 16 characters each; use '.' for transparent background and uppercase single-character palette keys like 'A', 'B', 'C', 'D', 'E' mapped to valid 6-digit hex colors in palette). Make swords look like diagonal pixel swords, ores look like stone blocks with embedded colored crystal flecks, armor look like chestplates/helmets, and mobs look like iconic Minecraft mob faces/sprites!
4. Generate realistic, syntactically valid Minecraft project files in "files" matching the chosen edition's exact folder conventions and APIs (Java 21 + ${settings.javaLoader} ${settings.minecraftVersion} OR Bedrock format_version 1.21.x + @minecraft/server). Include 5 to 9 complete, non-truncated key project files.
5. Support iterative refinement: if previous messages in the conversation already created a mod, evolve and expand that mod while preserving compatibility.`;

      const conversationParts = messages.map((m) => ({
        role: m.role === 'assistant' ? 'model' : 'user',
        parts: [{ text: m.text }],
      }));

      const attachments = Array.isArray(body.attachments) ? body.attachments : [];
      if (attachments.length > 0 && conversationParts.length > 0) {
        const lastUserTurn = conversationParts[conversationParts.length - 1];
        for (const att of attachments) {
          if (att.base64Data && att.mimeType.startsWith('image/')) {
            (lastUserTurn.parts as Array<Record<string, unknown>>).push({
              inlineData: {
                mimeType: att.mimeType,
                data: att.base64Data,
              },
            });
          } else if (att.textSnippet) {
            (lastUserTurn.parts as Array<Record<string, unknown>>).push({
              text: `\n[Attached file ${att.name}]:\n${att.textSnippet.slice(0, 6000)}`,
            });
          }
        }
      }

      const progressTimer1 = setTimeout(() => {
        sendEvent('step', {
          stepId: 'design',
          label: 'Designing blocks, items, mobs & crafting recipes...',
        });
      }, 900);

      const progressTimer2 = setTimeout(() => {
        sendEvent('step', {
          stepId: 'textures',
          label: 'Generating 16×16 pixel textures & 3D JSON models...',
        });
      }, 2400);

      const progressTimer3 = setTimeout(() => {
        sendEvent('step', {
          stepId: 'files',
          label: 'Compiling Minecraft project manifests & source files...',
        });
      }, 4200);

      const callGeminiWithRetry = async () => {
        const modelsToTry = ['gemini-4-hydrogen', 'gemini-4-hydrogen', 'gemini-3.1-flash-lite'];
        let lastErr: unknown;
        for (let attempt = 0; attempt < modelsToTry.length; attempt++) {
          const modelName = modelsToTry[attempt];
          try {
            const config: Record<string, unknown> = {
              systemInstruction,
              responseMimeType: 'application/json',
              responseSchema: MOD_RESPONSE_SCHEMA,
            };
            if (modelName === 'gemini-4-hydrogen') {
              config.thinkingConfig = {
                thinkingLevel: mapThinkingLevel(settings.reasoningDepth),
              };
            }
            return await ai.models.generateContent({
              model: modelName,
              contents: conversationParts as never,
              config: config as never,
            });
          } catch (err) {
            lastErr = err;
            const msg = err instanceof Error ? err.message : String(err);
            const isTransient =
              msg.includes('503') ||
              msg.includes('UNAVAILABLE') ||
              msg.includes('429') ||
              msg.includes('RESOURCE_EXHAUSTED');
            if (!isTransient || attempt === modelsToTry.length - 1) {
              throw err;
            }
            await new Promise((r) => setTimeout(r, 1200 * (attempt + 1)));
          }
        }
        throw lastErr;
      };

      try {
        const response = await callGeminiWithRetry();

        clearTimeout(progressTimer1);
        clearTimeout(progressTimer2);
        clearTimeout(progressTimer3);

        const rawText = (response.text ?? '').trim();
        if (!rawText) {
          throw new Error('AI returned an empty mod specification.');
        }

        const parsed = JSON.parse(rawText) as GeneratedMod;
        const completedMod = ensureEditionCompleteness(parsed);

        sendEvent('complete', { mod: completedMod });
        res.end();
      } catch (innerErr) {
        clearTimeout(progressTimer1);
        clearTimeout(progressTimer2);
        clearTimeout(progressTimer3);
        throw innerErr;
      }
    } catch (error) {
      let errMsg =
        error instanceof Error
          ? error.message
          : 'Failed to generate Minecraft mod with AI.';
      try {
        const parsedErr = JSON.parse(errMsg);
        if (parsedErr?.error?.message) {
          errMsg = parsedErr.error.message;
        }
      } catch {
        // Keep original message if not JSON
      }
      console.error('Mod generation error:', error);
      sendEvent('error', { message: errMsg });
      res.end();
    }
  });

  if (process.env.NODE_ENV !== 'production') {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa',
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.resolve(__dirname, 'dist');
    app.use(express.static(distPath));
    app.get('*', (_req, res) => {
      res.sendFile(path.join(distPath, 'index.html'));
    });
  }

  app.listen(PORT, '0.0.0.0', () => {
    console.log(`Minecraft AI Mod Maker Studio running at http://localhost:${PORT}`);
  });
}

startServer().catch((err) => {
  console.error('Server startup failed:', err);
  process.exit(1);
});
