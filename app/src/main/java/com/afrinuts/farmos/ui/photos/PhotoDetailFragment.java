package com.afrinuts.farmos.ui.photos;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.PhotoEntity;
import com.afrinuts.farmos.data.repository.BaseRepository;
import com.afrinuts.farmos.data.repository.PhotoRepository;
import com.afrinuts.farmos.utils.SessionManager;
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

public class PhotoDetailFragment extends Fragment {
    private static final String ARG_PHOTO_ID = "photo_id";
    private long photoId;
    private PhotoEntity photo;
    private PhotoRepository repository;
    private ImageView image;
    private TextView caption;
    private TextView date;
    private TextView location;
    private TextView type;
    private MaterialButton edit;
    private MaterialButton share;
    private MaterialButton delete;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    public static PhotoDetailFragment newInstance(long photoId) {
        PhotoDetailFragment fragment = new PhotoDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_PHOTO_ID, photoId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_photo_detail, container, false);
        photoId = requireArguments().getLong(ARG_PHOTO_ID, -1);
        image = root.findViewById(R.id.ivFullPhoto);
        caption = root.findViewById(R.id.tvCaption);
        date = root.findViewById(R.id.tvDate);
        location = root.findViewById(R.id.tvLocation);
        type = root.findViewById(R.id.tvType);
        edit = root.findViewById(R.id.btnEditCaption);
        share = root.findViewById(R.id.btnShare);
        delete = root.findViewById(R.id.btnDelete);

        boolean canEdit = new SessionManager(requireContext()).canEditData();
        edit.setVisibility(canEdit ? View.VISIBLE : View.GONE);
        delete.setVisibility(canEdit ? View.VISIBLE : View.GONE);
        if (!canEdit) {
            ViewGroup.LayoutParams params = share.getLayoutParams();
            if (params instanceof android.widget.LinearLayout.LayoutParams) {
                android.widget.LinearLayout.LayoutParams linear = (android.widget.LinearLayout.LayoutParams) params;
                linear.width = ViewGroup.LayoutParams.MATCH_PARENT;
                linear.weight = 0;
                share.setLayoutParams(linear);
            }
        }
        edit.setOnClickListener(v -> showEditCaptionDialog());
        share.setOnClickListener(v -> sharePhoto());
        delete.setOnClickListener(v -> confirmDelete());

        repository = new PhotoRepository(requireContext(), 1);
        repository.getPhotoById(photoId).observe(getViewLifecycleOwner(), loaded -> {
            if (loaded == null) return;
            photo = loaded;
            displayPhoto();
        });
        return root;
    }

    private void displayPhoto() {
        if (photo == null || image == null) return;
        String path = photo.getFilePath();
        RequestOptions options = new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_photo_placeholder).error(R.drawable.ic_photo_error).centerCrop();
        if (path != null && (path.startsWith("http://") || path.startsWith("https://"))) {
            Glide.with(this).load(path).apply(options).into(image);
        } else if (path != null && new File(path).exists()) {
            Glide.with(this).load(new File(path)).apply(options).into(image);
        } else {
            image.setImageResource(R.drawable.ic_photo_error);
        }
        caption.setText(photo.getCaption() == null || photo.getCaption().isEmpty() ? "No caption" : photo.getCaption());
        date.setText(dateFormat.format(new Date(photo.getTakenAt())));
        location.setText(photo.getLatitude() != 0 && photo.getLongitude() != 0
                ? String.format(Locale.getDefault(), "%.4f, %.4f", photo.getLatitude(), photo.getLongitude())
                : "Location not available");
        type.setText(photo.getTypeDisplayName());
    }

    private void showEditCaptionDialog() {
        if (photo == null) return;
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_caption_input, null);
        TextInputEditText input = dialogView.findViewById(R.id.etCaption);
        input.setText(photo.getCaption());
        if (input.getText() != null) input.setSelection(input.getText().length());
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext()).setTitle("Edit Caption")
                .setView(dialogView, 0, 0, 0, 0).create();
        dialogView.findViewById(R.id.btnCancelCaption).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnSaveCaption).setOnClickListener(v -> {
            String value = input.getText() == null ? "" : input.getText().toString().trim();
            dialog.dismiss();
            repository.updatePhotoCaption(photoId, value, new BaseRepository.RepositoryCallback<Boolean>() {
                @Override public void onSuccess(Boolean result) {
                    if (isAdded()) requireActivity().runOnUiThread(() -> caption.setText(value.isEmpty() ? "No caption" : value));
                }
                @Override public void onError(String error) {
                    if (isAdded()) requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_SHORT).show());
                }
            });
        });
        dialog.setOnShowListener(ignored -> styleDialog(dialog));
        dialog.show();
    }

    private void sharePhoto() {
        if (photo == null) return;
        File file = new File(photo.getFilePath());
        if (!file.exists()) {
            Toast.makeText(requireContext(), "Image file not found", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Uri uri = FileProvider.getUriForFile(requireContext(), "com.afrinuts.farmos.fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND).setType("image/*");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share Photo"));
        } catch (IllegalArgumentException error) {
            Toast.makeText(requireContext(), "Error sharing photo: " + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void confirmDelete() {
        if (photo == null) return;
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext()).setTitle("Delete Photo")
                .setMessage("Are you sure you want to delete this photo?")
                .setPositiveButton("Delete", (ignored, which) -> repository.deletePhoto(photo, new BaseRepository.RepositoryCallback<Boolean>() {
                    @Override public void onSuccess(Boolean result) {
                        if (isAdded()) requireActivity().runOnUiThread(() -> {
                            Bundle resultBundle = new Bundle();
                            resultBundle.putLong(PhotoGalleryActivity.RESULT_DELETED_PHOTO_ID, photoId);
                            getParentFragmentManager().setFragmentResult(PhotoGalleryActivity.RESULT_PHOTO_DELETED, resultBundle);
                        });
                    }
                    @Override public void onError(String error) {
                        if (isAdded()) requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_SHORT).show());
                    }
                })).setNegativeButton("Cancel", null).create();
        dialog.setOnShowListener(ignored -> styleDialog(dialog));
        dialog.show();
    }

    private void styleDialog(AlertDialog dialog) {
        float density = getResources().getDisplayMetrics().density;
        Window window = dialog.getWindow();
        if (window != null) {
            GradientDrawable background = new GradientDrawable();
            background.setColor(Color.WHITE);
            background.setCornerRadius(dp(28));
            window.setBackgroundDrawable(background);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.BOTTOM;
            params.y = dp(16);
            params.dimAmount = .48f;
            window.setAttributes(params);
            window.setLayout(getResources().getDisplayMetrics().widthPixels - dp(32), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        TextView title = dialog.findViewById(androidx.appcompat.R.id.alertTitle);
        if (title != null) {
            title.setTextColor(0xFF1C1B1A);
            title.setTextSize(20);
            title.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.manrope_extrabold));
        }
        styleButton(dialog.getButton(AlertDialog.BUTTON_NEGATIVE), false, density);
        styleButton(dialog.getButton(AlertDialog.BUTTON_POSITIVE), true, density);
    }

    private void styleButton(Button button, boolean primary, float density) {
        if (button == null) return;
        GradientDrawable background = new GradientDrawable();
        background.setColor(primary ? 0xFF0E6B3C : 0xFFF3EAD8);
        background.setCornerRadius(999 * density);
        button.setBackground(background);
        button.setTextColor(primary ? Color.WHITE : 0xFF1C1B1A);
        button.setTextSize(15);
        button.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.manrope_extrabold));
        button.setAllCaps(false);
        button.setMinHeight(dp(52));
        button.setPadding(dp(10), 0, dp(10), 0);
        ViewGroup.LayoutParams raw = button.getLayoutParams();
        if (raw instanceof android.widget.LinearLayout.LayoutParams) {
            android.widget.LinearLayout.LayoutParams params = (android.widget.LinearLayout.LayoutParams) raw;
            params.width = 0;
            params.weight = 1;
            params.height = dp(52);
            params.setMargins(dp(4), 0, dp(4), 0);
            button.setLayoutParams(params);
        }
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override public void onDestroyView() {
        super.onDestroyView();
        if (repository != null) repository.shutdown();
    }
}
