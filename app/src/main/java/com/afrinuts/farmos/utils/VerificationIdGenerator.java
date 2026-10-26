package com.afrinuts.farmos.utils;

import com.afrinuts.farmos.data.local.entity.VerificationEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class VerificationIdGenerator {

    private static final AtomicInteger counter = new AtomicInteger(1);

    public static String generateVerificationId(VerificationEntity.VerificationType verificationType,
                                                String certificationId) {
        String prefix = getPrefix(verificationType);
        String year = new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date());
        String sequence = String.format("%03d", counter.getAndIncrement() % 1000);

        return prefix + "-" + year + "-" + sequence;
    }

    private static String getPrefix(VerificationEntity.VerificationType verificationType) {
        if (verificationType == null) return "VER";

        switch (verificationType) {
            case DOCUMENTATION:
                return "DOC";
            case FIELD:
                return "FLD";
            case LABORATORY:
                return "LAB";
            case SURVEILLANCE:
                return "SURV";
            default:
                return "VER";
        }
    }

    public static void resetCounter() {
        counter.set(1);
    }
}