package com.afrinuts.farmos.utils;

import com.afrinuts.farmos.data.local.entity.DocumentEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class DocumentIdGenerator {

    private static final AtomicInteger counter = new AtomicInteger(1);

    public static String generateDocumentId(DocumentEntity.DocumentType documentType,
                                            String certificationId) {
        String prefix = getPrefix(documentType);
        String year = new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date());
        String sequence = String.format("%03d", counter.getAndIncrement() % 1000);

        return prefix + "-" + year + "-" + sequence;
    }

    private static String getPrefix(DocumentEntity.DocumentType documentType) {
        if (documentType == null) return "DOC";

        switch (documentType) {
            case CERTIFICATE:
                return "CERT";
            case AUDIT_REPORT:
                return "AUD";
            case INPUT_CERTIFICATE:
                return "INP";
            case INSPECTION_REPORT:
                return "INS";
            case VERIFICATION:
                return "VER";
            case APPLICATION:
                return "APP";
            default:
                return "DOC";
        }
    }

    public static void resetCounter() {
        counter.set(1);
    }
}