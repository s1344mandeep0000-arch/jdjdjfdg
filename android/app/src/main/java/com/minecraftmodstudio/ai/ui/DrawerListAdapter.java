package com.minecraftmodstudio.ai.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.minecraftmodstudio.ai.R;
import com.minecraftmodstudio.ai.model.ModModels.ChatSession;
import com.minecraftmodstudio.ai.model.ModModels.GeneratedMod;
import com.minecraftmodstudio.ai.model.ModModels.SavedProject;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter inside the navigation drawer supporting both Chat History and Saved Projects.
 */
public final class DrawerListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int MODE_HISTORY = 0;
    public static final int MODE_PROJECTS = 1;

    public interface Listener {
        void onSelectSession(ChatSession session);
        void onDeleteSession(ChatSession session);
        void onPreviewProject(GeneratedMod mod);
        void onExportProject(GeneratedMod mod);
        void onDeleteProject(SavedProject project);
    }

    private int mode = MODE_HISTORY;
    private final List<ChatSession> sessions = new ArrayList<>();
    private final List<SavedProject> projects = new ArrayList<>();
    private final Listener listener;

    public DrawerListAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setHistoryData(List<ChatSession> items) {
        this.mode = MODE_HISTORY;
        this.sessions.clear();
        if (items != null) this.sessions.addAll(items);
        notifyDataSetChanged();
    }

    public void setProjectsData(List<SavedProject> items) {
        this.mode = MODE_PROJECTS;
        this.projects.clear();
        if (items != null) this.projects.addAll(items);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return mode;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == MODE_HISTORY) {
            return new SessionViewHolder(inflater.inflate(R.layout.item_drawer_session, parent, false));
        }
        return new ProjectViewHolder(inflater.inflate(R.layout.item_drawer_project, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof SessionViewHolder) {
            ((SessionViewHolder) holder).bind(sessions.get(position));
        } else if (holder instanceof ProjectViewHolder) {
            ((ProjectViewHolder) holder).bind(projects.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return mode == MODE_HISTORY ? sessions.size() : projects.size();
    }

    final class SessionViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvMeta;
        private final ImageButton btnDelete;

        SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvSessionTitle);
            tvMeta = itemView.findViewById(R.id.tvSessionMeta);
            btnDelete = itemView.findViewById(R.id.btnDeleteSession);
        }

        void bind(ChatSession s) {
            tvTitle.setText(s.title);
            tvMeta.setText(s.messages.size() + " messages");
            itemView.setOnClickListener(v -> listener.onSelectSession(s));
            btnDelete.setOnClickListener(v -> listener.onDeleteSession(s));
        }
    }

    final class ProjectViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvMeta;
        private final ImageButton btnDelete;
        private final TextView btnPreview;
        private final TextView btnExport;

        ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvProjectTitle);
            tvMeta = itemView.findViewById(R.id.tvProjectMeta);
            btnDelete = itemView.findViewById(R.id.btnDeleteProject);
            btnPreview = itemView.findViewById(R.id.btnProjectPreview);
            btnExport = itemView.findViewById(R.id.btnProjectExport);
        }

        void bind(SavedProject p) {
            tvTitle.setText(p.mod.modMeta.name);
            tvMeta.setText(p.mod.modMeta.loader + " • MC " + p.mod.modMeta.minecraftVersion);
            btnPreview.setOnClickListener(v -> listener.onPreviewProject(p.mod));
            btnExport.setOnClickListener(v -> listener.onExportProject(p.mod));
            btnDelete.setOnClickListener(v -> listener.onDeleteProject(p));
        }
    }
}
