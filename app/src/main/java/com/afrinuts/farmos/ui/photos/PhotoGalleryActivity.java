package com.afrinuts.farmos.ui.photos;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.PhotoEntity;
import com.afrinuts.farmos.ui.BaseActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.IOException;
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

        Log.d(TAG, "Intent extras - blockId: " + blockId + ", taskId: " + taskId + ", expenseId: " + expenseId);

        initViews();
        setupToolbar();
        database = AppDatabase.getInstance(this);
        setupRecyclerView();
        loadPhotos();
        setupFab();
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
        Log.d(TAG, "Toolbar setup complete");
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
            Log.d(TAG, "Photo clicked: " + photo.getId() + " at path: " + photo.getFilePath());
            Intent intent = new Intent(this, PhotoDetailActivity.class);
            intent.putExtra(PhotoDetailActivity.EXTRA_PHOTO_ID, photo.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
        Log.d(TAG, "RecyclerView setup complete");
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
                        Log.d(TAG, "User selected: Take Photo");
                        dispatchTakePictureIntent();
                    } else {
                        Log.d(TAG, "User selected: Choose from Gallery");
                        dispatchPickPictureIntent();
                    }
                })
                .show();
    }

    private void dispatchTakePictureIntent() {
        Log.d(TAG, "dispatchTakePictureIntent called");
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
                Log.d(TAG, "Photo file created at: " + photoFile.getAbsolutePath());
            } catch (IOException ex) {
                Log.e(TAG, "Error creating image file", ex);
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
                return;
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.afrinuts.farmos.fileprovider",
                        photoFile);
                Log.d(TAG, "Photo URI: " + photoURI.toString());
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                Log.d(TAG, "Camera intent launched");
            }
        } else {
            Log.e(TAG, "No camera app found");
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }

    private void dispatchPickPictureIntent() {
        Log.d(TAG, "dispatchPickPictureIntent called");
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
        Log.d(TAG, "createImageFile: " + currentPhotoPath);
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult - requestCode: " + requestCode + ", resultCode: " + resultCode);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                Log.d(TAG, "Photo captured successfully. Path: " + currentPhotoPath);
                savePhotoToDatabase(currentPhotoPath);
            } else if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                Uri selectedImageUri = data.getData();
                Log.d(TAG, "Image selected from gallery: " + selectedImageUri);
                Toast.makeText(this, "Gallery picker coming soon", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.d(TAG, "Activity result not OK - code: " + resultCode);
        }
    }

    private void savePhotoToDatabase(String imagePath) {
        Log.d(TAG, "savePhotoToDatabase called with path: " + imagePath);
        showCaptionDialog(imagePath);
    }

    private void showCaptionDialog(String imagePath) {
        Log.d(TAG, "showCaptionDialog called");

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Add Caption");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_caption_input, null);
        TextInputEditText input = dialogView.findViewById(R.id.etCaption);
        builder.setView(dialogView);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String caption = input.getText().toString().trim();
            Log.d(TAG, "Caption entered: " + caption);

            new Thread(() -> {
                try {
                    long farmId = database.farmDao().getFirstFarm().getId();
                    Log.d(TAG, "Farm ID: " + farmId);

                    PhotoEntity photo = new PhotoEntity(
                            farmId,
                            blockId != -1 ? blockId : null,
                            taskId != -1 ? taskId : null,
                            expenseId != -1 ? expenseId : null,
                            imagePath,
                            caption.isEmpty() ? null : caption,
                            System.currentTimeMillis()
                    );

                    long id = database.photoDao().insert(photo);
                    Log.d(TAG, "Photo inserted with ID: " + id);

                    runOnUiThread(() -> {
                        if (id > 0) {
                            Toast.makeText(this, "Photo saved", Toast.LENGTH_SHORT).show();
                            loadPhotos(); // Refresh list
                        } else {
                            Toast.makeText(this, "Error saving photo", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error saving photo to database", e);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            Log.d(TAG, "Caption dialog cancelled");
            dialog.cancel();
        });

        builder.show();
    }

    private void loadPhotos() {
        Log.d(TAG, "loadPhotos started");
        new Thread(() -> {
            List<PhotoEntity> loadedPhotos;

            if (blockId != -1) {
                loadedPhotos = database.photoDao().getPhotosByBlockId(blockId);
                Log.d(TAG, "Loading photos for block: " + blockId);
            } else if (taskId != -1) {
                loadedPhotos = database.photoDao().getPhotosByTaskId(taskId);
                Log.d(TAG, "Loading photos for task: " + taskId);
            } else if (expenseId != -1) {
                loadedPhotos = database.photoDao().getPhotosByExpenseId(expenseId);
                Log.d(TAG, "Loading photos for expense: " + expenseId);
            } else {
                long farmId = database.farmDao().getFirstFarm().getId();
                loadedPhotos = database.photoDao().getPhotosByFarmId(farmId);
                Log.d(TAG, "Loading all photos for farm: " + farmId);
            }

            photos.clear();
            photos.addAll(loadedPhotos);
            Log.d(TAG, "Loaded " + loadedPhotos.size() + " photos");

            runOnUiThread(() -> {
                adapter.updatePhotos(photos);

                if (photos.isEmpty()) {
                    emptyView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            });
        }).start();
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
}