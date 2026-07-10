package com.afrinuts.farmos.utils;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class QRCodeGenerator {

    private static final String TAG = "QRCodeGenerator";
    private static final int QR_CODE_SIZE = 400;
    private static final String CHARACTER_SET = "UTF-8";

    public static Bitmap generateQRCode(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }

        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, CHARACTER_SET);
            hints.put(EncodeHintType.MARGIN, 2);

            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE, hints);

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            return bitmap;
        } catch (WriterException e) {
            Log.e(TAG, "Error generating QR code", e);
            return null;
        }
    }

    public static void shareQRCode(Context context, Bitmap qrBitmap, String batchNumber) {
        try {
            // Save to cache
            File cacheFile = new File(context.getCacheDir(), "qr_" + batchNumber + ".png");
            FileOutputStream fos = new FileOutputStream(cacheFile);
            qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(context,
                    context.getPackageName() + ".fileprovider", cacheFile);

            android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(android.content.Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);

            String shareText = "🌿 AfriNuts Traceability\n" +
                    "Batch: " + batchNumber + "\n" +
                    "🔗 Trace: https://www.afrinutsexport.com/trace/" + batchNumber;

            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareText);

            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share QR Code"));

        } catch (Exception e) {
            Log.e("QRCodeGenerator", "Error sharing QR code", e);
            Toast.makeText(context, "Error sharing QR code", Toast.LENGTH_SHORT).show();
        }
    }

    // ✅ Add this method for saving QR code to gallery
    public static String saveQRCodeToGallery(Context context, Bitmap qrBitmap, String batchNumber) {
        if (qrBitmap == null) {
            Log.e(TAG, "QR bitmap is null");
            return null;
        }

        try {
            String fileName = "QR_" + batchNumber + ".png";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ - Use MediaStore
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AfriNuts_QR");

                Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    FileOutputStream fos = (FileOutputStream) context.getContentResolver().openOutputStream(uri);
                    qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    if (fos != null) {
                        fos.close();
                    }
                    return uri.toString();
                }
            } else {
                // Android 9 and below
                File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File qrFolder = new File(directory, "AfriNuts_QR");
                if (!qrFolder.exists()) {
                    qrFolder.mkdirs();
                }

                File file = new File(qrFolder, fileName);
                FileOutputStream fos = new FileOutputStream(file);
                qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();

                // Scan file for gallery
                android.media.MediaScannerConnection.scanFile(context,
                        new String[]{file.getAbsolutePath()}, null, null);

                return file.getAbsolutePath();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error saving QR code", e);
        }
        return null;
    }

    // ✅ Add method for QR code with label
    public static Bitmap generateQRCodeWithLabel(String data, String label) {
        Bitmap qrBitmap = generateQRCode(data);
        if (qrBitmap == null) return null;

        // Create a larger bitmap with label
        int width = qrBitmap.getWidth();
        int height = qrBitmap.getHeight() + 80;

        Bitmap resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(resultBitmap);

        // Draw white background
        canvas.drawColor(Color.WHITE);

        // Draw QR code
        canvas.drawBitmap(qrBitmap, 0, 0, null);

        // Draw label
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(24);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        canvas.drawText(label, width / 2f, qrBitmap.getHeight() + 50, paint);

        return resultBitmap;
    }
}