package com.afrinuts.farmos.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AuditIdGenerator {

    /** Format: AUD-<PROG>-<yyyy>-<seq>. Type/category dropped from the ID (ruling 2). */
    public static String buildAuditId(String programmeCode, int sequence) {
        String prog = (programmeCode == null || programmeCode.isEmpty()) ? "CERT" : programmeCode;
        String year = new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date());
        return "AUD-" + prog + "-" + year + "-" + String.format("%03d", sequence);
    }

    /** LIKE anchor: AUD-<PROG>-<yyyy>- */
    public static String sequencePrefix(String programmeCode) {
        String prog = (programmeCode == null || programmeCode.isEmpty()) ? "CERT" : programmeCode;
        String year = new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date());
        return "AUD-" + prog + "-" + year + "-";
    }
}