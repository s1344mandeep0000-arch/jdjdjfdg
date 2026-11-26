package com.minecraftmodstudio.ai.service;

import android.os.Handler;
import android.os.Looper;

import com.minecraftmodstudio.ai.BuildConfig;
import com.minecraftmodstudio.ai.model.ModModels.AppSettings;
import com.minecraftmodstudio.ai.model.ModModels.ChatAttachment;
import com.minecraftmodstudio.ai.model.ModModels.ChatMessage;
import com.minecraftmodstudio.ai.model.ModModels.GeneratedMod;
import com.minecraftmodstudio.ai.util.ModPackageHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Pure Java SSE client that connects to the Minecraft AI Mod Maker Studio backend
 * (/api/mods/generate) via HttpURLConnection and streams real-time progress steps
 * plus the completed GeneratedMod payload back to the main thread.
 */
public final class ModGeneratorClient {

    public interface GenerationCallback {
        void onStep(String stepId, String label);
        void onComplete(GeneratedMod mod);
        void onError(String errorMessage);
    }

    public static final class RequestHandle {
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private volatile HttpURLConnection connection;

        public void cancel() {
            cancelled.set(true);
            HttpURLConnection conn = connection;
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception ignored) {
                }
            }
        }

        public boolean isCancelled() {
            return cancelled.get();
        }
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public RequestHandle generateMod(
            List<ChatMessage> conversationHistory,
            List<ChatAttachment> attachments,
            AppSettings settings,
            GenerationCallback callback
    ) {
        final RequestHandle handle = new RequestHandle();
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                String endpoint = ModPackageHelper.resolveApiUrl(
                        settings != null ? settings.apiBaseUrl : "",
                        BuildConfig.DEFAULT_API_BASE_URL,
                        "/api/mods/generate"
                );

                JSONObject payload = new JSONObject();
                JSONArray msgArray = new JSONArray();
                for (ChatMessage m : conversationHistory) {
                    if (m.text != null && !m.text.trim().isEmpty()) {
                        JSONObject item = new JSONObject();
                        item.put("role", "assistant".equals(m.role) ? "assistant" : "user");
                        item.put("text", m.text);
                        msgArray.put(item);
                    }
                }
                payload.put("messages", msgArray);

                JSONArray attArray = new JSONArray();
                if (attachments != null) {
                    for (ChatAttachment att : attachments) {
                        attArray.put(att.toJson());
                    }
                }
                payload.put("attachments", attArray);
                payload.put("settings", settings != null ? settings.toJson() : new AppSettings().toJson());

                URL url = new URL(endpoint);
                conn = (HttpURLConnection) url.openConnection();
                handle.connection = conn;
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(20000);
                conn.setReadTimeout(180000);
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "text/event-stream");

                byte[] bodyBytes = payload.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(bodyBytes);
                    os.flush();
                }

                int statusCode = conn.getResponseCode();
                if (statusCode < 200 || statusCode >= 300) {
                    String errBody = readStreamText(conn.getErrorStream());
                    String message = "Server returned HTTP " + statusCode
                            + (errBody.isEmpty() ? "" : ": " + errBody);
                    throw new IllegalStateException(message);
                }

                boolean receivedComplete = false;
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String currentEvent = "message";
                    StringBuilder dataBuilder = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        if (handle.isCancelled()) {
                            return;
                        }
                        if (line.isEmpty()) {
                            if (dataBuilder.length() > 0) {
                                String rawData = dataBuilder.toString().trim();
                                dataBuilder.setLength(0);
                                boolean done = dispatchSseEvent(currentEvent, rawData, handle, callback);
                                if (done) {
                                    receivedComplete = true;
                                    break;
                                }
                            }
                            currentEvent = "message";
                            continue;
                        }
                        if (line.startsWith("event:")) {
                            currentEvent = line.substring(6).trim();
                        } else if (line.startsWith("data:")) {
                            if (dataBuilder.length() > 0) dataBuilder.append('\n');
                            dataBuilder.append(line.substring(5).trim());
                        }
                    }

                    if (!receivedComplete && !handle.isCancelled() && dataBuilder.length() > 0) {
                        receivedComplete = dispatchSseEvent(currentEvent, dataBuilder.toString().trim(), handle, callback);
                    }
                }

                if (!receivedComplete && !handle.isCancelled()) {
                    postError(callback, "Stream ended before mod generation finished. Please try again.");
                }
            } catch (Exception ex) {
                if (handle.isCancelled()) return;
                String msg = ex.getMessage();
                if (msg == null || msg.trim().isEmpty()) {
                    msg = "Unable to connect to the AI Mod Studio server. Check your internet connection or Cloud Backend URL in Settings.";
                }
                postError(callback, msg);
            } finally {
                if (conn != null) {
                    try {
                        conn.disconnect();
                    } catch (Exception ignored) {
                    }
                }
            }
        });
        return handle;
    }

    private boolean dispatchSseEvent(
            String eventName,
            String rawJson,
            RequestHandle handle,
            GenerationCallback callback
    ) {
        if (handle.isCancelled() || rawJson.isEmpty()) return false;
        try {
            JSONObject data = new JSONObject(rawJson);
            if ("step".equals(eventName)) {
                final String stepId = data.optString("stepId", "step");
                final String label = data.optString("label", "Generating mod...");
                mainHandler.post(() -> {
                    if (!handle.isCancelled()) callback.onStep(stepId, label);
                });
                return false;
            } else if ("complete".equals(eventName)) {
                JSONObject modObj = data.optJSONObject("mod");
                final GeneratedMod mod = ModPackageHelper.ensureEditionCompleteness(GeneratedMod.fromJson(modObj));
                mainHandler.post(() -> {
                    if (!handle.isCancelled()) callback.onComplete(mod);
                });
                return true;
            } else if ("error".equals(eventName)) {
                final String errMsg = data.optString("message", "AI failed to generate mod.");
                postError(callback, errMsg);
                return true;
            }
        } catch (Exception ex) {
            postError(callback, "Failed to parse AI server response: " + ex.getMessage());
            return true;
        }
        return false;
    }

    private void postError(GenerationCallback callback, String message) {
        mainHandler.post(() -> callback.onError(message));
    }

    private static String readStreamText(InputStream stream) {
        if (stream == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() > 0) sb.append('\n');
                sb.append(line);
                if (sb.length() > 400) break;
            }
            return sb.toString().trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
