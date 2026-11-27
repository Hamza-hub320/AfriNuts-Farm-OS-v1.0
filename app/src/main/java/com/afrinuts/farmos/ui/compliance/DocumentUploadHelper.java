package com.afrinuts.farmos.ui.compliance;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

final class DocumentUploadHelper {
    private DocumentUploadHelper() { }

    static boolean isSupported(Context context, Uri uri) {
        String mime = context.getContentResolver().getType(uri);
        return "application/pdf".equalsIgnoreCase(mime)
                || (mime != null && mime.toLowerCase(java.util.Locale.ROOT).startsWith("image/"));
    }

    static File copyToCache(Context context, Uri uri) throws IOException {
        String name = fileName(context, uri);
        String suffix = name.contains(".") ? name.substring(name.lastIndexOf('.')) : ".bin";
        File file = File.createTempFile("document_upload_", suffix, context.getCacheDir());
        try (InputStream input = context.getContentResolver().openInputStream(uri);
             FileOutputStream output = new FileOutputStream(file)) {
            if (input == null) throw new IOException("Unable to open selected file");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
        }
        return file;
    }

    static String fileName(Context context, Uri uri) {
        String result = null;
        try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) result = cursor.getString(index);
            }
        } catch (Exception ignored) { }
        if (result == null || result.trim().isEmpty()) result = uri.getLastPathSegment();
        if (result == null || result.trim().isEmpty()) result = "document";
        return result.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
