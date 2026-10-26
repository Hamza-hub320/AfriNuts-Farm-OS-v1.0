package com.afrinuts.farmos.utils;

import com.afrinuts.farmos.data.local.entity.AuditEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class AuditIdGenerator {

    private static final AtomicInteger counter = new AtomicInteger(1);

    public static String generateAuditId(AuditEntity.AuditType auditType, String certificationId) {
        // Extract prefix from audit type
        String prefix = getPrefix(auditType);
        String year = new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date());
        String sequence = String.format("%03d", counter.getAndIncrement() % 1000);

        return prefix + "-" + year + "-" + sequence;
    }

    private static String getPrefix(AuditEntity.AuditType auditType) {
        if (auditType == null) return "AUDIT";

        switch (auditType) {
            case INTERNAL:
                return "INT";
            case EXTERNAL:
                return "EXT";
            case SURVEILLANCE:
                return "SURV";
            case RENEWAL:
                return "REN";
            default:
                return "AUDIT";
        }
    }

    public static void resetCounter() {
        counter.set(1);
    }
}