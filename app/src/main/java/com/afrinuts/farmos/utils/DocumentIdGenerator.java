package com.afrinuts.farmos.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DocumentIdGenerator {

    /** Format: DOC-<PROG>-<yyyy>-<seq>. DocumentType dropped from the ID (ruling 2). */
    public static String buildDocumentId(String programmeCode, int sequence) {
        String prog = (programmeCode == null || programmeCode.isEmpty()) ? "CERT" : programmeCode;
        String year = new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date());
        return "DOC-" + prog + "-" + year + "-" + String.format("%03d", sequence);
    }

    /** LIKE anchor: DOC-<PROG>-<yyyy>- */
    public static String sequencePrefix(String programmeCode) {
        String prog = (programmeCode == null || programmeCode.isEmpty()) ? "CERT" : programmeCode;
        String year = new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date());
        return "DOC-" + prog + "-" + year + "-";
    }
}