package com.minecraftmodstudio.ai;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.minecraftmodstudio.ai.model.ModModels.AppSettings;
import com.minecraftmodstudio.ai.model.ModModels.ChatAttachment;
import com.minecraftmodstudio.ai.model.ModModels.ChatMessage;
import com.minecraftmodstudio.ai.model.ModModels.ChatSession;
import com.minecraftmodstudio.ai.model.ModModels.GeneratedMod;
import com.minecraftmodstudio.ai.model.ModModels.GenerationStep;
import com.minecraftmodstudio.ai.model.ModModels.SavedProject;
import com.minecraftmodstudio.ai.service.BedrockAddonExporter;
import com.minecraftmodstudio.ai.service.ModGeneratorClient;
import com.minecraftmodstudio.ai.storage.StudioRepository;
import com.minecraftmodstudio.ai.ui.ChatAdapter;
import com.minecraftmodstudio.ai.ui.DrawerListAdapter;
import com.minecraftmodstudio.ai.util.ModPackageHelper;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Main native Android screen for Minecraft AI Mod Maker Studio.
 * Written 100% in Java with native Android XML views, matching the minimalist white ChatGPT UI.
 */
public class MainActivity extends AppCompatActivity implements
        ChatAdapter.Listener,
        DrawerListAdapter.Listener {

    private static final String[] MC_VERSIONS = {"1.21.4", "1.21.1", "1.20.6", "1.20.1", "1.19.2"};

    private DrawerLayout drawerLayout;
    private LinearLayout emptySuggestionsContainer;
    private RecyclerView chatRecyclerView;
    private HorizontalScrollView attachmentStrip;
    private LinearLayout attachmentChipsContainer;
    private EditText etPrompt;
    private ImageButton btnVoiceOrSend;

    // Drawer Views
    private EditText etDrawerSearch;
    private TextView tabChatHistory;
    private TextView tabSavedProjects;
    private RecyclerView drawerRecyclerView;
    private TextView tvDrawerEmpty;

    private StudioRepository repository;
    private ModGeneratorClient generatorClient;
    private ChatAdapter chatAdapter;
    private DrawerListAdapter drawerAdapter;

    private final List<ChatSession> sessions = new ArrayList<>();
    private final List<SavedProject> savedProjects = new ArrayList<>();
    private AppSettings settings = new AppSettings();
    private String activeSessionId = null;

    private final List<ChatAttachment> pendingAttachments = new ArrayList<>();
    private ModGeneratorClient.RequestHandle activeRequestHandle = null;
    private boolean isGenerating = false;
    private int drawerTabMode = DrawerListAdapter.MODE_HISTORY;
    private GeneratedMod pendingExportMod = null;

    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) handlePickedAttachment(uri);
                }
            });

    private final ActivityResultLauncher<Intent> speechLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<String> matches = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (matches != null && !matches.isEmpty()) {
                        String spoken = matches.get(0);
                        String existing = etPrompt.getText() != null ? etPrompt.getText().toString().trim() : "";
                        String nextText = existing.isEmpty() ? spoken : existing + " " + spoken;
                        etPrompt.setText(nextText);
                        etPrompt.setSelection(nextText.length());
                    }
                }
            });

    private final ActivityResultLauncher<String> legacyStoragePermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (pendingExportMod != null) {
                    GeneratedMod toExport = pendingExportMod;
                    pendingExportMod = null;
                    performExport(toExport);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repository = new StudioRepository(this);
        generatorClient = new ModGeneratorClient();

        sessions.addAll(repository.loadSessions());
        savedProjects.addAll(repository.loadProjects());
        settings = repository.loadSettings();
        if (!sessions.isEmpty()) {
            activeSessionId = sessions.get(0).id;
        }

        bindViews();
        setupAdapters();
        setupListeners();
        renderActiveSession();
        refreshDrawerList();
        updateComposerActionIcon();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void bindViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        emptySuggestionsContainer = findViewById(R.id.emptySuggestionsContainer);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        attachmentStrip = findViewById(R.id.attachmentStrip);
        attachmentChipsContainer = findViewById(R.id.attachmentChipsContainer);
        etPrompt = findViewById(R.id.etPrompt);
        btnVoiceOrSend = findViewById(R.id.btnVoiceOrSend);

        etDrawerSearch = findViewById(R.id.etDrawerSearch);
        tabChatHistory = findViewById(R.id.tabChatHistory);
        tabSavedProjects = findViewById(R.id.tabSavedProjects);
        drawerRecyclerView = findViewById(R.id.drawerRecyclerView);
        tvDrawerEmpty = findViewById(R.id.tvDrawerEmpty);
    }

    private void setupAdapters() {
        chatAdapter = new ChatAdapter(this);
        LinearLayoutManager chatLm = new LinearLayoutManager(this);
        chatLm.setStackFromEnd(false);
        chatRecyclerView.setLayoutManager(chatLm);
        chatRecyclerView.setAdapter(chatAdapter);

        drawerAdapter = new DrawerListAdapter(this);
        drawerRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        drawerRecyclerView.setAdapter(drawerAdapter);
    }

    private void setupListeners() {
        findViewById(R.id.btnMenu).setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        findViewById(R.id.btnCloseDrawer).setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
        findViewById(R.id.btnAiPro).setOnClickListener(v -> showAiSettingsDialog());
        findViewById(R.id.btnNewChat).setOnClickListener(v -> startNewChat());
        findViewById(R.id.btnDrawerNewChat).setOnClickListener(v -> {
            startNewChat();
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        findViewById(R.id.btnDrawerAiSettings).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            showAiSettingsDialog();
        });
        findViewById(R.id.btnDrawerSettings).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            showAppSettingsDialog();
        });

        // Centered Suggestions
        findViewById(R.id.suggestionCreateMod).setOnClickListener(v ->
                sendPrompt("Create a Minecraft Java Edition mod adding Amethyst Crystal Golems, Enderite Tools, and Luminous Cavern Ore.", null));
        findViewById(R.id.suggestionGenerateAddon).setOnClickListener(v ->
                sendPrompt("Generate a Minecraft Bedrock Edition add-on with Plasma Blasters, Energy Shield Armor, and a friendly Hover Drone mob.", null));
        findViewById(R.id.suggestionDesignElements).setOnClickListener(v ->
                sendPrompt("Design custom Minecraft blocks, weapons, armor, and a boss mob with 16x16 pixel textures for a Void Dimension expansion.", null));

        // Composer Controls
        findViewById(R.id.btnAttach).setOnClickListener(v -> launchAttachmentPicker());
        findViewById(R.id.btnMic).setOnClickListener(v -> startVoiceDictation());

        btnVoiceOrSend.setOnClickListener(v -> {
            if (isGenerating) {
                cancelActiveGeneration();
            } else if (hasPromptOrAttachments()) {
                sendCurrentInput();
            } else {
                startVoiceDictation();
            }
        });

        etPrompt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateComposerActionIcon();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etPrompt.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND && hasPromptOrAttachments() && !isGenerating) {
                sendCurrentInput();
                return true;
            }
            return false;
        });

        // Drawer Tabs & Search
        tabChatHistory.setOnClickListener(v -> switchDrawerTab(DrawerListAdapter.MODE_HISTORY));
        tabSavedProjects.setOnClickListener(v -> switchDrawerTab(DrawerListAdapter.MODE_PROJECTS));
        etDrawerSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshDrawerList();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private boolean hasPromptOrAttachments() {
        String text = etPrompt.getText() != null ? etPrompt.getText().toString().trim() : "";
        return !text.isEmpty() || !pendingAttachments.isEmpty();
    }

    private void updateComposerActionIcon() {
        if (isGenerating) {
            btnVoiceOrSend.setImageResource(R.drawable.ic_close);
        } else if (hasPromptOrAttachments()) {
            btnVoiceOrSend.setImageResource(R.drawable.ic_send);
        } else {
            btnVoiceOrSend.setImageResource(R.drawable.ic_voice_waves);
        }
    }

    private void startNewChat() {
        if (isGenerating) {
            cancelActiveGeneration();
        }
        activeSessionId = null;
        pendingAttachments.clear();
        renderAttachmentChips();
        etPrompt.setText("");
        renderActiveSession();
    }

    private ChatSession getActiveSession() {
        if (activeSessionId == null) return null;
        for (ChatSession s : sessions) {
            if (activeSessionId.equals(s.id)) return s;
        }
        return null;
    }

    private void renderActiveSession() {
        ChatSession active = getActiveSession();
        if (active == null || active.messages.isEmpty()) {
            emptySuggestionsContainer.setVisibility(View.VISIBLE);
            chatRecyclerView.setVisibility(View.GONE);
            chatAdapter.setMessages(new ArrayList<>(), isGenerating);
        } else {
            emptySuggestionsContainer.setVisibility(View.GONE);
            chatRecyclerView.setVisibility(View.VISIBLE);
            chatAdapter.setMessages(active.messages, isGenerating);
            if (!active.messages.isEmpty()) {
                chatRecyclerView.scrollToPosition(active.messages.size() - 1);
            }
        }
    }

    private void sendCurrentInput() {
        String text = etPrompt.getText() != null ? etPrompt.getText().toString().trim() : "";
        if (text.isEmpty() && pendingAttachments.isEmpty()) return;
        String promptToSend = text.isEmpty() ? "Create a Minecraft mod inspired by my attached files." : text;
        List<ChatAttachment> attachedCopy = new ArrayList<>(pendingAttachments);
        etPrompt.setText("");
        pendingAttachments.clear();
        renderAttachmentChips();
        updateComposerActionIcon();
        sendPrompt(promptToSend, attachedCopy);
    }

    private void sendPrompt(String promptText, List<ChatAttachment> attachments) {
        if (isGenerating || promptText == null || promptText.trim().isEmpty()) return;

        long now = System.currentTimeMillis();
        ChatSession session = getActiveSession();
        if (session == null) {
            session = new ChatSession();
            session.id = UUID.randomUUID().toString();
            session.title = promptText.length() > 42 ? promptText.substring(0, 42) + "..." : promptText;
            session.createdAt = now;
            session.updatedAt = now;
            sessions.add(0, session);
            activeSessionId = session.id;
        }

        ChatMessage userMsg = new ChatMessage();
        userMsg.id = UUID.randomUUID().toString();
        userMsg.role = "user";
        userMsg.text = promptText;
        userMsg.createdAt = now;
        if (attachments != null) userMsg.attachments.addAll(attachments);
        session.messages.add(userMsg);

        final ChatMessage assistantMsg = new ChatMessage();
        assistantMsg.id = UUID.randomUUID().toString();
        assistantMsg.role = "assistant";
        assistantMsg.createdAt = now + 1;
        assistantMsg.steps.add(new GenerationStep("analyze", "Connecting to Minecraft AI Studio...", "running"));
        session.messages.add(assistantMsg);
        session.updatedAt = now;

        repository.saveSessions(sessions);
        isGenerating = true;
        updateComposerActionIcon();
        renderActiveSession();
        refreshDrawerList();

        List<ChatMessage> conversationHistory = new ArrayList<>();
        for (ChatMessage m : session.messages) {
            if (!assistantMsg.id.equals(m.id) && m.error == null) {
                conversationHistory.add(m);
            }
        }

        final ChatSession targetSession = session;
        activeRequestHandle = generatorClient.generateMod(conversationHistory, attachments, settings,
                new ModGeneratorClient.GenerationCallback() {
                    @Override
                    public void onStep(String stepId, String label) {
                        for (GenerationStep s : assistantMsg.steps) {
                            s.status = "done";
                        }
                        GenerationStep existing = null;
                        for (GenerationStep s : assistantMsg.steps) {
                            if (stepId.equals(s.id)) {
                                existing = s;
                                break;
                            }
                        }
                        if (existing != null) {
                            existing.label = label;
                            existing.status = "running";
                        } else {
                            assistantMsg.steps.add(new GenerationStep(stepId, label, "running"));
                        }
                        renderActiveSession();
                    }

                    @Override
                    public void onComplete(GeneratedMod mod) {
                        isGenerating = false;
                        activeRequestHandle = null;
                        for (GenerationStep s : assistantMsg.steps) {
                            s.status = "done";
                        }
                        assistantMsg.text = mod.summary;
                        assistantMsg.mod = mod;
                        assistantMsg.error = null;

                        if (targetSession.messages.size() <= 2 && mod.modMeta.name != null && !mod.modMeta.name.isEmpty()) {
                            targetSession.title = mod.modMeta.name;
                        }
                        targetSession.updatedAt = System.currentTimeMillis();

                        if (settings.autoSaveProjects) {
                            saveProjectIfAbsent(mod);
                        }
                        repository.saveSessions(sessions);
                        updateComposerActionIcon();
                        renderActiveSession();
                        refreshDrawerList();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        isGenerating = false;
                        activeRequestHandle = null;
                        for (GenerationStep s : assistantMsg.steps) {
                            s.status = "done";
                        }
                        assistantMsg.error = errorMessage;
                        targetSession.updatedAt = System.currentTimeMillis();
                        repository.saveSessions(sessions);
                        updateComposerActionIcon();
                        renderActiveSession();
                    }
                });
    }

    private void cancelActiveGeneration() {
        if (activeRequestHandle != null) {
            activeRequestHandle.cancel();
            activeRequestHandle = null;
        }
        isGenerating = false;
        ChatSession active = getActiveSession();
        if (active != null && !active.messages.isEmpty()) {
            ChatMessage last = active.messages.get(active.messages.size() - 1);
            if ("assistant".equals(last.role) && last.mod == null && last.error == null) {
                last.error = "Generation stopped by user.";
                for (GenerationStep s : last.steps) s.status = "done";
            }
        }
        repository.saveSessions(sessions);
        updateComposerActionIcon();
        renderActiveSession();
    }

    private void launchAttachmentPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        filePickerLauncher.launch(intent);
    }

    private void handlePickedAttachment(Uri uri) {
        try {
            ContentResolver resolver = getContentResolver();
            String mimeType = resolver.getType(uri);
            if (mimeType == null) mimeType = "application/octet-stream";

            String displayName = "attachment";
            long size = 0;
            try (Cursor cursor = resolver.query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    int sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (nameIdx >= 0) displayName = cursor.getString(nameIdx);
                    if (sizeIdx >= 0) size = cursor.getLong(sizeIdx);
                }
            }

            byte[] bytes;
            try (InputStream is = resolver.openInputStream(uri)) {
                if (is == null) return;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                int total = 0;
                while ((n = is.read(buf)) != -1) {
                    baos.write(buf, 0, n);
                    total += n;
                    if (total > 4 * 1024 * 1024) break;
                }
                bytes = baos.toByteArray();
            }

            ChatAttachment att = new ChatAttachment();
            att.id = UUID.randomUUID().toString();
            att.name = displayName;
            att.mimeType = mimeType;
            att.size = size > 0 ? size : bytes.length;

            if (mimeType.startsWith("image/")) {
                att.base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP);
            } else {
                String snippet = new String(bytes, StandardCharsets.UTF_8);
                att.textSnippet = snippet.length() > 6000 ? snippet.substring(0, 6000) : snippet;
            }

            pendingAttachments.add(att);
            renderAttachmentChips();
            updateComposerActionIcon();
        } catch (Exception ex) {
            Toast.makeText(this, "Could not attach file: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void renderAttachmentChips() {
        attachmentChipsContainer.removeAllViews();
        if (pendingAttachments.isEmpty()) {
            attachmentStrip.setVisibility(View.GONE);
            return;
        }
        attachmentStrip.setVisibility(View.VISIBLE);
        for (int i = 0; i < pendingAttachments.size(); i++) {
            final int idx = i;
            ChatAttachment att = pendingAttachments.get(i);
            TextView chip = new TextView(this);
            chip.setBackgroundResource(R.drawable.bg_chip);
            chip.setPadding(24, 12, 24, 12);
            chip.setText(att.name + "  ✕");
            chip.setTextSize(12f);
            chip.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(12);
            chip.setLayoutParams(lp);
            chip.setOnClickListener(v -> {
                pendingAttachments.remove(idx);
                renderAttachmentChips();
                updateComposerActionIcon();
            });
            attachmentChipsContainer.addView(chip);
        }
    }

    private void startVoiceDictation() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.composer_hint));
        try {
            speechLauncher.launch(intent);
        } catch (Exception ex) {
            Toast.makeText(this, "Voice dictation is unavailable on this device.", Toast.LENGTH_SHORT).show();
        }
    }

    private void switchDrawerTab(int mode) {
        drawerTabMode = mode;
        if (mode == DrawerListAdapter.MODE_HISTORY) {
            tabChatHistory.setBackgroundResource(R.drawable.bg_outline_button);
            tabChatHistory.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            tabSavedProjects.setBackground(null);
            tabSavedProjects.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        } else {
            tabSavedProjects.setBackgroundResource(R.drawable.bg_outline_button);
            tabSavedProjects.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            tabChatHistory.setBackground(null);
            tabChatHistory.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        }
        refreshDrawerList();
    }

    private void refreshDrawerList() {
        String q = etDrawerSearch.getText() != null
                ? etDrawerSearch.getText().toString().trim().toLowerCase(Locale.ROOT)
                : "";

        if (drawerTabMode == DrawerListAdapter.MODE_HISTORY) {
            List<ChatSession> filtered = new ArrayList<>();
            for (ChatSession s : sessions) {
                if (q.isEmpty() || (s.title != null && s.title.toLowerCase(Locale.ROOT).contains(q))) {
                    filtered.add(s);
                }
            }
            drawerAdapter.setHistoryData(filtered);
            tvDrawerEmpty.setText(R.string.empty_history);
            tvDrawerEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        } else {
            List<SavedProject> filtered = new ArrayList<>();
            for (SavedProject p : savedProjects) {
                if (q.isEmpty()
                        || (p.mod.modMeta.name != null && p.mod.modMeta.name.toLowerCase(Locale.ROOT).contains(q))
                        || (p.mod.modMeta.loader != null && p.mod.modMeta.loader.toLowerCase(Locale.ROOT).contains(q))) {
                    filtered.add(p);
                }
            }
            drawerAdapter.setProjectsData(filtered);
            tvDrawerEmpty.setText(R.string.empty_projects);
            tvDrawerEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void saveProjectIfAbsent(GeneratedMod mod) {
        if (!isProjectSaved(mod.modMeta.modId, mod.modMeta.name)) {
            SavedProject project = new SavedProject();
            project.id = UUID.randomUUID().toString();
            project.savedAt = System.currentTimeMillis();
            project.mod = mod;
            savedProjects.add(0, project);
            repository.saveProjects(savedProjects);
        }
    }

    private void showAiSettingsDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_ai_settings, null);
        RadioGroup rgEdition = view.findViewById(R.id.rgEdition);
        RadioGroup rgLoader = view.findViewById(R.id.rgLoader);
        Spinner spinnerVersion = view.findViewById(R.id.spinnerMcVersion);
        RadioGroup rgReasoning = view.findViewById(R.id.rgReasoning);

        if ("java".equals(settings.editionPreference)) rgEdition.check(R.id.rbEditionJava);
        else if ("bedrock".equals(settings.editionPreference)) rgEdition.check(R.id.rbEditionBedrock);
        else rgEdition.check(R.id.rbEditionAuto);

        if ("Forge".equals(settings.javaLoader)) rgLoader.check(R.id.rbLoaderForge);
        else if ("NeoForge".equals(settings.javaLoader)) rgLoader.check(R.id.rbLoaderNeoForge);
        else rgLoader.check(R.id.rbLoaderFabric);

        ArrayAdapter<String> versionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, MC_VERSIONS);
        versionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVersion.setAdapter(versionAdapter);
        for (int i = 0; i < MC_VERSIONS.length; i++) {
            if (MC_VERSIONS[i].equals(settings.minecraftVersion)) {
                spinnerVersion.setSelection(i);
                break;
            }
        }

        if ("low".equals(settings.reasoningDepth)) rgReasoning.check(R.id.rbReasoningLow);
        else if ("high".equals(settings.reasoningDepth)) rgReasoning.check(R.id.rbReasoningHigh);
        else rgReasoning.check(R.id.rbReasoningMedium);

        new AlertDialog.Builder(this)
                .setView(view)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.done, (dialog, which) -> {
                    int edId = rgEdition.getCheckedRadioButtonId();
                    if (edId == R.id.rbEditionJava) settings.editionPreference = "java";
                    else if (edId == R.id.rbEditionBedrock) settings.editionPreference = "bedrock";
                    else settings.editionPreference = "auto";

                    int loaderId = rgLoader.getCheckedRadioButtonId();
                    if (loaderId == R.id.rbLoaderForge) settings.javaLoader = "Forge";
                    else if (loaderId == R.id.rbLoaderNeoForge) settings.javaLoader = "NeoForge";
                    else settings.javaLoader = "Fabric";

                    Object selectedVer = spinnerVersion.getSelectedItem();
                    if (selectedVer != null) settings.minecraftVersion = selectedVer.toString();

                    int reasonId = rgReasoning.getCheckedRadioButtonId();
                    if (reasonId == R.id.rbReasoningLow) settings.reasoningDepth = "low";
                    else if (reasonId == R.id.rbReasoningHigh) settings.reasoningDepth = "high";
                    else settings.reasoningDepth = "medium";

                    repository.saveSettings(settings);
                    Toast.makeText(this, "AI Model Settings saved.", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showAppSettingsDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_app_settings, null);
        CheckBox cbAutoSave = view.findViewById(R.id.cbAutoSave);
        CheckBox cbBedrockScripts = view.findViewById(R.id.cbBedrockScripts);
        EditText etApiBaseUrl = view.findViewById(R.id.etApiBaseUrl);
        TextView btnClearHistory = view.findViewById(R.id.btnClearHistory);
        TextView btnClearProjects = view.findViewById(R.id.btnClearProjects);

        cbAutoSave.setChecked(settings.autoSaveProjects);
        cbBedrockScripts.setChecked(settings.includeBedrockScripts);
        String displayApi = (settings.apiBaseUrl != null && !settings.apiBaseUrl.isEmpty())
                ? settings.apiBaseUrl
                : BuildConfig.DEFAULT_API_BASE_URL;
        etApiBaseUrl.setText(displayApi);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.done, (d, which) -> {
                    settings.autoSaveProjects = cbAutoSave.isChecked();
                    settings.includeBedrockScripts = cbBedrockScripts.isChecked();
                    String enteredUrl = etApiBaseUrl.getText() != null
                            ? etApiBaseUrl.getText().toString().trim()
                            : "";
                    settings.apiBaseUrl = enteredUrl.equals(BuildConfig.DEFAULT_API_BASE_URL) ? "" : enteredUrl;
                    repository.saveSettings(settings);
                    Toast.makeText(this, "Settings saved.", Toast.LENGTH_SHORT).show();
                })
                .create();

        btnClearHistory.setOnClickListener(v -> {
            sessions.clear();
            activeSessionId = null;
            repository.saveSessions(sessions);
            renderActiveSession();
            refreshDrawerList();
            Toast.makeText(this, "Chat history cleared.", Toast.LENGTH_SHORT).show();
        });

        btnClearProjects.setOnClickListener(v -> {
            savedProjects.clear();
            repository.saveProjects(savedProjects);
            renderActiveSession();
            refreshDrawerList();
            Toast.makeText(this, "Saved projects cleared.", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    // ChatAdapter.Listener
    @Override
    public void onPreviewMod(GeneratedMod mod) {
        repository.savePreviewMod(mod);
        startActivity(new Intent(this, ModPreviewActivity.class));
    }

    @Override
    public void onExportMod(GeneratedMod mod) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            pendingExportMod = mod;
            legacyStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return;
        }
        performExport(mod);
    }

    private void performExport(GeneratedMod mod) {
        try {
            BedrockAddonExporter.ExportResult result = BedrockAddonExporter.exportModToDownloads(this, mod);
            String mimeType = result.fileName.endsWith(".mcaddon") ? "application/octet-stream" : "application/zip";
            new AlertDialog.Builder(this)
                    .setTitle("Export Complete")
                    .setMessage("Saved " + result.fileName + " to " + result.displayPath)
                    .setPositiveButton("Share File", (d, w) ->
                            BedrockAddonExporter.shareFile(this, result.shareFile, mimeType, "Share " + result.fileName))
                    .setNegativeButton(R.string.done, null)
                    .show();
        } catch (Exception ex) {
            Toast.makeText(this, "Export failed: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onToggleSaveProject(GeneratedMod mod) {
        int foundIdx = -1;
        for (int i = 0; i < savedProjects.size(); i++) {
            SavedProject p = savedProjects.get(i);
            if (p.mod.modMeta.modId.equals(mod.modMeta.modId) && p.mod.modMeta.name.equals(mod.modMeta.name)) {
                foundIdx = i;
                break;
            }
        }
        if (foundIdx >= 0) {
            savedProjects.remove(foundIdx);
            Toast.makeText(this, "Removed from Saved Projects.", Toast.LENGTH_SHORT).show();
        } else {
            saveProjectIfAbsent(mod);
            Toast.makeText(this, "Saved to Projects.", Toast.LENGTH_SHORT).show();
        }
        repository.saveProjects(savedProjects);
        chatAdapter.notifyDataSetChanged();
        refreshDrawerList();
    }

    @Override
    public void onCreateVariation(GeneratedMod sourceMod, String customTweak) {
        if (isGenerating) return;
        String variationPrompt = ModPackageHelper.buildVariationPrompt(sourceMod, customTweak);
        sendPrompt(variationPrompt, null);
    }

    @Override
    public void onRetryLastPrompt() {
        ChatSession active = getActiveSession();
        if (active == null || isGenerating) return;
        for (int i = active.messages.size() - 1; i >= 0; i--) {
            ChatMessage m = active.messages.get(i);
            if ("user".equals(m.role) && m.text != null && !m.text.trim().isEmpty()) {
                sendPrompt(m.text, m.attachments);
                return;
            }
        }
    }

    @Override
    public boolean isProjectSaved(String modId, String name) {
        for (SavedProject p : savedProjects) {
            if (p.mod.modMeta.modId.equals(modId) && p.mod.modMeta.name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    // DrawerListAdapter.Listener
    @Override
    public void onSelectSession(ChatSession session) {
        if (isGenerating) cancelActiveGeneration();
        activeSessionId = session.id;
        renderActiveSession();
        drawerLayout.closeDrawer(GravityCompat.START);
    }

    @Override
    public void onDeleteSession(ChatSession session) {
        sessions.remove(session);
        if (session.id.equals(activeSessionId)) {
            activeSessionId = sessions.isEmpty() ? null : sessions.get(0).id;
        }
        repository.saveSessions(sessions);
        renderActiveSession();
        refreshDrawerList();
    }

    @Override
    public void onPreviewProject(GeneratedMod mod) {
        drawerLayout.closeDrawer(GravityCompat.START);
        onPreviewMod(mod);
    }

    @Override
    public void onExportProject(GeneratedMod mod) {
        drawerLayout.closeDrawer(GravityCompat.START);
        onExportMod(mod);
    }

    @Override
    public void onDeleteProject(SavedProject project) {
        savedProjects.remove(project);
        repository.saveProjects(savedProjects);
        chatAdapter.notifyDataSetChanged();
        refreshDrawerList();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (activeRequestHandle != null) activeRequestHandle.cancel();
        if (generatorClient != null) generatorClient.shutdown();
    }
}
