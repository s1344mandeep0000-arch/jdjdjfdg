package com.minecraftmodstudio.ai;

import com.minecraftmodstudio.ai.model.ModModels.GeneratedMod;
import com.minecraftmodstudio.ai.model.ModModels.ModElement;
import com.minecraftmodstudio.ai.util.ModPackageHelper;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ModPackageHelperTest {

    @Test
    public void normalizeModIdSanitizesAndPrefixesDigits() {
        assertEquals("void_sword_mod", ModPackageHelper.normalizeModId("Void Sword Mod!"));
        assertEquals("mod_123_pack", ModPackageHelper.normalizeModId("123 Pack"));
        assertEquals("custom_mod", ModPackageHelper.normalizeModId("   "));
    }

    @Test
    public void deterministicUuidIsStableAcrossCalls() {
        String a = ModPackageHelper.deterministicUuid("emerald_golem:bp:header");
        String b = ModPackageHelper.deterministicUuid("emerald_golem:bp:header");
        assertEquals(a, b);
        assertEquals(36, a.length());
        assertTrue(a.contains("-"));
    }

    @Test
    public void ensureEditionCompletenessInjectsBedrockManifests() {
        GeneratedMod mod = new GeneratedMod();
        mod.modMeta.name = "Solar Add-on";
        mod.modMeta.modId = "solar_addon";
        mod.modMeta.edition = "bedrock";
        mod.modMeta.loader = "Bedrock Add-on";

        ModElement el = new ModElement();
        el.id = "solar_blade";
        el.kind = "weapon";
        el.displayName = "Solar Blade";
        mod.elements.add(el);

        GeneratedMod completed = ModPackageHelper.ensureEditionCompleteness(mod);
        boolean hasBp = false;
        boolean hasRp = false;
        for (int i = 0; i < completed.files.size(); i++) {
            String path = completed.files.get(i).path;
            if ("behavior_pack/manifest.json".equals(path)) hasBp = true;
            if ("resource_pack/manifest.json".equals(path)) hasRp = true;
        }
        assertTrue(hasBp);
        assertTrue(hasRp);
        assertEquals("solar_addon-bedrock.mcaddon", ModPackageHelper.buildArchiveFileName(completed));
        assertEquals("resource_pack/textures/items/solar_blade.png", completed.elements.get(0).texturePath);
    }

    @Test
    public void ensureEditionCompletenessInjectsJavaFabricAndLang() {
        GeneratedMod mod = new GeneratedMod();
        mod.modMeta.name = "Frost Forge";
        mod.modMeta.modId = "frost_forge";
        mod.modMeta.edition = "java";
        mod.modMeta.loader = "Fabric";

        ModElement el = new ModElement();
        el.id = "frost_ore";
        el.kind = "block";
        el.displayName = "Frost Ore";
        mod.elements.add(el);

        GeneratedMod completed = ModPackageHelper.ensureEditionCompleteness(mod);
        boolean hasFabricJson = false;
        boolean hasLang = false;
        for (int i = 0; i < completed.files.size(); i++) {
            String path = completed.files.get(i).path;
            if (path.endsWith("fabric.mod.json")) hasFabricJson = true;
            if (path.endsWith("en_us.json")) hasLang = true;
        }
        assertTrue(hasFabricJson);
        assertTrue(hasLang);
        assertEquals("frost_forge-fabric.zip", ModPackageHelper.buildArchiveFileName(completed));
    }

    @Test
    public void buildVariationPromptIncludesTweakAndEdition() {
        GeneratedMod mod = new GeneratedMod();
        mod.modMeta.name = "Plasma Kit";
        mod.modMeta.edition = "bedrock";
        mod.modMeta.minecraftVersion = "1.21.4";
        ModElement el = new ModElement();
        el.displayName = "Plasma Blaster";
        mod.elements.add(el);

        String prompt = ModPackageHelper.buildVariationPrompt(mod, "Emerald glow");
        assertTrue(prompt.contains("Plasma Kit"));
        assertTrue(prompt.contains("Emerald glow"));
        assertTrue(prompt.contains("Bedrock"));
        assertFalse(prompt.isEmpty());
    }
}
