// FileAdapter.java
package com.example.googledriveapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.api.services.drive.model.File;

import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {
    private final List<File> files;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(File file);
        void onDownloadClick(File file);
        void onDeleteClick(File file);
        void onEditClick(File file);
    }

    public FileAdapter(List<File> files, OnItemClickListener listener) {
        this.files = files;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        File file = files.get(position);
        holder.bind(file, listener);
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        private final TextView fileName;
        private final Button buttonEdit;
        private final Button buttonDelete;
        private final Button buttonDownload;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileName = itemView.findViewById(R.id.file_name);
            buttonEdit = itemView.findViewById(R.id.button_edit);
            buttonDelete = itemView.findViewById(R.id.button_delete);
            buttonDownload = itemView.findViewById(R.id.button_download);
        }

        public void bind(File file, OnItemClickListener listener) {
            fileName.setText(file.getName());
            buttonEdit.setOnClickListener(v -> listener.onEditClick(file));
            buttonDelete.setOnClickListener(v -> listener.onDeleteClick(file));
            buttonDownload.setOnClickListener(v -> listener.onDownloadClick(file));
            itemView.setOnClickListener(v -> listener.onItemClick(file));
        }
    }
}
