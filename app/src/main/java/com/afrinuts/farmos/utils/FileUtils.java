package com.afrinuts.farmos.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileUtils {

    public static String getFileName(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            ContentResolver contentResolver = context.getContentResolver();
            String displayName = android.provider.DocumentsContract.getDocumentId(uri);
            if (displayName != null) {
                // Try to get the actual filename
                String[] split = displayName.split(":");
                if (split.length > 1) {
                    result = split[1];
                } else {
                    result = displayName;
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    public static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    public static File createImageFile(Context context) throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "DOC_" + timeStamp + "_";
        File storageDir = context.getExternalFilesDir(null);

        if (storageDir == null) {
            throw new IOException("External storage directory not available");
        }

        // Ensure directory exists
        if (!storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                throw new IOException("Failed to create storage directory");
            }
        }

        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }
}