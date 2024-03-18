package com.eliedersousa.painel;

import static android.app.PendingIntent.getActivity;

import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.os.Environment;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.FileViewHolder> implements ItemTouchHelperAdapter {

    private Context context;
    private final List<String> fileList;
    private final OnItemClickListener itemClickListener;

    public FileListAdapter(Context context, List<String> fileList, OnItemClickListener itemClickListener) {
        this.context = context;
        this.fileList = fileList;
        this.itemClickListener = itemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position, String fileName);
    }

    @Override
    public void onItemDismiss(int position) {
        // Remove the file associated with the dismissed item
        File root = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        File fileToRemove = new File(root, fileList.get(position) + ".txt");

        if (fileToRemove.exists()) {
            fileToRemove.delete();
        }

        fileList.remove(position);
        notifyItemRemoved(position);
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.itemlist, parent, false);
        final FileViewHolder viewHolder = new FileViewHolder(view);

        // Set click listener for the item view
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = viewHolder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && itemClickListener != null) {
                    String fileName = fileList.get(position);
                    itemClickListener.onItemClick(v, position, fileName);
                }
            }
        });

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        String fileName = fileList.get(position);
        holder.bind(fileName);
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        private final TextView fileNameTextView;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileNameTextView = itemView.findViewById(R.id.fileNameTextView);
        }

        public void bind(String fileName) {
            fileNameTextView.setText(fileName);
        }
    }
}