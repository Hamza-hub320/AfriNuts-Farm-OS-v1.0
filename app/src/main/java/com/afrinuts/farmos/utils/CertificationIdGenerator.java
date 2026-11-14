package com.afrinuts.farmos.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CertificationIdGenerator {

    /**
     * Format: CERT-<PROG>-<yyyy>-<seq>.
     * Programme code derived from the certification NAME (ruling 3, P1).
     * No counter — the sequence is derived from MAX at insert.
     */
    public static String buildCertificationId(String certificationName, int sequence) {
        String prog = CertificationIdUtils.programmeCodeFromName(certificationName);
        String year = new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date());
        return "CERT-" + prog + "-" + year + "-" + String.format("%03d", sequence);
    }

    /** LIKE anchor for MAX-derive: CERT-<PROG>-<yyyy>- */
    public static String sequencePrefix(String certificationName) {
        String prog = CertificationIdUtils.programmeCodeFromName(certificationName);
        String year = new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date());
        return "CERT-" + prog + "-" + year + "-";
    }
}