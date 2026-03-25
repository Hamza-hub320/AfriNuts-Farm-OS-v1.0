package com.afrinuts.farmos.ui.photos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.PhotoEntity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {

    private List<PhotoEntity> photos = new ArrayList<>();
    private OnPhotoClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public interface OnPhotoClickListener {
        void onPhotoClick(PhotoEntity photo);
    }

    public PhotoAdapter(OnPhotoClickListener listener) {
        this.listener = listener;
    }

    public void updatePhotos(List<PhotoEntity> newPhotos) {
        this.photos = newPhotos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_photo_grid, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        PhotoEntity photo = photos.get(position);
        holder.bind(photo, listener);
    }

    @Override
    public int getItemCount() {
        return photos.size();
    }

    class PhotoViewHolder extends RecyclerView.ViewHolder {

        private ImageView ivPhoto;
        private ImageView ivTypeIcon;
        private TextView tvCaption;
        private TextView tvDate;
        private CardView cardView;

        PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.ivPhoto);
            ivTypeIcon = itemView.findViewById(R.id.ivTypeIcon);
            tvCaption = itemView.findViewById(R.id.tvCaption);
            tvDate = itemView.findViewById(R.id.tvDate);
            cardView = itemView.findViewById(R.id.cardView); // Now this will find the CardView!
        }

        void bind(PhotoEntity photo, OnPhotoClickListener listener) {
            // Load image using Glide
            File imageFile = new File(photo.getFilePath());
            if (imageFile.exists()) {
                Glide.with(itemView.getContext())
                        .load(imageFile)
                        .centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_photo_placeholder)
                        .error(R.drawable.ic_photo_error)
                        .into(ivPhoto);
            } else {
                ivPhoto.setImageResource(R.drawable.ic_photo_error);
            }

            // Set type icon using the entity's method
            ivTypeIcon.setImageResource(photo.getIconResId());

            // Set caption (or placeholder)
            if (photo.getCaption() != null && !photo.getCaption().isEmpty()) {
                tvCaption.setText(photo.getCaption());
            } else {
                tvCaption.setText("No caption");
            }

            // Set date
            tvDate.setText(dateFormat.format(new Date(photo.getTakenAt())));

            // Click listener on CardView
            if (cardView != null) {
                cardView.setOnClickListener(v -> listener.onPhotoClick(photo));
            } else {
                // Fallback to itemView if cardView not found
                itemView.setOnClickListener(v -> listener.onPhotoClick(photo));
            }
        }
    }
}