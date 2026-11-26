package com.minecraftmodstudio.ai;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.minecraftmodstudio.ai.model.ModModels.GeneratedMod;
import com.minecraftmodstudio.ai.model.ModModels.ModElement;
import com.minecraftmodstudio.ai.model.ModModels.ModFile;
import com.minecraftmodstudio.ai.service.BedrockAddonExporter;
import com.minecraftmodstudio.ai.storage.StudioRepository;
import com.minecraftmodstudio.ai.util.ModPackageHelper;
import com.minecraftmodstudio.ai.util.PixelArtRenderer;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Pure Java Mod Preview Activity displaying generated Minecraft elements & 16x16 pixel textures,
 * complete source/JSON files, installation steps, and direct .mcaddon / .zip export.
 */
public class ModPreviewActivity extends AppCompatActivity {

    private GeneratedMod mod;
    private int selectedFileIndex = 0;

    private TextView tabElements;
    private TextView tabFiles;
    private TextView tabInstall;
    private ScrollView scrollElements;
    private LinearLayout containerFiles;
    private ScrollView scrollInstall;

    private LinearLayout previewElementsList;
    private LinearLayout previewFileChips;
    private TextView tvSelectedFilePath;
    private TextView tvSelectedFileContent;
    private LinearLayout previewInstallList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mod_preview);

        StudioRepository repository = new StudioRepository(this);
        mod = repository.loadPreviewMod();
        if (mod == null) {
            Toast.makeText(this, "No mod selected for preview.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        mod = ModPackageHelper.ensureEditionCompleteness(mod);

        bindViews();
        populateHeader();
        populateElements();
        populateFiles();
        populateInstallGuide();
        selectTab(0);
    }

    private void bindViews() {
        findViewById(R.id.btnPreviewBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnPreviewShare).setOnClickListener(v -> shareModArchive());
        findViewById(R.id.btnPreviewExport).setOnClickListener(v -> exportModArchive());

        tabElements = findViewById(R.id.tabPreviewElements);
        tabFiles = findViewById(R.id.tabPreviewFiles);
        tabInstall = findViewById(R.id.tabPreviewInstall);
        scrollElements = findViewById(R.id.scrollPreviewElements);
        containerFiles = findViewById(R.id.containerPreviewFiles);
        scrollInstall = findViewById(R.id.scrollPreviewInstall);

        previewElementsList = findViewById(R.id.previewElementsList);
        previewFileChips = findViewById(R.id.previewFileChips);
        tvSelectedFilePath = findViewById(R.id.tvSelectedFilePath);
        tvSelectedFileContent = findViewById(R.id.tvSelectedFileContent);
        previewInstallList = findViewById(R.id.previewInstallList);

        tabElements.setOnClickListener(v -> selectTab(0));
        tabFiles.setOnClickListener(v -> selectTab(1));
        tabInstall.setOnClickListener(v -> selectTab(2));

        findViewById(R.id.btnCopyFile).setOnClickListener(v -> copySelectedFile());
        findViewById(R.id.btnSaveSingleFile).setOnClickListener(v -> saveSelectedFile());
    }

    private void populateHeader() {
        TextView tvName = findViewById(R.id.tvPreviewModName);
        TextView tvSub = findViewById(R.id.tvPreviewModSubtitle);
        TextView tvExportLabel = findViewById(R.id.tvPreviewExportLabel);

        tvName.setText(mod.modMeta.name);
        tvSub.setText(mod.modMeta.loader + " • MC " + mod.modMeta.minecraftVersion + " • " + mod.modMeta.modId);
        tvExportLabel.setText("bedrock".equalsIgnoreCase(mod.modMeta.edition) ? R.string.export_addon : R.string.export_zip);
    }

    private void selectTab(int index) {
        scrollElements.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        containerFiles.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        scrollInstall.setVisibility(index == 2 ? View.VISIBLE : View.GONE);

        styleTab(tabElements, index == 0);
        styleTab(tabFiles, index == 1);
        styleTab(tabInstall, index == 2);
    }

    private void styleTab(TextView tab, boolean active) {
        if (active) {
            tab.setBackgroundResource(R.drawable.bg_outline_button);
            tab.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        } else {
            tab.setBackground(null);
            tab.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        }
    }

    private void populateElements() {
        previewElementsList.removeAllViews();
        int texPx = (int) (56 * getResources().getDisplayMetrics().density);

        for (ModElement el : mod.elements) {
            LinearLayout card = new LinearLayout(this);
            card.setBackgroundResource(R.drawable.bg_mod_card);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(36, 32, 36, 32);
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardLp.bottomMargin = 24;
            card.setLayoutParams(cardLp);

            LinearLayout headerRow = new LinearLayout(this);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

            ImageView iv = new ImageView(this);
            LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(texPx, texPx);
            ivLp.setMarginEnd(28);
            iv.setLayoutParams(ivLp);
            iv.setBackgroundResource(R.drawable.bg_element_box);
            iv.setPadding(10, 10, 10, 10);
            Bitmap bmp = PixelArtRenderer.toDisplayBitmap(el.pixelArt, texPx);
            iv.setImageBitmap(bmp);
            headerRow.addView(iv);

            LinearLayout titleCol = new LinearLayout(this);
            titleCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            titleCol.setLayoutParams(titleLp);

            TextView tvTitle = new TextView(this);
            tvTitle.setText(el.displayName + " (" + el.kind.toUpperCase(Locale.ROOT) + ")");
            tvTitle.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            tvTitle.setTextSize(15f);
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            titleCol.addView(tvTitle);

            TextView tvPath = new TextView(this);
            tvPath.setText(el.texturePath);
            tvPath.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            tvPath.setTextSize(11f);
            titleCol.addView(tvPath);

            headerRow.addView(titleCol);

            TextView btnSavePng = new TextView(this);
            btnSavePng.setBackgroundResource(R.drawable.bg_outline_button);
            btnSavePng.setPadding(24, 14, 24, 14);
            btnSavePng.setText(R.string.save_png);
            btnSavePng.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            btnSavePng.setTextSize(11f);
            btnSavePng.setTypeface(null, android.graphics.Typeface.BOLD);
            btnSavePng.setOnClickListener(v -> saveElementTexturePng(el));
            headerRow.addView(btnSavePng);

            card.addView(headerRow);

            addDetailLine(card, "Description", el.description);
            addDetailLine(card, "Stats", el.statsSummary);
            addDetailLine(card, "Recipe / Obtain", el.craftingRecipe);
            addDetailLine(card, "Behavior", el.behaviorSummary);

            previewElementsList.addView(card);
        }
    }

    private void addDetailLine(LinearLayout parent, String label, String value) {
        if (value == null || value.trim().isEmpty()) return;
        TextView tv = new TextView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 14;
        tv.setLayoutParams(lp);
        tv.setText(label + ": " + value);
        tv.setTextColor(ContextCompat.getColor(this, R.color.text_body));
        tv.setTextSize(13f);
        parent.addView(tv);
    }

    private void populateFiles() {
        previewFileChips.removeAllViews();
        if (mod.files.isEmpty()) {
            tvSelectedFilePath.setText("No generated files");
            tvSelectedFileContent.setText("");
            return;
        }
        if (selectedFileIndex >= mod.files.size()) selectedFileIndex = 0;

        for (int i = 0; i < mod.files.size(); i++) {
            final int idx = i;
            ModFile file = mod.files.get(i);
            String shortName = file.path;
            int slash = shortName.lastIndexOf('/');
            if (slash >= 0 && slash < shortName.length() - 1) {
                shortName = shortName.substring(slash + 1);
            }

            TextView chip = new TextView(this);
            chip.setBackgroundResource(idx == selectedFileIndex
                    ? R.drawable.bg_variation_button_active
                    : R.drawable.bg_chip);
            chip.setPadding(24, 14, 24, 14);
            chip.setText(shortName);
            chip.setTextColor(ContextCompat.getColor(this,
                    idx == selectedFileIndex ? R.color.primary_blue_dark : R.color.text_body));
            chip.setTextSize(12f);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(14);
            chip.setLayoutParams(lp);
            chip.setOnClickListener(v -> {
                selectedFileIndex = idx;
                populateFiles();
            });
            previewFileChips.addView(chip);
        }

        ModFile current = mod.files.get(selectedFileIndex);
        tvSelectedFilePath.setText(current.path);
        tvSelectedFileContent.setText(current.content != null ? current.content : "");
    }

    private void populateInstallGuide() {
        previewInstallList.removeAllViews();
        for (int i = 0; i < mod.installInstructions.size(); i++) {
            TextView step = new TextView(this);
            step.setBackgroundResource(R.drawable.bg_element_box);
            step.setPadding(32, 26, 32, 26);
            step.setText((i + 1) + ". " + mod.installInstructions.get(i));
            step.setTextColor(ContextCompat.getColor(this, R.color.text_body));
            step.setTextSize(14f);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = 16;
            step.setLayoutParams(lp);
            previewInstallList.addView(step);
        }
    }

    private void saveElementTexturePng(ModElement el) {
        try {
            byte[] pngBytes = PixelArtRenderer.toPngBytes(el.pixelArt);
            String fileName = ModPackageHelper.normalizeModId(el.id) + ".png";
            BedrockAddonExporter.ExportResult res = BedrockAddonExporter.saveBytesToDownloadsAndCache(
                    this, fileName, "image/png", pngBytes);
            Toast.makeText(this, "Saved texture to " + res.displayPath, Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
            Toast.makeText(this, "Failed to save PNG: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void copySelectedFile() {
        if (mod.files.isEmpty()) return;
        ModFile current = mod.files.get(selectedFileIndex);
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText(current.path, current.content));
            Toast.makeText(this, "Copied " + current.path, Toast.LENGTH_SHORT).show();
        }
    }

    private void saveSelectedFile() {
        if (mod.files.isEmpty()) return;
        ModFile current = mod.files.get(selectedFileIndex);
        String baseName = current.path;
        int slash = baseName.lastIndexOf('/');
        if (slash >= 0 && slash < baseName.length() - 1) {
            baseName = baseName.substring(slash + 1);
        }
        try {
            byte[] bytes = (current.content == null ? "" : current.content).getBytes(StandardCharsets.UTF_8);
            BedrockAddonExporter.ExportResult res = BedrockAddonExporter.saveBytesToDownloadsAndCache(
                    this, baseName, "text/plain", bytes);
            Toast.makeText(this, "Saved to " + res.displayPath, Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
            Toast.makeText(this, "Failed to save file: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void exportModArchive() {
        try {
            BedrockAddonExporter.ExportResult res = BedrockAddonExporter.exportModToDownloads(this, mod);
            String mimeType = res.fileName.endsWith(".mcaddon") ? "application/octet-stream" : "application/zip";
            new AlertDialog.Builder(this)
                    .setTitle("Export Complete")
                    .setMessage("Saved " + res.fileName + " to " + res.displayPath)
                    .setPositiveButton("Share File", (d, w) ->
                            BedrockAddonExporter.shareFile(this, res.shareFile, mimeType, "Share " + res.fileName))
                    .setNegativeButton(R.string.done, null)
                    .show();
        } catch (Exception ex) {
            Toast.makeText(this, "Export failed: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void shareModArchive() {
        try {
            BedrockAddonExporter.ExportResult res = BedrockAddonExporter.exportModToDownloads(this, mod);
            String mimeType = res.fileName.endsWith(".mcaddon") ? "application/octet-stream" : "application/zip";
            BedrockAddonExporter.shareFile(this, res.shareFile, mimeType, "Share " + res.fileName);
        } catch (Exception ex) {
            Toast.makeText(this, "Share failed: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
