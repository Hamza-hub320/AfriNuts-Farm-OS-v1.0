package com.afrinuts.farmos.utils;

import android.net.Uri;

import java.util.regex.Pattern;

public class BatchNumberUtils {

    private static final Pattern BATCH_PATTERN = Pattern.compile("AFN-[A-Z]{4}-[A-Z]{3}-\\d{6}-\\d{3}");

    /**
     * Extract batch number from QR code scan result
     * Supports:
     * - Direct batch number: "AFN-COOP-ORG-260625-001"
     * - URL: "https://www.afrinutsexport.com/trace/AFN-COOP-ORG-260625-001"
     * - URL with query param: "https://www.afrinutsexport.com/trace?batchId=AFN-COOP-ORG-260625-001"
     */
    public static String extractBatchNumber(String scanResult) {
        if (scanResult == null || scanResult.isEmpty()) {
            return null;
        }

        // Check if it's a URL
        if (scanResult.startsWith("http://") || scanResult.startsWith("https://")) {
            try {
                Uri uri = Uri.parse(scanResult);
                String path = uri.getPath();
                String query = uri.getQueryParameter("batchId");

                if (query != null && !query.isEmpty()) {
                    return query;
                }

                if (path != null && path.contains("/trace/")) {
                    return path.substring(path.lastIndexOf("/") + 1);
                }

                // Try to find batch number in the URL
                for (String part : scanResult.split("/")) {
                    if (isValidBatchNumber(part)) {
                        return part;
                    }
                }
            } catch (Exception e) {
                // Fall through to direct check
            }
        }

        // Check if the result is a valid batch number
        if (isValidBatchNumber(scanResult)) {
            return scanResult;
        }

        // Try to find batch number in the text
        for (String part : scanResult.split("\\s+")) {
            if (isValidBatchNumber(part)) {
                return part;
            }
        }

        return null;
    }

    /**
     * Check if a string is a valid AfriNuts batch number
     * Format: AFN-SOURCE-TYPE-YYMMDD-XXX
     */
    public static boolean isValidBatchNumber(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return BATCH_PATTERN.matcher(text).matches();
    }

    /**
     * Generate QR code URL for a batch number
     */
    public static String getTraceabilityUrl(String batchNumber) {
        return "https://www.afrinutsexport.com/trace?batchId=" + batchNumber;
    }
}