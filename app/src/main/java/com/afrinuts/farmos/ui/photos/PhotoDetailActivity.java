package com.afrinuts.farmos.ui.photos;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.Observer;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.PhotoEntity;
import com.afrinuts.farmos.data.repository.BaseRepository;
import com.afrinuts.farmos.data.repository.PhotoRepository;
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
    private PhotoRepository photoRepository;
    private long farmId = 1; // Will be updated from photo
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
        return com.afrinuts.farmos.utils.AccessMap.canOpenScreen(
                getClass().getSimpleName(), sessionManager.getUserRole());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_detail);

        photoId = getIntent().getLongExtra(EXTRA_PHOTO_ID, -1);
        if (photoId == -1) {
            Toast.makeText(this, "Photo not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        photoRepository = new PhotoRepository(this, farmId);

        initViews();
        setupToolbar();
        loadPhotoData();
    }

    private void initViews() {
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
        ((TextView) findViewById(R.id.farmDetailTitle)).setText("Photo details");
        findViewById(R.id.farmDetailBack).setOnClickListener(v -> finish());
        findViewById(R.id.farmDetailEdit).setVisibility(View.INVISIBLE);
        findViewById(R.id.farmDetailMore).setVisibility(View.INVISIBLE);
    }

    private void loadPhotoData() {
        photoRepository.getPhotoById(photoId).observe(this, new Observer<PhotoEntity>() {
            @Override
            public void onChanged(PhotoEntity loadedPhoto) {
                if (loadedPhoto != null) {
                    photo = loadedPhoto;
                    long correctFarmId = photo.getFarmId();
                    if (correctFarmId != farmId) {
                        farmId = correctFarmId;
                        photoRepository = new PhotoRepository(PhotoDetailActivity.this, farmId);
                    }
                    displayPhotoData();
                } else {
                    Toast.makeText(PhotoDetailActivity.this,
                            "Photo not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });
    }

    private void displayPhotoData() {
        if (photo == null) return;

        // FIXED: Use the filePath from Firestore (which now contains the cloud URL)
        String imagePath = photo.getFilePath();

        RequestOptions requestOptions = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_photo_placeholder)
                .error(R.drawable.ic_photo_error)
                .centerCrop();

        // Check if it's a cloud URL (starts with http) or local file
        if (imagePath != null && (imagePath.startsWith("http://") || imagePath.startsWith("https://"))) {
            // Load from cloud URL
            Glide.with(this)
                    .load(imagePath)
                    .apply(requestOptions)
                    .into(ivFullPhoto);
            Log.d(TAG, "Loading photo from cloud URL: " + imagePath);
        } else if (imagePath != null) {
            // Try local file as fallback
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                Glide.with(this)
                        .load(imageFile)
                        .apply(requestOptions)
                        .into(ivFullPhoto);
                Log.d(TAG, "Loading photo from local file: " + imagePath);
            } else {
                ivFullPhoto.setImageResource(R.drawable.ic_photo_error);
                Log.e(TAG, "Photo file not found at: " + imagePath);
            }
        } else {
            ivFullPhoto.setImageResource(R.drawable.ic_photo_error);
            Log.e(TAG, "No image path available");
        }

        // Set caption
        tvCaption.setText(photo.getCaption() != null ? photo.getCaption() : "No caption");

        // Set date
        tvDate.setText(dateFormat.format(new Date(photo.getTakenAt())));

        // Set location (if available)
        if (photo.getLatitude() != 0 && photo.getLongitude() != 0) {
            tvLocation.setText(String.format(Locale.getDefault(),
                    " %.4f, %.4f", photo.getLatitude(), photo.getLongitude()));
        } else {
            tvLocation.setText("Location not available");
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
        builder.setView(dialogView, 0, 0, 0, 0);

        AlertDialog dialog = builder.create();
        dialogView.findViewById(R.id.btnCancelCaption).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnSaveCaption).setOnClickListener(v -> {
            String newCaption = input.getText().toString().trim();
            dialog.dismiss();
            photoRepository.updatePhotoCaption(photoId, newCaption,
                    new BaseRepository.RepositoryCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            runOnUiThread(() -> {
                                tvCaption.setText(newCaption.isEmpty() ? "No caption" : newCaption);
                                Toast.makeText(PhotoDetailActivity.this,
                                        "Caption updated", Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() ->
                                    Toast.makeText(PhotoDetailActivity.this,
                                            "Error: " + error, Toast.LENGTH_SHORT).show()
                            );
                        }
                    });
        });
        dialog.setOnShowListener(ignored -> styleStandardDialog(dialog));
        dialog.show();
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

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Photo")
                .setMessage("Are you sure you want to delete this photo?")
                .setPositiveButton("Delete", (ignored, which) -> deletePhoto())
                .setNegativeButton("Cancel", null)
                .create();
        dialog.setOnShowListener(ignored -> styleStandardDialog(dialog));
        dialog.show();
    }

    private void styleStandardDialog(AlertDialog dialog) {
        float density = getResources().getDisplayMetrics().density;
        Window window = dialog.getWindow();
        if (window != null) {
            GradientDrawable sheet = new GradientDrawable();
            sheet.setColor(Color.WHITE);
            sheet.setCornerRadius(dp(28));
            window.setBackgroundDrawable(sheet);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.BOTTOM;
            params.y = dp(16);
            params.dimAmount = .48f;
            window.setAttributes(params);
            window.setLayout(getResources().getDisplayMetrics().widthPixels - dp(32),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView title = dialog.findViewById(androidx.appcompat.R.id.alertTitle);
        if (title != null) {
            title.setTextColor(0xFF1C1B1A);
            title.setTextSize(20);
            title.setTypeface(ResourcesCompat.getFont(this, R.font.manrope_extrabold));
        }
        styleDialogButton(dialog.getButton(AlertDialog.BUTTON_NEGATIVE), false, density);
        styleDialogButton(dialog.getButton(AlertDialog.BUTTON_POSITIVE), true, density);
    }

    private void styleDialogButton(Button button, boolean primary, float density) {
        if (button == null) return;
        GradientDrawable background = new GradientDrawable();
        background.setColor(primary ? 0xFF0E6B3C : 0xFFF3EAD8);
        background.setCornerRadius(999 * density);
        button.setBackground(background);
        button.setTextColor(primary ? Color.WHITE : 0xFF1C1B1A);
        button.setTextSize(15);
        button.setTypeface(ResourcesCompat.getFont(this, R.font.manrope_extrabold));
        button.setAllCaps(false);
        button.setMinHeight(dp(52));
        button.setPadding(dp(10), 0, dp(10), 0);
        button.setElevation(0);
        ViewGroup.LayoutParams rawParams = button.getLayoutParams();
        if (rawParams instanceof android.widget.LinearLayout.LayoutParams) {
            android.widget.LinearLayout.LayoutParams params =
                    (android.widget.LinearLayout.LayoutParams) rawParams;
            params.width = 0;
            params.weight = 1;
            params.height = dp(52);
            params.setMargins(dp(4), 0, dp(4), 0);
            button.setLayoutParams(params);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void deletePhoto() {
        if (photo == null) return;

        photoRepository.deletePhoto(photo, new BaseRepository.RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                runOnUiThread(() -> {
                    Toast.makeText(PhotoDetailActivity.this,
                            "Photo deleted", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        Toast.makeText(PhotoDetailActivity.this,
                                "Error: " + error, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (photoRepository != null) {
            photoRepository.shutdown();
        }
        executor.shutdown();
    }
}
