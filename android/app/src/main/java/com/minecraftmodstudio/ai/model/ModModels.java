package com.minecraftmodstudio.ai.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure Java domain models and JSON serialization for Minecraft AI Mod Maker Studio.
 */
public final class ModModels {

    private ModModels() {}

    public static class PaletteEntry {
        public String key = "";
        public String hex = "#64748b";

        public PaletteEntry() {}

        public PaletteEntry(String key, String hex) {
            this.key = key;
            this.hex = hex;
        }

        public static PaletteEntry fromJson(JSONObject obj) {
            PaletteEntry entry = new PaletteEntry();
            if (obj != null) {
                entry.key = obj.optString("key", "");
                entry.hex = obj.optString("hex", "#64748b");
            }
            return entry;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("key", key);
            obj.put("hex", hex);
            return obj;
        }
    }

    public static class PixelArtSpec {
        public final List<PaletteEntry> palette = new ArrayList<>();
        public final List<String> rows = new ArrayList<>();

        public static PixelArtSpec fromJson(JSONObject obj) {
            PixelArtSpec spec = new PixelArtSpec();
            if (obj == null) return spec;
            JSONArray paletteArr = obj.optJSONArray("palette");
            if (paletteArr != null) {
                for (int i = 0; i < paletteArr.length(); i++) {
                    spec.palette.add(PaletteEntry.fromJson(paletteArr.optJSONObject(i)));
                }
            }
            JSONArray rowsArr = obj.optJSONArray("rows");
            if (rowsArr != null) {
                for (int i = 0; i < rowsArr.length(); i++) {
                    spec.rows.add(rowsArr.optString(i, "................"));
                }
            }
            return spec;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            JSONArray paletteArr = new JSONArray();
            for (PaletteEntry p : palette) {
                paletteArr.put(p.toJson());
            }
            JSONArray rowsArr = new JSONArray();
            for (String r : rows) {
                rowsArr.put(r);
            }
            obj.put("palette", paletteArr);
            obj.put("rows", rowsArr);
            return obj;
        }
    }

    public static class ModElement {
        public String id = "";
        public String kind = "item";
        public String displayName = "";
        public String description = "";
        public String statsSummary = "";
        public String craftingRecipe = "";
        public String behaviorSummary = "";
        public String texturePath = "";
        public PixelArtSpec pixelArt = new PixelArtSpec();

        public static ModElement fromJson(JSONObject obj) {
            ModElement el = new ModElement();
            if (obj == null) return el;
            el.id = obj.optString("id", "");
            el.kind = obj.optString("kind", "item");
            el.displayName = obj.optString("displayName", "");
            el.description = obj.optString("description", "");
            el.statsSummary = obj.optString("statsSummary", "");
            el.craftingRecipe = obj.optString("craftingRecipe", "");
            el.behaviorSummary = obj.optString("behaviorSummary", "");
            el.texturePath = obj.optString("texturePath", "");
            el.pixelArt = PixelArtSpec.fromJson(obj.optJSONObject("pixelArt"));
            return el;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("id", id);
            obj.put("kind", kind);
            obj.put("displayName", displayName);
            obj.put("description", description);
            obj.put("statsSummary", statsSummary);
            obj.put("craftingRecipe", craftingRecipe);
            obj.put("behaviorSummary", behaviorSummary);
            obj.put("texturePath", texturePath);
            obj.put("pixelArt", pixelArt.toJson());
            return obj;
        }
    }

    public static class ModFile {
        public String path = "";
        public String language = "json";
        public String description = "";
        public String content = "";

        public ModFile() {}

        public ModFile(String path, String language, String description, String content) {
            this.path = path;
            this.language = language;
            this.description = description;
            this.content = content;
        }

        public static ModFile fromJson(JSONObject obj) {
            ModFile file = new ModFile();
            if (obj != null) {
                file.path = obj.optString("path", "");
                file.language = obj.optString("language", "text");
                file.description = obj.optString("description", "");
                file.content = obj.optString("content", "");
            }
            return file;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("path", path);
            obj.put("language", language);
            obj.put("description", description);
            obj.put("content", content);
            return obj;
        }
    }

    public static class ModMetadata {
        public String name = "Custom Mod";
        public String modId = "custom_mod";
        public String version = "1.0.0";
        public String minecraftVersion = "1.21.4";
        public String edition = "bedrock";
        public String loader = "Bedrock Add-on";
        public String description = "";
        public String author = "Mod Studio User";

        public static ModMetadata fromJson(JSONObject obj) {
            ModMetadata meta = new ModMetadata();
            if (obj != null) {
                meta.name = obj.optString("name", "Custom Mod");
                meta.modId = obj.optString("modId", "custom_mod");
                meta.version = obj.optString("version", "1.0.0");
                meta.minecraftVersion = obj.optString("minecraftVersion", "1.21.4");
                meta.edition = obj.optString("edition", "bedrock");
                meta.loader = obj.optString("loader", "Bedrock Add-on");
                meta.description = obj.optString("description", "");
                meta.author = obj.optString("author", "Mod Studio User");
            }
            return meta;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("name", name);
            obj.put("modId", modId);
            obj.put("version", version);
            obj.put("minecraftVersion", minecraftVersion);
            obj.put("edition", edition);
            obj.put("loader", loader);
            obj.put("description", description);
            obj.put("author", author);
            return obj;
        }
    }

    public static class GeneratedMod {
        public ModMetadata modMeta = new ModMetadata();
        public String summary = "";
        public final List<ModElement> elements = new ArrayList<>();
        public final List<ModFile> files = new ArrayList<>();
        public final List<String> installInstructions = new ArrayList<>();

        public static GeneratedMod fromJson(JSONObject obj) {
            GeneratedMod mod = new GeneratedMod();
            if (obj == null) return mod;
            mod.modMeta = ModMetadata.fromJson(obj.optJSONObject("modMeta"));
            mod.summary = obj.optString("summary", "");
            JSONArray elementsArr = obj.optJSONArray("elements");
            if (elementsArr != null) {
                for (int i = 0; i < elementsArr.length(); i++) {
                    mod.elements.add(ModElement.fromJson(elementsArr.optJSONObject(i)));
                }
            }
            JSONArray filesArr = obj.optJSONArray("files");
            if (filesArr != null) {
                for (int i = 0; i < filesArr.length(); i++) {
                    mod.files.add(ModFile.fromJson(filesArr.optJSONObject(i)));
                }
            }
            JSONArray instructionsArr = obj.optJSONArray("installInstructions");
            if (instructionsArr != null) {
                for (int i = 0; i < instructionsArr.length(); i++) {
                    mod.installInstructions.add(instructionsArr.optString(i, ""));
                }
            }
            return mod;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("modMeta", modMeta.toJson());
            obj.put("summary", summary);
            JSONArray elementsArr = new JSONArray();
            for (ModElement e : elements) elementsArr.put(e.toJson());
            obj.put("elements", elementsArr);
            JSONArray filesArr = new JSONArray();
            for (ModFile f : files) filesArr.put(f.toJson());
            obj.put("files", filesArr);
            JSONArray instArr = new JSONArray();
            for (String s : installInstructions) instArr.put(s);
            obj.put("installInstructions", instArr);
            return obj;
        }
    }

    public static class GenerationStep {
        public String id = "";
        public String label = "";
        public String status = "running"; // pending | running | done

        public GenerationStep() {}

        public GenerationStep(String id, String label, String status) {
            this.id = id;
            this.label = label;
            this.status = status;
        }

        public static GenerationStep fromJson(JSONObject obj) {
            GenerationStep s = new GenerationStep();
            if (obj != null) {
                s.id = obj.optString("id", "");
                s.label = obj.optString("label", "");
                s.status = obj.optString("status", "done");
            }
            return s;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("id", id);
            obj.put("label", label);
            obj.put("status", status);
            return obj;
        }
    }

    public static class ChatAttachment {
        public String id = "";
        public String name = "";
        public String mimeType = "";
        public long size = 0;
        public String base64Data = null;
        public String textSnippet = null;

        public static ChatAttachment fromJson(JSONObject obj) {
            ChatAttachment att = new ChatAttachment();
            if (obj != null) {
                att.id = obj.optString("id", "");
                att.name = obj.optString("name", "");
                att.mimeType = obj.optString("mimeType", "");
                att.size = obj.optLong("size", 0);
                if (!obj.isNull("base64Data")) att.base64Data = obj.optString("base64Data", null);
                if (!obj.isNull("textSnippet")) att.textSnippet = obj.optString("textSnippet", null);
            }
            return att;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("id", id);
            obj.put("name", name);
            obj.put("mimeType", mimeType);
            obj.put("size", size);
            if (base64Data != null) obj.put("base64Data", base64Data);
            if (textSnippet != null) obj.put("textSnippet", textSnippet);
            return obj;
        }
    }

    public static class ChatMessage {
        public String id = "";
        public String role = "user"; // user | assistant
        public String text = "";
        public long createdAt = 0;
        public final List<ChatAttachment> attachments = new ArrayList<>();
        public final List<GenerationStep> steps = new ArrayList<>();
        public GeneratedMod mod = null;
        public String error = null;

        public static ChatMessage fromJson(JSONObject obj) {
            ChatMessage msg = new ChatMessage();
            if (obj == null) return msg;
            msg.id = obj.optString("id", "");
            msg.role = obj.optString("role", "user");
            msg.text = obj.optString("text", "");
            msg.createdAt = obj.optLong("createdAt", System.currentTimeMillis());
            JSONArray attArr = obj.optJSONArray("attachments");
            if (attArr != null) {
                for (int i = 0; i < attArr.length(); i++) {
                    msg.attachments.add(ChatAttachment.fromJson(attArr.optJSONObject(i)));
                }
            }
            JSONArray stepArr = obj.optJSONArray("steps");
            if (stepArr != null) {
                for (int i = 0; i < stepArr.length(); i++) {
                    msg.steps.add(GenerationStep.fromJson(stepArr.optJSONObject(i)));
                }
            }
            if (!obj.isNull("mod") && obj.optJSONObject("mod") != null) {
                msg.mod = GeneratedMod.fromJson(obj.optJSONObject("mod"));
            }
            if (!obj.isNull("error")) {
                msg.error = obj.optString("error", null);
            }
            return msg;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("id", id);
            obj.put("role", role);
            obj.put("text", text);
            obj.put("createdAt", createdAt);
            JSONArray attArr = new JSONArray();
            for (ChatAttachment a : attachments) attArr.put(a.toJson());
            obj.put("attachments", attArr);
            JSONArray stepArr = new JSONArray();
            for (GenerationStep s : steps) stepArr.put(s.toJson());
            obj.put("steps", stepArr);
            if (mod != null) obj.put("mod", mod.toJson());
            if (error != null) obj.put("error", error);
            return obj;
        }
    }

    public static class ChatSession {
        public String id = "";
        public String title = "New Mod Chat";
        public long createdAt = 0;
        public long updatedAt = 0;
        public final List<ChatMessage> messages = new ArrayList<>();

        public static ChatSession fromJson(JSONObject obj) {
            ChatSession s = new ChatSession();
            if (obj == null) return s;
            s.id = obj.optString("id", "");
            s.title = obj.optString("title", "New Mod Chat");
            s.createdAt = obj.optLong("createdAt", System.currentTimeMillis());
            s.updatedAt = obj.optLong("updatedAt", s.createdAt);
            JSONArray msgArr = obj.optJSONArray("messages");
            if (msgArr != null) {
                for (int i = 0; i < msgArr.length(); i++) {
                    s.messages.add(ChatMessage.fromJson(msgArr.optJSONObject(i)));
                }
            }
            return s;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("id", id);
            obj.put("title", title);
            obj.put("createdAt", createdAt);
            obj.put("updatedAt", updatedAt);
            JSONArray msgArr = new JSONArray();
            for (ChatMessage m : messages) msgArr.put(m.toJson());
            obj.put("messages", msgArr);
            return obj;
        }
    }

    public static class SavedProject {
        public String id = "";
        public long savedAt = 0;
        public GeneratedMod mod = new GeneratedMod();

        public static SavedProject fromJson(JSONObject obj) {
            SavedProject p = new SavedProject();
            if (obj == null) return p;
            p.id = obj.optString("id", "");
            p.savedAt = obj.optLong("savedAt", System.currentTimeMillis());
            p.mod = GeneratedMod.fromJson(obj.optJSONObject("mod"));
            return p;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("id", id);
            obj.put("savedAt", savedAt);
            obj.put("mod", mod.toJson());
            return obj;
        }
    }

    public static class AppSettings {
        public String editionPreference = "auto"; // auto | java | bedrock
        public String javaLoader = "Fabric"; // Fabric | Forge | NeoForge
        public String minecraftVersion = "1.21.4";
        public String reasoningDepth = "medium"; // low | medium | high
        public boolean autoSaveProjects = true;
        public boolean includeBedrockScripts = true;
        public String apiBaseUrl = "";

        public static AppSettings fromJson(JSONObject obj) {
            AppSettings s = new AppSettings();
            if (obj == null) return s;
            s.editionPreference = obj.optString("editionPreference", "auto");
            s.javaLoader = obj.optString("javaLoader", "Fabric");
            s.minecraftVersion = obj.optString("minecraftVersion", "1.21.4");
            s.reasoningDepth = obj.optString("reasoningDepth", "medium");
            s.autoSaveProjects = obj.optBoolean("autoSaveProjects", true);
            s.includeBedrockScripts = obj.optBoolean("includeBedrockScripts", true);
            s.apiBaseUrl = obj.optString("apiBaseUrl", "");
            return s;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("editionPreference", editionPreference);
            obj.put("javaLoader", javaLoader);
            obj.put("minecraftVersion", minecraftVersion);
            obj.put("reasoningDepth", reasoningDepth);
            obj.put("autoSaveProjects", autoSaveProjects);
            obj.put("includeBedrockScripts", includeBedrockScripts);
            obj.put("apiBaseUrl", apiBaseUrl);
            return obj;
        }
    }
}
