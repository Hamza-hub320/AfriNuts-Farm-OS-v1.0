package com.afrinuts.farmos.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class InspectionIdGenerator {

    /** Format: INS-<PROG>-<yyyy>-<seq>. Type/category dropped from the ID (ruling 2). */
    public static String buildInspectionId(String programmeCode, int sequence) {
        String prog = (programmeCode == null || programmeCode.isEmpty()) ? "CERT" : programmeCode;
        String year = new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date());
        return "INS-" + prog + "-" + year + "-" + String.format("%03d", sequence);
    }

    /** LIKE anchor: INS-<PROG>-<yyyy>- */
    public static String sequencePrefix(String programmeCode) {
        String prog = (programmeCode == null || programmeCode.isEmpty()) ? "CERT" : programmeCode;
        String year = new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date());
        return "INS-" + prog + "-" + year + "-";
    }
}