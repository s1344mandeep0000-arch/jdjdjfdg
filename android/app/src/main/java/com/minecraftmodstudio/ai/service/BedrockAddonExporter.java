package com.minecraftmodstudio.ai.service;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;

import com.minecraftmodstudio.ai.model.ModModels.GeneratedMod;
import com.minecraftmodstudio.ai.model.ModModels.ModElement;
import com.minecraftmodstudio.ai.model.ModModels.ModFile;
import com.minecraftmodstudio.ai.util.ModPackageHelper;
import com.minecraftmodstudio.ai.util.PixelArtRenderer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Packages generated Minecraft Java (.zip) and Bedrock (.mcaddon) mods with real 16x16 PNG textures,
 * manifests, and source/JSON files, and saves or shares them cleanly across Android 5.0 - 15.
 */
public final class BedrockAddonExporter {

    public static final class ExportResult {
        public final String fileName;
        public final String displayPath;
        public final File shareFile;

        public ExportResult(String fileName, String displayPath, File shareFile) {
            this.fileName = fileName;
            this.displayPath = displayPath;
            this.shareFile = shareFile;
        }
    }

    private BedrockAddonExporter() {}

    public static byte[] buildArchiveBytes(GeneratedMod rawMod) throws IOException {
        GeneratedMod mod = ModPackageHelper.ensureEditionCompleteness(rawMod);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Set<String> addedPaths = new HashSet<>();

        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (ModFile f : mod.files) {
                String cleanPath = sanitizeZipPath(f.path);
                if (cleanPath.isEmpty() || addedPaths.contains(cleanPath)) continue;
                addedPaths.add(cleanPath);
                zos.putNextEntry(new ZipEntry(cleanPath));
                byte[] bytes = (f.content == null ? "" : f.content).getBytes(StandardCharsets.UTF_8);
                zos.write(bytes);
                zos.closeEntry();
            }

            for (ModElement el : mod.elements) {
                String cleanTexPath = sanitizeZipPath(el.texturePath);
                if (cleanTexPath.isEmpty() || addedPaths.contains(cleanTexPath)) continue;
                addedPaths.add(cleanTexPath);
                byte[] pngBytes = PixelArtRenderer.toPngBytes(el.pixelArt);
                zos.putNextEntry(new ZipEntry(cleanTexPath));
                zos.write(pngBytes);
                zos.closeEntry();
            }

            if (!addedPaths.contains("README.md")) {
                StringBuilder readme = new StringBuilder();
                readme.append("# ").append(mod.modMeta.name).append("\n\n");
                readme.append(mod.modMeta.description).append("\n\n");
                readme.append("- Edition: ").append(mod.modMeta.edition).append(" (").append(mod.modMeta.loader).append(")\n");
                readme.append("- Minecraft Version: ").append(mod.modMeta.minecraftVersion).append("\n\n");
                readme.append("## Installation\n");
                for (int i = 0; i < mod.installInstructions.size(); i++) {
                    readme.append(i + 1).append(". ").append(mod.installInstructions.get(i)).append("\n");
                }
                zos.putNextEntry(new ZipEntry("README.md"));
                zos.write(readme.toString().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }

        return baos.toByteArray();
    }

    public static ExportResult exportModToDownloads(Context context, GeneratedMod mod) throws IOException {
        GeneratedMod completed = ModPackageHelper.ensureEditionCompleteness(mod);
        String fileName = ModPackageHelper.buildArchiveFileName(completed);
        byte[] archiveBytes = buildArchiveBytes(completed);
        String mimeType = fileName.endsWith(".mcaddon") ? "application/octet-stream" : "application/zip";
        return saveBytesToDownloadsAndCache(context, fileName, mimeType, archiveBytes);
    }

    public static ExportResult saveBytesToDownloadsAndCache(
            Context context,
            String fileName,
            String mimeType,
            byte[] data
    ) throws IOException {
        File cacheDir = new File(context.getCacheDir(), "exports");
        if (!cacheDir.exists()) cacheDir.mkdirs();
        File shareFile = new File(cacheDir, fileName);
        try (FileOutputStream fos = new FileOutputStream(shareFile)) {
            fos.write(data);
            fos.flush();
        }

        String displayLocation;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = context.getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, mimeType);
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            values.put(MediaStore.Downloads.IS_PENDING, 1);

            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                throw new IOException("Unable to create Downloads entry.");
            }
            try (OutputStream out = resolver.openOutputStream(uri)) {
                if (out == null) throw new IOException("Unable to open Downloads output stream.");
                out.write(data);
                out.flush();
            }
            ContentValues doneValues = new ContentValues();
            doneValues.put(MediaStore.Downloads.IS_PENDING, 0);
            resolver.update(uri, doneValues, null, null);
            displayLocation = "Downloads/" + fileName;
        } else {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) downloadsDir.mkdirs();
            File target = new File(downloadsDir, fileName);
            try (FileOutputStream fos = new FileOutputStream(target)) {
                fos.write(data);
                fos.flush();
                displayLocation = target.getAbsolutePath();
            } catch (IOException legacyErr) {
                File appDownloads = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                if (appDownloads != null) {
                    if (!appDownloads.exists()) appDownloads.mkdirs();
                    File fallback = new File(appDownloads, fileName);
                    try (FileOutputStream fos = new FileOutputStream(fallback)) {
                        fos.write(data);
                        fos.flush();
                    }
                    displayLocation = fallback.getAbsolutePath();
                } else {
                    throw legacyErr;
                }
            }
        }

        return new ExportResult(fileName, displayLocation, shareFile);
    }

    public static void shareFile(Context context, File file, String mimeType, String chooserTitle) {
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intent, chooserTitle));
    }

    private static String sanitizeZipPath(String rawPath) {
        if (rawPath == null) return "";
        String cleaned = rawPath.trim().replace('\\', '/');
        while (cleaned.startsWith("/")) cleaned = cleaned.substring(1);
        cleaned = cleaned.replace("../", "");
        return cleaned;
    }
}
