package com.afrinuts.farmos.utils;

import android.content.Context;
import android.content.SharedPreferences;
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

public class DocumentGenerator {

    private static final String TAG = "DocumentGenerator";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
    private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd", Locale.US);

    public interface DocumentUploadCallback {
        void onSuccess(String downloadUrl);
        void onError(String error);
        void onProgress(int progress);
    }

    // ========================================
    // UPLOAD METHODS
    // ========================================

    public static void generateAndUploadCertificateOfOrigin(Context context,
                                                            SupplierEntity supplier,
                                                            QualityBatchEntity batch,
                                                            BatchTraceabilityEntity traceability,
                                                            String farmId,
                                                            DocumentUploadCallback callback) {
        String content = generateCertificateOfOrigin(supplier, batch, traceability);
        String fileName = "Certificate_of_Origin_" + batch.getBatchNumber() + "_" + FILE_DATE_FORMAT.format(new Date()) + ".txt";
        uploadDocument(context, content, fileName, farmId, "documents", callback);
    }

    public static void generateAndUploadPackingList(Context context,
                                                    QualityBatchEntity batch,
                                                    SupplierEntity supplier,
                                                    BatchTraceabilityEntity traceability,
                                                    String farmId,
                                                    DocumentUploadCallback callback) {
        String content = generatePackingList(batch, supplier, traceability);
        String fileName = "Packing_List_" + batch.getBatchNumber() + "_" + FILE_DATE_FORMAT.format(new Date()) + ".txt";
        uploadDocument(context, content, fileName, farmId, "documents", callback);
    }

    public static void generateAndUploadCommercialInvoice(Context context,
                                                          QualityBatchEntity batch,
                                                          RevenueEntity revenue,
                                                          SupplierEntity supplier,
                                                          BatchTraceabilityEntity traceability,
                                                          String farmId,
                                                          DocumentUploadCallback callback) {
        String invoiceNumber = nextInvoiceNumber(context, farmId);
        String content = generateCommercialInvoice(batch, revenue, supplier, traceability, invoiceNumber);
        String fileName = "Commercial_Invoice_" + invoiceNumber + "_" + batch.getBatchNumber() + ".txt";
        uploadDocument(context, content, fileName, farmId, "documents", callback);
    }

    public static void generateAndUploadDocumentPackage(Context context,
                                                        SupplierEntity supplier,
                                                        QualityBatchEntity batch,
                                                        RevenueEntity revenue,
                                                        BatchTraceabilityEntity traceability,
                                                        String farmId,
                                                        DocumentUploadCallback callback) {
        String invoiceNumber = nextInvoiceNumber(context, farmId);
        String content = generateBatchDocumentPackage(supplier, batch, revenue, traceability, invoiceNumber);
        String fileName = "Document_Package_" + batch.getBatchNumber() + "_" + FILE_DATE_FORMAT.format(new Date()) + ".txt";
        uploadDocument(context, content, fileName, farmId, "documents", callback);
    }

    private static void uploadDocument(Context context,
                                       String content,
                                       String fileName,
                                       String farmId,
                                       String folder,
                                       DocumentUploadCallback callback) {
        try {
            File tempFile = new File(context.getCacheDir(), fileName);
            FileWriter writer = new FileWriter(tempFile);
            writer.write(content);
            writer.close();

            StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                    .child("farms")
                    .child(farmId)
                    .child("documents")
                    .child(fileName);

            UploadTask uploadTask = storageRef.putFile(Uri.fromFile(tempFile));

            uploadTask.addOnProgressListener(taskSnapshot -> {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                if (callback != null) {
                    callback.onProgress((int) progress);
                }
            });

            uploadTask.addOnSuccessListener(taskSnapshot -> {
                storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();
                    Log.d(TAG, "✅ Document uploaded: " + downloadUrl);
                    tempFile.delete();
                    if (callback != null) {
                        callback.onSuccess(downloadUrl);
                    }
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to get download URL", e);
                    tempFile.delete();
                    if (callback != null) {
                        callback.onError("Failed to get download URL: " + e.getMessage());
                    }
                });
            });

            uploadTask.addOnFailureListener(e -> {
                Log.e(TAG, "❌ Upload failed", e);
                tempFile.delete();
                if (callback != null) {
                    callback.onError("Upload failed: " + e.getMessage());
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "❌ Error uploading document", e);
            if (callback != null) {
                callback.onError("Error: " + e.getMessage());
            }
        }
    }

    // ========================================
    // CERTIFICATE OF ORIGIN
    // ========================================

    public static String generateCertificateOfOrigin(SupplierEntity supplier,
                                                     QualityBatchEntity batch,
                                                     BatchTraceabilityEntity traceability) {
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
            appendProductDetails(doc, batch);
            doc.append("Grade: ").append(batch.getQualityGrade().getDisplayName()).append("\n");

            // ✅ Show both raw and kernel weights
            if (traceability != null) {
                if (traceability.getRawWeightKg() > 0) {
                    doc.append("Raw Input Weight: ").append(String.format(Locale.US, "%.0f kg", traceability.getRawWeightKg())).append("\n");
                }
                if (traceability.getKernelWeightKg() > 0) {
                    doc.append("Kernel Output Weight: ").append(String.format(Locale.US, "%.0f kg", traceability.getKernelWeightKg())).append("\n");
                }
                if (traceability.getYieldPercentage() > 0) {
                    doc.append("Yield Percentage: ").append(String.format(Locale.US, "%.1f%%", traceability.getYieldPercentage())).append("\n");
                }
            } else {
                doc.append("Weight: ").append(String.format(Locale.US, "%.0f kg", batch.getWeightKg())).append("\n");
            }
            doc.append("Harvest Date: ").append(DATE_FORMAT.format(new Date(batch.getProcessingDate()))).append("\n");
        }
        doc.append("\n");

        doc.append("--- Origin Information ---\n");
        doc.append("Country of Origin: Cote d'Ivoire (Ivory Coast)\n");
        doc.append("Region: Odienne, Cote d'Ivoire\n");
        doc.append("Coordinates: 9.7886° N, -6.8342° W\n\n");

        doc.append("--- Processing Information ---\n");
        if (traceability != null) {
            if (traceability.getProcessingMethod() != null) {
                doc.append("Processing Method: ").append(traceability.getProcessingMethod()).append("\n");
            }
            if (traceability.getProcessingFacility() != null) {
                doc.append("Processing Facility: ").append(traceability.getProcessingFacility()).append("\n");
            }
            if (traceability.getProcessingLocation() != null) {
                doc.append("Processing Location: ").append(traceability.getProcessingLocation()).append("\n");
            }
            if (traceability.getShellingMethod() != null) {
                doc.append("Shelling Method: ").append(traceability.getShellingMethod()).append("\n");
            }
            if (traceability.getGradingMethod() != null) {
                doc.append("Grading Method: ").append(traceability.getGradingMethod()).append("\n");
            }
        }
        doc.append("\n");

        doc.append("--- Certification ---\n");
        doc.append("This certifies that the above product is of Ivorian origin and was produced\n");
        doc.append("in compliance with local agricultural standards.\n\n");

        doc.append("--- Verification ---\n");
        doc.append("This document is digitally verified through Farm OS.\n");
        doc.append("Traceability URL: ").append(traceabilityUrl(batch)).append("\n\n");

        doc.append("Authorized Signature: ____________________\n");
        doc.append("Issued by: Farm OS - AfriNuts Export\n");
        doc.append("Verification Code: ").append(verificationCode(batch, traceability)).append("\n");
        doc.append("================================\n");

        return doc.toString();
    }

    // ========================================
    // PACKING LIST
    // ========================================

    public static String generatePackingList(QualityBatchEntity batch,
                                             SupplierEntity supplier,
                                             BatchTraceabilityEntity traceability) {
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
        // ✅ Use kernel weight if available
        double weightKg = traceability != null && traceability.getKernelWeightKg() > 0 ?
                traceability.getKernelWeightKg() :
                (batch != null ? batch.getWeightKg() : 0);

        int bags = (int) Math.ceil(weightKg / 50);
        int boxes = (int) Math.ceil(weightKg / 25);

        doc.append("Total Weight: ").append(String.format(Locale.US, "%.0f kg", weightKg)).append("\n");
        if (traceability != null && traceability.getRawWeightKg() > 0) {
            doc.append("Raw Input Weight: ").append(String.format(Locale.US, "%.0f kg", traceability.getRawWeightKg())).append("\n");
        }
        doc.append("Number of Bags: ").append(bags).append(" (50kg each)\n");
        doc.append("Number of Boxes: ").append(boxes).append(" (25kg each)\n");
        doc.append("Package Type: Jute Bags / Carton Boxes\n");
        doc.append("Storage Conditions: Dry, ventilated area\n\n");

        doc.append("--- Product Details ---\n");
        appendProductDetails(doc, batch);
        doc.append("Grade: ").append(batch != null ? batch.getQualityGrade().getDisplayName() : "N/A").append("\n");
        doc.append("Moisture Content: ").append(batch != null ? String.format(Locale.US, "%.1f%%", batch.getMoistureContent()) : "N/A").append("\n");
        doc.append("Defective Rate: ").append(batch != null ? String.format(Locale.US, "%.1f%%", batch.getDefectiveRate()) : "N/A").append("\n");
        if (batch != null && batch.isRcn()) {
            doc.append("KOR (Kernel Outturn Ratio): ")
                    .append(String.format(Locale.US, "%.0f g/kg", batch.getKorGPerKg())).append("\n\n");
        } else {
            doc.append("Kernel Ratio: ")
                    .append(batch != null ? String.format(Locale.US, "%.0f%%", batch.getKernelRatio()) : "N/A")
                    .append("\n\n");
        }

        doc.append("--- Packaging Details ---\n");
        doc.append("Each bag/box is labeled with:\n");
        doc.append("  - Batch Number\n");
        doc.append("  - Grade\n");
        doc.append("  - Weight\n");
        doc.append("  - QR Code for traceability\n\n");

        doc.append("--- Inspection ---\n");
        doc.append("This packing list was generated from Farm OS inventory data.\n");
        doc.append("Verification Code: ").append(verificationCode(batch, traceability)).append("\n");
        doc.append("======================\n");

        return doc.toString();
    }

    // ========================================
    // COMMERCIAL INVOICE
    // ========================================

    public static String generateCommercialInvoice(QualityBatchEntity batch,
                                                   RevenueEntity revenue,
                                                   SupplierEntity supplier,
                                                   BatchTraceabilityEntity traceability,
                                                   String invoiceNumber) {
        StringBuilder doc = new StringBuilder();

        doc.append("=== COMMERCIAL INVOICE ===\n");
        doc.append("===========================\n\n");

        doc.append("AfriNuts Export\n");
        doc.append("Registered address: Abidjan, Abobo PK18 ILOT 1176 114\n");
        doc.append("Farm location: Odienné, Côte d'Ivoire\n");
        doc.append("RCCM: CI-ABJ-03-2026-B12-04223\n");
        doc.append("IDU: CI-2026-0066959 J\n");
        doc.append("Phone: +225 07 02 27 27 70\n");
        doc.append("Email: info@afrinutsexport.com\n\n");

        doc.append("Invoice Number: ").append(invoiceNumber).append("\n");
        doc.append("Invoice Date: ").append(DATE_FORMAT.format(new Date())).append("\n\n");

        // Buyer information
        doc.append("--- Bill To ---\n");
        if (revenue != null && revenue.getBuyer() != null) {
            doc.append("Buyer: ").append(revenue.getBuyer()).append("\n");
        }
        if (supplier != null && supplier.getLocation() != null) {
            doc.append("Origin: ").append(supplier.getLocation()).append("\n");
        }
        doc.append("\n");

        // Product details with pricing
        doc.append("--- Product Details ---\n");
        appendProductDetails(doc, batch);
        doc.append("Batch Number: ").append(batch != null ? batch.getBatchNumber() : "N/A").append("\n");
        doc.append("Grade: ").append(batch != null ? batch.getQualityGrade().getDisplayName() : "N/A").append("\n");

        // ✅ Use kernel weight from traceability if available
        double weightKg = traceability != null && traceability.getKernelWeightKg() > 0 ?
                traceability.getKernelWeightKg() :
                (batch != null ? batch.getWeightKg() : 0);
        doc.append("Quantity: ").append(String.format(Locale.US, "%.0f kg", weightKg)).append("\n");

        double pricePerKg = revenue != null ? revenue.getPricePerKg() : 0;
        double totalAmount = revenue != null ? revenue.getTotalAmount() : (pricePerKg * weightKg);

        doc.append("Unit Price: ").append(String.format(Locale.US, "%,.0f XAF/kg", pricePerKg)).append("\n");
        doc.append("Total Amount: ").append(String.format(Locale.US, "%,.0f XAF", totalAmount)).append("\n\n");

        // ✅ Add processing info if available
        if (traceability != null) {
            doc.append("--- Processing Information ---\n");
            if (traceability.getProcessingMethod() != null) {
                doc.append("Method: ").append(traceability.getProcessingMethod()).append("\n");
            }
            if (traceability.getProcessingFacility() != null) {
                doc.append("Facility: ").append(traceability.getProcessingFacility()).append("\n");
            }
            if (traceability.getYieldPercentage() > 0) {
                doc.append("Yield: ").append(String.format(Locale.US, "%.1f%%", traceability.getYieldPercentage())).append("\n");
            }
            doc.append("\n");
        }

        doc.append("--- Payment Terms ---\n");
        doc.append("Payment Due: 30 days from invoice date\n");
        doc.append("Payment Method: Bank Transfer / Letter of Credit\n");
        doc.append("Currency: XAF (CFA Franc)\n\n");

        doc.append("--- Shipping Details ---\n");
        doc.append("Shipping Terms: FOB Abidjan Port\n");
        doc.append("Delivery Time: 14-21 days after order confirmation\n\n");

        doc.append("--- Verification ---\n");
        doc.append("This invoice is digitally generated from Farm OS.\n");
        doc.append("Traceability: ").append(traceabilityUrl(batch)).append("\n\n");

        doc.append("Authorized Signature: ____________________\n");
        doc.append("Company Stamp: AfriNuts Export\n");
        doc.append("Verification Code: ").append(verificationCode(batch, traceability)).append("\n");
        doc.append("===========================\n");

        return doc.toString();
    }

    // ========================================
    // DOCUMENT PACKAGE
    // ========================================

    public static String generateBatchDocumentPackage(SupplierEntity supplier,
                                                      QualityBatchEntity batch,
                                                      RevenueEntity revenue,
                                                      BatchTraceabilityEntity traceability,
                                                      String invoiceNumber) {
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
        packageDoc.append("4. Processing Report\n\n");
        packageDoc.append("--- DOCUMENTS ---\n\n");

        // Certificate of Origin
        packageDoc.append("=== 1. CERTIFICATE OF ORIGIN ===\n");
        packageDoc.append(generateCertificateOfOrigin(supplier, batch, traceability));
        packageDoc.append("\n\n");

        // Packing List
        packageDoc.append("=== 2. PACKING LIST ===\n");
        packageDoc.append(generatePackingList(batch, supplier, traceability));
        packageDoc.append("\n\n");

        // Commercial Invoice
        packageDoc.append("=== 3. COMMERCIAL INVOICE ===\n");
        packageDoc.append(generateCommercialInvoice(batch, revenue, supplier, traceability, invoiceNumber));
        packageDoc.append("\n\n");

        // Processing Report
        packageDoc.append("=== 4. PROCESSING REPORT ===\n");
        packageDoc.append("Raw Input Weight: ").append(traceability != null ? String.format(Locale.US, "%.0f kg", traceability.getRawWeightKg()) : "N/A").append("\n");
        packageDoc.append("Kernel Output Weight: ").append(traceability != null ? String.format(Locale.US, "%.0f kg", traceability.getKernelWeightKg()) : "N/A").append("\n");
        packageDoc.append("Yield Percentage: ").append(traceability != null ? String.format(Locale.US, "%.1f%%", traceability.getYieldPercentage()) : "N/A").append("\n");
        if (traceability != null && traceability.getProcessingMethod() != null) {
            packageDoc.append("Processing Method: ").append(traceability.getProcessingMethod()).append("\n");
        }
        if (traceability != null && traceability.getProcessingFacility() != null) {
            packageDoc.append("Processing Facility: ").append(traceability.getProcessingFacility()).append("\n");
        }
        packageDoc.append("\n");

        packageDoc.append("--- Verification ---\n");
        packageDoc.append("Farm OS Verified\n");
        packageDoc.append("Traceability URL: ").append(traceabilityUrl(batch)).append("\n");
        packageDoc.append("Verification Code: ").append(verificationCode(batch, traceability)).append("\n");
        packageDoc.append("============================================\n");

        return packageDoc.toString();
    }

    // ========================================
    // HELPERS
    // ========================================

    private static String verificationCode(QualityBatchEntity batch, BatchTraceabilityEntity traceability) {
        if (traceability != null && traceability.getVerificationCode() != null
                && !traceability.getVerificationCode().trim().isEmpty()) {
            return traceability.getVerificationCode().trim();
        }
        if (batch != null && batch.getVerificationCode() != null && !batch.getVerificationCode().trim().isEmpty()) {
            return batch.getVerificationCode().trim();
        }
        String batchNumber = batch != null ? batch.getBatchNumber() : null;
        return batchNumber != null && !batchNumber.trim().isEmpty() ? "FOS-" + batchNumber.trim() : "FOS-UNASSIGNED";
    }

    private static String traceabilityUrl(QualityBatchEntity batch) {
        String batchNumber = batch != null && batch.getBatchNumber() != null ? batch.getBatchNumber() : "";
        try {
            return "https://www.afrinutsexport.com/trace?batchId="
                    + java.net.URLEncoder.encode(batchNumber, "UTF-8");
        } catch (java.io.UnsupportedEncodingException impossible) {
            return "https://www.afrinutsexport.com/trace?batchId=" + batchNumber;
        }
    }

    private static synchronized String nextInvoiceNumber(Context context, String farmId) {
        String year = new SimpleDateFormat("yyyy", Locale.US).format(new Date());
        String key = "invoice_sequence_" + year + "_" + (farmId == null ? "default" : farmId);
        SharedPreferences preferences = context.getApplicationContext()
                .getSharedPreferences("trade_document_sequences", Context.MODE_PRIVATE);
        int sequence = preferences.getInt(key, 0) + 1;
        if (!preferences.edit().putInt(key, sequence).commit()) {
            throw new IllegalStateException("Could not persist the invoice sequence");
        }
        return "INV-" + year + "-" + String.format(Locale.US, "%04d", sequence);
    }

    private static void appendProductDetails(StringBuilder doc, QualityBatchEntity batch) {
        if (batch != null && batch.isRcn()) {
            doc.append("Product: Raw Cashew Nuts (RCN), in shell\n");
            doc.append("HS Code: 0801.31\n");
        } else {
            doc.append("Product: Cashew Kernels, processed\n");
            doc.append("HS Code: 0801.32\n");
        }
    }

    public static String saveDocumentToFile(Context context, String fileName, String content) {
        try {
            String fileNameWithExt = fileName + "_" + FILE_DATE_FORMAT.format(new Date()) + ".txt";
            File file = new File(context.getExternalFilesDir(null), fileNameWithExt);
            FileWriter writer = new FileWriter(file);
            writer.write(content);
            writer.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}