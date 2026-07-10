package com.afrinuts.farmos.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.afrinuts.farmos.data.local.entity.BatchTraceabilityEntity;
import com.afrinuts.farmos.data.local.entity.MasterBatchTraceabilityEntity;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.SupplierEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class DocumentGenerator {

    private static final String TAG = "DocumentGenerator";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

    public interface DocumentUploadCallback {
        void onSuccess(String downloadUrl);
        void onError(String error);
        void onProgress(int progress);
    }

    /**
     * Generate and upload Certificate of Origin to Firebase Storage
     */
    public static void generateAndUploadCertificateOfOrigin(Context context,
                                                            SupplierEntity supplier,
                                                            QualityBatchEntity batch,
                                                            String farmId,
                                                            DocumentUploadCallback callback) {
        String content = generateCertificateOfOrigin(supplier, batch);
        String fileName = "Certificate_of_Origin_" + batch.getBatchNumber() + "_" + FILE_DATE_FORMAT.format(new Date()) + ".txt";
        uploadDocument(context, content, fileName, farmId, "documents", callback);
    }

    /**
     * Generate and upload Packing List to Firebase Storage
     */
    public static void generateAndUploadPackingList(Context context,
                                                    QualityBatchEntity batch,
                                                    SupplierEntity supplier,
                                                    String farmId,
                                                    DocumentUploadCallback callback) {
        String content = generatePackingList(batch, supplier);
        String fileName = "Packing_List_" + batch.getBatchNumber() + "_" + FILE_DATE_FORMAT.format(new Date()) + ".txt";
        uploadDocument(context, content, fileName, farmId, "documents", callback);
    }

    /**
     * Generate and upload Commercial Invoice to Firebase Storage
     */
    public static void generateAndUploadCommercialInvoice(Context context,
                                                          QualityBatchEntity batch,
                                                          RevenueEntity revenue,
                                                          SupplierEntity supplier,
                                                          String farmId,
                                                          DocumentUploadCallback callback) {
        String content = generateCommercialInvoice(batch, revenue, supplier);
        String fileName = "Commercial_Invoice_" + batch.getBatchNumber() + "_" + FILE_DATE_FORMAT.format(new Date()) + ".txt";
        uploadDocument(context, content, fileName, farmId, "documents", callback);
    }

    /**
     * Generate and upload Full Document Package to Firebase Storage
     */
    public static void generateAndUploadDocumentPackage(Context context,
                                                        SupplierEntity supplier,
                                                        QualityBatchEntity batch,
                                                        RevenueEntity revenue,
                                                        String farmId,
                                                        DocumentUploadCallback callback) {
        String content = generateBatchDocumentPackage(supplier, batch, revenue);
        String fileName = "Document_Package_" + batch.getBatchNumber() + "_" + FILE_DATE_FORMAT.format(new Date()) + ".txt";
        uploadDocument(context, content, fileName, farmId, "documents", callback);
    }

    /**
     * Upload document to Firebase Storage
     */
    private static void uploadDocument(Context context,
                                       String content,
                                       String fileName,
                                       String farmId,
                                       String folder,
                                       DocumentUploadCallback callback) {
        try {
            // 1. Save content to local file
            File tempFile = new File(context.getCacheDir(), fileName);
            FileWriter writer = new FileWriter(tempFile);
            writer.write(content);
            writer.close();

            // 2. Upload to Firebase Storage
            // ✅ Fix: Use the correct path structure
            StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                    .child("farms")
                    .child(farmId)
                    .child("documents")  // ✅ Use "documents" folder
                    .child(fileName);

            UploadTask uploadTask = storageRef.putFile(Uri.fromFile(tempFile));

            // Track progress
            uploadTask.addOnProgressListener(taskSnapshot -> {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                if (callback != null) {
                    callback.onProgress((int) progress);
                }
            });

            // Handle success
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();
                    Log.d(TAG, "Document uploaded: " + downloadUrl);

                    // Clean up temp file
                    tempFile.delete();

                    if (callback != null) {
                        callback.onSuccess(downloadUrl);
                    }
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get download URL", e);
                    tempFile.delete();
                    if (callback != null) {
                        callback.onError("Failed to get download URL: " + e.getMessage());
                    }
                });
            });

            // Handle failure
            uploadTask.addOnFailureListener(e -> {
                Log.e(TAG, "Upload failed", e);
                tempFile.delete();
                if (callback != null) {
                    callback.onError("Upload failed: " + e.getMessage());
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error uploading document", e);
            if (callback != null) {
                callback.onError("Error: " + e.getMessage());
            }
        }
    }

    /**
     * Generate Certificate of Origin
     * Auto-generated from supplier and farm data
     */
    public static String generateCertificateOfOrigin(SupplierEntity supplier, QualityBatchEntity batch) {
        StringBuilder doc = new StringBuilder();

        doc.append("=== CERTIFICATE OF ORIGIN ===\n");
        doc.append("================================\n\n");

        doc.append("Issued By: AfriNuts Export\n");
        doc.append("Date: ").append(DATE_FORMAT.format(new Date())).append("\n\n");

        doc.append("--- Supplier Information ---\n");
        if (supplier != null) {
            doc.append("Supplier Name: ").append(supplier.getName()).append("\n");
            doc.append("Supplier Type: ").append(supplier.getType().getDisplayName()).append("\n");
            doc.append("Registration Number: ").append(supplier.getRegistrationNumber()).append("\n");
            if (supplier.getLocation() != null) {
                doc.append("Location: ").append(supplier.getLocation()).append("\n");
            }
            if (supplier.getCountry() != null) {
                doc.append("Country: ").append(supplier.getCountry()).append("\n");
            }
        }
        doc.append("\n");

        doc.append("--- Product Information ---\n");
        if (batch != null) {
            doc.append("Batch Number: ").append(batch.getBatchNumber()).append("\n");
            doc.append("Product: Cashew Nuts (Raw/Processed)\n");
            doc.append("Grade: ").append(batch.getQualityGrade().getDisplayName()).append("\n");
            doc.append("Weight: ").append(String.format(Locale.getDefault(), "%.0f kg", batch.getWeightKg())).append("\n");
            doc.append("Harvest Date: ").append(DATE_FORMAT.format(new Date(batch.getProcessingDate()))).append("\n");
        }
        doc.append("\n");

        doc.append("--- Origin Information ---\n");
        doc.append("Country of Origin: Côte d'Ivoire (Ivory Coast)\n");
        doc.append("Region: Odienné, Côte d'Ivoire\n");
        doc.append("Coordinates: 9.51° N, 7.57° W\n\n");

        doc.append("--- Certification ---\n");
        doc.append("This certifies that the above product is of Ivorian origin and was produced\n");
        doc.append("in compliance with local agricultural standards.\n\n");

        doc.append("--- Verification ---\n");
        doc.append("This document is digitally verified through Farm OS.\n");
        doc.append("Traceability URL: https://www.afrinutsexport.com/trace/").append(batch != null ? batch.getBatchNumber() : "").append("\n\n");

        doc.append("Authorized Signature: ____________________\n");
        doc.append("Issued by: Farm OS - AfriNuts Export\n");
        doc.append("Verification Code: ").append(generateVerificationCode()).append("\n");
        doc.append("================================\n");

        return doc.toString();
    }

    /**
     * Generate Packing List
     * Auto-generated from batch and inventory data
     */
    public static String generatePackingList(QualityBatchEntity batch, SupplierEntity supplier) {
        StringBuilder doc = new StringBuilder();

        doc.append("=== PACKING LIST ===\n");
        doc.append("======================\n\n");

        doc.append("Issued By: AfriNuts Export\n");
        doc.append("Date: ").append(DATE_FORMAT.format(new Date())).append("\n\n");

        doc.append("--- Shipment Details ---\n");
        doc.append("Batch Number: ").append(batch != null ? batch.getBatchNumber() : "N/A").append("\n");
        doc.append("Supplier: ").append(supplier != null ? supplier.getName() : "N/A").append("\n");
        if (supplier != null && supplier.getLocation() != null) {
            doc.append("Origin: ").append(supplier.getLocation()).append("\n");
        }
        doc.append("\n");

        doc.append("--- Package Information ---\n");
        double weightKg = batch != null ? batch.getWeightKg() : 0;
        int bags = (int) Math.ceil(weightKg / 50); // 50kg bags
        int boxes = (int) Math.ceil(weightKg / 25); // 25kg boxes

        doc.append("Total Weight: ").append(String.format(Locale.getDefault(), "%.0f kg", weightKg)).append("\n");
        doc.append("Number of Bags: ").append(bags).append(" (50kg each)\n");
        doc.append("Number of Boxes: ").append(boxes).append(" (25kg each)\n");
        doc.append("Package Type: Jute Bags / Carton Boxes\n");
        doc.append("Storage Conditions: Dry, ventilated area\n\n");

        doc.append("--- Product Details ---\n");
        doc.append("Product: Cashew Nuts\n");
        doc.append("Grade: ").append(batch != null ? batch.getQualityGrade().getDisplayName() : "N/A").append("\n");
        doc.append("Moisture Content: ").append(batch != null ? String.format(Locale.getDefault(), "%.1f%%", batch.getMoistureContent()) : "N/A").append("\n");
        doc.append("Defective Rate: ").append(batch != null ? String.format(Locale.getDefault(), "%.1f%%", batch.getDefectiveRate()) : "N/A").append("\n");
        doc.append("Kernel Ratio: ").append(batch != null ? String.format(Locale.getDefault(), "%.0f%%", batch.getKernelRatio()) : "N/A").append("\n\n");

        doc.append("--- Packaging Details ---\n");
        doc.append("Each bag/box is labeled with:\n");
        doc.append("  - Batch Number\n");
        doc.append("  - Grade\n");
        doc.append("  - Weight\n");
        doc.append("  - QR Code for traceability\n\n");

        doc.append("--- Inspection ---\n");
        doc.append("This packing list was generated from Farm OS inventory data.\n");
        doc.append("Verification Code: ").append(generateVerificationCode()).append("\n");
        doc.append("======================\n");

        return doc.toString();
    }

    /**
     * Generate Commercial Invoice
     * Auto-generated from batch, pricing, and buyer data
     */
    public static String generateCommercialInvoice(QualityBatchEntity batch,
                                                   RevenueEntity revenue,
                                                   SupplierEntity supplier) {
        StringBuilder doc = new StringBuilder();

        doc.append("=== COMMERCIAL INVOICE ===\n");
        doc.append("===========================\n\n");

        doc.append("AfriNuts Export\n");
        doc.append("Odienné, Côte d'Ivoire\n");
        doc.append("Phone: +225 00 00 00 00\n");
        doc.append("Email: info@afrinutsexport.com\n\n");

        // ✅ Generate unique invoice number
        String invoiceNumber = "INV-" + FILE_DATE_FORMAT.format(new Date())
                + "-" + String.format("%03d", System.currentTimeMillis() % 1000);
        doc.append("Invoice Number: ").append(invoiceNumber).append("\n");
        doc.append("Invoice Date: ").append(DATE_FORMAT.format(new Date())).append("\n\n");

        // Buyer information
        doc.append("--- Bill To ---\n");
        if (revenue != null && revenue.getBuyer() != null) {
            doc.append("Buyer: ").append(revenue.getBuyer()).append("\n");
        } else if (batch != null) {
            // Try to get buyer from traceability
            doc.append("Buyer: ").append("N/A (Please add buyer)\n");
        }
        if (supplier != null && supplier.getLocation() != null) {
            doc.append("Origin: ").append(supplier.getLocation()).append("\n");
        }
        doc.append("\n");

        // Product details with pricing
        doc.append("--- Product Details ---\n");
        doc.append("Product: Cashew Nuts\n");
        doc.append("Batch Number: ").append(batch != null ? batch.getBatchNumber() : "N/A").append("\n");
        doc.append("Grade: ").append(batch != null ? batch.getQualityGrade().getDisplayName() : "N/A").append("\n");
        doc.append("Quantity: ").append(batch != null ? String.format(Locale.getDefault(), "%.0f kg", batch.getWeightKg()) : "0 kg").append("\n");

        double pricePerKg = revenue != null ? revenue.getPricePerKg() : 0;
        double totalAmount = revenue != null ? revenue.getTotalAmount() : (pricePerKg * (batch != null ? batch.getWeightKg() : 0));

        doc.append("Unit Price: ").append(String.format(Locale.getDefault(), "%,.0f XAF/kg", pricePerKg)).append("\n");
        doc.append("Total Amount: ").append(String.format(Locale.getDefault(), "%,.0f XAF", totalAmount)).append("\n\n");

        // Payment terms
        doc.append("--- Payment Terms ---\n");
        doc.append("Payment Due: 30 days from invoice date\n");
        doc.append("Payment Method: Bank Transfer / Letter of Credit\n");
        doc.append("Currency: XAF (CFA Franc)\n\n");

        // Shipping details
        doc.append("--- Shipping Details ---\n");
        doc.append("Shipping Terms: FOB Abidjan Port\n");
        doc.append("Delivery Time: 14-21 days after order confirmation\n\n");

        // Verification
        doc.append("--- Verification ---\n");
        doc.append("This invoice is digitally generated from Farm OS.\n");
        doc.append("Traceability: https://www.afrinutsexport.com/trace/")
                .append(batch != null ? batch.getBatchNumber() : "").append("\n\n");

        doc.append("Authorized Signature: ____________________\n");
        doc.append("Company Stamp: AfriNuts Export\n");
        doc.append("Verification Code: ").append(generateVerificationCode()).append("\n");
        doc.append("===========================\n");

        return doc.toString();
    }

    /**
     * Generate Batch Document Package (all documents combined)
     */
    public static String generateBatchDocumentPackage(SupplierEntity supplier,
                                                      QualityBatchEntity batch,
                                                      RevenueEntity revenue) {
        StringBuilder packageDoc = new StringBuilder();

        packageDoc.append("============================================\n");
        packageDoc.append("   AFRI-NUTS EXPORT - BATCH DOCUMENT PACKAGE\n");
        packageDoc.append("============================================\n\n");

        packageDoc.append("Generated: ").append(DATE_FORMAT.format(new Date())).append("\n");
        packageDoc.append("Batch: ").append(batch != null ? batch.getBatchNumber() : "N/A").append("\n\n");
        packageDoc.append("This document package contains:\n");
        packageDoc.append("1. Certificate of Origin\n");
        packageDoc.append("2. Packing List\n");
        packageDoc.append("3. Commercial Invoice\n");
        packageDoc.append("4. Traceability Report\n\n");
        packageDoc.append("--- DOCUMENTS ---\n\n");

        // Certificate of Origin
        packageDoc.append("=== 1. CERTIFICATE OF ORIGIN ===\n");
        packageDoc.append(generateCertificateOfOrigin(supplier, batch));
        packageDoc.append("\n\n");

        // Packing List
        packageDoc.append("=== 2. PACKING LIST ===\n");
        packageDoc.append(generatePackingList(batch, supplier));
        packageDoc.append("\n\n");

        // Commercial Invoice
        packageDoc.append("=== 3. COMMERCIAL INVOICE ===\n");
        packageDoc.append(generateCommercialInvoice(batch, revenue, supplier));
        packageDoc.append("\n\n");

        // Traceability Summary
        packageDoc.append("=== 4. TRACEABILITY SUMMARY ===\n");
        packageDoc.append("Farm OS Verified\n");
        packageDoc.append("Traceability URL: https://www.afrinutsexport.com/trace/")
                .append(batch != null ? batch.getBatchNumber() : "").append("\n");
        packageDoc.append("Verification Code: ").append(generateVerificationCode()).append("\n");
        packageDoc.append("============================================\n");

        return packageDoc.toString();
    }

    /**
     * Generate verification code for documents
     */
    private static String generateVerificationCode() {
        return "FOS-" + FILE_DATE_FORMAT.format(new Date()) +
                "-" + String.format("%06d", System.currentTimeMillis() % 1000000);
    }

    /**
     * Save document to file
     */
    public static String saveDocumentToFile(Context context, String fileName, String content) {
        try {
            String fileNameWithExt = fileName + "_" + FILE_DATE_FORMAT.format(new Date()) + ".txt";
            java.io.File file = new java.io.File(context.getExternalFilesDir(null), fileNameWithExt);
            java.io.FileWriter writer = new java.io.FileWriter(file);
            writer.write(content);
            writer.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}