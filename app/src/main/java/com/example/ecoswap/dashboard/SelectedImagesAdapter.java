package com.example.ecoswap.dashboard;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.ecoswap.R;
import java.util.ArrayList;
import java.util.List;

public class SelectedImagesAdapter extends RecyclerView.Adapter<SelectedImagesAdapter.ViewHolder> {

    private final List<Uri> imageUris;
    private final Context context;
    private final OnImageRemoveListener removeListener;

    public interface OnImageRemoveListener {
        void onImageRemoved(int position);
    }

    public SelectedImagesAdapter(Context context, OnImageRemoveListener removeListener) {
        this.context = context;
        this.removeListener = removeListener;
        this.imageUris = new ArrayList<>();
    }

    public void addImage(Uri uri) {
        imageUris.add(uri);
        notifyItemInserted(imageUris.size() - 1);
    }

    public void removeImage(int position) {
        if (position >= 0 && position < imageUris.size()) {
            imageUris.remove(position);
            notifyItemRemoved(position);
        }
    }

    public List<Uri> getImages() {
        return imageUris;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_selected_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Uri uri = imageUris.get(position);
        Glide.with(context)
                .load(uri)
                .centerCrop()
                .into(holder.ivSelectedImage);

        holder.ivRemoveImage.setOnClickListener(v -> {
            if (removeListener != null) {
                removeListener.onImageRemoved(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageUris.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivSelectedImage, ivRemoveImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivSelectedImage = itemView.findViewById(R.id.ivSelectedImage);
            ivRemoveImage = itemView.findViewById(R.id.ivRemoveImage);
        }
    }
}
