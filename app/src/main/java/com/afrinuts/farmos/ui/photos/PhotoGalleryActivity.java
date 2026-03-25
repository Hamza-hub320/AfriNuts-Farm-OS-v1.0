package com.afrinuts.farmos.ui.photos;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.PhotoEntity;
import com.afrinuts.farmos.data.repository.BaseRepository;
import com.afrinuts.farmos.data.repository.PhotoRepository;
import com.afrinuts.farmos.ui.BaseActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PhotoGalleryActivity extends BaseActivity {

    private static final String TAG = "PhotoGallery";
    private static final int REQUEST_IMAGE_CAPTURE = 100;
    private static final int REQUEST_IMAGE_PICK = 101;

    private RecyclerView recyclerView;
    private PhotoAdapter adapter;
    private LinearLayout emptyView;
    private FloatingActionButton fabAddPhoto;

    private AppDatabase database;
    private PhotoRepository photoRepository;
    private long farmId = -1;
    private List<PhotoEntity> photos = new ArrayList<>();

    private long blockId = -1;
    private long taskId = -1;
    private long expenseId = -1;
    private String currentPhotoPath;

    @Override
    protected boolean checkRoleAccess() {
        return !sessionManager.isInvestor();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_gallery);

        Log.d(TAG, "onCreate started");

        blockId = getIntent().getLongExtra("block_id", -1);
        taskId = getIntent().getLongExtra("task_id", -1);
        expenseId = getIntent().getLongExtra("expense_id", -1);

        database = AppDatabase.getInstance(this);

        // Get farm ID first
        new Thread(() -> {
            farmId = database.farmDao().getFirstFarm().getId();
            photoRepository = new PhotoRepository(PhotoGalleryActivity.this, farmId);

            runOnUiThread(() -> {
                initViews();
                setupToolbar();
                setupRecyclerView();
                loadPhotos();
                setupFab();
            });
        }).start();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.emptyView);
        fabAddPhoto = findViewById(R.id.fabAddPhoto);
        Log.d(TAG, "Views initialized");
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_backward);
            getSupportActionBar().setTitle(getToolbarTitle());
        }
    }

    private String getToolbarTitle() {
        if (blockId != -1) return "Block Photos";
        if (taskId != -1) return "Task Photos";
        if (expenseId != -1) return "Expense Photos";
        return "Photo Gallery";
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new PhotoAdapter(photo -> {
            Intent intent = new Intent(this, PhotoDetailActivity.class);
            intent.putExtra(PhotoDetailActivity.EXTRA_PHOTO_ID, photo.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
    }

    private void setupFab() {
        fabAddPhoto.setOnClickListener(v -> {
            Log.d(TAG, "FAB clicked");
            showPhotoSourceDialog();
        });
    }

    private void showPhotoSourceDialog() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Add Photo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        dispatchTakePictureIntent();
                    } else {
                        dispatchPickPictureIntent();
                    }
                })
                .show();
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG, "Error creating image file", ex);
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
                return;
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.afrinuts.farmos.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }

    private void dispatchPickPictureIntent() {
        Intent pickPictureIntent = new Intent(Intent.ACTION_PICK);
        pickPictureIntent.setType("image/*");
        startActivityForResult(pickPictureIntent, REQUEST_IMAGE_PICK);
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                // Camera photo taken
                savePhotoToDatabase(currentPhotoPath);
            } else if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                // Gallery image selected
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    saveGalleryImageToFile(selectedImageUri);
                } else {
                    Toast.makeText(this, "Failed to get image", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * Save gallery image to a local file before saving to database
     */
    private void saveGalleryImageToFile(Uri uri) {
        try {
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

            if (storageDir == null) {
                throw new IOException("Storage directory not available");
            }

            if (!storageDir.exists()) {
                boolean created = storageDir.mkdirs();
                if (!created) {
                    throw new IOException("Failed to create storage directory");
                }
            }

            String fileName = "GALLERY_" + System.currentTimeMillis() + ".jpg";
            File destFile = new File(storageDir, fileName);

            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                throw new IOException("Cannot open input stream from URI");
            }

            OutputStream outputStream = new FileOutputStream(destFile);

            byte[] buffer = new byte[4096];
            int length;

            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.flush();
            outputStream.close();
            inputStream.close();

            // Validate file
            if (!destFile.exists() || destFile.length() == 0) {
                throw new IOException("File copy failed or empty file");
            }

            Log.d(TAG, "✅ Gallery image saved: " + destFile.getAbsolutePath());
            Log.d(TAG, "File size: " + destFile.length() + " bytes");

            savePhotoToDatabase(destFile.getAbsolutePath());

        } catch (Exception e) {
            Log.e(TAG, "❌ Error saving gallery image", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    private void savePhotoToDatabase(String imagePath) {
        showCaptionDialog(imagePath);
    }

    private void showCaptionDialog(String imagePath) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Add Caption");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_caption_input, null);
        TextInputEditText input = dialogView.findViewById(R.id.etCaption);
        builder.setView(dialogView);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String caption = input.getText().toString().trim();

            new Thread(() -> {
                long farmId = database.farmDao().getFirstFarm().getId();

                PhotoEntity photo = new PhotoEntity(
                        farmId,
                        blockId != -1 ? blockId : null,
                        taskId != -1 ? taskId : null,
                        expenseId != -1 ? expenseId : null,
                        imagePath,
                        caption.isEmpty() ? null : caption,
                        System.currentTimeMillis()
                );

                File photoFile = new File(imagePath);

                // FIXED: Pass all 3 required arguments
                photoRepository.insertPhoto(photo, photoFile, new PhotoRepository.PhotoUploadCallback() {
                    @Override
                    public void onSuccess(long id, String downloadUrl) {
                        runOnUiThread(() -> {
                            Toast.makeText(PhotoGalleryActivity.this,
                                    "Photo saved and uploaded!", Toast.LENGTH_SHORT).show();
                            loadPhotos();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(PhotoGalleryActivity.this,
                                    "Error: " + error, Toast.LENGTH_LONG).show();
                            loadPhotos(); // Still refresh to show local photo
                        });
                    }

                    @Override
                    public void onProgress(double progress) {
                        // Optional: show progress in UI
                        Log.d(TAG, "Upload progress: " + progress + "%");
                    }
                });
            }).start();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void loadPhotos() {
        if (photoRepository == null) return;

        if (blockId != -1) {
            photoRepository.getPhotosForBlock(blockId).observe(this,
                    new Observer<List<PhotoEntity>>() {
                        @Override
                        public void onChanged(List<PhotoEntity> loadedPhotos) {
                            updatePhotoList(loadedPhotos);
                        }
                    });
        } else if (taskId != -1) {
            photoRepository.getPhotosForTask(taskId).observe(this,
                    new Observer<List<PhotoEntity>>() {
                        @Override
                        public void onChanged(List<PhotoEntity> loadedPhotos) {
                            updatePhotoList(loadedPhotos);
                        }
                    });
        } else if (expenseId != -1) {
            photoRepository.getPhotosForExpense(expenseId).observe(this,
                    new Observer<List<PhotoEntity>>() {
                        @Override
                        public void onChanged(List<PhotoEntity> loadedPhotos) {
                            updatePhotoList(loadedPhotos);
                        }
                    });
        } else {
            photoRepository.getPhotosForFarm().observe(this,
                    new Observer<List<PhotoEntity>>() {
                        @Override
                        public void onChanged(List<PhotoEntity> loadedPhotos) {
                            updatePhotoList(loadedPhotos);
                        }
                    });
        }
    }

    private void updatePhotoList(List<PhotoEntity> loadedPhotos) {
        photos.clear();
        photos.addAll(loadedPhotos);
        adapter.updatePhotos(photos);

        if (photos.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_photo_gallery, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_delete_all) {
            Toast.makeText(this, "Delete all coming soon", Toast.LENGTH_SHORT).show();
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
    }
}