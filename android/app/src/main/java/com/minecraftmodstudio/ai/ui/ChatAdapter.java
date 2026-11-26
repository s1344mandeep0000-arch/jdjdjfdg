package com.minecraftmodstudio.ai.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.minecraftmodstudio.ai.R;
import com.minecraftmodstudio.ai.model.ModModels.ChatAttachment;
import com.minecraftmodstudio.ai.model.ModModels.ChatMessage;
import com.minecraftmodstudio.ai.model.ModModels.GeneratedMod;
import com.minecraftmodstudio.ai.model.ModModels.GenerationStep;
import com.minecraftmodstudio.ai.model.ModModels.ModElement;
import com.minecraftmodstudio.ai.util.PixelArtRenderer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * RecyclerView adapter for the ChatGPT-style conversation thread, supporting user bubbles,
 * streaming step indicators, generated mod cards, Create Variation panels, Mod Preview, and Export.
 */
public final class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_USER = 1;
    private static final int TYPE_ASSISTANT = 2;

    public interface Listener {
        void onPreviewMod(GeneratedMod mod);
        void onExportMod(GeneratedMod mod);
        void onToggleSaveProject(GeneratedMod mod);
        void onCreateVariation(GeneratedMod sourceMod, String customTweak);
        void onRetryLastPrompt();
        boolean isProjectSaved(String modId, String name);
    }

    private final List<ChatMessage> messages = new ArrayList<>();
    private final Set<String> expandedVariationMessageIds = new HashSet<>();
    private final Listener listener;
    private boolean isGenerating = false;

    public ChatAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setMessages(List<ChatMessage> newMessages, boolean generating) {
        this.messages.clear();
        if (newMessages != null) {
            this.messages.addAll(newMessages);
        }
        this.isGenerating = generating;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return "user".equals(messages.get(position).role) ? TYPE_USER : TYPE_ASSISTANT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_USER) {
            View v = inflater.inflate(R.layout.item_chat_user, parent, false);
            return new UserViewHolder(v);
        }
        View v = inflater.inflate(R.layout.item_chat_assistant, parent, false);
        return new AssistantViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        if (holder instanceof UserViewHolder) {
            ((UserViewHolder) holder).bind(msg);
        } else if (holder instanceof AssistantViewHolder) {
            ((AssistantViewHolder) holder).bind(msg);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static final class UserViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvAttachments;
        private final TextView tvMessage;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAttachments = itemView.findViewById(R.id.tvUserAttachments);
            tvMessage = itemView.findViewById(R.id.tvUserMessage);
        }

        void bind(ChatMessage msg) {
            tvMessage.setText(msg.text != null ? msg.text : "");
            if (msg.attachments.isEmpty()) {
                tvAttachments.setVisibility(View.GONE);
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < msg.attachments.size(); i++) {
                    ChatAttachment att = msg.attachments.get(i);
                    if (i > 0) sb.append(", ");
                    sb.append(att.name);
                }
                tvAttachments.setText("Attached: " + sb);
                tvAttachments.setVisibility(View.VISIBLE);
            }
        }
    }

    final class AssistantViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout stepsContainer;
        private final TextView tvAssistantText;
        private final LinearLayout errorContainer;
        private final TextView tvErrorMessage;
        private final TextView btnTryAgain;
        private final LinearLayout modCardContainer;
        private final TextView tvModTitle;
        private final TextView tvModMeta;
        private final ImageButton btnToggleSaveProject;
        private final LinearLayout modElementsContainer;
        private final LinearLayout btnModPreview;
        private final LinearLayout btnCreateVariation;
        private final LinearLayout btnExportMod;
        private final TextView tvExportLabel;
        private final LinearLayout variationPanel;
        private final TextView btnPresetColors;
        private final TextView btnPresetBehavior;
        private final TextView btnPresetFire;
        private final EditText etVariationTweak;
        private final TextView btnSubmitVariation;

        AssistantViewHolder(@NonNull View itemView) {
            super(itemView);
            stepsContainer = itemView.findViewById(R.id.stepsContainer);
            tvAssistantText = itemView.findViewById(R.id.tvAssistantText);
            errorContainer = itemView.findViewById(R.id.errorContainer);
            tvErrorMessage = itemView.findViewById(R.id.tvErrorMessage);
            btnTryAgain = itemView.findViewById(R.id.btnTryAgain);
            modCardContainer = itemView.findViewById(R.id.modCardContainer);
            tvModTitle = itemView.findViewById(R.id.tvModTitle);
            tvModMeta = itemView.findViewById(R.id.tvModMeta);
            btnToggleSaveProject = itemView.findViewById(R.id.btnToggleSaveProject);
            modElementsContainer = itemView.findViewById(R.id.modElementsContainer);
            btnModPreview = itemView.findViewById(R.id.btnModPreview);
            btnCreateVariation = itemView.findViewById(R.id.btnCreateVariation);
            btnExportMod = itemView.findViewById(R.id.btnExportMod);
            tvExportLabel = itemView.findViewById(R.id.tvExportLabel);
            variationPanel = itemView.findViewById(R.id.variationPanel);
            btnPresetColors = itemView.findViewById(R.id.btnPresetColors);
            btnPresetBehavior = itemView.findViewById(R.id.btnPresetBehavior);
            btnPresetFire = itemView.findViewById(R.id.btnPresetFire);
            etVariationTweak = itemView.findViewById(R.id.etVariationTweak);
            btnSubmitVariation = itemView.findViewById(R.id.btnSubmitVariation);
        }

        void bind(ChatMessage msg) {
            Context ctx = itemView.getContext();

            // 1. Live generation steps
            stepsContainer.removeAllViews();
            if (!msg.steps.isEmpty() && msg.mod == null) {
                stepsContainer.setVisibility(View.VISIBLE);
                for (GenerationStep step : msg.steps) {
                    LinearLayout row = new LinearLayout(ctx);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setPadding(0, 6, 0, 6);
                    row.setGravity(android.view.Gravity.CENTER_VERTICAL);

                    if ("done".equals(step.status)) {
                        ImageView check = new ImageView(ctx);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(36, 36);
                        lp.setMarginEnd(16);
                        check.setLayoutParams(lp);
                        check.setImageResource(R.drawable.ic_check);
                        check.setColorFilter(ContextCompat.getColor(ctx, R.color.success_green));
                        row.addView(check);
                    } else {
                        ProgressBar spinner = new ProgressBar(ctx, null, android.R.attr.progressBarStyleSmall);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(36, 36);
                        lp.setMarginEnd(16);
                        spinner.setLayoutParams(lp);
                        row.addView(spinner);
                    }

                    TextView tv = new TextView(ctx);
                    tv.setText(step.label);
                    tv.setTextSize(13f);
                    tv.setTextColor(ContextCompat.getColor(ctx,
                            "done".equals(step.status) ? R.color.text_secondary : R.color.text_primary));
                    row.addView(tv);
                    stepsContainer.addView(row);
                }
            } else {
                stepsContainer.setVisibility(View.GONE);
            }

            // 2. Assistant summary text
            if (msg.text != null && !msg.text.trim().isEmpty()) {
                tvAssistantText.setText(msg.text);
                tvAssistantText.setVisibility(View.VISIBLE);
            } else {
                tvAssistantText.setVisibility(View.GONE);
            }

            // 3. Error box
            if (msg.error != null && !msg.error.trim().isEmpty()) {
                errorContainer.setVisibility(View.VISIBLE);
                tvErrorMessage.setText(msg.error);
                btnTryAgain.setOnClickListener(v -> listener.onRetryLastPrompt());
            } else {
                errorContainer.setVisibility(View.GONE);
            }

            // 4. Generated Mod Card
            if (msg.mod != null) {
                GeneratedMod mod = msg.mod;
                modCardContainer.setVisibility(View.VISIBLE);
                tvModTitle.setText(mod.modMeta.name);
                tvModMeta.setText(mod.modMeta.loader + " • MC " + mod.modMeta.minecraftVersion
                        + " • " + mod.elements.size() + " elements • " + mod.files.size() + " files");

                boolean saved = listener.isProjectSaved(mod.modMeta.modId, mod.modMeta.name);
                btnToggleSaveProject.setImageResource(saved ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark);
                btnToggleSaveProject.setColorFilter(ContextCompat.getColor(ctx,
                        saved ? R.color.primary_blue : R.color.text_secondary));
                btnToggleSaveProject.setOnClickListener(v -> listener.onToggleSaveProject(mod));

                // Render elements with crisp 16x16 pixel art
                modElementsContainer.removeAllViews();
                LayoutInflater inflater = LayoutInflater.from(ctx);
                int iconPx = (int) (36 * ctx.getResources().getDisplayMetrics().density);
                for (ModElement el : mod.elements) {
                    View elView = inflater.inflate(R.layout.item_mod_element, modElementsContainer, false);
                    ImageView iv = elView.findViewById(R.id.ivElementPixelArt);
                    TextView tvName = elView.findViewById(R.id.tvElementName);
                    TextView tvKind = elView.findViewById(R.id.tvElementKind);
                    TextView tvStats = elView.findViewById(R.id.tvElementStats);

                    Bitmap bmp = PixelArtRenderer.toDisplayBitmap(el.pixelArt, iconPx);
                    iv.setImageBitmap(bmp);
                    tvName.setText(el.displayName);
                    tvKind.setText(el.kind != null ? el.kind.toUpperCase(Locale.ROOT) : "ITEM");
                    tvStats.setText(el.statsSummary != null && !el.statsSummary.isEmpty()
                            ? el.statsSummary
                            : el.description);
                    modElementsContainer.addView(elView);
                }

                boolean isBedrock = "bedrock".equalsIgnoreCase(mod.modMeta.edition);
                tvExportLabel.setText(isBedrock ? R.string.export_addon : R.string.export_zip);

                btnModPreview.setOnClickListener(v -> listener.onPreviewMod(mod));
                btnExportMod.setOnClickListener(v -> listener.onExportMod(mod));

                // Create Variation panel state
                boolean isOpen = expandedVariationMessageIds.contains(msg.id);
                variationPanel.setVisibility(isOpen ? View.VISIBLE : View.GONE);
                btnCreateVariation.setBackgroundResource(
                        isOpen ? R.drawable.bg_variation_button_active : R.drawable.bg_outline_button);
                btnCreateVariation.setEnabled(!isGenerating);

                btnCreateVariation.setOnClickListener(v -> {
                    if (expandedVariationMessageIds.contains(msg.id)) {
                        expandedVariationMessageIds.remove(msg.id);
                    } else {
                        expandedVariationMessageIds.add(msg.id);
                    }
                    notifyItemChanged(getBindingAdapterPosition());
                });

                View.OnClickListener presetListener = v -> {
                    String presetText = ((TextView) v).getText().toString();
                    expandedVariationMessageIds.remove(msg.id);
                    listener.onCreateVariation(mod, presetText);
                };
                btnPresetColors.setOnClickListener(presetListener);
                btnPresetBehavior.setOnClickListener(presetListener);
                btnPresetFire.setOnClickListener(presetListener);

                btnSubmitVariation.setEnabled(!isGenerating);
                btnSubmitVariation.setOnClickListener(v -> {
                    String tweak = etVariationTweak.getText() != null
                            ? etVariationTweak.getText().toString().trim()
                            : "";
                    etVariationTweak.setText("");
                    expandedVariationMessageIds.remove(msg.id);
                    listener.onCreateVariation(mod, tweak);
                });
            } else {
                modCardContainer.setVisibility(View.GONE);
            }
        }
    }
}
