package com.afrinuts.farmos.ui.photos;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.PhotoEntity;
import com.afrinuts.farmos.ui.BaseActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PhotoGalleryActivity extends BaseActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 100;
    private static final int REQUEST_IMAGE_PICK = 101;

    private RecyclerView recyclerView;
    private PhotoAdapter adapter;
    private View emptyView;
    private FloatingActionButton fabAddPhoto;

    private AppDatabase database;
    private List<PhotoEntity> photos = new ArrayList<>();

    private long blockId = -1;
    private long taskId = -1;
    private long expenseId = -1;
    private String currentPhotoPath;

    @Override
    protected boolean checkRoleAccess() {
        // Owners and Managers can access photos
        return !sessionManager.isInvestor();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_gallery);

        // Get IDs from intent
        blockId = getIntent().getLongExtra("block_id", -1);
        taskId = getIntent().getLongExtra("task_id", -1);
        expenseId = getIntent().getLongExtra("expense_id", -1);

        // Initialize views
        initViews();

        // Setup toolbar
        setupToolbar();

        // Initialize database
        database = AppDatabase.getInstance(this);

        // Setup recycler view
        setupRecyclerView();

        // Load photos
        loadPhotos();

        // Setup FAB
        setupFab();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.emptyView);
        fabAddPhoto = findViewById(R.id.fabAddPhoto);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
        adapter = new PhotoAdapter(photos, photo -> {
            // Open photo detail
            Intent intent = new Intent(this, PhotoDetailActivity.class);
            intent.putExtra("photo_id", photo.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
    }

    private void setupFab() {
        fabAddPhoto.setOnClickListener(v -> showPhotoSourceDialog());
    }

    private void showPhotoSourceDialog() {
        String[] options = {"Take Photo", "Choose from Gallery"};

        new androidx.appcompat.app.AlertDialog.Builder(this)
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
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.afrinuts.farmos.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
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
                // Photo taken, save to database
                savePhotoToDatabase(currentPhotoPath);
            } else if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                Uri selectedImageUri = data.getData();
                // Handle gallery image
                String imagePath = getPathFromUri(selectedImageUri);
                savePhotoToDatabase(imagePath);
            }
        }
    }

    private void savePhotoToDatabase(String imagePath) {
        // Show caption dialog
        showCaptionDialog(imagePath);
    }

    private void showCaptionDialog(String imagePath) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Add Caption");

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Enter caption...");
        builder.setView(input);

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

                long id = database.photoDao().insert(photo);

                runOnUiThread(() -> {
                    if (id > 0) {
                        Toast.makeText(this, "Photo saved", Toast.LENGTH_SHORT).show();
                        loadPhotos(); // Refresh list
                    } else {
                        Toast.makeText(this, "Error saving photo", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private String getPathFromUri(Uri uri) {
        // Implement based on your needs
        return uri.getPath();
    }

    private void loadPhotos() {
        new Thread(() -> {
            long farmId = database.farmDao().getFirstFarm().getId();

            if (blockId != -1) {
                photos = database.photoDao().getPhotosByBlockId(blockId);
            } else if (taskId != -1) {
                photos = database.photoDao().getPhotosByTaskId(taskId);
            } else if (expenseId != -1) {
                photos = database.photoDao().getPhotosByExpenseId(expenseId);
            } else {
                photos = database.photoDao().getPhotosByFarmId(farmId);
            }

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
            // Implement delete all
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}