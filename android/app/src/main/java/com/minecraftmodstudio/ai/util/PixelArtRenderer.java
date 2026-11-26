package com.minecraftmodstudio.ai.util;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.minecraftmodstudio.ai.model.ModModels.PaletteEntry;
import com.minecraftmodstudio.ai.model.ModModels.PixelArtSpec;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Renders AI-generated 16x16 PixelArtSpec grids into crisp Android Bitmaps and RGBA PNG files.
 */
public final class PixelArtRenderer {

    private PixelArtRenderer() {}

    public static Map<Character, Integer> buildPaletteMap(PixelArtSpec spec) {
        Map<Character, Integer> map = new HashMap<>();
        if (spec == null) return map;
        for (PaletteEntry entry : spec.palette) {
            if (entry.key != null && !entry.key.isEmpty()) {
                char keyChar = entry.key.charAt(0);
                if (keyChar != '.') {
                    map.put(keyChar, parseHexColor(entry.hex));
                }
            }
        }
        return map;
    }

    public static int parseHexColor(String hex) {
        if (hex == null) return Color.rgb(130, 140, 160);
        String clean = hex.trim();
        if (!clean.startsWith("#")) clean = "#" + clean;
        try {
            return Color.parseColor(clean);
        } catch (IllegalArgumentException ignored) {
            return Color.rgb(130, 140, 160);
        }
    }

    /**
     * Renders a native 16x16 ARGB_8888 Bitmap with no anti-aliasing or blur.
     */
    public static Bitmap toNativeBitmap(PixelArtSpec spec) {
        Bitmap bmp = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888);
        Map<Character, Integer> palette = buildPaletteMap(spec);
        for (int y = 0; y < 16; y++) {
            String row = (spec != null && y < spec.rows.size()) ? spec.rows.get(y) : "";
            for (int x = 0; x < 16; x++) {
                char ch = (row != null && x < row.length()) ? row.charAt(x) : '.';
                if (ch == '.' || ch == ' ') {
                    bmp.setPixel(x, y, Color.TRANSPARENT);
                } else {
                    Integer color = palette.get(ch);
                    bmp.setPixel(x, y, color != null ? color : Color.rgb(100, 116, 139));
                }
            }
        }
        return bmp;
    }

    /**
     * Scales the 16x16 bitmap to a crisp UI display size using nearest-neighbor sampling.
     */
    public static Bitmap toDisplayBitmap(PixelArtSpec spec, int targetPx) {
        Bitmap nativeBmp = toNativeBitmap(spec);
        if (targetPx <= 16) return nativeBmp;
        Bitmap scaled = Bitmap.createScaledBitmap(nativeBmp, targetPx, targetPx, false);
        nativeBmp.recycle();
        return scaled;
    }

    /**
     * Compresses the native 16x16 pixel-art texture as an RGBA PNG byte array for .mcaddon / .zip export.
     */
    public static byte[] toPngBytes(PixelArtSpec spec) {
        Bitmap bmp = toNativeBitmap(spec);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
        bmp.recycle();
        return baos.toByteArray();
    }
}
