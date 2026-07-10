package com.afrinuts.farmos.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BatchTraceabilityEntity;
import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.data.repository.BatchTraceabilityRepository;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BulkQRGenerator {

    private static final String TAG = "BulkQRGenerator";
    private final Context context;
    private final AppDatabase database;
    private final BatchTraceabilityRepository repository;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public BulkQRGenerator(Context context) {
        this.context = context;
        this.database = AppDatabase.getInstance(context);
        this.repository = new BatchTraceabilityRepository(context, 1);
    }

    public interface BulkQRCallback {
        void onSuccess(File pdfFile);
        void onError(String error);
        void onProgress(int current, int total);
    }

    /**
     * Generate QR codes for all batches in a shipment
     */
    public void generateBulkQRForShipment(List<Long> batchIds, String shipmentName, BulkQRCallback callback) {
        new Thread(() -> {
            try {
                List<BatchTraceabilityEntity> batches = new ArrayList<>();
                for (Long id : batchIds) {
                    BatchTraceabilityEntity traceability = repository.getTraceabilityByIdSync(id);
                    if (traceability != null) {
                        batches.add(traceability);
                    }
                }

                if (batches.isEmpty()) {
                    callback.onError("No batches found for the selected shipment");
                    return;
                }

                // Generate PDF
                File pdfFile = createBulkQRPDF(batches, shipmentName, callback);
                if (pdfFile != null) {
                    callback.onSuccess(pdfFile);
                } else {
                    callback.onError("Failed to generate PDF");
                }

            } catch (Exception e) {
                Log.e(TAG, "Error generating bulk QR", e);
                callback.onError("Error: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Generate QR codes for all batches in a date range
     */
    public void generateBulkQRForDateRange(long startDate, long endDate, String shipmentName, BulkQRCallback callback) {
        new Thread(() -> {
            try {
                // Get all traceabilities in date range
                List<BatchTraceabilityEntity> batches = database.batchTraceabilityDao()
                        .getBatchesInDateRange(startDate, endDate);

                if (batches.isEmpty()) {
                    callback.onError("No batches found for the selected date range");
                    return;
                }

                // Generate PDF
                File pdfFile = createBulkQRPDF(batches, shipmentName, callback);
                if (pdfFile != null) {
                    callback.onSuccess(pdfFile);
                } else {
                    callback.onError("Failed to generate PDF");
                }

            } catch (Exception e) {
                Log.e(TAG, "Error generating bulk QR for date range", e);
                callback.onError("Error: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Generate QR codes for all batches for a specific supplier
     */
    public void generateBulkQRForSupplier(long supplierId, String shipmentName, BulkQRCallback callback) {
        new Thread(() -> {
            try {
                List<BatchTraceabilityEntity> batches = repository.getBySupplierIdSync(supplierId);

                if (batches.isEmpty()) {
                    callback.onError("No batches found for this supplier");
                    return;
                }

                File pdfFile = createBulkQRPDF(batches, shipmentName, callback);
                if (pdfFile != null) {
                    callback.onSuccess(pdfFile);
                } else {
                    callback.onError("Failed to generate PDF");
                }

            } catch (Exception e) {
                Log.e(TAG, "Error generating bulk QR for supplier", e);
                callback.onError("Error: " + e.getMessage());
            }
        }).start();
    }

    private File createBulkQRPDF(List<BatchTraceabilityEntity> batches, String shipmentName, BulkQRCallback callback) throws Exception {
        // Create PDF file
        String fileName = "Bulk_QR_" + shipmentName.replace(" ", "_") + ".pdf";
        File pdfFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);

        // Ensure directory exists
        if (!pdfFile.getParentFile().exists()) {
            pdfFile.getParentFile().mkdirs();
        }

        // Initialize PDF writer
        PdfWriter writer = new PdfWriter(new FileOutputStream(pdfFile));
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc, PageSize.A4);
        document.setMargins(20, 20, 20, 20);

        // Add header
        addHeader(document, shipmentName, batches.size());

        // Create table with 3 columns (for QR codes)
        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1}));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setHorizontalAlignment(HorizontalAlignment.CENTER);

        int total = batches.size();
        int processed = 0;

        for (BatchTraceabilityEntity batch : batches) {
            // Generate QR code for this batch
            String qrUrl = batch.getQrCodeUrl();
            Bitmap qrBitmap = QRCodeGenerator.generateQRCodeWithLabel(qrUrl, batch.getBatchNumber());

            if (qrBitmap != null) {
                // Convert bitmap to byte array for iText
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] imageData = stream.toByteArray();
                Image qrImage = new Image(com.itextpdf.io.image.ImageDataFactory.create(imageData));
                qrImage.setAutoScale(true);
                qrImage.setWidth(120);
                qrImage.setHeight(120);

                // Create cell with QR code and batch info
                Cell cell = new Cell();
                cell.add(qrImage);
                cell.add(new Paragraph(batch.getBatchNumber())
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(10));

                if (batch.getGrade() != null) {
                    cell.add(new Paragraph("Grade: " + batch.getGrade())
                            .setTextAlignment(TextAlignment.CENTER)
                            .setFontSize(8)
                            .setFontColor(new DeviceRgb(100, 100, 100)));
                }

                if (batch.getWeightKg() > 0) {
                    cell.add(new Paragraph(String.format(Locale.getDefault(), "%.0f kg", batch.getWeightKg()))
                            .setTextAlignment(TextAlignment.CENTER)
                            .setFontSize(8)
                            .setFontColor(new DeviceRgb(100, 100, 100)));
                }

                cell.setPadding(8);
                cell.setBackgroundColor(new DeviceRgb(250, 250, 250));
                cell.setBorder(null);

                table.addCell(cell);
                processed++;
                callback.onProgress(processed, total);
            } else {
                Log.w(TAG, "Failed to generate QR code for batch: " + batch.getBatchNumber());
            }
        }

        // Add table to document
        document.add(table);

        // Add footer
        addFooter(document, batches);

        // Close document
        document.close();

        Log.d(TAG, "PDF generated: " + pdfFile.getAbsolutePath());
        return pdfFile;
    }

    private void addHeader(Document document, String shipmentName, int totalBatches) {
        // Title
        Paragraph title = new Paragraph("AfriNuts Export - Bulk Traceability QR Codes")
                .setFontSize(18)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(new DeviceRgb(90, 116, 17)); // Primary color

        // Subtitle
        String dateStr = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(new Date());
        Paragraph subtitle = new Paragraph("Shipment: " + shipmentName + " | Date: " + dateStr + " | Total Batches: " + totalBatches)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(new DeviceRgb(100, 100, 100));

        // Instructions
        Paragraph instructions = new Paragraph("Scan each QR code to view detailed traceability information for each batch.")
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(new DeviceRgb(150, 150, 150))
                .setMarginBottom(20);

        document.add(title);
        document.add(subtitle);
        document.add(instructions);
    }

    private void addFooter(Document document, List<BatchTraceabilityEntity> batches) {
        // Add a line separator
        Paragraph separator = new Paragraph("───────────────────────────────────────────────")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(new DeviceRgb(200, 200, 200))
                .setMarginTop(20);
        document.add(separator);

        // Footer text
        String footerText = "This document was generated from Farm OS - AfriNuts Export's proprietary traceability system. " +
                "Data is verified and auditable. " +
                "Total batches: " + batches.size();

        Paragraph footer = new Paragraph(footerText)
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(new DeviceRgb(150, 150, 150))
                .setMarginTop(5);

        document.add(footer);

        // Verification note
        Paragraph verification = new Paragraph("✓ Farm OS Verified")
                .setFontSize(8)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(new DeviceRgb(90, 116, 17))
                .setMarginTop(5);

        document.add(verification);
    }

    /**
     * Share the generated PDF
     */
    public void sharePDF(File pdfFile) {
        if (pdfFile == null || !pdfFile.exists()) {
            return;
        }

        try {
            Uri pdfUri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".fileprovider", pdfFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(Intent.createChooser(shareIntent, "Share Bulk QR Codes"));
        } catch (Exception e) {
            Log.e(TAG, "Error sharing PDF", e);
        }
    }
}