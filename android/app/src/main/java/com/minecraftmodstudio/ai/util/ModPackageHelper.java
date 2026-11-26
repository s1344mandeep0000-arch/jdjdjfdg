package com.minecraftmodstudio.ai.util;

import com.minecraftmodstudio.ai.model.ModModels.GeneratedMod;
import com.minecraftmodstudio.ai.model.ModModels.ModElement;
import com.minecraftmodstudio.ai.model.ModModels.ModFile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Pure Java package normalization, UUID generation, edition manifest completeness checks,
 * and variation prompt construction. Kept free of Android framework dependencies so unit tests
 * run directly on the JVM.
 */
public final class ModPackageHelper {

    private ModPackageHelper() {}

    public static String normalizeModId(String input) {
        if (input == null) return "custom_mod";
        String cleaned = input.toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (cleaned.isEmpty()) return "custom_mod";
        if (Character.isDigit(cleaned.charAt(0))) {
            cleaned = "mod_" + cleaned;
        }
        return cleaned.length() > 32 ? cleaned.substring(0, 32) : cleaned;
    }

    public static String deterministicUuid(String seed) {
        if (seed == null) seed = "";
        int h1 = 0x811c9dc5;
        int h2 = 0x9e3779b9;
        for (int i = 0; i < seed.length(); i++) {
            int ch = seed.charAt(i);
            h1 = (h1 ^ ch) * 0x01000193;
            h2 = (h2 ^ (ch + i)) * 0x85ebca6b;
        }
        String p1 = padHex8(h1);
        String p2 = padHex8(h2).substring(0, 4);
        String p3 = "4" + padHex8(h1 ^ h2).substring(1, 4);
        String p4 = "a" + padHex8((h1 >>> 3) ^ h2).substring(1, 4);
        String p5 = (padHex8(h1 ^ 0x12345678) + padHex8(h2 ^ 0x87654321)).substring(0, 12);
        return p1 + "-" + p2 + "-" + p3 + "-" + p4 + "-" + p5;
    }

    private static String padHex8(int value) {
        String hex = Long.toHexString(value & 0xffffffffL);
        StringBuilder sb = new StringBuilder(8);
        for (int i = hex.length(); i < 8; i++) {
            sb.append('0');
        }
        sb.append(hex);
        return sb.toString();
    }

    private static boolean hasFileEndingWith(GeneratedMod mod, String suffix) {
        String lowerSuffix = suffix.toLowerCase(Locale.ROOT);
        for (ModFile f : mod.files) {
            if (f.path != null && f.path.toLowerCase(Locale.ROOT).endsWith(lowerSuffix)) {
                return true;
            }
        }
        return false;
    }

    public static GeneratedMod ensureEditionCompleteness(GeneratedMod mod) {
        if (mod == null) mod = new GeneratedMod();
        String rawId = (mod.modMeta.modId != null && !mod.modMeta.modId.trim().isEmpty())
                ? mod.modMeta.modId
                : mod.modMeta.name;
        String modId = normalizeModId(rawId);
        mod.modMeta.modId = modId;

        if ("bedrock".equalsIgnoreCase(mod.modMeta.edition)) {
            mod.modMeta.edition = "bedrock";
            String bpHeaderUuid = deterministicUuid(modId + ":bp:header");
            String bpModuleUuid = deterministicUuid(modId + ":bp:module");
            String rpHeaderUuid = deterministicUuid(modId + ":rp:header");
            String rpModuleUuid = deterministicUuid(modId + ":rp:module");

            if (!hasFileEndingWith(mod, "behavior_pack/manifest.json")) {
                try {
                    JSONObject manifest = new JSONObject();
                    manifest.put("format_version", 2);

                    JSONObject header = new JSONObject();
                    header.put("name", mod.modMeta.name + " BP");
                    header.put("description", mod.modMeta.description);
                    header.put("uuid", bpHeaderUuid);
                    header.put("version", new JSONArray("[1,0,0]"));
                    header.put("min_engine_version", new JSONArray("[1,21,0]"));
                    manifest.put("header", header);

                    JSONArray modules = new JSONArray();
                    JSONObject dataMod = new JSONObject();
                    dataMod.put("type", "data");
                    dataMod.put("uuid", bpModuleUuid);
                    dataMod.put("version", new JSONArray("[1,0,0]"));
                    modules.put(dataMod);
                    manifest.put("modules", modules);

                    JSONArray deps = new JSONArray();
                    JSONObject rpDep = new JSONObject();
                    rpDep.put("uuid", rpHeaderUuid);
                    rpDep.put("version", new JSONArray("[1,0,0]"));
                    deps.put(rpDep);
                    manifest.put("dependencies", deps);

                    mod.files.add(0, new ModFile(
                            "behavior_pack/manifest.json",
                            "json",
                            "Bedrock Behavior Pack manifest linking Resource Pack and Script APIs.",
                            manifest.toString(2)
                    ));
                } catch (JSONException ignored) {
                }
            }

            if (!hasFileEndingWith(mod, "resource_pack/manifest.json")) {
                try {
                    JSONObject manifest = new JSONObject();
                    manifest.put("format_version", 2);

                    JSONObject header = new JSONObject();
                    header.put("name", mod.modMeta.name + " RP");
                    header.put("description", mod.modMeta.description);
                    header.put("uuid", rpHeaderUuid);
                    header.put("version", new JSONArray("[1,0,0]"));
                    header.put("min_engine_version", new JSONArray("[1,21,0]"));
                    manifest.put("header", header);

                    JSONArray modules = new JSONArray();
                    JSONObject resMod = new JSONObject();
                    resMod.put("type", "resources");
                    resMod.put("uuid", rpModuleUuid);
                    resMod.put("version", new JSONArray("[1,0,0]"));
                    modules.put(resMod);
                    manifest.put("modules", modules);

                    int insertIdx = Math.min(1, mod.files.size());
                    mod.files.add(insertIdx, new ModFile(
                            "resource_pack/manifest.json",
                            "json",
                            "Bedrock Resource Pack manifest for textures, models, and localization.",
                            manifest.toString(2)
                    ));
                } catch (JSONException ignored) {
                }
            }
        } else {
            mod.modMeta.edition = "java";
            String loader = mod.modMeta.loader != null ? mod.modMeta.loader : "Fabric";
            if ("Fabric".equalsIgnoreCase(loader) && !hasFileEndingWith(mod, "fabric.mod.json")) {
                try {
                    JSONObject fabric = new JSONObject();
                    fabric.put("schemaVersion", 1);
                    fabric.put("id", modId);
                    fabric.put("version", mod.modMeta.version.isEmpty() ? "1.0.0" : mod.modMeta.version);
                    fabric.put("name", mod.modMeta.name);
                    fabric.put("description", mod.modMeta.description);
                    JSONArray authors = new JSONArray();
                    authors.put(mod.modMeta.author.isEmpty() ? "Mod Studio User" : mod.modMeta.author);
                    fabric.put("authors", authors);
                    fabric.put("license", "MIT");
                    fabric.put("environment", "*");

                    JSONObject entrypoints = new JSONObject();
                    JSONArray mainEp = new JSONArray();
                    mainEp.put("com.modstudio." + modId + ".ModMain");
                    entrypoints.put("main", mainEp);
                    fabric.put("entrypoints", entrypoints);

                    JSONObject depends = new JSONObject();
                    depends.put("fabricloader", ">=0.16.0");
                    depends.put("minecraft", "~" + (mod.modMeta.minecraftVersion.isEmpty() ? "1.21.4" : mod.modMeta.minecraftVersion));
                    depends.put("java", ">=21");
                    depends.put("fabric-api", "*");
                    fabric.put("depends", depends);

                    mod.files.add(0, new ModFile(
                            "src/main/resources/fabric.mod.json",
                            "json",
                            "Fabric mod metadata and entrypoint declaration.",
                            fabric.toString(2)
                    ));
                } catch (JSONException ignored) {
                }
            } else if (("Forge".equalsIgnoreCase(loader) || "NeoForge".equalsIgnoreCase(loader))
                    && !hasFileEndingWith(mod, "mods.toml")
                    && !hasFileEndingWith(mod, "neoforge.mods.toml")) {
                String tomlPath = "NeoForge".equalsIgnoreCase(loader)
                        ? "src/main/resources/META-INF/neoforge.mods.toml"
                        : "src/main/resources/META-INF/mods.toml";
                String ver = mod.modMeta.version.isEmpty() ? "1.0.0" : mod.modMeta.version;
                String tomlContent = "modLoader=\"javafml\"\n"
                        + "loaderVersion=\"[4,)\"\n"
                        + "license=\"MIT\"\n\n"
                        + "[[mods]]\n"
                        + "modId=\"" + modId + "\"\n"
                        + "version=\"" + ver + "\"\n"
                        + "displayName=\"" + mod.modMeta.name + "\"\n"
                        + "description='''" + mod.modMeta.description + "'''\n";
                mod.files.add(0, new ModFile(tomlPath, "toml", loader + " mod descriptor file.", tomlContent));
            }

            if (!hasFileEndingWith(mod, "en_us.json")) {
                try {
                    JSONObject lang = new JSONObject();
                    for (ModElement el : mod.elements) {
                        String keyPrefix = "block".equals(el.kind) ? "block"
                                : "mob".equals(el.kind) ? "entity" : "item";
                        String elId = normalizeModId(el.id != null && !el.id.isEmpty() ? el.id : el.displayName);
                        lang.put(keyPrefix + "." + modId + "." + elId, el.displayName);
                    }
                    mod.files.add(new ModFile(
                            "src/main/resources/assets/" + modId + "/lang/en_us.json",
                            "json",
                            "English localization strings for all generated blocks, items, and mobs.",
                            lang.toString(2)
                    ));
                } catch (JSONException ignored) {
                }
            }
        }

        for (ModElement el : mod.elements) {
            String cleanId = normalizeModId(el.id != null && !el.id.isEmpty() ? el.id : el.displayName);
            el.id = cleanId;
            if (el.texturePath == null || el.texturePath.trim().isEmpty()) {
                if ("bedrock".equals(mod.modMeta.edition)) {
                    String folder = "block".equals(el.kind) ? "blocks"
                            : "mob".equals(el.kind) ? "entity" : "items";
                    el.texturePath = "resource_pack/textures/" + folder + "/" + cleanId + ".png";
                } else {
                    String folder = "block".equals(el.kind) ? "block"
                            : "mob".equals(el.kind) ? "entity" : "item";
                    el.texturePath = "src/main/resources/assets/" + modId + "/textures/" + folder + "/" + cleanId + ".png";
                }
            }
        }

        return mod;
    }

    public static String buildArchiveFileName(GeneratedMod mod) {
        String modId = normalizeModId(mod.modMeta.modId != null && !mod.modMeta.modId.isEmpty()
                ? mod.modMeta.modId
                : mod.modMeta.name);
        if ("bedrock".equalsIgnoreCase(mod.modMeta.edition)) {
            return modId + "-bedrock.mcaddon";
        }
        String loaderSlug = (mod.modMeta.loader == null ? "fabric" : mod.modMeta.loader)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "");
        if (loaderSlug.isEmpty()) loaderSlug = "java";
        return modId + "-" + loaderSlug + ".zip";
    }

    public static String buildVariationPrompt(GeneratedMod mod, String customTweak) {
        String editionLabel = "bedrock".equalsIgnoreCase(mod.modMeta.edition)
                ? "Minecraft Bedrock Edition Add-on"
                : "Minecraft Java Edition (" + mod.modMeta.loader + ")";

        StringBuilder names = new StringBuilder();
        for (int i = 0; i < mod.elements.size(); i++) {
            if (i > 0) names.append(", ");
            names.append(mod.elements.get(i).displayName);
        }

        String tweak = customTweak == null ? "" : customTweak.trim();
        if (!tweak.isEmpty()) {
            return "Create a secondary variation of the \"" + mod.modMeta.name + "\" (" + editionLabel
                    + ", MC " + mod.modMeta.minecraftVersion + ") mod featuring [" + names + "]. "
                    + "Apply these specific tweaks: " + tweak + ". "
                    + "Give the variation a distinct modId, updated 16x16 pixel-art textures, and updated behaviors and files.";
        }

        return "Create a secondary variation of the \"" + mod.modMeta.name + "\" (" + editionLabel
                + ", MC " + mod.modMeta.minecraftVersion + ") mod featuring [" + names + "]. "
                + "Give it an alternate color palette, refreshed 16x16 pixel-art textures, tweaked item/mob behaviors, and a distinct modId and name.";
    }

    public static String resolveApiUrl(String configuredBaseUrl, String defaultBaseUrl, String endpointPath) {
        String path = endpointPath.startsWith("/") ? endpointPath : "/" + endpointPath;
        String base = (configuredBaseUrl != null && !configuredBaseUrl.trim().isEmpty())
                ? configuredBaseUrl.trim()
                : (defaultBaseUrl != null ? defaultBaseUrl.trim() : "");
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.isEmpty()) {
            return path;
        }
        return base + path;
    }
}
