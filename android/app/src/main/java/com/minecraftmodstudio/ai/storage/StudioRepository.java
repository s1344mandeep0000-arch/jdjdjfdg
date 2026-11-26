package com.minecraftmodstudio.ai.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.minecraftmodstudio.ai.model.ModModels.AppSettings;
import com.minecraftmodstudio.ai.model.ModModels.ChatSession;
import com.minecraftmodstudio.ai.model.ModModels.GeneratedMod;
import com.minecraftmodstudio.ai.model.ModModels.SavedProject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Local persistent storage backed by SharedPreferences for chat history, saved mods, and settings.
 * Also keeps a lightweight cache for passing large GeneratedMod objects to ModPreviewActivity
 * without hitting Android's Binder TransactionTooLargeException limit.
 */
public final class StudioRepository {

    private static final String PREFS_NAME = "mc_mod_studio_prefs";
    private static final String KEY_SESSIONS = "sessions_json";
    private static final String KEY_PROJECTS = "projects_json";
    private static final String KEY_SETTINGS = "settings_json";
    private static final String KEY_PREVIEW_MOD = "preview_mod_json";

    private final SharedPreferences prefs;

    public StudioRepository(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public List<ChatSession> loadSessions() {
        List<ChatSession> list = new ArrayList<>();
        String raw = prefs.getString(KEY_SESSIONS, null);
        if (raw == null || raw.isEmpty()) return list;
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                list.add(ChatSession.fromJson(arr.optJSONObject(i)));
            }
        } catch (JSONException ignored) {
        }
        return list;
    }

    public void saveSessions(List<ChatSession> sessions) {
        JSONArray arr = new JSONArray();
        for (ChatSession s : sessions) {
            try {
                arr.put(s.toJson());
            } catch (JSONException ignored) {
            }
        }
        prefs.edit().putString(KEY_SESSIONS, arr.toString()).apply();
    }

    public List<SavedProject> loadProjects() {
        List<SavedProject> list = new ArrayList<>();
        String raw = prefs.getString(KEY_PROJECTS, null);
        if (raw == null || raw.isEmpty()) return list;
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                list.add(SavedProject.fromJson(arr.optJSONObject(i)));
            }
        } catch (JSONException ignored) {
        }
        return list;
    }

    public void saveProjects(List<SavedProject> projects) {
        JSONArray arr = new JSONArray();
        for (SavedProject p : projects) {
            try {
                arr.put(p.toJson());
            } catch (JSONException ignored) {
            }
        }
        prefs.edit().putString(KEY_PROJECTS, arr.toString()).apply();
    }

    public AppSettings loadSettings() {
        String raw = prefs.getString(KEY_SETTINGS, null);
        if (raw == null || raw.isEmpty()) return new AppSettings();
        try {
            return AppSettings.fromJson(new JSONObject(raw));
        } catch (JSONException ignored) {
            return new AppSettings();
        }
    }

    public void saveSettings(AppSettings settings) {
        try {
            prefs.edit().putString(KEY_SETTINGS, settings.toJson().toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    public void savePreviewMod(GeneratedMod mod) {
        if (mod == null) return;
        try {
            prefs.edit().putString(KEY_PREVIEW_MOD, mod.toJson().toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    public GeneratedMod loadPreviewMod() {
        String raw = prefs.getString(KEY_PREVIEW_MOD, null);
        if (raw == null || raw.isEmpty()) return null;
        try {
            return GeneratedMod.fromJson(new JSONObject(raw));
        } catch (JSONException ignored) {
            return null;
        }
    }
}
