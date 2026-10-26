package com.afrinuts.farmos.utils;

import com.afrinuts.farmos.data.local.entity.InspectionEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class InspectionIdGenerator {

    private static final AtomicInteger counter = new AtomicInteger(1);

    public static String generateInspectionId(InspectionEntity.InspectionType inspectionType,
                                              InspectionEntity.RequirementCategory category) {
        String prefix = getPrefix(inspectionType, category);
        String year = new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date());
        String sequence = String.format("%03d", counter.getAndIncrement() % 1000);

        return prefix + "-" + year + "-" + sequence;
    }

    private static String getPrefix(InspectionEntity.InspectionType inspectionType,
                                    InspectionEntity.RequirementCategory category) {
        String typeCode = "INSP";

        if (inspectionType != null) {
            switch (inspectionType) {
                case EXTERNAL:
                    typeCode = "EXT";
                    break;
                case INTERNAL:
                    typeCode = "INT";
                    break;
                case SURVEILLANCE:
                    typeCode = "SURV";
                    break;
                case RENEWAL:
                    typeCode = "REN";
                    break;
            }
        }

        if (category != null) {
            switch (category) {
                case FIELD:
                    typeCode += "-FLD";
                    break;
                case PROCESSING:
                    typeCode += "-PROC";
                    break;
                case STORAGE:
                    typeCode += "-STOR";
                    break;
                case DOCUMENTATION:
                    typeCode += "-DOC";
                    break;
                case LABORATORY:
                    typeCode += "-LAB";
                    break;
            }
        }

        return typeCode;
    }

    public static void resetCounter() {
        counter.set(1);
    }
}