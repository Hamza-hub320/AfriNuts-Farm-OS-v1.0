package com.afrinuts.farmos.ui.photos;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.PhotoEntity;
import com.afrinuts.farmos.ui.BaseActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PhotoDetailActivity extends BaseActivity {

    public static final String EXTRA_PHOTO_ID = "photo_id";

    private long photoId;
    private PhotoEntity photo;
    private AppDatabase database;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI Elements
    private ImageView ivFullPhoto;
    private TextView tvCaption;
    private TextView tvDate;
    private TextView tvLocation;
    private TextView tvType;
    private MaterialButton btnEditCaption;
    private MaterialButton btnShare;
    private MaterialButton btnDelete;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    @Override
    protected boolean checkRoleAccess() {
        // Owners and Managers can access photo details
        return !sessionManager.isInvestor();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_detail);

        // Get photo ID from intent
        photoId = getIntent().getLongExtra(EXTRA_PHOTO_ID, -1);
        if (photoId == -1) {
            Toast.makeText(this, "Photo not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize database
        database = AppDatabase.getInstance(this);

        // Initialize views
        initViews();

        // Setup toolbar
        setupToolbar();

        // Load photo data
        loadPhotoData();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ivFullPhoto = findViewById(R.id.ivFullPhoto);
        tvCaption = findViewById(R.id.tvCaption);
        tvDate = findViewById(R.id.tvDate);
        tvLocation = findViewById(R.id.tvLocation);
        tvType = findViewById(R.id.tvType);
        btnEditCaption = findViewById(R.id.btnEditCaption);
        btnShare = findViewById(R.id.btnShare);
        btnDelete = findViewById(R.id.btnDelete);

        btnEditCaption.setOnClickListener(v -> showEditCaptionDialog());
        btnShare.setOnClickListener(v -> sharePhoto());
        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_backward);
            getSupportActionBar().setTitle("Photo Details");
        }
    }

    private void loadPhotoData() {
        executor.execute(() -> {
            PhotoEntity loadedPhoto = database.photoDao().getPhotoById(photoId);

            runOnUiThread(() -> {
                if (loadedPhoto != null) {
                    photo = loadedPhoto;
                    displayPhotoData();
                } else {
                    Toast.makeText(this, "Photo not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        });
    }

    private void displayPhotoData() {
        if (photo == null) return;

        // Load full image with Glide
        File imageFile = new File(photo.getFilePath());

        RequestOptions requestOptions = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_photo_placeholder)
                .error(R.drawable.ic_photo_error)
                .centerCrop();

        if (imageFile.exists()) {
            Glide.with(this)
                    .load(imageFile)
                    .apply(requestOptions)
                    .into(ivFullPhoto);
        } else {
            // File doesn't exist, show error
            ivFullPhoto.setImageResource(R.drawable.ic_photo_error);
            Toast.makeText(this, "Image file not found", Toast.LENGTH_SHORT).show();
        }

        // Set caption
        if (photo.getCaption() != null && !photo.getCaption().isEmpty()) {
            tvCaption.setText(photo.getCaption());
        } else {
            tvCaption.setText("No caption");
        }

        // Set date
        tvDate.setText(dateFormat.format(new Date(photo.getTakenAt())));

        // Set location (if available)
        if (photo.getLatitude() != 0 && photo.getLongitude() != 0) {
            tvLocation.setText(String.format(Locale.getDefault(),
                    "📍 %.4f, %.4f", photo.getLatitude(), photo.getLongitude()));
        } else {
            tvLocation.setText("📍 Location not available");
        }

        // Set type
        tvType.setText(photo.getTypeDisplayName());
    }

    private void showEditCaptionDialog() {
        if (photo == null) return;

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Edit Caption");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_caption_input, null);
        TextInputEditText input = dialogView.findViewById(R.id.etCaption);
        input.setText(photo.getCaption());
        if (input.getText() != null) {
            input.setSelection(input.getText().length());
        }
        builder.setView(dialogView);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newCaption = input.getText().toString().trim();

            executor.execute(() -> {
                photo.setCaption(newCaption.isEmpty() ? null : newCaption);
                database.photoDao().update(photo);

                runOnUiThread(() -> {
                    tvCaption.setText(newCaption.isEmpty() ? "No caption" : newCaption);
                    Toast.makeText(this, "Caption updated", Toast.LENGTH_SHORT).show();
                });
            });
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void sharePhoto() {
        if (photo == null) return;

        File imageFile = new File(photo.getFilePath());
        if (!imageFile.exists()) {
            Toast.makeText(this, "Image file not found", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri imageUri = FileProvider.getUriForFile(this,
                    "com.afrinuts.farmos.fileprovider",
                    imageFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share Photo"));
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, "Error sharing photo: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void confirmDelete() {
        if (photo == null) return;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Photo")
                .setMessage("Are you sure you want to delete this photo?")
                .setPositiveButton("Delete", (dialog, which) -> deletePhoto())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deletePhoto() {
        if (photo == null) return;

        executor.execute(() -> {
            // Delete the actual file
            File imageFile = new File(photo.getFilePath());
            if (imageFile.exists()) {
                imageFile.delete();
            }

            // Delete thumbnail if exists
            if (photo.getThumbnailPath() != null) {
                File thumbFile = new File(photo.getThumbnailPath());
                if (thumbFile.exists()) {
                    thumbFile.delete();
                }
            }

            // Delete from database
            database.photoDao().delete(photo);

            runOnUiThread(() -> {
                Toast.makeText(this, "Photo deleted", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_photo_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_edit) {
            showEditCaptionDialog();
            return true;
        } else if (item.getItemId() == R.id.action_delete) {
            confirmDelete();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}